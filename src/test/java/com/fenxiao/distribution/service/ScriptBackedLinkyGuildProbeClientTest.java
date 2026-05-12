package com.fenxiao.distribution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptBackedLinkyGuildProbeClientTest {

    @Test
    void shouldParseMatchedOursClassification() throws Exception {
        ScriptBackedLinkyGuildProbeClient client = new ScriptBackedLinkyGuildProbeClient(new ObjectMapper(), "scripts/linke_guild_probe.py", "", "");

        LinkyGuildProbeResult result = client.parseProbeOutput("12345678", """
                {
                  "http_status": 200,
                  "payload": {
                    "classification": "MATCHED_OURS",
                    "guild_id": "413",
                    "guild_name": "Permata",
                    "reason": "anchor found in current guild"
                  }
                }
                """);

        assertThat(result.matchedOurs()).isTrue();
        assertThat(result.guildId()).isEqualTo("413");
        assertThat(result.guildName()).isEqualTo("Permata");
    }

    @Test
    void shouldParseJoinedOtherGuildClassification() throws Exception {
        ScriptBackedLinkyGuildProbeClient client = new ScriptBackedLinkyGuildProbeClient(new ObjectMapper(), "scripts/linke_guild_probe.py", "", "");

        LinkyGuildProbeResult result = client.parseProbeOutput("34567890", """
                {
                  "http_status": 200,
                  "payload": {
                    "classification": "JOINED_OTHER_GUILD",
                    "guild_id": "999",
                    "guild_name": "Other Guild",
                    "reason": "anchor info belongs to another guild"
                  }
                }
                """);

        assertThat(result.joinedOtherGuild()).isTrue();
        assertThat(result.guildId()).isEqualTo("999");
        assertThat(result.guildName()).isEqualTo("Other Guild");
    }

    @Test
    void shouldParseNotJoinedClassification() throws Exception {
        ScriptBackedLinkyGuildProbeClient client = new ScriptBackedLinkyGuildProbeClient(new ObjectMapper(), "scripts/linke_guild_probe.py", "", "");

        LinkyGuildProbeResult result = client.parseProbeOutput("45678901", """
                {
                  "http_status": 200,
                  "payload": {
                    "classification": "NOT_JOINED",
                    "reason": "not found in current guild scope; outside guild status unconfirmed"
                  }
                }
                """);

        assertThat(result.notJoined()).isTrue();
        assertThat(result.guildId()).isNull();
        assertThat(result.guildName()).isNull();
        assertThat(result.remark()).contains("outside guild status unconfirmed");
    }

    @Test
    void shouldParseRealMatchedOursPayloadWithoutGuildFields() throws Exception {
        ScriptBackedLinkyGuildProbeClient client = new ScriptBackedLinkyGuildProbeClient(new ObjectMapper(), "scripts/linke_guild_probe.py", "", "");

        LinkyGuildProbeResult result = client.parseProbeOutput("45690394", """
                {
                  "http_status": 200,
                  "payload": {
                    "classification": "MATCHED_OURS",
                    "guild_id": null,
                    "guild_name": null,
                    "item": {
                      "user_id": "9007199286039809",
                      "sid": 45690394,
                      "nick_name": "skylove"
                    }
                  }
                }
                """);

        assertThat(result.matchedOurs()).isTrue();
        assertThat(result.guildId()).isNull();
        assertThat(result.guildName()).isNull();
        assertThat(result.remark()).contains("skylove");
        assertThat(result.remark()).contains("45690394");
    }
}
