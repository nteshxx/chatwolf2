package com.chatwolf.api.repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "storage-service", path = "/storage")
public interface StorageClient {

    @GetMapping("/presign/{objectId}")
    String getPresignedUrl(@PathVariable String objectId);
}
