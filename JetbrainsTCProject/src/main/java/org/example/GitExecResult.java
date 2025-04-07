package org.example;

public record GitExecResult(String output, String error, int exitCode) {}
