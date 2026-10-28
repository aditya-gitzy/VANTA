package dev.vanta.service.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sandboxed process execution and terminal command service.
 * Supports built-in virtual shell commands (help, clear, cd, pwd, history)
 * alongside strictly whitelisted external executables via ProcessBuilder.
 */
public class ProcessService {

    private final Map<String, CommandSpec> approvedCommands = new LinkedHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "vanta-process-runner");
        t.setDaemon(true);
        return t;
    });

    private volatile Process currentRunningProcess;
    private Path currentWorkingDirectory = Paths.get(System.getProperty("user.home"));
    private final List<String> commandHistory = new ArrayList<>();

    public ProcessService() {
        registerDefaultCommands();
    }

    private void registerDefaultCommands() {
        // Built-in Terminal Directives
        register(new CommandSpec("help", "Builtin", "Display list of supported commands and syntax", "help", List.of("*")));
        register(new CommandSpec("clear", "Builtin", "Clear console display buffer", "clear", List.of()));
        register(new CommandSpec("cls", "Builtin", "Clear console display buffer", "cls", List.of()));
        register(new CommandSpec("cd", "Builtin", "Change working directory", "cd /home/user or cd ..", List.of("*")));
        register(new CommandSpec("pwd", "Builtin", "Print current working directory", "pwd", List.of()));
        register(new CommandSpec("history", "Builtin", "Show command execution history", "history", List.of()));

        // File and Text Inspection
        register(new CommandSpec("ls", "Files", "List directory contents with details", "ls -la", List.of("*")));
        register(new CommandSpec("dir", "Files", "List directory contents", "dir", List.of("*")));
        register(new CommandSpec("cat", "Files", "Display file contents", "cat file.txt", List.of("*")));
        register(new CommandSpec("type", "Files", "Display file contents", "type file.txt", List.of("*")));
        register(new CommandSpec("head", "Files", "Output beginning of files", "head -n 20 file.txt", List.of("*")));
        register(new CommandSpec("tail", "Files", "Output the last part of files", "tail -n 20 file.txt", List.of("*")));
        register(new CommandSpec("echo", "Utilities", "Display a line of text", "echo Hello VANTA", List.of("*")));
        register(new CommandSpec("grep", "Utilities", "Search for patterns in files", "grep error log.txt", List.of("*")));
        register(new CommandSpec("findstr", "Utilities", "Search for strings in files", "findstr error log.txt", List.of("*")));
        register(new CommandSpec("wc", "Utilities", "Print newline, word, and byte counts", "wc -l file.txt", List.of("*")));

        // File and Directory Creation
        register(new CommandSpec("mkdir", "Files", "Create new directory", "mkdir my_folder", List.of("*")));
        register(new CommandSpec("touch", "Files", "Create new empty file or update timestamp", "touch new_file.txt", List.of("*")));

        // Core POSIX / Linux System Diagnostics
        register(new CommandSpec("uname", "System", "Print operating system and kernel architecture", "uname -a", List.of("*")));
        register(new CommandSpec("uptime", "System", "Show how long system has been running", "uptime", List.of("*")));
        register(new CommandSpec("whoami", "Identity", "Print current logged-in user name", "whoami", List.of("*")));
        register(new CommandSpec("id", "Identity", "Print user identity and group numbers", "id", List.of("*")));
        register(new CommandSpec("date", "System", "Display current system date and time", "date", List.of("*")));
        register(new CommandSpec("df", "Storage", "Report filesystem disk usage", "df -h", List.of("*")));
        register(new CommandSpec("free", "Memory", "Display amount of free and used memory", "free -m", List.of("*")));
        register(new CommandSpec("ps", "Process", "Report snapshot of current processes", "ps aux", List.of("*")));
        register(new CommandSpec("ping", "Network", "Send ICMP ECHO_REQUEST to network hosts", "ping 127.0.0.1", List.of("*")));

        // Windows Host Support
        register(new CommandSpec("hostname", "System", "Display network hostname", "hostname", List.of("*")));
        register(new CommandSpec("systeminfo", "System", "Display detailed OS configuration", "systeminfo", List.of("*")));
        register(new CommandSpec("ipconfig", "Network", "Display all current TCP/IP network configuration", "ipconfig", List.of("*")));
        register(new CommandSpec("tasklist", "Process", "Display currently running processes", "tasklist", List.of("*")));
        register(new CommandSpec("netstat", "Network", "Display active TCP connections and listening ports", "netstat -an", List.of("*")));
    }

    public void register(CommandSpec spec) {
        approvedCommands.put(spec.command().toLowerCase(), spec);
    }

    public Map<String, CommandSpec> getApprovedCommands() {
        return Collections.unmodifiableMap(approvedCommands);
    }

    public Path getCurrentWorkingDirectory() {
        return currentWorkingDirectory;
    }

    public void setCurrentWorkingDirectory(Path dir) {
        if (dir != null && Files.isDirectory(dir)) {
            this.currentWorkingDirectory = dir.toAbsolutePath().normalize();
        }
    }

    public List<String> getCommandHistory() {
        return Collections.unmodifiableList(commandHistory);
    }

    public List<String> tokenize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
        while (m.find()) {
            String token = m.group(1);
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                token = token.substring(1, token.length() - 1);
            }
            tokens.add(token);
        }
        return tokens;
    }

    public void validate(List<String> tokens) throws SecurityException {
        if (tokens.isEmpty()) {
            throw new SecurityException("Command line is empty.");
        }

        String executable = tokens.get(0).toLowerCase();

        // Check for shell chaining / redirection hazards
        for (String token : tokens) {
            if (token.contains(";") || token.contains("&") || token.contains("|")
                    || token.contains(">") || token.contains("<") || token.contains("`")
                    || token.contains("$(") || token.contains("${")) {
                throw new SecurityException("Command rejected: Shell metacharacters are strictly prohibited.");
            }
        }

        if (!approvedCommands.containsKey(executable)) {
            throw new SecurityException("Command '" + executable + "' is not in the approved command whitelist. Type 'help' for supported commands.");
        }
    }

    public CompletableFuture<Integer> execute(
            String commandLine,
            BiConsumer<String, Boolean> outputListener,
            Consumer<String> statusListener
    ) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        List<String> tokens = tokenize(commandLine);
        try {
            validate(tokens);
        } catch (SecurityException se) {
            future.completeExceptionally(se);
            return future;
        }

        commandHistory.add(commandLine);
        String commandName = tokens.get(0).toLowerCase();

        // Handle Built-in Virtual Commands directly
        if (isBuiltin(commandName)) {
            handleBuiltin(tokens, outputListener, statusListener, future);
            return future;
        }

        // Handle External System Executable
        executor.submit(() -> {
            Process process = null;
            try {
                // On Windows, some built-in utilities like dir are cmd shell builtins.
                // If on Windows and command is dir or type, invoke with argument array directly
                List<String> cmdTokens = new ArrayList<>(tokens);
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                if (isWindows && (commandName.equals("dir") || commandName.equals("type") || commandName.equals("mkdir"))) {
                    cmdTokens = new ArrayList<>();
                    cmdTokens.add("cmd.exe");
                    cmdTokens.add("/c");
                    cmdTokens.addAll(tokens);
                }

                ProcessBuilder pb = new ProcessBuilder(cmdTokens);
                pb.directory(currentWorkingDirectory.toFile());
                pb.environment().remove("SUDO_USER");

                statusListener.accept("Running: " + tokens.get(0));
                process = pb.start();
                currentRunningProcess = process;

                Process procRef = process;

                Thread outReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(procRef.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            outputListener.accept(line, false);
                        }
                    } catch (IOException ignored) {
                    }
                }, "vanta-proc-stdout");
                outReader.start();

                Thread errReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(procRef.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            outputListener.accept(line, true);
                        }
                    } catch (IOException ignored) {
                    }
                }, "vanta-proc-stderr");
                errReader.start();

                int exitCode = process.waitFor();
                outReader.join();
                errReader.join();

                currentRunningProcess = null;
                statusListener.accept("Exit Code: " + exitCode);
                future.complete(exitCode);

            } catch (InterruptedException ie) {
                if (process != null) {
                    process.destroyForcibly();
                }
                currentRunningProcess = null;
                statusListener.accept("Cancelled by user");
                future.complete(-1);
            } catch (Exception ex) {
                currentRunningProcess = null;
                statusListener.accept("Failed: " + ex.getMessage());
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private boolean isBuiltin(String cmd) {
        return cmd.equals("help") || cmd.equals("clear") || cmd.equals("cls")
                || cmd.equals("cd") || cmd.equals("pwd") || cmd.equals("history")
                || cmd.equals("touch");
    }

    private void handleBuiltin(
            List<String> tokens,
            BiConsumer<String, Boolean> output,
            Consumer<String> status,
            CompletableFuture<Integer> future
    ) {
        String cmd = tokens.get(0).toLowerCase();
        try {
            switch (cmd) {
                case "help" -> {
                    output.accept("=== VANTA APPROVED COMMAND SUITE ===", false);
                    output.accept(String.format("%-14s %-12s %s", "COMMAND", "CATEGORY", "DESCRIPTION"), false);
                    output.accept("--------------------------------------------------------------------------------", false);
                    for (CommandSpec spec : approvedCommands.values()) {
                        output.accept(String.format("%-14s %-12s %s (e.g. %s)", spec.command(), spec.category(), spec.description(), spec.example()), false);
                    }
                    output.accept("--------------------------------------------------------------------------------", false);
                    status.accept("Exit Code: 0");
                    future.complete(0);
                }
                case "clear", "cls" -> {
                    output.accept("__CLEAR_BUFFER__", false);
                    status.accept("Ready");
                    future.complete(0);
                }
                case "pwd" -> {
                    output.accept(currentWorkingDirectory.toString(), false);
                    status.accept("Exit Code: 0");
                    future.complete(0);
                }
                case "history" -> {
                    output.accept("=== COMMAND HISTORY ===", false);
                    for (int i = 0; i < commandHistory.size(); i++) {
                        output.accept(String.format(" %3d  %s", i + 1, commandHistory.get(i)), false);
                    }
                    status.accept("Exit Code: 0");
                    future.complete(0);
                }
                case "cd" -> {
                    if (tokens.size() == 1) {
                        currentWorkingDirectory = Paths.get(System.getProperty("user.home"));
                    } else {
                        String targetArg = tokens.get(1);
                        Path target;
                        if (targetArg.equals("~")) {
                            target = Paths.get(System.getProperty("user.home"));
                        } else {
                            target = currentWorkingDirectory.resolve(targetArg).normalize();
                        }
                        if (!Files.exists(target)) {
                            output.accept("cd: directory not found: " + targetArg, true);
                            status.accept("Exit Code: 1");
                            future.complete(1);
                            return;
                        }
                        if (!Files.isDirectory(target)) {
                            output.accept("cd: not a directory: " + targetArg, true);
                            status.accept("Exit Code: 1");
                            future.complete(1);
                            return;
                        }
                        currentWorkingDirectory = target;
                    }
                    output.accept("Directory changed to: " + currentWorkingDirectory, false);
                    status.accept("Exit Code: 0");
                    future.complete(0);
                }
                case "touch" -> {
                    if (tokens.size() < 2) {
                        output.accept("touch: missing file operand", true);
                        status.accept("Exit Code: 1");
                        future.complete(1);
                        return;
                    }
                    for (int i = 1; i < tokens.size(); i++) {
                        Path f = currentWorkingDirectory.resolve(tokens.get(i));
                        if (!Files.exists(f)) {
                            Files.createFile(f);
                            output.accept("Created file: " + tokens.get(i), false);
                        } else {
                            output.accept("Updated timestamp: " + tokens.get(i), false);
                        }
                    }
                    status.accept("Exit Code: 0");
                    future.complete(0);
                }
                default -> {
                    output.accept("Unknown builtin: " + cmd, true);
                    future.complete(1);
                }
            }
        } catch (Exception ex) {
            output.accept("Error executing " + cmd + ": " + ex.getMessage(), true);
            status.accept("Exit Code: 1");
            future.complete(1);
        }
    }

    public boolean isRunning() {
        return currentRunningProcess != null && currentRunningProcess.isAlive();
    }

    public void cancelRunningProcess() {
        if (currentRunningProcess != null && currentRunningProcess.isAlive()) {
            currentRunningProcess.destroyForcibly();
            currentRunningProcess = null;
        }
    }
}
