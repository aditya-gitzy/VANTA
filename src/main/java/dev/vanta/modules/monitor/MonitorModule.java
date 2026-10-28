package dev.vanta.modules.monitor;

import dev.vanta.module.VantaModule;
import dev.vanta.service.platform.DiskPartition;
import dev.vanta.service.platform.MemoryMetrics;
import dev.vanta.service.platform.PlatformService;
import dev.vanta.service.platform.ProcessInfo;
import dev.vanta.service.platform.SystemSnapshot;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * System Monitor Module providing live hardware and kernel telemetry.
 */
public class MonitorModule implements VantaModule {

    private final PlatformService platformService;
    private final BorderPane rootNode = new BorderPane();

    // CPU Card
    private final Label cpuPercentLabel = new Label("0.0%");
    private final ProgressBar cpuBar = new ProgressBar(0.0);
    private final Label cpuDetailsLabel = new Label("Detecting processor...");

    // Memory Card
    private final Label memPercentLabel = new Label("0.0%");
    private final ProgressBar memBar = new ProgressBar(0.0);
    private final Label memDetailsLabel = new Label("Detecting RAM...");

    // System Info Card
    private final Label osDetailsLabel = new Label("Detecting OS...");
    private final Label uptimeLabel = new Label("Uptime: ...");
    private final Label adapterBadge = new Label();

    // Tables
    private final TableView<DiskPartition> diskTable = new TableView<>();
    private final TableView<ProcessInfo> processTable = new TableView<>();

    private final Button togglePauseBtn = new Button("⏸ Pause Telemetry");
    private boolean isPaused = false;
    private final Consumer<SystemSnapshot> telemetryListener;

    public MonitorModule(PlatformService platformService) {
        this.platformService = platformService;
        this.telemetryListener = this::onSnapshotReceived;

        initializeUI();
        platformService.addListener(telemetryListener);
    }

    private void initializeUI() {
        rootNode.setPadding(new Insets(12));
        rootNode.getStyleClass().add("card-panel");

        // Top Summary Cards
        VBox topBox = new VBox(12);
        topBox.setPadding(new Insets(0, 0, 12, 0));

        HBox banner = createHeaderBanner();
        GridPane metricsGrid = createMetricsGrid();

        topBox.getChildren().addAll(banner, metricsGrid);
        rootNode.setTop(topBox);

        // Center Tab Pane (Storage & Processes)
        TabPane tabPane = createTabPane();
        rootNode.setCenter(tabPane);
    }

    private HBox createHeaderBanner() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("System Resource Monitor");
        title.getStyleClass().add("label-heading");

        adapterBadge.getStyleClass().add("app-badge");
        adapterBadge.setText(platformService.getAdapter().getName());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        togglePauseBtn.setOnAction(e -> {
            isPaused = !isPaused;
            togglePauseBtn.setText(isPaused ? "▶ Resume Telemetry" : "⏸ Pause Telemetry");
        });

