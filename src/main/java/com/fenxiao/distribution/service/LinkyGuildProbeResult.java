package com.fenxiao.distribution.service;

public record LinkyGuildProbeResult(
        String linkyAccount,
        ProbeStatus status,
        String guildId,
        String guildName,
        String remark
) {
    public enum ProbeStatus {
        MATCHED_OURS,
        JOINED_OTHER_GUILD,
        NOT_JOINED,
        UNAVAILABLE
    }

    public static LinkyGuildProbeResult matchedOurs(String linkyAccount, String guildId, String guildName, String remark) {
        return new LinkyGuildProbeResult(linkyAccount, ProbeStatus.MATCHED_OURS, guildId, guildName, remark);
    }

    public static LinkyGuildProbeResult joinedOtherGuild(String linkyAccount, String guildId, String guildName, String remark) {
        return new LinkyGuildProbeResult(linkyAccount, ProbeStatus.JOINED_OTHER_GUILD, guildId, guildName, remark);
    }

    public static LinkyGuildProbeResult notMatched(String linkyAccount, String remark) {
        return new LinkyGuildProbeResult(linkyAccount, ProbeStatus.NOT_JOINED, null, null, remark);
    }

    public static LinkyGuildProbeResult unavailable(String linkyAccount, String remark) {
        return new LinkyGuildProbeResult(linkyAccount, ProbeStatus.UNAVAILABLE, null, null, remark);
    }

    public boolean matchedOurs() {
        return status == ProbeStatus.MATCHED_OURS;
    }

    public boolean joinedOtherGuild() {
        return status == ProbeStatus.JOINED_OTHER_GUILD;
    }

    public boolean notJoined() {
        return status == ProbeStatus.NOT_JOINED;
    }

    public boolean available() {
        return status != ProbeStatus.UNAVAILABLE;
    }
}
