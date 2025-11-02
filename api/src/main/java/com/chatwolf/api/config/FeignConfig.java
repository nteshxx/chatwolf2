package com.chatwolf.api.config;

import com.chatwolf.api.exception.BadRequestException;
import com.chatwolf.api.exception.ForbiddenException;
import com.chatwolf.api.exception.InternalServerErrorException;
import com.chatwolf.api.exception.NotFoundException;
import com.chatwolf.api.exception.ServiceUnavailableException;
import com.chatwolf.api.exception.UnauthorizedException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class FeignConfig {

    /**
     * Feign logging level configuration
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Custom error decoder for handling HTTP errors
     */
    @Bean
    ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            HttpStatus status = HttpStatus.resolve(response.status());

            if (status != null) {
                switch (status) {
                    case BAD_REQUEST:
                        return new BadRequestException("Bad Request: " + methodKey);
                    case UNAUTHORIZED:
                        return new UnauthorizedException("Unauthorized: " + methodKey);
                    case FORBIDDEN:
                        return new ForbiddenException("Forbidden: " + methodKey);
                    case NOT_FOUND:
                        return new NotFoundException("Resource Not Found: " + methodKey);
                    case INTERNAL_SERVER_ERROR:
                        return new InternalServerErrorException("Internal Server Error: " + methodKey);
                    case SERVICE_UNAVAILABLE:
                        return new ServiceUnavailableException("Service Unavailable: " + methodKey);
                    default:
                        return new Exception("Generic error: " + response.reason());
                }
            }

            return new Exception("Unknown error: " + response.status());
        };
    }

    /**
     * Request interceptor to add common headers
     */
    @Bean
    RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Content-Type", "application/json");
            // Add custom headers here
            // requestTemplate.header("X-Custom-Header", "value");
        };
    }

    /**
     * Spring encoder configuration
     */
    @Bean
    Encoder feignEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringEncoder(messageConverters);
    }

    /**
     * Spring decoder configuration
     */
    @Bean
    Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringDecoder(messageConverters);
    }
}
