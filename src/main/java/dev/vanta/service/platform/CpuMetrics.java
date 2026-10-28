package dev.vanta.service.platform;

public record CpuMetrics(
        double totalUsagePercent,
        int availableProcessors,
        String architecture,
        String modelName
) {
}
