package dev.vanta.module;

import javafx.scene.Node;

/**
 * Interface contract for all built-in VANTA modules.
 * UI view is decoupled from service lifecycle.
 */
public interface VantaModule {

    /**
     * Unique machine-readable identifier (e.g., "files", "console", "monitor").
     */
    String getId();

    /**
     * Human-readable display label for navigation.
     */
    String getDisplayName();

    /**
     * Short description of the module's capability.
     */
    String getDescription();

    /**
     * Icon text glyph or symbol for sidebar navigation.
     */
    String getIconGlyph();

    /**
     * Returns the primary JavaFX root Node for this module.
     */
    Node getView();

    /**
     * Lifecycle callback invoked when the user switches to this module.
     */
    default void onActivated() {
    }

    /**
     * Lifecycle callback invoked when the user navigates away from this module.
     */
    default void onDeactivated() {
    }
}
