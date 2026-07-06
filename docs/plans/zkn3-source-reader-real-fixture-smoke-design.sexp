(:id zkn3-source-reader-real-fixture-smoke-design
 :kind :real-source-smoke-design-only
 :scope AC-06

 :task
 (!design-zkn3-source-reader-real-fixture-smoke
  :module zk-source-zkn3
  :mode :real-source-smoke-design-only
  :source-path #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
  :requires-complete-import-record-vocabulary t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "8c2ab820b4c34fb528f3b80f6f63a303efe60286"
   :subject "feat(zettelkasten): extract zkn3 luhmann sequence records"))

 :current-source-reader-capability
 (:reader zk.source.zkn3.Zkn3DomSourceReader
  :reads-zkn3-zip-p t
  :validates-zknFile-xml-p t
  :validates-keywordFile-xml-p t
  :extracts-Zkn3NoteRecord-p t
  :extracts-Zkn3KeywordRecord-p t
  :extracts-manual-Zkn3LinkRecord-p t
  :extracts-Zkn3SequenceRecord-p t
  :guards-unsupported-nonempty-links-link-p t
  :returns-diagnostics-p t
  :sqlite-write-allowed-p nil
  :ui-touch-allowed-p nil
  :Zkn3AttachmentRecord-available-p nil)

 :purpose
 "Exercise the source reader against the real rgb.zkn3 file and report whether the file is importable under the currently modeled full-fidelity import vocabulary."

 :scope
 (:module zk-source-zkn3
  :reader zk.source.zkn3.Zkn3DomSourceReader
  :source-path #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
  :writes-sqlite-p nil
  :touches-ui-p nil
  :default-maven-test-p nil
  :opt-in-p t)

 :important-limitation
 (:missing-record Zkn3AttachmentRecord
  :effect "The real source may be correctly rejected if it contains nonempty links/link attachment metadata."
  :policy "Unsupported source data must reject the complete import batch rather than being silently ignored to make the smoke pass.")

 :expected-outcomes
 ((:importable
   :meaning "reader returns notes/keywords/manual-links/sequences and no ERROR diagnostics"
   :status PASS)

  (:correctly-rejected
   :meaning "reader returns zero records and ERROR diagnostics for unsupported or invalid source data"
   :status PASS-WITH-REJECTION
   :examples
   (:unsupported-links-link-attachment
    :invalid-keyword-reference
    :dangling-manlink
    :dangling-luhmann-sequence
    :malformed-source-xml))

  (:reader-failure
   :meaning "unexpected exception or boundary violation"
   :status FAIL))

 :full-fidelity-invariant
 (:no-incomplete-imports t
  :no-partial-successful-batches t
  :no-silent-source-data-loss t
  :unsupported-attachment-metadata-rejects-import t
  :sqlite-write-allowed-p nil
  :ui-touch-allowed-p nil)

 :future-smoke-shape
 (:preferred-shape
  (:kind :opt-in-junit-test
   :test-class zk.source.zkn3.Zkn3RealSourceSmokeTest
   :activation "system property zkn3.real.source"
   :path-source "System.getProperty(\"zkn3.real.source\")"
   :skip-when-property-missing-p true
   :skip-status "disabled/skipped, not failed"
   :default-mvn-test-runs-real-file-p nil)

  :rejected-alternative
  (:kind :small-main-or-cli
   :class zk.source.zkn3.Zkn3SourceReaderSmoke
   :argument "<path-to-rgb.zkn3>"
   :writes-json-summary-p true
   :reason "JUnit keeps the smoke near the reader tests while still allowing normal Maven tests to stay host-independent."))

 :future-command
 (:do-not-run-in-this-slice t
  :command "mvn -pl zk-source-zkn3 -am test -Dzkn3.real.source=/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3")

 :smoke-summary
 (:source-path "<path>"
  :status (:importable :correctly-rejected :reader-failure)
  :note-count <n>
  :keyword-count <n>
  :manual-link-count <n>
  :sequence-count <n>
  :diagnostic-count <n>
  :error-count <n>
  :warning-count <n>
  :first-errors (...))

 :acceptance-criteria
 ((:normal-build
   "mvn -pl zk-source-zkn3 -am test passes without requiring the real rgb.zkn3 path")

  (:opt-in-smoke
   "when the real path is provided, the reader runs without SQLite/UI access")

  (:importable-result
   "if there are no ERROR diagnostics, batch contains records and counts are reported")

  (:rejected-result
   "if unsupported attachment metadata or invalid references are present, batch has zero records and ERROR diagnostics")

  (:no-silent-loss
   "unsupported or invalid source data must not be ignored to make the smoke pass"))

 :non-scope
 (:do-not-read-real-source-in-this-slice t
  :do-not-add-real-source-to-repo t
  :do-not-copy-rgb-zkn3 t
  :do-not-write-sqlite t
  :do-not-touch-ui t
  :do-not-change-production-reader t
  :do-not-change-core-records t
  :do-not-add-real-source-test-yet t
  :do-not-add-jaxb-dependency t
  :do-not-repair-javafx-web-blocker t
  :do-not-edit-legacy-swing-repo t
  :do-not-edit-repomix-files t)

 :confirmed-non-changes
 (:real-source-smoke-implemented-p nil
  :real-rgb-zkn3-read-p nil
  :real-rgb-zkn3-copied-or-committed-p nil
  :production-reader-changed-p nil
  :sqlite-import-logic-added-p nil
  :ui-wiring-changed-p nil
  :persistence-schema-changed-p nil
  :attachment-record-added-p nil
  :jaxb-dependency-added-p nil
  :Zkn3SourceReader-changed-p nil
  :import-record-classes-changed-p nil
  :FedWikiPane-changed-p nil
  :legacy-swing-repo-edited-p nil
  :repomix-files-edited-or-committed-p nil)

 :next-implementation-task
 (!implement-zkn3-source-reader-real-fixture-smoke
  :module zk-source-zkn3
  :mode :opt-in-smoke-only
  :source-property "zkn3.real.source"
  :default-test-runs-real-file-p nil
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-task-if-real-smoke-correctly-rejected-because-of-attachments
 (!design-zkn3-attachment-record
  :module zk-core
  :mode :core-vocabulary-design-only
  :record Zkn3AttachmentRecord
  :source-field links
  :requires-real-source-smoke-diagnostics t
  :must-not-implement-record-yet t))
