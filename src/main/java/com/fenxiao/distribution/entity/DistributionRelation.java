package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.distribution.domain.BindSource;
import com.fenxiao.distribution.domain.LockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "distribution_relation")
public class DistributionRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "level1_inviter_id")
    private Long level1InviterId;

    @Column(name = "level2_inviter_id")
    private Long level2InviterId;

    @Column(name = "level3_inviter_id")
    private Long level3InviterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bind_source", nullable = false, length = 32)
    private BindSource bindSource;

    @Column(name = "bind_time", nullable = false)
    private LocalDateTime bindTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status", nullable = false, length = 32)
    private LockStatus lockStatus;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "cross_country", nullable = false)
    private boolean crossCountry;
}
