package com.chatwolf.presence.dto;

import com.chatwolf.presence.constant.PresenceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresenceEvent {

    @JsonProperty("eventId")
    @NotBlank
    private String eventId;

    @JsonProperty("userId")
    @NotBlank
    private String userId;

    @JsonProperty("status")
    @NotNull
    private PresenceStatus status;

    @JsonProperty("timestamp")
    @NotNull
    private Instant timestamp;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("connectionId")
    private String connectionId;
}
