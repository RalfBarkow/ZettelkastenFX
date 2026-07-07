package zk.core.importing;

/**
 * Reason why a syntactically valid source reference could not be resolved.
 */
public enum Zkn3UnresolvedReferenceReason {
    /**
     * Integer reference points outside the available source entry range.
     */
    OUT_OF_RANGE,

    /**
     * Referenced target exists structurally but lacks a stable source id.
     */
    TARGET_MISSING_ID,

    /**
     * Referenced target could not be found in the source model.
     */
    TARGET_NOT_FOUND,

    /**
     * Resolution failure exists but has not yet been classified.
     */
    UNKNOWN
}
