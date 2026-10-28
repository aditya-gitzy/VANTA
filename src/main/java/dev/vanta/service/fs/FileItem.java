package dev.vanta.service.fs;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Immutable value representation of a file or directory on disk.
 */
public record FileItem(
        Path path,
        String name,
        boolean isDirectory,
        long sizeBytes,
        FileTime lastModified,
        boolean isReadable,
        boolean isWritable
) {
    public String getFormattedSize() {
        if (isDirectory) {
            return "<DIR>";
        }
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        int exp = (int) (Math.log(sizeBytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", sizeBytes / Math.pow(1024, exp), pre);
    }
}
