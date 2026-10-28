package dev.vanta.service.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * NIO-based Filesystem service isolating all file I/O operations from the JavaFX UI thread.
 */
public class FileService {

    public List<FileItem> listDirectory(Path dir) throws IOException {
        Objects.requireNonNull(dir, "Directory path cannot be null");
        if (!Files.exists(dir)) {
            throw new IOException("Directory does not exist: " + dir);
        }
        if (!Files.isDirectory(dir)) {
            throw new IOException("Path is not a directory: " + dir);
        }
        if (!Files.isReadable(dir)) {
            throw new IOException("Permission denied: Cannot read directory " + dir);
        }

        List<FileItem> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    items.add(new FileItem(
                            entry,
                            entry.getFileName() != null ? entry.getFileName().toString() : entry.toString(),
                            attrs.isDirectory(),
                            attrs.isDirectory() ? 0 : attrs.size(),
                            attrs.lastModifiedTime(),
                            Files.isReadable(entry),
                            Files.isWritable(entry)
                    ));
                } catch (IOException e) {
                    // Collect item with fallback values if attributes cannot be read
                    boolean isDir = Files.isDirectory(entry);
                    items.add(new FileItem(
                            entry,
                            entry.getFileName() != null ? entry.getFileName().toString() : entry.toString(),
                            isDir,
                            0,
                            null,
                            Files.isReadable(entry),
                            Files.isWritable(entry)
                    ));
                }
            }
        }

        // Sort directories first, then alphabetical by name
        items.sort(Comparator
                .comparing(FileItem::isDirectory).reversed()
                .thenComparing(item -> item.name().toLowerCase()));

        return items;
    }

    public Path createDirectory(Path parentDir, String dirName) throws IOException {
        validateName(dirName);
        Path target = parentDir.resolve(dirName);
        if (Files.exists(target)) {
            throw new IOException("A file or directory with name '" + dirName + "' already exists.");
        }
        return Files.createDirectory(target);
    }

    public Path createFile(Path parentDir, String fileName) throws IOException {
        validateName(fileName);
        Path target = parentDir.resolve(fileName);
        if (Files.exists(target)) {
            throw new IOException("File '" + fileName + "' already exists.");
        }
        return Files.createFile(target);
    }

    public Path rename(Path target, String newName) throws IOException {
        validateName(newName);
        if (!Files.exists(target)) {
            throw new IOException("Target file does not exist: " + target);
        }
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Cannot rename root path.");
        }
        Path destination = parent.resolve(newName);
        if (Files.exists(destination)) {
            throw new IOException("Destination already exists: " + destination);
        }
        return Files.move(target, destination);
    }

    public void copy(Path source, Path destinationDir) throws IOException {
        Objects.requireNonNull(source, "Source path cannot be null");
        Objects.requireNonNull(destinationDir, "Destination directory cannot be null");

        if (!Files.exists(source)) {
            throw new IOException("Source does not exist: " + source);
        }
        if (!Files.isDirectory(destinationDir)) {
            throw new IOException("Destination is not a directory: " + destinationDir);
        }

        Path target = destinationDir.resolve(source.getFileName().toString());

        if (Files.isDirectory(source)) {
            copyDirectoryRecursively(source, target);
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Path destDir = target.resolve(rel);
                if (!Files.exists(destDir)) {
                    Files.createDirectories(destDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Path destFile = target.resolve(rel);
                Files.copy(file, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void move(Path source, Path destinationDir) throws IOException {
        Objects.requireNonNull(source, "Source path cannot be null");
        Objects.requireNonNull(destinationDir, "Destination directory cannot be null");

        if (!Files.exists(source)) {
            throw new IOException("Source does not exist: " + source);
        }
        if (!Files.isDirectory(destinationDir)) {
            throw new IOException("Destination is not a directory: " + destinationDir);
        }

        Path target = destinationDir.resolve(source.getFileName().toString());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public void delete(Path target) throws IOException {
        Objects.requireNonNull(target, "Target path cannot be null");
        if (!Files.exists(target)) {
            throw new IOException("Target does not exist: " + target);
        }

        if (Files.isDirectory(target)) {
            Files.walkFileTree(target, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) throw exc;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(target);
        }
    }

    private void validateName(String name) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IOException("Name cannot be empty.");
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..") || name.contains(":")) {
            throw new IOException("Invalid characters in filename: " + name);
        }
    }
}
