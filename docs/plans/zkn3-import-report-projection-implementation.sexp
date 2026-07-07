(:artifact zkn3-import-report-projection-implementation
 :kind implementation-record
 :created-from-task
 (!implement-import-report-projection-for-unresolved-references
  :module zk-source-zkn3
  :mode :report-only
  :must-not-create-resolved-edge true)

 :implementation
 (:commit "deff040"
  :message "feat(zettelkasten): add zkn3 unresolved reference import report"
  :files
  ("zk-source-zkn3/src/main/java/zk/source/zkn3/Zkn3ImportReport.java"
   "zk-source-zkn3/src/test/java/zk/source/zkn3/Zkn3ImportReportTest.java"))

 :model
 (:record Zkn3ImportReport
  :input Zkn3ImportBatch
  :projection-of Zkn3ImportBatch.unresolvedReferences
  :outputs
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

 :semantic-boundary
 (:non-ui true
  :does-not-touch-reader-semantics true
  :does-not-create-Zkn3LinkRecord true
  :does-not-create-resolved-edge true
  :does-not-write-sqlite true
  :does-not-repair-source true)

 :validation
 (:zk-core
  (:tests-run 8
   :failures 0
   :errors 0
   :skipped 0
   :result :passed)
  :zk-source-zkn3
  (:tests-run 83
   :failures 0
   :errors 0
   :skipped 2
   :result :passed))

 :relation-to-design
 (:design-artifact "docs/plans/zkn3-unresolved-reference-projection-design.sexp"
  :design-commit "b16c591"
  :satisfies
  (:report-projection
   "Zkn3ImportReport counts and groups unresolved references by kind and reason and keeps examples."))

 :protected-unowned-state
 ("zk-ui-javafx/src/main/java/zk/ui/javafx/FedWikiPane.java"
  "repomix.config.json"
  "zettelkasten-fx-repomix-output.md")

 :next
 (!verify-real-source-import-report-projection
  :source "rgb.zkn3"
  :expected
  (:unresolvedReferenceCount 1
   :by-kind ((MANUAL_LINK 1))
   :by-reason ((OUT_OF_RANGE 1))
   :example-rawReference "64444")
  :must-not-create-resolved-edge true))
