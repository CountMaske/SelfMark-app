package com.nortidart.selfmark.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "selfmark.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
