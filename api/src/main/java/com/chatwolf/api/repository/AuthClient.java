package com.chatwolf.api.repository;

import com.chatwolf.api.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/auth")
public interface AuthClient {

    @GetMapping("/users/{id}")
    ResponseEntity<UserDTO> getUserById(@PathVariable String id);
}
