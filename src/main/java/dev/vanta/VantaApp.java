package dev.vanta;

import dev.vanta.module.ModuleRegistry;
import dev.vanta.modules.console.ConsoleModule;
import dev.vanta.modules.files.FilesModule;
import dev.vanta.modules.monitor.MonitorModule;
import dev.vanta.modules.overview.OverviewModule;
import dev.vanta.service.fs.FileService;
import dev.vanta.service.platform.PlatformService;
import dev.vanta.service.pref.PreferenceService;
import dev.vanta.service.process.ProcessService;
import dev.vanta.shell.ShellView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main JavaFX Application entry point for VANTA.
 * Orchestrates backend services, module registration, and shell UI bootstrap.
 */
public class VantaApp extends Application {

    public static final String APP_NAME = "VANTA";
    public static final String APP_VERSION = "1.0.0-SNAPSHOT";

    private PlatformService platformService;
    private ProcessService processService;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Core Decoupled Backend Services
        PreferenceService preferenceService = new PreferenceService();
        FileService fileService = new FileService();
        this.processService = new ProcessService();
        this.platformService = new PlatformService();

        // Initialize Module Registry
        ModuleRegistry registry = new ModuleRegistry();

        // Shell View forward reference for Overview navigation clicks
        ShellView[] shellHolder = new ShellView[1];

        // Register Built-in Modules
        OverviewModule overviewModule = new OverviewModule(targetModuleId -> {
            if (shellHolder[0] != null) {
                shellHolder[0].navigateTo(targetModuleId);
            }
        });
        FilesModule filesModule = new FilesModule(fileService);
        ConsoleModule consoleModule = new ConsoleModule(processService);
        MonitorModule monitorModule = new MonitorModule(platformService);

        registry.register(overviewModule);
        registry.register(filesModule);
        registry.register(consoleModule);
        registry.register(monitorModule);

        // Build Persistent Shell
        ShellView shellView = new ShellView(registry, preferenceService);
        shellHolder[0] = shellView;

        // Restore last selected module from user preferences (default to 'overview')
        String lastModuleId = preferenceService.getLastModuleId("overview");
        if (registry.contains(lastModuleId)) {
            shellView.navigateTo(lastModuleId);
        } else {
            shellView.navigateTo("overview");
        }

        // Configure Scene and Theme
        Scene scene = new Scene(shellView, 1120, 720);
        String stylesheet = Objects.requireNonNull(
                getClass().getResource("/styles/theme.css")
        ).toExternalForm();
        scene.getStylesheets().add(stylesheet);

        primaryStage.setTitle(APP_NAME + " — Desktop Management Interface");
        primaryStage.setMinWidth(880);
        primaryStage.setMinHeight(580);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            if (platformService != null) {
                platformService.shutdown();
            }
            if (processService != null) {
                processService.cancelRunningProcess();
            }
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
