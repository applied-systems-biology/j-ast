package org.hkijena.jast.tasks;

/**
 * Determines rules on how the filling of data slots is validated
 */
public enum BackendTaskWorkloadDataSlotValidationMode {
    /**
     * Each data slot must contain the data
     */
    Always,
    /**
     * Optionally contains the data
     */
    Optional,
    /**
     * Only one slot per row needs the data
     */
    OncePerRow
}
