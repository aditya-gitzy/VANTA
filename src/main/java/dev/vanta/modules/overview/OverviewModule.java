package dev.vanta.modules.overview;

import dev.vanta.VantaApp;
import dev.vanta.module.VantaModule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Overview / Dashboard module welcoming the user and presenting fast launcher shortcuts.
 */
public class OverviewModule implements VantaModule {

    private final VBox rootNode = new VBox(20);
    private final Consumer<String> navigationHandler;

    public OverviewModule(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        initializeView();
    }

    private void initializeView() {
        rootNode.setPadding(new Insets(24));
        rootNode.setAlignment(Pos.TOP_LEFT);

        // Header Title
        VBox headerBox = new VBox(6);
        Label title = new Label("VANTA Control Hub");
        title.getStyleClass().add("label-heading");
        title.setStyle("-fx-font-size: 20px; -fx-text-fill: #f0f6fc;");

        Label subtitle = new Label("Mission control interface for Linux operations, system diagnostics, and file orchestration.");
        subtitle.getStyleClass().add("label-subheading");
        headerBox.getChildren().addAll(title, subtitle);

        // Quick Launch Action Cards Grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        VBox filesCard = createActionCard(
                "📁", "File Browser",
                "Explore local directory trees, inspect file properties, and safely create, rename, copy, or remove files with confirmation safeguards.",
                "files"
        );

        VBox consoleCard = createActionCard(
                "💻", "Command Console",
                "Execute approved Linux and system diagnostic commands with isolated argument parsing, live streaming output, and process cancellation.",
                "console"
        );

        VBox monitorCard = createActionCard(
                "📊", "System Monitor",
                "Direct low-overhead hardware telemetry for CPU utilization, RAM distribution, partition capacities, and top active processes.",
                "monitor"
        );

        grid.add(filesCard, 0, 0);
        grid.add(consoleCard, 1, 0);
        grid.add(monitorCard, 2, 0);

        // Architecture Banner
        VBox infoCard = new VBox(8);
        infoCard.getStyleClass().add("card-panel");
        infoCard.setMaxWidth(Double.MAX_VALUE);

        Label infoTitle = new Label("Security & Isolation Architecture");
        infoTitle.getStyleClass().add("label-heading");

        Label infoDesc = new Label(
                "• Strict Command Sandboxing: No arbitrary shell strings (sh -c / bash -c prohibited). Command arguments are validated and tokenized.\n" +
                "• Privilege Containment: Executes strictly with current user permissions; never requests or retains elevated root privileges.\n" +
                "• Native Performance: Low-memory Java 21 LTS runtime with asynchronous non-blocking background workers."
        );
        infoDesc.getStyleClass().add("label-subheading");
        infoDesc.setStyle("-fx-line-spacing: 4px;");

        infoCard.getChildren().addAll(infoTitle, infoDesc);

        rootNode.getChildren().addAll(headerBox, grid, infoCard);
    }

    private VBox createActionCard(String icon, String titleText, String descText, String targetModuleId) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-panel");
        card.setPrefWidth(260);
        card.setMinHeight(160);
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px;");

        Label title = new Label(titleText);
        title.getStyleClass().add("label-heading");

        topRow.getChildren().addAll(iconLabel, title);

        Label desc = new Label(descText);
        desc.getStyleClass().add("label-subheading");
        desc.setWrapText(true);
        VBox.setVgrow(desc, Priority.ALWAYS);

        Label openLink = new Label("Launch Module →");
        openLink.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px;");

        card.getChildren().addAll(topRow, desc, openLink);

        card.setOnMouseClicked(e -> {
            if (navigationHandler != null) {
                navigationHandler.accept(targetModuleId);
            }
        });

        // Hover highlight
        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #38bdf8; -fx-background-color: #1a1f2c;"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    @Override
    public String getId() {
        return "overview";
    }

    @Override
    public String getDisplayName() {
        return "Overview";
    }

    @Override
    public String getDescription() {
        return "Welcome dashboard and module status overview";
    }

    @Override
    public String getIconGlyph() {
        return "⬚";
    }

    @Override
    public Node getView() {
        return rootNode;
    }
}
