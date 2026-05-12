package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "withdraw_request_item")
public class WithdrawRequestItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdraw_request_id", nullable = false)
    private Long withdrawRequestId;

    @Column(name = "reward_record_id", nullable = false)
    private Long rewardRecordId;

    @Column(name = "reward_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal rewardAmount;

    protected WithdrawRequestItem() {
    }

    public Long getId() {
        return id;
    }

    public Long getWithdrawRequestId() {
        return withdrawRequestId;
    }

    public Long getRewardRecordId() {
        return rewardRecordId;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public static WithdrawRequestItem create(Long withdrawRequestId, Long rewardRecordId, BigDecimal rewardAmount) {
        WithdrawRequestItem item = new WithdrawRequestItem();
        item.withdrawRequestId = withdrawRequestId;
        item.rewardRecordId = rewardRecordId;
        item.rewardAmount = rewardAmount;
        return item;
    }
}
