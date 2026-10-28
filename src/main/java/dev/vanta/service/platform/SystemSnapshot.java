package dev.vanta.service.platform;

import java.util.List;

public record SystemSnapshot(
        CpuMetrics cpu,
        MemoryMetrics memory,
        List<DiskPartition> disks,
        List<ProcessInfo> topProcesses,
        String osName,
        String kernelVersion,
        String uptime,
        String adapterName
) {
}
