package com.fenxiao.experiment.service;

import com.fenxiao.experiment.dto.*;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class ControlledExperimentService {
    private static final Set<String> EXPERIMENT_STATUSES = Set.of("DRAFT", "ENROLLING", "RUNNING", "COMPLETED", "CANCELLED");
    private static final Set<String> PARTICIPANT_STATUSES = Set.of("QUEUED", "ACTIVE", "COMPLETED", "WITHDRAWN");
    private final JdbcTemplate jdbc;
    private final UserDistributionProfileRepository users;
    private final Clock clock;

    public ControlledExperimentService(JdbcTemplate jdbc, UserDistributionProfileRepository users, Clock clock) {
        this.jdbc = jdbc;
        this.users = users;
        this.clock = clock;
    }

    public long create(CreateExperimentRequest request, long actorId) {
        if (request.plannedSampleSize() < 1 || request.plannedSampleSize() > 100) throw new IllegalArgumentException("planned sample size must be between 1 and 100");
        if (!request.enrollmentEndsAt().isAfter(request.enrollmentStartsAt())) throw new IllegalArgumentException("enrollment window is invalid");
        if (request.observationEndsAt().isBefore(request.enrollmentEndsAt())) throw new IllegalArgumentException("observation window is invalid");
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("insert into controlled_experiment(experiment_code,experiment_name,experiment_status,planned_sample_size,primary_metric_code,enrollment_starts_at,enrollment_ends_at,observation_ends_at,created_by) values(?,?,'DRAFT',?,?,?,?,?,?)", new String[]{"id"});
            statement.setString(1, upper(request.experimentCode())); statement.setString(2, request.experimentName().trim());
            statement.setInt(3, request.plannedSampleSize()); statement.setString(4, upper(request.primaryMetricCode()));
            statement.setObject(5, request.enrollmentStartsAt()); statement.setObject(6, request.enrollmentEndsAt());
            statement.setObject(7, request.observationEndsAt()); statement.setLong(8, actorId); return statement;
        }, keys);
        long id = Objects.requireNonNull(keys.getKey()).longValue();
        transition(id, null, null, "DRAFT", actorId, "experiment created");
        return id;
    }

    public void changeStatus(String code, ExperimentStatusRequest request, long actorId) {
        var experiment = experiment(code);
        String target = upper(request.status());
        if (!EXPERIMENT_STATUSES.contains(target)) throw new IllegalArgumentException("experiment status is invalid");
        String current = string(experiment, "experiment_status");
        if (!allowedExperimentTransition(current, target)) throw new IllegalStateException("experiment status transition is invalid");
        jdbc.update("update controlled_experiment set experiment_status=?,updated_at=? where id=?", target, now(), id(experiment));
        transition(id(experiment), null, current, target, actorId, request.reason());
    }

    public Map<String, Object> enroll(String code, EnrollParticipantRequest request, long actorId) {
        var experiment = experimentForUpdate(code);
        long experimentId = id(experiment);
        if (!"ENROLLING".equals(string(experiment, "experiment_status"))) throw new IllegalStateException("experiment is not enrolling");
        LocalDateTime now = now();
        if (now.isBefore(time(experiment, "enrollment_starts_at")) || now.isAfter(time(experiment, "enrollment_ends_at"))) throw new IllegalStateException("enrollment window is closed");
        if (!users.existsById(request.userId())) throw new IllegalArgumentException("user not found");
        Integer enrolled = jdbc.queryForObject("select count(*) from experiment_participant where experiment_id=?", Integer.class, experimentId);
        int cap = Math.min(number(experiment, "planned_sample_size"), 100);
        if (enrolled >= cap) throw new IllegalStateException("experiment sample is full");
        int enrollmentNo = enrolled + 1;
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("insert into experiment_participant(experiment_id,enrollment_no,user_id,participant_status,cohort_code,eligibility_snapshot,enrolled_at) values(?,?,?,'QUEUED',?,?,?)", new String[]{"id"});
            statement.setLong(1, experimentId); statement.setInt(2, enrollmentNo); statement.setLong(3, request.userId());
            statement.setString(4, upper(request.cohortCode())); statement.setString(5, request.eligibilitySnapshot().trim()); statement.setObject(6, now); return statement;
        }, keys);
        long participantId = Objects.requireNonNull(keys.getKey()).longValue();
        transition(experimentId, participantId, null, "QUEUED", actorId, "participant enrolled");
        return Map.of("participantId", participantId, "enrollmentNo", enrollmentNo, "denominator", enrollmentNo, "capacity", cap);
    }

    public void changeParticipantStatus(String code, long userId, ExperimentStatusRequest request, long actorId) {
        var experiment = experiment(code); long experimentId = id(experiment);
        var participant = participant(experimentId, userId);
        String current = string(participant, "participant_status"); String target = upper(request.status());
        if (!PARTICIPANT_STATUSES.contains(target) || !allowedParticipantTransition(current, target)) throw new IllegalStateException("participant status transition is invalid");
        LocalDateTime now = now();
        jdbc.update("update experiment_participant set participant_status=?,activated_at=case when ?='ACTIVE' and activated_at is null then ? else activated_at end,completed_at=case when ?='COMPLETED' then ? else completed_at end,updated_at=? where id=?",
                target, target, now, target, now, now, id(participant));
        transition(experimentId, id(participant), current, target, actorId, request.reason());
    }

    public boolean recordMetric(String code, ExperimentMetricEventRequest request) {
        var experiment = experiment(code); long experimentId = id(experiment);
        var participant = participant(experimentId, request.userId());
        LocalDateTime occurredAt = request.occurredAt();
        if (occurredAt.isBefore(time(experiment, "enrollment_starts_at")) || occurredAt.isAfter(time(experiment, "observation_ends_at"))) throw new IllegalArgumentException("metric is outside experiment observation window");
        int duplicate = jdbc.queryForObject("select count(*) from experiment_metric_event where source_system=? and source_event_id=?", Integer.class, request.sourceSystem().trim(), request.sourceEventId().trim());
        if (duplicate > 0) return false;
        jdbc.update("insert into experiment_metric_event(experiment_id,participant_id,metric_code,metric_value,source_system,source_event_id,occurred_at,received_at) values(?,?,?,?,?,?,?,?)",
                experimentId, id(participant), upper(request.metricCode()), request.metricValue(), request.sourceSystem().trim(), request.sourceEventId().trim(), occurredAt, now());
        return true;
    }

    public Map<String, Object> dashboard(String code) {
        var experiment = experiment(code); long experimentId = id(experiment);
        int denominator = jdbc.queryForObject("select count(*) from experiment_participant where experiment_id=?", Integer.class, experimentId);
        int active = jdbc.queryForObject("select count(*) from experiment_participant where experiment_id=? and participant_status='ACTIVE'", Integer.class, experimentId);
        int completed = jdbc.queryForObject("select count(*) from experiment_participant where experiment_id=? and participant_status='COMPLETED'", Integer.class, experimentId);
        int withdrawn = jdbc.queryForObject("select count(*) from experiment_participant where experiment_id=? and participant_status='WITHDRAWN'", Integer.class, experimentId);
        String metric = string(experiment, "primary_metric_code");
        var metricRow = jdbc.queryForMap("select count(distinct participant_id) as converted_count,coalesce(sum(metric_value),0) as metric_total from experiment_metric_event where experiment_id=? and metric_code=?", experimentId, metric);
        var result = new LinkedHashMap<String, Object>();
        result.put("experimentCode", string(experiment, "experiment_code")); result.put("status", string(experiment, "experiment_status"));
        result.put("plannedSampleSize", number(experiment, "planned_sample_size")); result.put("fixedDenominator", denominator);
        result.put("active", active); result.put("completed", completed); result.put("withdrawn", withdrawn);
        result.put("primaryMetricCode", metric); result.put("convertedCount", ((Number) metricRow.get("converted_count")).intValue()); result.put("metricTotal", metricRow.get("metric_total"));
        result.put("observationEndsAt", time(experiment, "observation_ends_at"));
        return result;
    }

    private Map<String,Object> experiment(String code) { return one("select * from controlled_experiment where experiment_code=?", upper(code)); }
    private Map<String,Object> experimentForUpdate(String code) { return one("select * from controlled_experiment where experiment_code=? for update", upper(code)); }
    private Map<String,Object> participant(long experimentId, long userId) { return one("select * from experiment_participant where experiment_id=? and user_id=?", experimentId, userId); }
    private Map<String,Object> one(String sql, Object... args) { var rows = jdbc.queryForList(sql, args); if (rows.isEmpty()) throw new IllegalArgumentException("experiment record not found"); return rows.get(0); }
    private void transition(long experimentId, Long participantId, String from, String to, long actorId, String reason) { jdbc.update("insert into experiment_status_transition(experiment_id,participant_id,from_status,to_status,actor_id,reason,occurred_at) values(?,?,?,?,?,?,?)", experimentId, participantId, from, to, actorId, trim(reason), now()); }
    private boolean allowedExperimentTransition(String from, String to) { return from.equals(to) || switch (from) { case "DRAFT" -> to.equals("ENROLLING") || to.equals("CANCELLED"); case "ENROLLING" -> to.equals("RUNNING") || to.equals("CANCELLED"); case "RUNNING" -> to.equals("COMPLETED") || to.equals("CANCELLED"); default -> false; }; }
    private boolean allowedParticipantTransition(String from, String to) { return from.equals(to) || switch (from) { case "QUEUED" -> to.equals("ACTIVE") || to.equals("WITHDRAWN"); case "ACTIVE" -> to.equals("COMPLETED") || to.equals("WITHDRAWN"); default -> false; }; }
    private String upper(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required"); return value.trim().toUpperCase(Locale.ROOT); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String string(Map<String,Object> row, String key) { return Objects.toString(row.get(key)); }
    private long id(Map<String,Object> row) { return ((Number) row.get("id")).longValue(); }
    private int number(Map<String,Object> row, String key) { return ((Number) row.get(key)).intValue(); }
    private LocalDateTime time(Map<String,Object> row, String key) { Object value = row.get(key); if (value instanceof LocalDateTime local) return local; if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime(); return LocalDateTime.parse(value.toString()); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
}
