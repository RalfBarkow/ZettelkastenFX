(:id zkn3-note-raw-timestamp-preservation-design
 :kind :core-vocabulary-design-only
 :scope AC-06

 :task
 (!design-zkn3-note-raw-timestamp-preservation
  :module zk-core
  :mode :core-vocabulary-design-only
  :record Zkn3NoteRecord
  :source-fields (ts_created ts_edited)
  :requires-timestamp-diagnostic-report t
  :requires-raw-values-in-diagnostics t
  :observed-ts-edited-raw-values (:blank)
  :must-not-implement-record-change-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "a0c43d8361787898a7d9228c89319ffdb06e935f"
   :subject "docs(zettelkasten): design zkn3 timestamp compatibility policy")
  (:commit "7b4e0ffa12bee92421ed52d09c64c2d29cd8b5f2"
   :subject "test(zettelkasten): report zkn3 timestamp diagnostics")
  (:commit "6185f1e6e8cbe9938883dc478df4ecf740bf334e"
   :subject "test(zettelkasten): include raw timestamp values in diagnostics"))

 :real-source-observation
 (:status :correctly-rejected
  :field ts_edited
  :raw-values (:blank)
  :ts_created-error-groups 0
  :ts_edited-error-groups 1
  :records-returned
  (:notes 0
   :keywords 0
   :links 0
   :sequences 0)
  :diagnostics
  (:total 3475
   :errors 3396
   :warnings 79)
  :sample-source-ids
  ("09080900024Zettelkasten4"
   "09080900135Zettelkasten5"
   "09080900277Zettelkasten7"
   "090809024314Zettelkasten14"
   "090809130019Zettelkasten19")
  :finding "The enriched diagnostic report shows blank ts_edited values as the current real-source blocker. ERROR diagnostics still return zero import records.")

 :current-record-shape
 (:record Zkn3NoteRecord
  :package zk.core.importing
  :fields
  ((:id sourceId)
   (:title title)
   (:content body)
   (:created-at createdAt)
   (:edited-at modifiedAt)
   (:rating rating)
   (:raw-ts-created nil)
   (:raw-ts-edited nil))
  :field-types
  ((sourceId String)
   (title String)
   (body String)
   (createdAt java.time.Instant)
   (modifiedAt java.time.Instant)
   (rating java.util.OptionalInt))
  :raw-source-timestamp-preservation-p false
  :current-limitation "Zkn3NoteRecord has only normalized Instant timestamps. It cannot preserve exact ts_created or ts_edited source values, and it cannot distinguish a blank edited timestamp from a normalized modifiedAt fallback.")

 :current-reader-behavior
 (:class zk.source.zkn3.Zkn3DomSourceReader
  :timestamp-parser "digits-only; >=13 digits as epoch milliseconds, shorter as epoch seconds"
  :ts_created-error-effect "reject note and complete batch"
  :ts_edited-error-effect "reject note and complete batch"
  :diagnostic-enrichment "timestamp diagnostics now include source note id and raw source value"
  :acceptance-changed-p nil)

 :problem
 "Blank ts_edited values occur in the real legacy source and are visible in current diagnostics. The reader must not accept them yet because the core note import record cannot preserve the exact raw timestamp provenance that would make acceptance full-fidelity."

 :recommended-design
 (:decision :preserve-raw-and-derived-timestamps
  :reason "Full-fidelity import needs to preserve legacy source timestamp fields separately from normalized application timestamps."
  :record-impact
  (:add-raw-ts-created true
   :add-raw-ts-edited true
   :keep-derived-created-at true
   :keep-derived-edited-at true)
  :blank-ts-edited-policy
  (:raw-value-preserved-as ""
   :derived-edited-at "fallback to derived created-at"
   :diagnostic-severity WARNING
   :reason "blank edited timestamp is present in real legacy data and should not by itself make the note unimportable once raw provenance is preserved"))

 :candidate-record-fields
 ((rawCreatedTimestamp
   :type String
   :source ts_created
   :required true
   :meaning "exact source value from zknFile.xml")

  (rawEditedTimestamp
   :type String
   :source ts_edited
   :required false
   :meaning "exact source value from zknFile.xml; may be blank")

  (createdAt
   :type java.time.Instant
   :source "derived from rawCreatedTimestamp"
   :required true
   :meaning "normalized application timestamp")

  (modifiedAt
   :type java.time.Instant
   :source "derived from rawEditedTimestamp if parseable, otherwise deterministic fallback"
   :required true
   :meaning "normalized application timestamp; current field name is modifiedAt, so keep it unless a broader vocabulary rename is explicitly scoped"))

 :blank-ts-edited-policy
 (:condition "ts_edited is missing or blank"
  :raw-preservation "preserve exact raw value, including blank"
  :derived-edited-at-policy :use-created-at
  :diagnostic WARNING
  :batch-effect "batch remains valid once raw preservation is implemented"
  :must-not-use-import-time t
  :must-not-use-epoch t
  :must-not-silently-coerce t
  :reason "Using createdAt is deterministic and avoids inventing edit history; the WARNING preserves that a fallback was applied.")

 :ts-created-policy
 (:condition "ts_created missing, blank, or unparseable"
  :severity ERROR
  :batch-effect "reject complete batch"
  :reason "created timestamp is required for deterministic note provenance unless core model supports unknown createdAt explicitly")

 :diagnostic-policy-after-record-change
 (:blank-ts_edited WARNING
  :unparseable-ts_edited WARNING
  :missing-ts_created ERROR
  :blank-ts_created ERROR
  :unparseable-ts_created ERROR
  :must-include-source-id t
  :must-include-raw-value t)

 :implementation-sequencing
 ((:step :core-record-change
   :meaning "Add rawCreatedTimestamp and rawEditedTimestamp to Zkn3NoteRecord and update core record tests."
   :reader-acceptance-changed-p nil)

  (:step :source-reader-population
   :meaning "Populate raw timestamp fields while preserving current rejection behavior."
   :reader-acceptance-changed-p nil)

  (:step :blank-edited-compatibility
   :meaning "Allow blank ts_edited only after raw field population exists; derive modifiedAt from createdAt and emit WARNING."
   :reader-acceptance-changed-p true))

 :rejected-behaviors
 ((:drop-raw-ts-edited
   :reason "would lose source provenance")

  (:treat-blank-ts-edited-as-permanent-source-corruption
   :reason "blank edited timestamps occur in real legacy data and ts_edited is string-like")

  (:use-import-time-as-edited-at
   :reason "non-deterministic and historically false")

  (:use-epoch-as-edited-at-without-provenance
   :reason "fake timestamp")

  (:change-reader-acceptance-before-core-vocabulary
   :reason "source reader cannot preserve what core record cannot represent")

  (:write-sqlite-during-core-vocabulary-design
   :reason "persistence application is downstream")

  (:touch-ui
   :reason "UI projection is downstream"))

 :open-questions
 ((:field-names
   "Use rawCreatedTimestamp/rawEditedTimestamp unless a later core vocabulary slice standardizes raw source field names across record types.")

  (:unknown-modified-time
   "A future model could represent modifiedAt as optional, but this design keeps the current required Instant and uses createdAt as deterministic fallback for blank edited timestamps.")

  (:legacy-calendar-normalization
   "This design does not decide the timezone/century policy for yyMMddHHmm compatibility; it only ensures raw provenance can survive that later decision."))

 :next-task-if-raw-fields-needed
 (!implement-zkn3-note-raw-timestamp-fields
  :module zk-core
  :mode :core-record-change-only
  :record Zkn3NoteRecord
  :fields (rawCreatedTimestamp rawEditedTimestamp)
  :keep-existing-derived-timestamps t
  :must-not-change-source-reader-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :next-task-if-raw-fields-already-exist
 (!implement-zkn3-blank-ts-edited-compatibility
  :module zk-source-zkn3
  :mode :parser-compatibility-only
  :source-field ts_edited
  :blank-policy :preserve-raw-and-use-created-at
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :confirmed-non-changes
 (:core-record-changed-p nil
  :reader-acceptance-changed-p nil
  :raw-timestamp-parser-implemented-p nil
  :legacy-timestamp-normalization-implemented-p nil
  :source-repair-p nil
  :sqlite-import-logic-added-p nil
  :ui-wiring-changed-p nil
  :persistence-schema-changed-p nil
  :attachment-record-added-p nil
  :jaxb-dependency-added-p nil
  :Zkn3SourceReader-changed-p nil
  :Zkn3DomSourceReader-changed-p nil
  :FedWikiPane-changed-p nil
  :legacy-swing-repo-edited-p nil
  :repomix-files-edited-or-committed-p nil
  :real-rgb-zkn3-copied-or-committed-p nil))
