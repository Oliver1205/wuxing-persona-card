package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class PersonaNameRegistry {

    private PersonaNameRegistry() {
    }

    static String label(Map<String, String> labels, String id) {
        String label = labels.get(id);
        if (label == null) {
            throw new IllegalStateException("persona label missing for " + id);
        }
        requireValid(label);
        return label;
    }

    static void putLabel(Map<String, String> labels,
                         ElementType primary,
                         ElementType secondary,
                         ElementType accent,
                         RelationKind relationKind,
                         String label) {
        requireValid(label);
        String id = personaTypeKey(primary, secondary, accent, relationKind);
        String previous = labels.put(id, label);
        if (previous != null) {
            throw new IllegalStateException("duplicate persona label id: " + id);
        }
    }

    static void verifyComplete(Map<String, String> labels, int expectedSize) {
        if (labels.size() != expectedSize) {
            throw new IllegalStateException("Persona label registry must contain " + expectedSize
                    + " entries, actual=" + labels.size());
        }
        Set<String> uniqueLabels = new HashSet<>(labels.values());
        if (uniqueLabels.size() != expectedSize) {
            throw new IllegalStateException("Persona label registry must use unique user-facing labels");
        }
        labels.values().forEach(PersonaNameRegistry::requireValid);
    }

    static boolean isValidPersonaName(String label) {
        return StarToneRegistry.isValidStarToneName(label);
    }

    static String personaTypeKey(ElementType primary,
                                 ElementType secondary,
                                 ElementType accent,
                                 RelationKind relationKind) {
        return primary.name() + "-" + secondary.name() + "-" + accent.name() + "-" + relationKind.getCode();
    }

    private static void requireValid(String label) {
        if (!isValidPersonaName(label)) {
            throw new IllegalStateException("persona label must be a four-character star tone name: " + label);
        }
    }
}
