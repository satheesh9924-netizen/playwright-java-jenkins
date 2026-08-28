package com.example.aitriage;

public record TestFailure(
        String className,
        String testName,
        String errorMessage,
        String stackTrace
) {
}
