(:artifact zkn3-unresolved-reference-inspector-view-design
 :kind design-record
 :created-from-task
 (!design-future-inspector-view-for-unresolved-zkn3-references
  :from Zkn3ImportReport
  :input Zkn3ImportBatch.unresolvedReferences
  :must-not-create-resolved-edge true
  :must-not-touch-javafx true)

 :operational-definition
 (:inspector-view
  "An inspector view is an operator-facing projection over existing model/report
   records. It makes objects selectable, comparable, and explainable by source
   fields and decisions. It must not mutate the import model, repair source data,
   or create semantic edges.")

 :scope
 (:design-only true
  :implements-view false
  :touches-javafx false
  :touches-swing false
  :touches-sqlite false
  :changes-reader-semantics false)

 :input-model
 (:primary Zkn3ImportReport
  :source Zkn3ImportBatch.unresolvedReferences
  :records
  (Zkn3UnresolvedReferenceRecord
   Zkn3UnresolvedReferenceKind
   Zkn3UnresolvedReferenceReason)
  :already-verified-by
  (:implementation-commit "deff040"
   :implementation-record-commit "cf85959"
   :real-source-verification-commit "c2a9e34"))

 :view-purpose
 (:primary
  "Let the operator see that a source reference was preserved as unresolved,
   why it was not resolved, and which resolved edge was intentionally not created."
  :benefits
  (:auditability "The unresolved datum remains visible as model data, not only prose."
   :navigation "The operator can navigate from report summary to individual source reference."
   :edge-safety "The operator can distinguish unresolved reference records from resolved links."
   :future-repair-planning "A future repair task can be planned from the preserved source datum."))

 :required-summary-panel
 (:title "Unresolved references"
  :source Zkn3ImportReport
  :fields
  (:unresolvedReferenceCount
   :unresolvedReferencesByKind
   :unresolvedReferencesByReason)
  :real-source-example
  (:unresolvedReferenceCount 1
   :by-kind ((MANUAL_LINK 1))
   :by-reason ((OUT_OF_RANGE 1))))

 :required-record-table
 (:source Zkn3ImportBatch.unresolvedReferences
  :row-record Zkn3UnresolvedReferenceRecord
  :columns
  ((:name :sourceNoteId
    :meaning "Source note that contains the unresolved reference.")

   (:name :sourceField
    :meaning "Original ZKN3 field that contained the reference, e.g. manlinks.")

   (:name :rawReference
    :meaning "Raw source token preserved without normalization or repair.")

   (:name :referenceKind
    :meaning "Operational kind of source reference, e.g. MANUAL_LINK.")

   (:name :reason
    :meaning "Why the reference is unresolved, e.g. OUT_OF_RANGE.")

   (:name :order
    :meaning "Token order within the source field.")))

 :required-row-detail
 (:for-record Zkn3UnresolvedReferenceRecord
  :sections
  ((:section :source-fact
    :fields (:sourceNoteId :sourceField :rawReference :order))

   (:section :classification
    :fields (:referenceKind :reason))

   (:section :resolution-decision
    :content
    (:status :not-resolved
     :reason "The raw reference points outside the available zettel-entry range."
     :created-Zkn3LinkRecord false
     :created-resolved-edge false))

   (:section :diagnostic
    :content
    "Show matching WARNING diagnostic when available.")

   (:section :future-task
    :content
    "Offer a planning handle for source repair or target investigation, but do not perform repair.")))

 :allowed-actions
 ((:action :show-source-note
   :input :sourceNoteId
   :effect "Navigate to or inspect the source note record if available."
   :mutation false)

  (:action :show-warning-diagnostic
   :input (:sourceNoteId :sourceField :rawReference)
   :effect "Show the diagnostic that recorded preservation of the unresolved reference."
   :mutation false)

  (:action :show-why-not-resolved
   :input (:referenceKind :reason)
   :effect "Explain the decision boundary using model fields and diagnostics."
   :mutation false)

  (:action :show-no-edge-created
   :input :rawReference
   :effect "Show that no Zkn3LinkRecord exists for the unresolved raw reference."
   :mutation false)

  (:action :plan-source-repair
   :input Zkn3UnresolvedReferenceRecord
   :effect "Create or hand off a future task proposal; do not repair source data directly."
   :mutation :future-artifact-only))

 :forbidden-actions
 ((:action :resolve-to-link
   :why "Would create a resolved edge from unresolved source data.")

  (:action :invent-target-note
   :why "Would fabricate model data not present in source.")

  (:action :edit-real-zkn3-source
   :why "Source repair is outside inspector projection.")

  (:action :write-sqlite
   :why "This design concerns inspection/projection, not storage.")

  (:action :touch-javafx-now
   :why "This slice is a future inspector-view contract only."))

 :real-source-example
 (:source "rgb.zkn3"
  :record
  (:sourceNoteId "240611105406688rgb50919"
   :sourceField "manlinks"
   :rawReference "64444"
   :referenceKind MANUAL_LINK
   :reason OUT_OF_RANGE
   :order 15)
  :report
  (:unresolvedReferenceCount 1
   :by-kind ((MANUAL_LINK 1))
   :by-reason ((OUT_OF_RANGE 1)))
  :edge-boundary
  (:no-Zkn3LinkRecord-for-rawReference true
   :no-resolved-edge-created true))

 :acceptance-criteria
 ((:name :summary-visible
   :given Zkn3ImportReport
   :then "The inspector can show unresolvedReferenceCount and groupings by kind/reason.")

  (:name :records-visible
   :given Zkn3ImportBatch.unresolvedReferences
   :then "The inspector can list each Zkn3UnresolvedReferenceRecord as a row.")

  (:name :row-detail-visible
   :given "rawReference 64444"
   :then "The inspector can show source field, raw token, kind, reason, order, and non-resolution decision.")

  (:name :edge-boundary-visible
   :given "unresolved MANUAL_LINK OUT_OF_RANGE"
   :then "The inspector can show that no Zkn3LinkRecord was created.")

  (:name :non-mutating
   :given "operator opens unresolved-reference inspector"
   :then "No source, SQLite, JavaFX, Swing, or import model mutation occurs."))

 :next
 (!inspect-existing-inspector-surfaces-before-implementation
  :scope (:hyperdoc :clog :future-javafx)
  :input Zkn3ImportReport
  :must-not-touch-javafx true
  :must-not-create-resolved-edge true))
