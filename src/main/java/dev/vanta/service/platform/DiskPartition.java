package dev.vanta.service.platform;

public record DiskPartition(
        String mountPoint,
        String fsType,
        long totalBytes,
        long freeBytes,
        long usableBytes,
        double usagePercent
) {
}
