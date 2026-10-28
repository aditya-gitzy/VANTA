package dev.vanta.modules.files;

import dev.vanta.module.VantaModule;
import dev.vanta.service.fs.FileItem;
import dev.vanta.service.fs.FileService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * File Browser Module providing safe, visual exploration and file management.
 */
public class FilesModule implements VantaModule {

    private final FileService fileService;
    private final BorderPane rootNode = new BorderPane();

    private final TextField pathField = new TextField();
    private final TableView<FileItem> fileTable = new TableView<>();
    private final Label statusLabel = new Label("Ready");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private Path currentDirectory;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FilesModule(FileService fileService) {
        this.fileService = fileService;
        this.currentDirectory = Paths.get(System.getProperty("user.home"));

        initializeUI();
        loadDirectory(currentDirectory);
    }

    private void initializeUI() {
        rootNode.setPadding(new Insets(12));
        rootNode.getStyleClass().add("card-panel");

        // Top Navigation and Actions Bar
        VBox topContainer = new VBox(10);
        topContainer.setPadding(new Insets(0, 0, 10, 0));

        HBox addressBar = createAddressBar();
        HBox actionBar = createActionBar();
        topContainer.getChildren().addAll(addressBar, actionBar);
        rootNode.setTop(topContainer);

        // Center Table View
        setupTable();
        rootNode.setCenter(fileTable);

        // Bottom Status Bar
        HBox statusBar = createStatusBar();
        rootNode.setBottom(statusBar);
    }

    private HBox createAddressBar() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Button upBtn = new Button("↑ Up");
        upBtn.setOnAction(e -> navigateUp());

        Button homeBtn = new Button("⌂ Home");
        homeBtn.setOnAction(e -> navigateTo(Paths.get(System.getProperty("user.home"))));

