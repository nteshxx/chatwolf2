package com.chatwolf.presence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PresenceResponse {

    private String userId;

    private String status;
}
