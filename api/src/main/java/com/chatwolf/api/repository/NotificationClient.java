package com.chatwolf.api.repository;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notification-service", path = "/notify")
public interface NotificationClient {

    @PostMapping("/send")
    void sendNotification(@RequestBody Map<String, Object> notification);
}
