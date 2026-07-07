(:artifact zkn3-real-source-unresolved-manlink-verification
 :kind verification-record
 :created-from-task
 (!verify-real-source-now-preserves-out-of-range-manlink)

 :source
 (:name "rgb.zkn3"
  :path "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
  :copied-into-repo nil
  :repaired nil
  :read-only true)

 :implementation-slice
 (:commit "f302cca"
  :message "feat(zettelkasten): preserve unresolved zkn3 manlinks")

 :observed-result
 (:result :passed
  :notes 9017
  :keywords 13602
  :links 2436
  :sequences 7115
  :attachments 716
  :unresolvedReferences 1
  :diagnostics 3514)

 :verified-invariants
 ((:raw-reference "64444"
   :source-field "manlinks"
   :preserved-as Zkn3UnresolvedReferenceRecord
   :reference-kind MANUAL_LINK
   :reason OUT_OF_RANGE
   :diagnostic WARNING
   :must-not-drop-reference true
   :must-not-create-Zkn3LinkRecord true
   :no-error-diagnostics true))

 :temporary-test
 (:class Zkn3RealSourceUnresolvedManlinkVerificationTest
  :committed nil
  :cleaned-up true
  :diff-after-cleanup empty)

 :boundary
 (:no-sqlite-write true
  :no-ui-change true
  :no-source-repair true
  :protected-unowned-state
  ("zk-ui-javafx/src/main/java/zk/ui/javafx/FedWikiPane.java"
   "repomix.config.json"
   "zettelkasten-fx-repomix-output.md"))

 :next
 (!design-projection-of-unresolved-zkn3-references
  :from Zkn3UnresolvedReferenceRecord
  :to (:diagnostics :import-report :future-inspector-view)
  :must-not-create-resolved-edge true))
