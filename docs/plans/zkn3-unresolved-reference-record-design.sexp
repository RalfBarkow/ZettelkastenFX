(:task
 (!design-zkn3-unresolved-reference-record
  :module zk-core
  :mode :core-vocabulary-design-only
  :records (Zkn3UnresolvedReferenceRecord
            Zkn3UnresolvedReferenceKind
            Zkn3UnresolvedReferenceReason)
  :batch-field unresolvedReferences
  :source-fields (manlinks luhmann)
  :first-use-case (:manlinks :out-of-range-reference)
  :must-not-implement-record-yet t
  :must-not-change-source-reader-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))

(:task-location
 (:source-zettel 8992
  :operation "attempt task localization before declaring a task a problem"
  :localized-status :expected-localized-or-partially-localized
  :task-space "DMX-like task topic map mirrored in SQLite"))

(:problem-48.2-guard
 (:source-zettel 6129
  :definition "A problem is a task that cannot be solved by existing routines of behavior, action, or interaction, so methods and patterns of interaction have to be found."
  :htn-effect "This task is not declared a problem until the task-location topic map fails to localize an adequate existing routine."))

(:context
 (:current-real-source-blocker
  (:status :correctly-rejected
   :field manlinks
   :class :out-of-range-reference
   :reference 64444
   :zettel-entry-count 8979
   :records-returned
   (:notes 0
    :keywords 0
    :links 0
    :sequences 0
    :attachments 0)))

 (:current-policy
  (:no-incomplete-imports true
   :no-partial-successful-batches true
   :error-diagnostics-require-zero-records true
   :manual-links-not-silently-dropped true
   :unresolved-manlinks-not-mapped-to-Zkn3LinkRecord true
   :real-source-not-repaired true)))

(:design-problem
 (:name unresolved-source-reference-vocabulary)
 (:why-needed
  "A syntactically valid source reference can be unresolved against the current source model. The source datum must be preserved without inventing a resolved graph edge.")
 (:first-use-case
  "manlinks contains token 64444, but the real source has 8979 zettel entries")
 (:must-not
  (:drop-reference
   :invent-placeholder-note
   :map-to-Zkn3LinkRecord
   :repair-rgb-zkn3
   :downgrade-error-before-preservation-vocabulary
   :write-sqlite
   :touch-ui)))

(:recommended-records
 ((:record Zkn3UnresolvedReferenceRecord
   :package zk.core.importing
   :purpose "Preserve a syntactically valid source reference that cannot be resolved to an existing modeled target."
   :fields
   ((sourceNoteId
     :type String
     :meaning "source zettel id containing the unresolved reference")

    (sourceField
     :type String
     :meaning "source field containing the reference, e.g. manlinks or luhmann")

    (rawReference
     :type String
     :meaning "exact token text from the source field")

    (referenceKind
     :type Zkn3UnresolvedReferenceKind
     :meaning "semantic class of source reference")

    (reason
     :type Zkn3UnresolvedReferenceReason
     :meaning "why the source reference could not be resolved")

    (order
     :type int
     :meaning "zero-based order of the token within the source field")))

  (:enum Zkn3UnresolvedReferenceKind
   :values
   ((MANUAL_LINK
     :source-field manlinks
     :meaning "legacy manual note reference")

    (LUHMANN_SEQUENCE
     :source-field luhmann
     :meaning "legacy sequence/child reference")

    (UNKNOWN
     :source-field "future or not-yet-classified source field"
     :meaning "preserve source data without premature classification")))

  (:enum Zkn3UnresolvedReferenceReason
   :values
   ((OUT_OF_RANGE
     :meaning "integer token refers outside the available source entry range")

    (TARGET_MISSING_ID
     :meaning "target entry exists structurally but lacks a stable source id")

    (TARGET_NOT_FOUND
     :meaning "reference is syntactically valid but no corresponding target can be found")

    (UNKNOWN
     :meaning "preserve reason when classification is not yet known")))))

(:batch-design
 (:add-field
  (:class Zkn3ImportBatch
   :field unresolvedReferences
   :type "List<Zkn3UnresolvedReferenceRecord>"))
 (:default-empty true)
 (:immutability-policy "copy defensively / expose unmodifiable list, matching existing batch record policy if present")
 (:rejected-alternative
  (:unresolvedManualLinks-only
   :reason "too narrow; luhmann or future source-reference fields can produce the same unresolved-reference class")))

(:severity-policy
 (:current-before-implementation
  (:out-of-range-manlink ERROR
   :batch-effect "complete batch rejected"))

 (:future-after-record-implementation-and-reader-mapping
  (:out-of-range-manlink WARNING
   :preserve-as Zkn3UnresolvedReferenceRecord
   :batch-effect "batch may be accepted because source datum is preserved"))

 (:always-error
  ((:non-integer-token
    :reason "syntactically invalid reference token")
   (:zero-index
    :reason "not a valid one-based legacy reference")
   (:negative-index
    :reason "not a valid one-based legacy reference"))))

(:resolved-link-boundary
 (:Zkn3LinkRecord
  (:meaning "resolved note-to-note edge only"
   :must-have-source-note-id true
   :must-have-target-note-id true
   :must-not-represent-dangling-reference true))

 (:Zkn3UnresolvedReferenceRecord
  (:meaning "preserved source reference that did not resolve"
   :must-preserve-raw-token true
   :must-preserve-source-field true
   :must-preserve-source-note-id true
   :must-not-create-graph-edge true)))

(:implementation-followup-candidate
 (!implement-zkn3-unresolved-reference-record
  :module zk-core
  :mode :core-records-only
  :records (Zkn3UnresolvedReferenceRecord
            Zkn3UnresolvedReferenceKind
            Zkn3UnresolvedReferenceReason)
  :batch-field unresolvedReferences
  :must-not-change-source-reader-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))

(:reader-followup-candidate
 (!map-zkn3-out-of-range-manlinks-to-unresolved-references
  :module zk-source-zkn3
  :mode :reader-mapping-only
  :requires (Zkn3UnresolvedReferenceRecord)
  :source-field manlinks
  :case (:out-of-range-reference 64444)
  :future-severity WARNING
  :must-not-map-to-Zkn3LinkRecord t
  :must-not-drop-reference t
  :must-not-repair-source t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))

(:confirmed-non-changes
 (:no-core-production-code-changed true
  :no-reader-acceptance-changed true
  :no-sqlite-import-logic true
  :no-schema-change true
  :no-ui-wiring true
  :no-jaxb-dependency true
  :real-rgb-zkn3-not-copied-or-committed true))

