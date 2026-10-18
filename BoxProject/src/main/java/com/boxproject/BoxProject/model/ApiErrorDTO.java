package com.boxproject.BoxProject.model;

import java.time.Instant;
import java.util.Map;

public record ApiErrorDTO(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {
}
