package dev.vanta.service.platform;

public record ProcessInfo(
        long pid,
        String user,
        double cpuPercent,
        long memoryBytes,
        String command,
        String state
) {
}
