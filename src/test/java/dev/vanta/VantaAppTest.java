package dev.vanta;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VantaAppTest {

    @Test
    void testAppMetadata() {
        assertEquals("VANTA", VantaApp.APP_NAME);
        assertNotNull(VantaApp.APP_VERSION);
    }
}
