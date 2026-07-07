package zk.core.importing;

/**
 * Semantic class of a syntactically valid source reference that could not be
 * resolved into a normal imported model edge.
 */
public enum Zkn3UnresolvedReferenceKind {
    /**
     * Legacy manual note reference from the zknFile manlinks field.
     */
    MANUAL_LINK,

    /**
     * Legacy sequence or child reference from the zknFile luhmann field.
     */
    LUHMANN_SEQUENCE,

    /**
     * Source reference whose field or semantic class is not yet classified.
     */
    UNKNOWN
}
