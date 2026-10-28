package dev.vanta.service.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileServiceTest {

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
    }

    @Test
    void testCreateDirectoryAndFile(@TempDir Path tempDir) throws IOException {
        Path newDir = fileService.createDirectory(tempDir, "test_folder");
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));

        Path newFile = fileService.createFile(newDir, "sample.txt");
        assertTrue(Files.exists(newFile));
        assertFalse(Files.isDirectory(newFile));

        List<FileItem> items = fileService.listDirectory(tempDir);
        assertEquals(1, items.size());
        assertEquals("test_folder", items.get(0).name());
        assertTrue(items.get(0).isDirectory());
    }

    @Test
    void testRename(@TempDir Path tempDir) throws IOException {
        Path orig = fileService.createFile(tempDir, "original.txt");
        assertTrue(Files.exists(orig));

        Path renamed = fileService.rename(orig, "renamed.txt");
        assertFalse(Files.exists(orig));
        assertTrue(Files.exists(renamed));
        assertEquals("renamed.txt", renamed.getFileName().toString());
    }

    @Test
    void testDeleteFileAndDirectory(@TempDir Path tempDir) throws IOException {
        Path subDir = fileService.createDirectory(tempDir, "to_delete");
        fileService.createFile(subDir, "inner.txt");

        fileService.delete(subDir);
        assertFalse(Files.exists(subDir));
    }

    @Test
    void testInvalidFilenameThrows(@TempDir Path tempDir) {
        assertThrows(IOException.class, () -> fileService.createFile(tempDir, "../bad.txt"));
        assertThrows(IOException.class, () -> fileService.createFile(tempDir, "foo/bar"));
        assertThrows(IOException.class, () -> fileService.createFile(tempDir, ""));
    }
}
