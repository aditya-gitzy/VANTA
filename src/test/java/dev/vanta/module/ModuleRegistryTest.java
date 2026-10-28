package dev.vanta.module;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleRegistryTest {

    private ModuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ModuleRegistry();
    }

    private VantaModule createDummyModule(String id, String name) {
        return new VantaModule() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getDisplayName() {
                return name;
            }

            @Override
            public String getDescription() {
                return "Description for " + name;
            }

            @Override
            public String getIconGlyph() {
                return "★";
            }

            @Override
            public Node getView() {
                return new Pane();
            }
        };
    }

    @Test
    void testRegisterAndRetrieve() {
        VantaModule mod = createDummyModule("test-mod", "Test Module");
        registry.register(mod);

        assertEquals(1, registry.size());
        assertTrue(registry.contains("test-mod"));

        Optional<VantaModule> retrieved = registry.getModule("test-mod");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Module", retrieved.get().getDisplayName());
    }

    @Test
    void testDuplicateRegistrationThrows() {
        VantaModule mod1 = createDummyModule("duplicate", "Mod 1");
        VantaModule mod2 = createDummyModule("duplicate", "Mod 2");

        registry.register(mod1);
        assertThrows(IllegalArgumentException.class, () -> registry.register(mod2));
    }

    @Test
    void testNullModuleThrows() {
        assertThrows(NullPointerException.class, () -> registry.register(null));
    }

    @Test
    void testNonExistentModuleReturnsEmpty() {
        assertFalse(registry.getModule("nonexistent").isPresent());
    }
}
