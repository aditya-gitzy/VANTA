package dev.vanta.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry managing built-in VANTA modules.
 * Strictly manages compile-time registered modules without external dynamic plugin loading.
 */
public class ModuleRegistry {

    private final Map<String, VantaModule> modules = new LinkedHashMap<>();

    public void register(VantaModule module) {
        Objects.requireNonNull(module, "Module must not be null");
        Objects.requireNonNull(module.getId(), "Module ID must not be null");
        if (modules.containsKey(module.getId())) {
            throw new IllegalArgumentException("Module already registered with ID: " + module.getId());
        }
        modules.put(module.getId(), module);
    }

    public Optional<VantaModule> getModule(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modules.get(id));
    }

    public List<VantaModule> getAllModules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    public boolean contains(String id) {
        return modules.containsKey(id);
    }

    public int size() {
        return modules.size();
    }
}
