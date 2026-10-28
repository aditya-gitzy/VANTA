package dev.vanta.service.platform;

/**
 * Common platform abstraction decoupling UI from Linux / OS specific system access.
 */
public interface PlatformAdapter {

    String getName();

    boolean isSupported();

    SystemSnapshot captureSnapshot();
}
