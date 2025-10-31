package com.chatwolf.gateway.utility;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

public class ResponseBuilder {

    public static ResponseEntity<Object> build(
            HttpStatus httpStatus, ResponseCookie cookie, String error, String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", httpStatus.value());
        response.put("error", error);
        response.put("message", message);
        response.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return new ResponseEntity<>(response, headers, httpStatus);
    }
}
