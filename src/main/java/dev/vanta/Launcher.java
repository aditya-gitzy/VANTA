package dev.vanta;

/**
 * Separate main class launcher to bypass module-path requirements
 * when running packaged fat JARs in standard classpath environments.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        VantaApp.main(args);
    }
}