        box.getChildren().addAll(title, adapterBadge, spacer, togglePauseBtn);
        return box;
    }

    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        // CPU Metric Card
        VBox cpuCard = createMetricCard("CPU Utilization", cpuPercentLabel, cpuBar, cpuDetailsLabel);
        // Memory Metric Card
        VBox memCard = createMetricCard("Memory (RAM)", memPercentLabel, memBar, memDetailsLabel);
        // Host Info Card
        VBox hostCard = createHostInfoCard();

        grid.add(cpuCard, 0, 0);
        grid.add(memCard, 1, 0);
        grid.add(hostCard, 2, 0);

        GridPane.setHgrow(cpuCard, Priority.ALWAYS);
        GridPane.setHgrow(memCard, Priority.ALWAYS);
        GridPane.setHgrow(hostCard, Priority.ALWAYS);

        return grid;
    }

    private VBox createMetricCard(String title, Label percentLabel, ProgressBar bar, Label detailsLabel) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card-panel");
        card.setStyle("-fx-background-color: #1a1e29;");
        card.setPadding(new Insets(12));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-subtitle");

        percentLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);

        detailsLabel.getStyleClass().add("app-subtitle");
        detailsLabel.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(titleLabel, percentLabel, bar, detailsLabel);
        return card;
    }

    private VBox createHostInfoCard() {
        VBox card = new VBox(6);
        card.getStyleClass().add("card-panel");
        card.setStyle("-fx-background-color: #1a1e29;");
        card.setPadding(new Insets(12));

        Label title = new Label("Host Environment");
        title.getStyleClass().add("app-subtitle");

        osDetailsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");

        uptimeLabel.getStyleClass().add("app-subtitle");
        uptimeLabel.setStyle("-fx-font-size: 11px;");

        Label kernelLabel = new Label("Kernel: " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        kernelLabel.getStyleClass().add("app-subtitle");
        kernelLabel.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(title, osDetailsLabel, uptimeLabel, kernelLabel);
        return card;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Disks Tab
        setupDiskTable();
        Tab diskTab = new Tab("Storage Partitions", diskTable);

        // Processes Tab
        setupProcessTable();
        Tab procTab = new Tab("Active Processes", processTable);

        tabPane.getTabs().addAll(diskTab, procTab);
        return tabPane;
    }

    private void setupDiskTable() {
        TableColumn<DiskPartition, String> mountCol = new TableColumn<>("Mount / Drive");
        mountCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().mountPoint()));
        mountCol.setPrefWidth(180);

        TableColumn<DiskPartition, String> totalCol = new TableColumn<>("Total Space");
        totalCol.setCellValueFactory(cell -> new SimpleStringProperty(MemoryMetrics.formatBytes(cell.getValue().totalBytes())));
        totalCol.setPrefWidth(120);

        TableColumn<DiskPartition, String> freeCol = new TableColumn<>("Free Space");
        freeCol.setCellValueFactory(cell -> new SimpleStringProperty(MemoryMetrics.formatBytes(cell.getValue().freeBytes())));
        freeCol.setPrefWidth(120);

        TableColumn<DiskPartition, String> usageCol = new TableColumn<>("Used %");
        usageCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f %%", cell.getValue().usagePercent())));
        usageCol.setPrefWidth(100);

        diskTable.getColumns().addAll(mountCol, totalCol, freeCol, usageCol);
        diskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupProcessTable() {
        TableColumn<ProcessInfo, String> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().pid())));
        pidCol.setPrefWidth(90);

        TableColumn<ProcessInfo, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().user()));
        userCol.setPrefWidth(120);

        TableColumn<ProcessInfo, String> cmdCol = new TableColumn<>("Command");
        cmdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().command()));
        cmdCol.setPrefWidth(400);

        TableColumn<ProcessInfo, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().state()));
        stateCol.setPrefWidth(100);

        processTable.getColumns().addAll(pidCol, userCol, cmdCol, stateCol);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void onSnapshotReceived(SystemSnapshot snapshot) {
        if (isPaused || snapshot == null) return;

        Platform.runLater(() -> {
            // CPU
            double cpuVal = snapshot.cpu().totalUsagePercent();
            cpuPercentLabel.setText(String.format("%.1f%%", cpuVal));
            cpuBar.setProgress(cpuVal / 100.0);
            cpuDetailsLabel.setText(snapshot.cpu().availableProcessors() + " Cores • " + snapshot.cpu().modelName());

            // Memory
            double memVal = snapshot.memory().usagePercent();
            memPercentLabel.setText(String.format("%.1f%%", memVal));
            memBar.setProgress(memVal / 100.0);
            memDetailsLabel.setText(String.format("%s used of %s",
                    MemoryMetrics.formatBytes(snapshot.memory().usedBytes()),
                    MemoryMetrics.formatBytes(snapshot.memory().totalBytes())));

            // Host
            osDetailsLabel.setText(snapshot.osName() + " " + snapshot.kernelVersion());
            uptimeLabel.setText("Uptime: " + snapshot.uptime());

            // Disks & Processes
            diskTable.getItems().setAll(snapshot.disks());
            processTable.getItems().setAll(snapshot.topProcesses());
        });
    }

    @Override
    public String getId() {
        return "monitor";
    }

    @Override
    public String getDisplayName() {
        return "Monitor";
    }

    @Override
    public String getDescription() {
        return "Direct telemetry for CPU, Memory, Disk storage, and Processes";
    }

    @Override
    public String getIconGlyph() {
        return "📊";
    }

    @Override
    public Node getView() {
        return rootNode;
    }
}
