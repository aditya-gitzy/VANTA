package dev.vanta.service.platform;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service managing platform metric collection.
 * Selects the native Linux adapter when available and polls metrics asynchronously.
 */
public class PlatformService {

    private final PlatformAdapter adapter;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "vanta-platform-poller");
        t.setDaemon(true);
        return t;
    });

    private final CopyOnWriteArrayList<Consumer<SystemSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private volatile SystemSnapshot latestSnapshot;

    public PlatformService() {
        PlatformAdapter linux = new LinuxPlatformAdapter();
        if (linux.isSupported()) {
            this.adapter = linux;
        } else {
            this.adapter = new GenericPlatformAdapter();
        }

        // Capture initial snapshot
        this.latestSnapshot = adapter.captureSnapshot();

        // Schedule periodic collection every 2 seconds
        scheduler.scheduleWithFixedDelay(this::poll, 1, 2, TimeUnit.SECONDS);
    }

    public PlatformService(PlatformAdapter customAdapter) {
        this.adapter = customAdapter;
        this.latestSnapshot = adapter.captureSnapshot();
        scheduler.scheduleWithFixedDelay(this::poll, 1, 2, TimeUnit.SECONDS);
    }

    private void poll() {
        try {
            SystemSnapshot snapshot = adapter.captureSnapshot();
            this.latestSnapshot = snapshot;
            for (Consumer<SystemSnapshot> listener : listeners) {
                try {
                    listener.accept(snapshot);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public SystemSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public PlatformAdapter getAdapter() {
        return adapter;
    }

    public void addListener(Consumer<SystemSnapshot> listener) {
        listeners.add(listener);
        if (latestSnapshot != null) {
            listener.accept(latestSnapshot);
        }
    }

    public void removeListener(Consumer<SystemSnapshot> listener) {
        listeners.remove(listener);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
