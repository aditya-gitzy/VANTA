package dev.vanta.service.process;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessServiceTest {

    private ProcessService processService;

    @BeforeEach
    void setUp() {
        processService = new ProcessService();
    }

    @Test
    void testTokenizationWithQuotes() {
        List<String> tokens = processService.tokenize("echo \"hello world\" -n");
        assertEquals(3, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
        assertEquals("-n", tokens.get(2));
    }

    @Test
    void testApprovedCommandsValidationPasses() {
        assertDoesNotThrow(() -> processService.validate(List.of("whoami")));
        assertDoesNotThrow(() -> processService.validate(List.of("uname", "-a")));
        assertDoesNotThrow(() -> processService.validate(List.of("df", "-h")));
    }

    @Test
    void testUnapprovedCommandRejected() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> processService.validate(List.of("rm", "-rf", "/")));
        assertTrue(ex.getMessage().contains("not in the approved command whitelist"));
    }

    @Test
    void testBuiltinCommandsValidationPasses() {
        assertDoesNotThrow(() -> processService.validate(List.of("help")));
        assertDoesNotThrow(() -> processService.validate(List.of("cd", "..")));
        assertDoesNotThrow(() -> processService.validate(List.of("pwd")));
        assertDoesNotThrow(() -> processService.validate(List.of("history")));
        assertDoesNotThrow(() -> processService.validate(List.of("clear")));
    }

    @Test
    void testShellMetacharactersRejected() {
        assertThrows(SecurityException.class,
                () -> processService.validate(List.of("whoami", ";", "id")));

        assertThrows(SecurityException.class,
                () -> processService.validate(List.of("echo", "test", "|", "cat")));

        assertThrows(SecurityException.class,
                () -> processService.validate(List.of("uname", ">", "out.txt")));

        assertThrows(SecurityException.class,
                () -> processService.validate(List.of("echo", "$(whoami)")));
    }
}
