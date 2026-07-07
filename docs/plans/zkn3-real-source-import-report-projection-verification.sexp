(:artifact zkn3-real-source-import-report-projection-verification
 :kind verification-record
 :created-from-task
 (!verify-real-source-import-report-projection
  :source "rgb.zkn3"
  :expected
  (:unresolvedReferenceCount 1
   :by-kind ((MANUAL_LINK 1))
   :by-reason ((OUT_OF_RANGE 1))
   :example-rawReference "64444")
  :must-not-create-resolved-edge true)

 :source
 (:name "rgb.zkn3"
  :path "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
  :copied-into-repo nil
  :repaired nil
  :read-only true)

 :report
 (:class Zkn3ImportReport
  :input Zkn3ImportBatch
  :implementation-commit "deff040"
  :implementation-record "docs/plans/zkn3-import-report-projection-implementation.sexp"
  :implementation-record-commit "cf85959")

 :observed-result
 (:result :passed
  :notes 9023
  :keywords 13602
  :links 2436
  :sequences 7115
  :attachments 716
  :diagnostics 3520
  :unresolvedReferenceCount 1
  :by-kind ((MANUAL_LINK 1))
  :by-reason ((OUT_OF_RANGE 1)))

 :verified-example
 (:sourceNoteId "240611105406688rgb50919"
  :sourceField "manlinks"
  :rawReference "64444"
  :referenceKind MANUAL_LINK
  :reason OUT_OF_RANGE
  :order 15)

 :verified-invariants
 ((:name :report-count
   :then "Zkn3ImportReport.unresolvedReferenceCount is 1")

  (:name :report-kind-grouping
   :then "Zkn3ImportReport.unresolvedReferencesByKind contains MANUAL_LINK count 1")

  (:name :report-reason-grouping
   :then "Zkn3ImportReport.unresolvedReferencesByReason contains OUT_OF_RANGE count 1")

  (:name :report-example
   :then "Zkn3ImportReport.unresolvedReferenceExamples contains rawReference 64444")

  (:name :edge-boundary
   :then "No resolved Zkn3LinkRecord is created or implied for rawReference 64444"))

 :real-source-inventory-policy
 (:note-count-is-observed-state true
  :note-count-is-not-acceptance-criterion true
  :previous-observed-note-counts (9017)
  :current-observed-note-count 9023)

 :temporary-test
 (:class Zkn3RealSourceImportReportProjectionVerificationTest
  :committed nil
  :cleaned-up true
  :diff-after-cleanup empty)

 :boundary
 (:no-sqlite-write true
  :no-ui-change true
  :no-source-repair true
  :no-resolved-edge-created true)

 :protected-unowned-state
 ("zk-ui-javafx/src/main/java/zk/ui/javafx/FedWikiPane.java"
  "repomix.config.json"
  "zettelkasten-fx-repomix-output.md")

 :next
 (!design-future-inspector-view-for-unresolved-zkn3-references
  :from Zkn3ImportReport
  :input Zkn3ImportBatch.unresolvedReferences
  :must-not-create-resolved-edge true
  :must-not-touch-javafx true))
