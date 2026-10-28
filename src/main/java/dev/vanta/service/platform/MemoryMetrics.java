package dev.vanta.service.platform;

public record MemoryMetrics(
        long totalBytes,
        long usedBytes,
        long freeBytes,
        long availableBytes,
        double usagePercent
) {
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
