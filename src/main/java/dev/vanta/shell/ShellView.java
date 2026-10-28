package dev.vanta.shell;

import dev.vanta.VantaApp;
import dev.vanta.module.ModuleRegistry;
import dev.vanta.module.VantaModule;
import dev.vanta.service.pref.PreferenceService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent Shell layout containing:
 * - Persistent Top Header
 * - Left-docked Navigation Sidebar
 * - Main Dynamic Workspace Container
 */
public class ShellView extends BorderPane {

    private final ModuleRegistry moduleRegistry;
    private final PreferenceService preferenceService;

    private final VBox navButtonBox = new VBox(6);
    private final StackPane workspaceContainer = new StackPane();
    private final Map<String, Button> navButtons = new HashMap<>();

    private final Label currentModuleTitle = new Label("Overview");
    private final Label clockLabel = new Label();
    private VantaModule activeModule;

    public ShellView(ModuleRegistry moduleRegistry, PreferenceService preferenceService) {
        this.moduleRegistry = moduleRegistry;
        this.preferenceService = preferenceService;

        getStyleClass().add("app-container");
        setTop(createHeader());
        setLeft(createSidebar());
        setCenter(createWorkspace());

        setupClock();
    }

    private HBox createHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label(VantaApp.APP_NAME);
        logo.getStyleClass().add("app-title-logo");

        Label badge = new Label("v" + VantaApp.APP_VERSION);
        badge.getStyleClass().add("app-badge");

        Label divider = new Label("•");
        divider.getStyleClass().add("app-subtitle");

        currentModuleTitle.getStyleClass().add("label-heading");
        currentModuleTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #e6edf3;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        clockLabel.getStyleClass().add("app-subtitle");

        Label osIndicator = new Label(System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        osIndicator.getStyleClass().add("app-subtitle");

        Label statusPill = new Label("● System Ready");
        statusPill.getStyleClass().add("status-pill");

        header.getChildren().addAll(logo, badge, divider, currentModuleTitle, spacer, clockLabel, osIndicator, statusPill);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("sidebar-container");
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(180);
        sidebar.setMaxWidth(220);
        sidebar.setPadding(new Insets(16, 12, 16, 12));

        Label navSectionLabel = new Label("MODULES");
        navSectionLabel.getStyleClass().add("sidebar-section-title");

        navButtonBox.setFillWidth(true);
        VBox.setVgrow(navButtonBox, Priority.ALWAYS);

        // Populate navigation buttons from registry
        for (VantaModule module : moduleRegistry.getAllModules()) {
            Button btn = createNavButton(module);
            navButtons.put(module.getId(), btn);
            navButtonBox.getChildren().add(btn);
        }

        // Bottom user and host indicator
        VBox bottomBox = new VBox(4);
        bottomBox.getStyleClass().add("sidebar-footer");
        Label userLabel = new Label("User: " + System.getProperty("user.name"));
        userLabel.getStyleClass().add("app-subtitle");
        userLabel.setStyle("-fx-font-size: 11px;");

        Label javaLabel = new Label("Java: " + Runtime.version().feature());
        javaLabel.getStyleClass().add("app-subtitle");
        javaLabel.setStyle("-fx-font-size: 11px;");

        bottomBox.getChildren().addAll(userLabel, javaLabel);

        sidebar.getChildren().addAll(navSectionLabel, navButtonBox, bottomBox);
        return sidebar;
    }

    private Button createNavButton(VantaModule module) {
        String label = (module.getIconGlyph() != null ? module.getIconGlyph() + "  " : "") + module.getDisplayName();
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setOnAction(e -> navigateTo(module.getId()));
        return btn;
    }

    private StackPane createWorkspace() {
        workspaceContainer.getStyleClass().add("workspace-container");
        workspaceContainer.setPadding(new Insets(12));
        return workspaceContainer;
    }

    public void navigateTo(String moduleId) {
        moduleRegistry.getModule(moduleId).ifPresent(module -> {
            if (activeModule != null) {
                activeModule.onDeactivated();
            }

            activeModule = module;
            currentModuleTitle.setText(module.getDisplayName());

            // Update UI buttons
            navButtons.forEach((id, btn) -> {
                btn.getStyleClass().remove("nav-button-active");
                if (id.equals(moduleId)) {
                    btn.getStyleClass().add("nav-button-active");
                }
            });

            // Switch workspace view
            Node view = module.getView();
            workspaceContainer.getChildren().setAll(view);

            activeModule.onActivated();

            // Persist preference
            preferenceService.setLastModuleId(moduleId);
        });
    }

    private void setupClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
}
