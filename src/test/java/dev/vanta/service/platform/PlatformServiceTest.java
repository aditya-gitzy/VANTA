package dev.vanta.service.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformServiceTest {

    private PlatformService platformService;

    @BeforeEach
    void setUp() {
        platformService = new PlatformService();
    }

    @AfterEach
    void tearDown() {
        if (platformService != null) {
            platformService.shutdown();
        }
    }

    @Test
    void testAdapterSelectionAndSnapshot() {
        PlatformAdapter adapter = platformService.getAdapter();
        assertNotNull(adapter);
        assertTrue(adapter.isSupported());

        SystemSnapshot snapshot = platformService.getLatestSnapshot();
        assertNotNull(snapshot);
        assertNotNull(snapshot.cpu());
        assertTrue(snapshot.cpu().availableProcessors() > 0);
        assertNotNull(snapshot.memory());
        assertTrue(snapshot.memory().totalBytes() > 0);
        assertNotNull(snapshot.disks());
        assertNotNull(snapshot.topProcesses());
    }

    @Test
    void testMemoryFormatBytes() {
        assertEquals("500 B", MemoryMetrics.formatBytes(500));
        assertEquals("1.00 KB", MemoryMetrics.formatBytes(1024));
        assertEquals("1.00 MB", MemoryMetrics.formatBytes(1024 * 1024));
        assertEquals("1.00 GB", MemoryMetrics.formatBytes(1024L * 1024 * 1024));
    }
}
