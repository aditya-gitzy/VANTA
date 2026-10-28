package dev.vanta.service.platform;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Universal cross-platform fallback adapter leveraging Java standard platform MXBeans
 * and Java 9+ ProcessHandle API.
 */
public class GenericPlatformAdapter implements PlatformAdapter {

    @Override
    public String getName() {
        return "Universal JVM Platform Adapter";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public SystemSnapshot captureSnapshot() {
        CpuMetrics cpu = captureCpu();
        MemoryMetrics memory = captureMemory();
        List<DiskPartition> disks = captureDisks();
        List<ProcessInfo> processes = captureProcesses();

        return new SystemSnapshot(
                cpu,
                memory,
                disks,
                processes,
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                "Active session",
                getName()
        );
    }

    private CpuMetrics captureCpu() {
        int cores = Runtime.getRuntime().availableProcessors();
        double usage = 0.0;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            // Check com.sun.management.OperatingSystemMXBean via reflection
            Method method = osBean.getClass().getMethod("getCpuLoad");
            Object val = method.invoke(osBean);
            if (val instanceof Double d && d >= 0) {
                usage = d * 100.0;
            }
        } catch (Exception e) {
            // Fallback load average
            double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            if (load >= 0 && cores > 0) {
                usage = Math.min(100.0, (load / cores) * 100.0);
            }
        }

        return new CpuMetrics(usage, cores, System.getProperty("os.arch"), System.getProperty("os.name") + " Host Processor");
    }

    private MemoryMetrics captureMemory() {
        long totalMemory = 0;
        long freeMemory = 0;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Method getTotal = osBean.getClass().getMethod("getTotalMemorySize");
            Method getFree = osBean.getClass().getMethod("getFreeMemorySize");
            totalMemory = (Long) getTotal.invoke(osBean);
            freeMemory = (Long) getFree.invoke(osBean);
        } catch (Exception e) {
            // Fallback to JVM heap if OS values inaccessible
            totalMemory = Runtime.getRuntime().totalMemory();
            freeMemory = Runtime.getRuntime().freeMemory();
        }

        long usedMemory = Math.max(0, totalMemory - freeMemory);
        double usagePercent = totalMemory > 0 ? (100.0 * usedMemory / totalMemory) : 0.0;

        return new MemoryMetrics(totalMemory, usedMemory, freeMemory, freeMemory, usagePercent);
    }

    private List<DiskPartition> captureDisks() {
        List<DiskPartition> disks = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                long total = root.getTotalSpace();
                long usable = root.getUsableSpace();
                long free = root.getFreeSpace();
                if (total > 0) {
                    long used = total - usable;
                    double usagePercent = 100.0 * used / total;
                    disks.add(new DiskPartition(root.getAbsolutePath(), "local", total, free, usable, usagePercent));
                }
            }
        }
        return disks;
    }

    private List<ProcessInfo> captureProcesses() {
        List<ProcessInfo> list = new ArrayList<>();
        try {
            ProcessHandle.allProcesses()
                    .filter(ProcessHandle::isAlive)
                    .limit(50)
                    .forEach(ph -> {
                        String cmd = ph.info().command().orElse("[system process]");
                        String user = ph.info().user().orElse("—");
                        list.add(new ProcessInfo(ph.pid(), user, 0.0, 0, cmd, "Alive"));
                    });
        } catch (Exception ignored) {
        }
        return list;
    }
}
