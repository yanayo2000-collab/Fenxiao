package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "guild_account_config")
public class GuildAccountConfig extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;
    @Column(name = "inviter_user_id")
    private Long inviterUserId;
    @Column(name = "guild_id", nullable = false, length = 64)
    private String guildId;
    @Column(name = "guild_name", nullable = false, length = 128)
    private String guildName;
    @Column(name = "guild_invite_code", nullable = false, length = 64)
    private String guildInviteCode;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    protected GuildAccountConfig() {}
    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public Long getInviterUserId() { return inviterUserId; }
    public String getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public String getGuildInviteCode() { return guildInviteCode; }
    public boolean isEnabled() { return enabled; }
    public static GuildAccountConfig create(String productCode, Long inviterUserId, String guildId, String guildName, String guildInviteCode) {
        GuildAccountConfig c = new GuildAccountConfig();
        c.productCode = productCode;
        c.inviterUserId = inviterUserId;
        c.guildId = guildId;
        c.guildName = guildName;
        c.guildInviteCode = guildInviteCode;
        c.enabled = true;
        return c;
    }
    public void update(String guildId, String guildName, String guildInviteCode, boolean enabled) {
        this.guildId = guildId; this.guildName = guildName; this.guildInviteCode = guildInviteCode; this.enabled = enabled;
    }
}
