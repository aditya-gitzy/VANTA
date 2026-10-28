package dev.vanta.modules.console;

import dev.vanta.module.VantaModule;
import dev.vanta.service.process.ProcessService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Sandboxed Terminal Console with:
 * - Top command bar (input box + Run + Cancel + quick chips)
 * - Rich color-coded output (Green user, Cyan path, Bold White commands, Red errors, Green directories)
 * - Full suite of built-in commands (help, cd, pwd, history, clear, touch, dir, ls, etc.)
 */
public class ConsoleModule implements VantaModule {

    private final ProcessService processService;
    private final BorderPane rootNode = new BorderPane();

    private final TextFlow terminalScreen = new TextFlow();
    private final ScrollPane scrollPane = new ScrollPane(terminalScreen);
    private final TextField commandInput = new TextField();
    private final Label promptLabel = new Label();
    private final Button runButton = new Button("▶ Run");
    private final Button cancelButton = new Button("■ Cancel");
    private final Label statusBadge = new Label("Ready");

    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    public ConsoleModule(ProcessService processService) {
        this.processService = processService;
        initializeUI();
        printWelcomeBanner();
    }

    private void initializeUI() {
        rootNode.setPadding(new Insets(12));
        rootNode.getStyleClass().add("card-panel");

        // Top Area: Banner, Quick Action Chips, and Command Bar
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        HBox topRow = createHeaderRow();
        FlowPane quickChips = createQuickChips();
        HBox commandBar = createCommandBar();

        topBox.getChildren().addAll(topRow, quickChips, commandBar);
        rootNode.setTop(topBox);

        // Center: Terminal Output Screen with syntax colors
        terminalScreen.getStyleClass().add("terminal-screen");
        terminalScreen.setPadding(new Insets(12));
        terminalScreen.setLineSpacing(3);

        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("terminal-scroll");
        scrollPane.setStyle("-fx-background: #08090d; -fx-background-color: #08090d;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        rootNode.setCenter(scrollPane);

        // Bottom: Status and housekeeping
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(8, 0, 0, 0));

        Button clearBtn = new Button("Clear Console");
        clearBtn.setOnAction(e -> terminalScreen.getChildren().clear());
        bottomBar.getChildren().add(clearBtn);
        rootNode.setBottom(bottomBar);
    }

    private HBox createHeaderRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Restricted Command Execution");
        title.getStyleClass().add("label-heading");

        Label badge = new Label("Current User Permissions • No Shell Elevation");
        badge.getStyleClass().add("app-subtitle");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBadge.getStyleClass().add("status-pill");

