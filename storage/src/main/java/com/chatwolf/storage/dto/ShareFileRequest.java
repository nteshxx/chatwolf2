package com.chatwolf.storage.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Data;

@Data
public class ShareFileRequest {
    @NotEmpty
    private Set<String> userIds;
}
