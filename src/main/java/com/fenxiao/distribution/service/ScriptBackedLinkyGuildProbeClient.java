package com.fenxiao.distribution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class ScriptBackedLinkyGuildProbeClient implements LinkyGuildProbeClient {

    private final ObjectMapper objectMapper;
    private final String scriptPath;
    private final String oauthToken;
    private final String oauthSecret;

    public ScriptBackedLinkyGuildProbeClient(ObjectMapper objectMapper,
                                             @Value("${app.distribution.linky-guild-probe-script:scripts/linke_guild_probe.py}") String scriptPath,
                                             @Value("${app.distribution.linky-oauth-token:${LINKE_OAUTH_TOKEN:}}") String oauthToken,
                                             @Value("${app.distribution.linky-oauth-token-secret:${LINKE_OAUTH_TOKEN_SECRET:}}") String oauthSecret) {
        this.objectMapper = objectMapper;
        this.scriptPath = scriptPath;
        this.oauthToken = oauthToken;
        this.oauthSecret = oauthSecret;
    }

    @Override
    public LinkyGuildProbeResult probe(String linkyAccount) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(List.of(
                    "python3",
                    scriptPath,
                    "classify-linky",
                    "--id",
                    linkyAccount
            ));
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            if (oauthToken != null && !oauthToken.isBlank()) {
                env.put("LINKE_OAUTH_TOKEN", oauthToken);
            }
            if (oauthSecret != null && !oauthSecret.isBlank()) {
                env.put("LINKE_OAUTH_TOKEN_SECRET", oauthSecret);
            }
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return LinkyGuildProbeResult.unavailable(linkyAccount, trimRemark(output));
            }
            return parseProbeOutput(linkyAccount, output);
        } catch (Exception exception) {
            return LinkyGuildProbeResult.unavailable(linkyAccount, trimRemark(exception.getMessage()));
        }
    }

    LinkyGuildProbeResult parseProbeOutput(String linkyAccount, String output) throws Exception {
        JsonNode root = objectMapper.readTree(output);
        JsonNode payload = root.path("payload");
        String classification = payload.path("classification").asText("").trim().toUpperCase();
        return switch (classification) {
            case "MATCHED_OURS" -> LinkyGuildProbeResult.matchedOurs(
                    linkyAccount,
                    payload.path("guild_id").isMissingNode() || payload.path("guild_id").isNull() ? null : payload.path("guild_id").asText(),
                    payload.path("guild_name").isMissingNode() || payload.path("guild_name").isNull() ? null : payload.path("guild_name").asText(),
                    trimRemark(buildMatchedOursRemark(payload))
            );
            case "JOINED_OTHER_GUILD" -> LinkyGuildProbeResult.joinedOtherGuild(
                    linkyAccount,
                    payload.path("guild_id").isMissingNode() || payload.path("guild_id").isNull() ? null : payload.path("guild_id").asText(),
                    payload.path("guild_name").isMissingNode() || payload.path("guild_name").isNull() ? null : payload.path("guild_name").asText(),
                    trimRemark(payload.path("reason").asText("probe found other guild"))
            );
            case "NOT_JOINED" -> LinkyGuildProbeResult.notMatched(
                    linkyAccount,
                    trimRemark(payload.path("reason").asText("not found in current guild scope; outside guild status unconfirmed"))
            );
            default -> LinkyGuildProbeResult.unavailable(
                    linkyAccount,
                    trimRemark(payload.path("reason").asText("guild probe unavailable"))
            );
        };
    }

    private String buildMatchedOursRemark(JsonNode payload) {
        String explicitReason = payload.path("reason").asText("").trim();
        if (!explicitReason.isBlank()) {
            return explicitReason;
        }
        JsonNode item = payload.path("item");
        if (item.isObject()) {
            String sid = item.path("sid").isMissingNode() || item.path("sid").isNull() ? "" : item.path("sid").asText("");
            String userId = item.path("user_id").isMissingNode() || item.path("user_id").isNull() ? "" : item.path("user_id").asText("");
            String nickName = item.path("nick_name").isMissingNode() || item.path("nick_name").isNull() ? "" : item.path("nick_name").asText("");
            StringBuilder summary = new StringBuilder("anchor matched in current guild");
            if (!nickName.isBlank()) {
                summary.append(" nick=").append(nickName);
            }
            if (!sid.isBlank()) {
                summary.append(" sid=").append(sid);
            }
            if (!userId.isBlank()) {
                summary.append(" user_id=").append(userId);
            }
            return summary.toString();
        }
        return "auto matched from guild probe";
    }

    private String trimRemark(String source) {
        if (source == null || source.isBlank()) {
            return "guild probe unavailable";
        }
        String singleLine = source.replaceAll("\\s+", " ").trim();
        return singleLine.length() > 180 ? singleLine.substring(0, 180) : singleLine;
    }
}