        row.getChildren().addAll(title, badge, spacer, statusBadge);
        return row;
    }

    private FlowPane createQuickChips() {
        FlowPane pane = new FlowPane(6, 6);
        pane.setAlignment(Pos.CENTER_LEFT);

        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        String[] samples = isWin
                ? new String[]{"help", "whoami", "hostname", "ipconfig", "dir", "cd ..", "systeminfo", "date", "ping 127.0.0.1"}
                : new String[]{"help", "whoami", "uname -a", "uptime", "ls -la", "cd ..", "df -h", "free -m", "date"};

        for (String cmd : samples) {
            Button btn = new Button(cmd);
            btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8 3 8;");
            btn.setOnAction(e -> {
                commandInput.setText(cmd);
                handleExecute();
            });
            pane.getChildren().add(btn);
        }
        return pane;
    }

    private HBox createCommandBar() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        updatePromptLabel();

        commandInput.setPromptText("Enter command (e.g. help, whoami, dir, cd, ipconfig, ping)...");
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        commandInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleExecute();
            } else if (e.getCode() == KeyCode.UP) {
                navigateHistory(-1);
            } else if (e.getCode() == KeyCode.DOWN) {
                navigateHistory(1);
            }
        });

        runButton.getStyleClass().add("button-primary");
        runButton.setOnAction(e -> handleExecute());

        cancelButton.getStyleClass().add("button-danger");
        cancelButton.setDisable(true);
        cancelButton.setOnAction(e -> handleCancel());

        box.getChildren().addAll(promptLabel, commandInput, runButton, cancelButton);
        return box;
    }

    private void updatePromptLabel() {
        Path cwd = processService.getCurrentWorkingDirectory();
        String pathStr = cwd.getFileName() != null ? cwd.getFileName().toString() : cwd.toString();
        promptLabel.setText("vanta:[" + pathStr + "] $");
        promptLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 13px;");
    }

    private void printWelcomeBanner() {
        appendStyledText("VANTA Command Console [Sandboxed]\n", "#38bdf8", true);
        appendStyledText("Direct ProcessBuilder execution • Type ", "#94a3b8", false);
        appendStyledText("help", "#4ade80", true);
        appendStyledText(" for all approved commands.\n", "#94a3b8", false);
        appendStyledText("Built-in directives: cd, pwd, clear, history, touch, dir, ls, cat, etc.\n", "#64748b", false);
        appendStyledText("--------------------------------------------------------------------------------\n", "#334155", false);
    }

    private void handleExecute() {
        String input = commandInput.getText().trim();
        if (input.isEmpty() || processService.isRunning()) {
            return;
        }

        // Add to history
        history.add(input);
        historyIndex = history.size();
        commandInput.clear();

        // Print command prompt line with colors
        Path cwd = processService.getCurrentWorkingDirectory();
        String pathStr = cwd.getFileName() != null ? cwd.getFileName().toString() : cwd.toString();

        appendStyledText("\n" + System.getProperty("user.name") + "@vanta", "#4ade80", true);
        appendStyledText(":", "#64748b", false);
        appendStyledText("[" + pathStr + "]", "#38bdf8", true);
        appendStyledText(" $ ", "#fbbf24", true);
        appendStyledText(input + "\n", "#ffffff", true);

        setRunningState(true, "Running: " + input);

        processService.execute(
                input,
                (line, isErr) -> Platform.runLater(() -> {
                    if ("__CLEAR_BUFFER__".equals(line)) {
                        terminalScreen.getChildren().clear();
                        return;
                    }
                    if (isErr) {
                        appendStyledText(line + "\n", "#f87171", false);
                    } else {
                        formatAndAppendLine(line);
                    }
                }),
                status -> Platform.runLater(() -> {
                    statusBadge.setText(status);
                    updatePromptLabel();
                })
        ).whenComplete((exitCode, throwable) -> Platform.runLater(() -> {
            setRunningState(false, throwable == null ? "Exit Code: " + exitCode : "Error");
            if (throwable != null) {
                appendStyledText("[Error] " + throwable.getMessage() + "\n", "#f87171", true);
            }
            updatePromptLabel();
            scrollToBottom();
            commandInput.requestFocus();
        }));
    }

    private void formatAndAppendLine(String line) {
        if (line.startsWith("<DIR>") || line.contains("<DIR>")) {
            appendStyledText(line + "\n", "#4ade80", true);
        } else if (line.startsWith("drw") || line.startsWith("d--") || line.startsWith("d-r")) {
            appendStyledText(line + "\n", "#4ade80", true);
        } else if (line.startsWith("===") || line.startsWith("COMMAND")) {
            appendStyledText(line + "\n", "#38bdf8", true);
        } else if (line.toLowerCase().contains("reply from") || line.toLowerCase().contains("64 bytes from")) {
            appendStyledText(line + "\n", "#34d399", false);
        } else {
            appendStyledText(line + "\n", "#e2e8f0", false);
        }
    }

    private void handleCancel() {
        processService.cancelRunningProcess();
        appendStyledText("\n[Process terminated by user]\n", "#fbbf24", true);
        setRunningState(false, "Terminated");
        commandInput.requestFocus();
    }

    private void setRunningState(boolean running, String status) {
        runButton.setDisable(running);
        cancelButton.setDisable(!running);
        statusBadge.setText(status);
    }

    private void appendStyledText(String text, String hexColor, boolean bold) {
        Text t = new Text(text);
        t.setFont(Font.font("Consolas", 12.5));
        t.setStyle("-fx-fill: " + hexColor + ";" + (bold ? " -fx-font-weight: bold;" : ""));
        terminalScreen.getChildren().add(t);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void navigateHistory(int direction) {
        if (history.isEmpty()) return;
        historyIndex += direction;
        if (historyIndex < 0) {
            historyIndex = 0;
        } else if (historyIndex >= history.size()) {
            historyIndex = history.size();
            commandInput.clear();
            return;
        }
        commandInput.setText(history.get(historyIndex));
        commandInput.positionCaret(commandInput.getText().length());
    }

    @Override
    public String getId() {
        return "console";
    }

    @Override
    public String getDisplayName() {
        return "Console";
    }

    @Override
    public String getDescription() {
        return "Command console with quick chips, multicolor output, and sandboxed ProcessBuilder execution";
    }

    @Override
    public String getIconGlyph() {
        return "💻";
    }

    @Override
    public Node getView() {
        return rootNode;
    }

    @Override
    public void onActivated() {
        Platform.runLater(commandInput::requestFocus);
    }
}