        pathField.setText(currentDirectory.toAbsolutePath().toString());
        HBox.setHgrow(pathField, Priority.ALWAYS);
        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String input = pathField.getText().trim();
                if (!input.isEmpty()) {
                    navigateTo(Paths.get(input));
                }
            }
        });

        Button goBtn = new Button("Go");
        goBtn.setOnAction(e -> {
            String input = pathField.getText().trim();
            if (!input.isEmpty()) {
                navigateTo(Paths.get(input));
            }
        });

        Button refreshBtn = new Button("⟳ Refresh");
        refreshBtn.setOnAction(e -> loadDirectory(currentDirectory));

        box.getChildren().addAll(upBtn, homeBtn, pathField, goBtn, refreshBtn);
        return box;
    }

    private HBox createActionBar() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Button newFolderBtn = new Button("+ New Folder");
        newFolderBtn.setOnAction(e -> handleCreateFolder());

        Button newFileBtn = new Button("+ New File");
        newFileBtn.setOnAction(e -> handleCreateFile());

        Button renameBtn = new Button("Rename");
        renameBtn.setOnAction(e -> handleRename());

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("button-danger");
        deleteBtn.setOnAction(e -> handleDelete());

        box.getChildren().addAll(newFolderBtn, newFileBtn, renameBtn, deleteBtn);
        return box;
    }

    private void setupTable() {
        TableColumn<FileItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> {
            FileItem item = cell.getValue();
            String prefix = item.isDirectory() ? "📁  " : "📄  ";
            return new SimpleStringProperty(prefix + item.name());
        });
        nameCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    FileItem fileItem = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fileItem != null && fileItem.isDirectory()) {
                        setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e2e8f0;");
                    }
                }
            }
        });
        nameCol.setPrefWidth(340);

        TableColumn<FileItem, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFormattedSize()));
        sizeCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("<DIR>".equals(item)) {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #94a3b8;");
                    }
                }
            }
        });
        sizeCol.setPrefWidth(110);

        TableColumn<FileItem, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory(cell -> {
            FileItem item = cell.getValue();
            if (item.lastModified() == null) return new SimpleStringProperty("—");
            String formatted = item.lastModified().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(dateFormatter);
            return new SimpleStringProperty(formatted);
        });
        dateCol.setPrefWidth(160);

        TableColumn<FileItem, String> permCol = new TableColumn<>("Permissions");
        permCol.setCellValueFactory(cell -> {
            FileItem item = cell.getValue();
            String perm = (item.isReadable() ? "r" : "-") + (item.isWritable() ? "w" : "-");
            return new SimpleStringProperty(perm);
        });
        permCol.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #a78bfa; -fx-font-family: monospace; -fx-font-weight: bold;");
                }
            }
        });
        permCol.setPrefWidth(100);

        fileTable.getColumns().addAll(nameCol, sizeCol, dateCol, permCol);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Double-click navigation
        fileTable.setRowFactory(tv -> {
            TableRow<FileItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    FileItem item = row.getItem();
                    if (item.isDirectory()) {
                        navigateTo(item.path());
                    }
                }
            });
            return row;
        });
    }

    private HBox createStatusBar() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 0, 0));

        spinner.setPrefSize(14, 14);
        spinner.setVisible(false);

        statusLabel.getStyleClass().add("app-subtitle");

        box.getChildren().addAll(spinner, statusLabel);
        return box;
    }

    public void navigateTo(Path dir) {
        currentDirectory = dir.toAbsolutePath().normalize();
        pathField.setText(currentDirectory.toString());
        loadDirectory(currentDirectory);
    }

    public void navigateUp() {
        Path parent = currentDirectory.getParent();
        if (parent != null) {
            navigateTo(parent);
        }
    }

    private void loadDirectory(Path dir) {
        spinner.setVisible(true);
        statusLabel.setText("Reading " + dir.getFileName() + "...");

        Task<List<FileItem>> task = new Task<>() {
            @Override
            protected List<FileItem> call() throws Exception {
                return fileService.listDirectory(dir);
            }
        };

        task.setOnSucceeded(e -> {
            spinner.setVisible(false);
            List<FileItem> items = task.getValue();
            fileTable.getItems().setAll(items);
            statusLabel.setText("Loaded " + items.size() + " items in " + dir);
        });

        task.setOnFailed(e -> {
            spinner.setVisible(false);
            Throwable err = task.getException();
            showError("Directory Read Error", err.getMessage());
            statusLabel.setText("Error: " + err.getMessage());
        });

        Thread worker = new Thread(task, "vanta-fs-reader");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleCreateFolder() {
        TextInputDialog dialog = new TextInputDialog("NewFolder");
        dialog.setTitle("Create Folder");
        dialog.setHeaderText("Create a new directory in:\n" + currentDirectory);
        dialog.setContentText("Folder Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                fileService.createDirectory(currentDirectory, name);
                loadDirectory(currentDirectory);
            } catch (Exception ex) {
                showError("Folder Creation Failed", ex.getMessage());
            }
        });
    }

    private void handleCreateFile() {
        TextInputDialog dialog = new TextInputDialog("new_file.txt");
        dialog.setTitle("Create File");
        dialog.setHeaderText("Create a new file in:\n" + currentDirectory);
        dialog.setContentText("File Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                fileService.createFile(currentDirectory, name);
                loadDirectory(currentDirectory);
            } catch (Exception ex) {
                showError("File Creation Failed", ex.getMessage());
            }
        });
    }

    private void handleRename() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Rename", "Please select a file or folder from the list first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.name());
        dialog.setTitle("Rename Item");
        dialog.setHeaderText("Rename: " + selected.name());
        dialog.setContentText("New Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            try {
                fileService.rename(selected.path(), newName);
                loadDirectory(currentDirectory);
            } catch (Exception ex) {
                showError("Rename Failed", ex.getMessage());
            }
        });
    }

    private void handleDelete() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Delete", "Please select an item to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Are you sure you want to permanently delete this " + (selected.isDirectory() ? "folder" : "file") + "?");
        alert.setContentText(selected.path().toString());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                fileService.delete(selected.path());
                loadDirectory(currentDirectory);
            } catch (Exception ex) {
                showError("Deletion Failed", ex.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public String getId() {
        return "files";
    }

    @Override
    public String getDisplayName() {
        return "Files";
    }

    @Override
    public String getDescription() {
        return "Browse directories, view file details, and manage local filesystem";
    }

    @Override
    public String getIconGlyph() {
        return "📁";
    }

    @Override
    public Node getView() {
        return rootNode;
    }

    @Override
    public void onActivated() {
        loadDirectory(currentDirectory);
    }
}
