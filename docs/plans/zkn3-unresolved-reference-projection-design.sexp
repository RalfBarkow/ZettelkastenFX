(:artifact zkn3-unresolved-reference-projection-design
 :kind design-record
 :created-from-task
 (!design-projection-of-unresolved-zkn3-references
  :from Zkn3UnresolvedReferenceRecord
  :to (:diagnostics :import-report :future-inspector-view)
  :must-not-create-resolved-edge true)

 :operational-definition
 (:projection
  "A projection is a derived view/report/diagnostic surface over an import model record.
   It makes the source datum inspectable and navigable without changing the semantic
   model boundary of that record.")

 :source-record
 (:record Zkn3UnresolvedReferenceRecord
  :fields
  (:sourceNoteId
   :sourceField
   :rawReference
   :referenceKind
   :reason
   :order)
  :example
  (:sourceField "manlinks"
   :rawReference "64444"
   :referenceKind MANUAL_LINK
   :reason OUT_OF_RANGE))

 :semantic-boundary
 (:must-not-create-resolved-edge true
  :must-not-create-Zkn3LinkRecord true
  :must-not-invent-target-note true
  :must-not-repair-source true
  :must-not-drop-reference true)

 :projection-surfaces
 ((:surface :diagnostics
   :purpose "Make preservation visible during import."
   :input Zkn3UnresolvedReferenceRecord
   :expected-output
   (:severity WARNING
    :field "manlinks"
    :message "Manual link index 64444 is out of range ... preserving unresolved manual link reference.")
   :non-goal "Do not use diagnostics as the only storage of the unresolved datum.")

  (:surface :import-report
   :purpose "Summarize unresolved source references as first-class import outcomes."
   :input Zkn3ImportBatch.unresolvedReferences
   :expected-output
   (:unresolved-reference-count 1
    :by-kind ((MANUAL_LINK 1))
    :by-reason ((OUT_OF_RANGE 1))
    :examples ((:sourceField "manlinks" :rawReference "64444")))
   :benefit "An accepted import can still report preserved unresolved source facts.")

  (:surface :future-inspector-view
   :purpose "Let the operator inspect unresolved references as model objects."
   :input Zkn3ImportBatch.unresolvedReferences
   :expected-view
   (:table-columns
    (:sourceNoteId :sourceField :rawReference :referenceKind :reason :order)
    :actions
    (:show-source-note
     :show-diagnostic
     :show-why-not-resolved
     :show-no-edge-created))
   :benefit "The operator can distinguish a missing target from a resolved link."))

 :anti-projections
 ((:bad "Project unresolved manlinks as Zkn3LinkRecord to rawReference 64444."
   :why "Creates a fake graph edge to a non-existing target.")

  (:bad "Keep only a warning diagnostic and discard the unresolved record."
   :why "Loses the source datum as model data.")

  (:bad "Silently ignore rawReference 64444."
   :why "Violates no-silent-data-loss and import-completeness policy."))

 :acceptance-criteria
 ((:name :diagnostic-projection
   :given "Zkn3UnresolvedReferenceRecord for manlinks rawReference 64444"
   :then "import diagnostics contain a WARNING that says the unresolved manual link was preserved")

  (:name :report-projection
   :given "Zkn3ImportBatch with unresolvedReferences"
   :then "import report can count and group unresolved references by kind and reason")

  (:name :inspector-projection
   :given "future inspector receives Zkn3ImportBatch"
   :then "unresolved references are shown as records, not as resolved links")

  (:name :edge-boundary
   :given "rawReference 64444 is out of range"
   :then "no Zkn3LinkRecord is created for 64444"))

 :current-evidence
 (:implementation-commit "f302cca"
  :verification-artifact "docs/plans/zkn3-real-source-unresolved-manlink-verification.sexp"
  :verification-commit "332ff73"
  :real-source-result
  (:notes 9017
   :keywords 13602
   :links 2436
   :sequences 7115
   :attachments 716
   :unresolvedReferences 1
   :diagnostics 3514
   :rawReference "64444"))

 :next
 (!implement-import-report-projection-for-unresolved-references
  :module zk-source-zkn3
  :mode :report-only
  :must-not-create-resolved-edge true))
