package dev.vanta.service.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Native Linux Platform Adapter reading directly from kernel /proc and /sys interfaces.
 */
public class LinuxPlatformAdapter implements PlatformAdapter {

    private long lastTotalCpuTime = 0;
    private long lastIdleCpuTime = 0;

    @Override
    public String getName() {
        return "Linux /proc Direct Kernel Adapter";
    }

    @Override
    public boolean isSupported() {
        return Files.exists(Paths.get("/proc/stat")) && Files.exists(Paths.get("/proc/meminfo"));
    }

    @Override
    public SystemSnapshot captureSnapshot() {
        CpuMetrics cpu = readCpuMetrics();
        MemoryMetrics memory = readMemoryMetrics();
        List<DiskPartition> disks = readDiskPartitions();
        List<ProcessInfo> processes = readProcesses();
        String uptime = readUptime();

        return new SystemSnapshot(
                cpu,
                memory,
                disks,
                processes,
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                uptime,
                getName()
        );
    }

    private CpuMetrics readCpuMetrics() {
        double usagePercent = 0.0;
        String modelName = "Linux CPU";
        int cores = Runtime.getRuntime().availableProcessors();

        // Read /proc/stat
        Path statPath = Paths.get("/proc/stat");
        if (Files.exists(statPath)) {
            try (BufferedReader reader = Files.newBufferedReader(statPath)) {
                String line = reader.readLine();
                if (line != null && line.startsWith("cpu ")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 5) {
                        long user = Long.parseLong(parts[1]);
                        long nice = Long.parseLong(parts[2]);
                        long system = Long.parseLong(parts[3]);
                        long idle = Long.parseLong(parts[4]);
                        long iowait = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
                        long irq = parts.length > 6 ? Long.parseLong(parts[6]) : 0;
                        long softirq = parts.length > 7 ? Long.parseLong(parts[7]) : 0;

                        long total = user + nice + system + idle + iowait + irq + softirq;
                        long idleTime = idle + iowait;

                        if (lastTotalCpuTime > 0) {
                            long totalDiff = total - lastTotalCpuTime;
                            long idleDiff = idleTime - lastIdleCpuTime;
                            if (totalDiff > 0) {
                                usagePercent = Math.max(0.0, Math.min(100.0, 100.0 * (totalDiff - idleDiff) / totalDiff));
                            }
                        }
                        lastTotalCpuTime = total;
                        lastIdleCpuTime = idleTime;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Read /proc/cpuinfo
        Path cpuInfo = Paths.get("/proc/cpuinfo");
        if (Files.exists(cpuInfo)) {
            try (BufferedReader reader = Files.newBufferedReader(cpuInfo)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("model name") || line.startsWith("Hardware")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            modelName = parts[1].trim();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return new CpuMetrics(usagePercent, cores, System.getProperty("os.arch"), modelName);
    }

    private MemoryMetrics readMemoryMetrics() {
        long totalKb = 0;
        long freeKb = 0;
        long availKb = 0;

        Path memPath = Paths.get("/proc/meminfo");
        if (Files.exists(memPath)) {
            try (BufferedReader reader = Files.newBufferedReader(memPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("MemTotal:")) {
                        totalKb = parseKb(line);
                    } else if (line.startsWith("MemFree:")) {
                        freeKb = parseKb(line);
                    } else if (line.startsWith("MemAvailable:")) {
                        availKb = parseKb(line);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (availKb == 0) {
            availKb = freeKb;
        }

        long totalBytes = totalKb * 1024L;
        long availableBytes = availKb * 1024L;
        long freeBytes = freeKb * 1024L;
        long usedBytes = Math.max(0, totalBytes - availableBytes);
        double usagePercent = totalBytes > 0 ? (100.0 * usedBytes / totalBytes) : 0.0;

        return new MemoryMetrics(totalBytes, usedBytes, freeBytes, availableBytes, usagePercent);
    }

    private long parseKb(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private List<DiskPartition> readDiskPartitions() {
        List<DiskPartition> partitions = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                long total = root.getTotalSpace();
                long usable = root.getUsableSpace();
                long free = root.getFreeSpace();
                if (total > 0) {
                    long used = total - usable;
                    double usagePercent = 100.0 * used / total;
                    partitions.add(new DiskPartition(root.getAbsolutePath(), "standard", total, free, usable, usagePercent));
                }
            }
        }
        return partitions;
    }

    private List<ProcessInfo> readProcesses() {
        List<ProcessInfo> list = new ArrayList<>();
        Path proc = Paths.get("/proc");
        if (Files.exists(proc)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(proc, path -> {
                String name = path.getFileName().toString();
                return name.chars().allMatch(Character::isDigit);
            })) {
                for (Path p : stream) {
                    long pid = Long.parseLong(p.getFileName().toString());
                    String cmd = readCmdline(p);
                    if (cmd.isEmpty()) {
                        cmd = "[" + p.getFileName() + "]";
                    }
                    list.add(new ProcessInfo(pid, System.getProperty("user.name"), 0.0, 0, cmd, "Running"));
                    if (list.size() >= 40) break;
                }
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    private String readCmdline(Path procDir) {
        Path cmdline = procDir.resolve("cmdline");
        if (Files.exists(cmdline)) {
            try {
                byte[] bytes = Files.readAllBytes(cmdline);
                if (bytes.length > 0) {
                    return new String(bytes).replace('\0', ' ').trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String readUptime() {
        Path uptimePath = Paths.get("/proc/uptime");
        if (Files.exists(uptimePath)) {
            try (BufferedReader reader = Files.newBufferedReader(uptimePath)) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    double seconds = Double.parseDouble(parts[0]);
                    long totalSec = (long) seconds;
                    long hours = totalSec / 3600;
                    long minutes = (totalSec % 3600) / 60;
                    long secs = totalSec % 60;
                    return String.format("%d hours, %d min, %d sec", hours, minutes, secs);
                }
            } catch (Exception ignored) {
            }
        }
        return "Unknown";
    }
}
