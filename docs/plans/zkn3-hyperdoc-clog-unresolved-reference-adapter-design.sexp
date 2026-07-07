(:artifact zkn3-hyperdoc-clog-unresolved-reference-adapter-design
 :kind design-record
 :created-from-task
 (!design-hyperdoc-clog-projection-adapter-for-Zkn3ImportReport
  :mode :adapter-design-only
  :must-not-touch-javafx true
  :must-not-create-resolved-edge true)

 :localized-surface
 (:surface :hyperdoc-clog-moldable-inspector
  :repo "/Users/rgb/workspace/hyperdoc"
  :mechanisms
  (:html-inspector-views:defview
   :html-inspector-views:html-view
   :html-inspector-views:object-ref
   :html-inspector-views:view-contracts
   :clog-moldable-inspector:clog-inspect)
  :evidence
  (:hyperdoc-inspector-system true
   :custom-defview-patterns true
   :view-contract-objects true
   :smoke-tests-load-views true))

 :non-targets
 (:javafx
  (:status :protected-unowned
   :path "zk-ui-javafx/src/main/java/zk/ui/javafx/FedWikiPane.java"
   :decision "Do not touch JavaFX in this slice.")

  :swing
  (:status :forbidden-by-model-boundary
   :decision "Do not introduce Swing/AWT dependencies.")

  :sqlite
  (:status :out-of-scope
   :decision "Inspection adapter is a view/projection contract, not persistence."))

 :input-boundary
 (:java-model Zkn3ImportReport
  :source Zkn3ImportBatch.unresolvedReferences
  :required-export-shape
  (:format :sexp-or-json
   :producer "future Java-side export or test fixture"
   :must-contain
   (:noteCount
    :keywordCount
    :linkCount
    :sequenceCount
    :attachmentCount
    :diagnosticCount
    :unresolvedReferenceCount
    :unresolvedReferencesByKind
    :unresolvedReferencesByReason
    :unresolvedReferenceExamples))
  :must-not-require-live-javafx true)

 :adapter-model
 (:lisp-object zkn3-import-report-projection
  :role
  "A Common Lisp adapter object that represents the Java import report
   for HyperDoc inspection. It mirrors report facts; it does not own import
   semantics and does not resolve references."
  :slots
  (:source-name
   :source-path
   :observed-counts
   :unresolved-reference-count
   :by-kind
   :by-reason
   :examples
   :edge-boundary
   :evidence-artifacts))

 :required-views
 ((:view "Summary"
   :subject zkn3-import-report-projection
   :question "What did the import report observe?"
   :content
   (:counts-table
    (:notes :keywords :links :sequences :attachments :diagnostics :unresolved-references)
    :grouping-table
    (:by-kind :by-reason)))

  (:view "Unresolved references"
   :subject zkn3-import-report-projection
   :question "Which source references were preserved as unresolved?"
   :content
   (:table-columns
    (:sourceNoteId :sourceField :rawReference :referenceKind :reason :order)
    :row-object-type zkn3-unresolved-reference-projection))

  (:view "Edge boundary"
   :subject zkn3-unresolved-reference-projection
   :question "Which resolved edge was intentionally not created?"
   :content
   (:rawReference
    :referenceKind
    :reason
    :created-Zkn3LinkRecord false
    :created-resolved-edge false))

  (:view "Evidence"
   :subject zkn3-import-report-projection
   :question "Which committed artifacts and validations support this projection?"
   :content
   (:implementation-commit
    :verification-commits
    :design-artifacts
    :real-source-observation)))

 :object-navigation
 (:summary-to-row
  (:from zkn3-import-report-projection
   :via :examples
   :to zkn3-unresolved-reference-projection
   :mechanism :html-inspector-views:object-ref)

  :row-to-evidence
  (:from zkn3-unresolved-reference-projection
   :via :evidence-artifacts
   :to zkn3-import-report-projection
   :mechanism :html-inspector-views:object-ref))

 :real-source-example
 (:source "rgb.zkn3"
  :observed
  (:notes 9023
   :keywords 13602
   :links 2436
   :sequences 7115
   :attachments 716
   :diagnostics 3520
   :unresolvedReferenceCount 1)
  :unresolved-reference
  (:sourceNoteId "240611105406688rgb50919"
   :sourceField "manlinks"
   :rawReference "64444"
   :referenceKind MANUAL_LINK
   :reason OUT_OF_RANGE
   :order 15)
  :edge-boundary
  (:no-Zkn3LinkRecord-for-rawReference true
   :no-resolved-edge-created true))

 :acceptance-criteria
 ((:name :adapter-is-non-semantic
   :then "The Lisp adapter mirrors Java report facts and does not resolve references.")

  (:name :summary-view
   :then "The inspector can show counts and unresolved-reference groupings.")

  (:name :unresolved-reference-view
   :then "The inspector can show each unresolved reference as an object row.")

  (:name :edge-boundary-view
   :then "The inspector can show that rawReference 64444 did not become a Zkn3LinkRecord.")

  (:name :no-javafx-touch
   :then "No JavaFX file is modified.")

  (:name :no-swing-awt
   :then "No Swing/AWT dependency is introduced."))

 :implementation-plan
 ((!record-adapter-design)
  (!create-page-attached-or-hyperdoc-local-asdf-system
   :contains (:adapter-model :views :smoke-test)
   :must-not-touch-javafx true)
  (!load-adapter-in-hyperdoc)
  (!inspect-example-Zkn3ImportReport-projection)
  (!verify-views-with-smoke-test)
  (!commit-path-limited-adapter))

 :next
 (!create-hyperdoc-clog-zkn3-import-report-adapter
  :mode :adapter-model-and-views
  :target :hyperdoc
  :input :sexp-fixture-from-Zkn3ImportReport
  :must-not-touch-javafx true
  :must-not-create-resolved-edge true))
