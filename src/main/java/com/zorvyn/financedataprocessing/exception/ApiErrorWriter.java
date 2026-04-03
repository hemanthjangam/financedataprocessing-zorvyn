package com.zorvyn.financedataprocessing.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorWriter {

    public void write(HttpServletResponse response, HttpStatus status, String message, List<String> details) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","details":%s}
                """.formatted(
                Instant.now(),
                status.value(),
                escape(status.getReasonPhrase()),
                escape(message),
                formatDetails(details)
        ));
    }

    private String formatDetails(List<String> details) {
        return details.stream()
                .map(detail -> "\"" + escape(detail) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
