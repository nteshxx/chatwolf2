package com.chatwolf.presence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceEvent {
    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("status") // "online", "offline", "heartbeat", "away"
    private String status;

    @JsonProperty("timestamp")
    private Instant timestamp;
}
