package dev.vanta.service.process;

import java.util.List;

/**
 * Specification for an approved system command with security validation rules.
 */
public record CommandSpec(
        String command,
        String category,
        String description,
        String example,
        List<String> allowedFlags
) {
    public boolean allowsFlag(String flag) {
        if (allowedFlags.isEmpty() || allowedFlags.contains("*")) {
            return true;
        }
        return allowedFlags.contains(flag);
    }
}
