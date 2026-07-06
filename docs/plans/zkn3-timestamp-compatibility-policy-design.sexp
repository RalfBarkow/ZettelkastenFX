(:id zkn3-timestamp-compatibility-policy-design
 :kind :diagnostic-policy-design-only
 :scope AC-06

 :task
 (!design-zkn3-timestamp-compatibility-policy
  :module zk-source-zkn3
  :mode :diagnostic-policy-design-only
  :source-fields (ts_created ts_edited)
  :real-source-smoke-status :correctly-rejected
  :real-source-first-error-class :malformed-ts_edited
  :must-not-change-reader-yet t
  :must-not-repair-source t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "2583a7a1a0434b387db221b07b9c7bbf016af867"
   :subject "test(zettelkasten): add opt-in zkn3 real source smoke"))

 :real-source-smoke-observation
 (:status :correctly-rejected
  :diagnostics 3473
  :errors 3394
  :warnings 79
  :records
  (:notes 0
   :keywords 0
   :links 0
   :sequences 0)
  :first-error-class :malformed-ts_edited
  :first-error-examples
  ("09080900024Zettelkasten4"
   "09080900135Zettelkasten5"
   "09080900277Zettelkasten7")
  :important-observation
  "The current smoke summary reports diagnostic sourceId values. These examples are zknid values, not raw ts_edited values; a diagnostic report slice is needed to group raw timestamp values and patterns.")

 :legacy-evidence
 ((:constant
   (:class de.danielluedecke.zettelkasten.database.Daten
    :name ATTRIBUTE_TIMESTAMP_CREATED
    :value "ts_created"))

  (:constant
   (:class de.danielluedecke.zettelkasten.database.Daten
    :name ATTRIBUTE_TIMESTAMP_EDITED
    :value "ts_edited"))

  (:generated-schema
   (:class ch.dreyeck.zettelkasten.xml.Zettel
    :ts_created-type "integer / BigInteger"
    :ts_edited-type "anySimpleType / String"
    :finding "Generated JAXB schema requires both attributes but does not require ts_edited to be numeric. getTsCreated/setTsCreated use BigInteger; getTsEdited/setTsEdited use String."))

  (:timestamp-format
   (:class de.danielluedecke.zettelkasten.util.Tools
    :method getTimeStamp
    :format "yyMMddHHmm"
    :example "0811271548"
    :finding "Legacy entry timestamps are compact calendar strings, not Unix epoch seconds."))

  (:timestamp-format
   (:class de.danielluedecke.zettelkasten.util.Tools
    :method getTimeStampWithMilliseconds
    :format "yyMMddHHmmssSSS"
    :finding "Used for generated IDs and author/keyword timestamps where extra uniqueness is needed."))

  (:timestamp-format
   (:class de.danielluedecke.zettelkasten.util.Tools
    :method getProperDate
    :finding "Converts strings by slicing yy, MM, dd, HH, mm positions; this confirms legacy date semantics for recognized timestamp-shaped strings."))

  (:read-usage
   (:class de.danielluedecke.zettelkasten.database.Daten
    :methods (getTimestamp getTimestampCreated getTimestampEdited)
    :finding "Returns created and edited timestamp attributes as raw strings without schema-level numeric validation."))

  (:write-usage
   (:class de.danielluedecke.zettelkasten.database.Daten
    :methods (setTimestamp setTimestampCreated setTimestampEdited changeEditTimeStamp)
    :finding "Writes timestamp attributes as strings; changeEditTimeStamp writes Tools.getTimeStamp() into ts_edited."))

  (:new-entry-policy
   (:class de.danielluedecke.zettelkasten.database.Daten
    :method addEntry
    :finding "New entries set ts_created to Tools.getTimeStamp() and ts_edited to the empty string because a new entry is not edited yet."))

  (:legacy-update
   (:class de.danielluedecke.zettelkasten.database.Daten
    :methods (fixWrongEditTags db_updateTimestampAttributes)
    :finding "Legacy update paths repair older timestamp element layouts and migrate timestamp/created and timestamp/edited child text into ts_created and ts_edited attributes."))

  (:legacy-import
   (:class de.danielluedecke.zettelkasten.tasks.importtasks.ImportFromZkn
    :finding "Old ZKN import converts localized textual timestamps into yyMMddHHmm strings; on parse failure it sets a default created timestamp and an empty edited timestamp."))

  (:manual-edit
   (:class de.danielluedecke.zettelkasten.ZettelkastenView
    :finding "Manual timestamp edits accept dd.MM.yy HH:mm and convert to yyMMddHHmm before writing created or edited timestamps."))

  (:zettel-id
   (:class de.danielluedecke.zettelkasten.database.Daten
    :method db_updateZettelIDs
    :finding "Generated zknid values append created timestamp, entry number, main data filename, and entry number; examples like 09080900024Zettelkasten4 are source ids, not raw ts_edited values."))

  (:zettel-id
   (:class de.danielluedecke.zettelkasten.util.Tools
    :method createZknID
    :finding "Alternative unique IDs combine getTimeStampWithMilliseconds(), filename, and a random suffix."))

  (:tests
   (:finding "Legacy tests under src/test/java do not provide direct timestamp compatibility fixtures for malformed ts_edited values or zknid suffix patterns.")))

 :current-reader-behavior
 (:class zk.source.zkn3.Zkn3DomSourceReader
  :method parseTimestamp
  :accepted-shapes
  (:digits-only t
   :length-gte-13 "parsed as epoch milliseconds"
   :length-lt-13 "parsed as epoch seconds")
  :rejection
  (:blank ERROR
   :nondigit ERROR
   :overflow-or-date-error ERROR)
  :mismatch
  "The parser normalizes Unix-like epoch seconds/millis. Legacy entry timestamps are yyMMddHHmm calendar strings, and legacy ts_edited may be empty or string-like.")

 :problem
 "The current reader treats `ts_edited` as a strict normalized timestamp. The real source contains values that are not accepted by that parser. Since legacy schema treats `ts_edited` as string-like and legacy code permits empty edited timestamps, rejection may be a parser compatibility problem or raw-metadata vocabulary gap rather than corrupt source data."

 :policy-options
 ((:strict-instant-only
   :meaning "accept only values parseable as the reader's current normalized timestamp"
   :pros ("simple" "keeps Zkn3NoteRecord timestamps normalized")
   :cons ("real legacy source rejected" "legacy yyMMddHHmm timestamps can be mis-normalized as epoch seconds" "may reject valid legacy metadata"))

  (:legacy-compatible-normalization
   :meaning "recognize documented legacy formats and normalize to Instant where possible"
   :pros ("can import real source when semantics are known" "matches Tools.getTimeStamp and Tools.getProperDate evidence")
   :cons ("requires a precise timezone/calendar policy" "still loses raw ts_edited unless represented separately"))

  (:raw-preserve-with-derived-normalization
   :meaning "preserve raw timestamp source values and derive normalized Instant only when safe"
   :pros ("full-fidelity" "does not erase legacy metadata" "does not invent precision" "can distinguish empty edited timestamp from unknown edited timestamp")
   :cons ("requires core record vocabulary change because Zkn3NoteRecord currently has only normalized Instant fields"))

  (:treat-ts_edited-as-optional-metadata
   :meaning "do not reject batch if `ts_edited` is unparseable; emit WARNING and use fallback modifiedAt"
   :pros ("allows import")
   :cons ("risks silent semantic loss unless raw value is preserved" "fallback may imply a false edit time")))

 :recommended-policy
 (:decision :raw-preserve-with-derived-normalization
  :reason "The real source is rejected on ts_edited diagnostics; legacy generated schema treats ts_edited as string-like; legacy code writes, reads, and migrates timestamp values as raw strings; and full-fidelity import must not discard unknown legacy metadata."
  :normalization
  (:ts_created "preserve raw; normalize recognized yyMMddHHmm or yyMMddHHmmssSSS values only after timezone and century policy are explicit"
   :ts_edited "preserve raw; normalize only recognized safe nonblank formats; empty legacy edited timestamp likely means no edit timestamp")
  :diagnostics
  (:unknown-ts_created-format ERROR
   :unknown-ts_edited-format WARNING
   :missing-required-created ERROR
   :missing-or-empty-edited WARNING-or-INFO-to-be-decided-after-report
   :recognized-legacy-format INFO)
  :core-impact "Requires raw timestamp preservation in core import vocabulary before acceptance can change without source metadata loss.")

 :core-vocabulary-impact
 (:current-record Zkn3NoteRecord
  :raw-ts-created-preserved-p false
  :raw-ts-edited-preserved-p false
  :normalized-created-field createdAt
  :normalized-edited-field modifiedAt
  :decision
  (:needs-core-record-revision true
   :reason "Zkn3NoteRecord stores only Instant createdAt and modifiedAt. It cannot preserve raw ts_created/ts_edited strings or represent unknown/empty modified time without inventing a normalized Instant."))

 :timestamp-diagnostic-taxonomy
 ((:missing-ts_created
   :severity ERROR
   :effect "reject complete batch until a policy explicitly allows raw-only creation metadata")

  (:missing-ts_edited
   :severity :policy-open
   :effect "likely WARNING or INFO when raw preservation exists; current schema marks it required but legacy new-entry policy writes empty edited timestamps")

  (:empty-ts_edited
   :severity :policy-open
   :effect "likely INFO as no-edit-yet when raw value is preserved; do not coerce silently")

  (:unparseable-ts_created
   :severity ERROR
   :effect "reject complete batch unless raw-preserve policy and required creation fallback are implemented")

  (:unparseable-ts_edited
   :severity WARNING
   :effect "preserve raw source value and continue only after raw preservation exists")

  (:recognized-legacy-ts_created
   :severity INFO
   :effect "normalize and preserve raw value")

  (:recognized-legacy-ts_edited
   :severity INFO
   :effect "normalize and preserve raw value")

  (:unknown-legacy-ts_edited
   :severity WARNING
   :effect "preserve raw value; derived modifiedAt fallback policy required"))

 :modifiedAt-fallback-candidates
 ((:use-createdAt
   :meaning "when edited timestamp cannot be normalized, set modifiedAt equal to createdAt while preserving raw ts_edited"
   :risk "may imply false edit time")

  (:use-import-time
   :meaning "set modifiedAt to import execution time"
   :risk "non-repeatable import; bad for deterministic tests")

  (:use-null-or-optional
   :meaning "represent unknown modified time explicitly"
   :risk "requires record/model support")

  (:use-epoch
   :meaning "sentinel Instant.EPOCH"
   :risk "fake data; should be avoided unless explicitly marked"))

 :recommended-fallback
 (:decision :use-createdAt-only-if-raw-ts-edited-is-preserved
  :must-emit-warning t
  :must-not-use-import-time t
  :must-not-silently-coerce t
  :open-question "A nullable/optional modifiedAt in import vocabulary may be more honest than using createdAt; decide in the raw timestamp preservation design.")

 :rejected-behaviors
 ((:silently-drop-ts_edited
   :reason "would lose source metadata")

  (:silently-coerce-unknown-timestamp-to-now
   :reason "non-deterministic and historically false")

  (:silently-coerce-unknown-timestamp-to-epoch
   :reason "fake timestamp without provenance")

  (:treat-all-ts_edited-errors-as-source-corruption
   :reason "legacy schema permits string-like values and legacy new-entry policy writes empty edited timestamps")

  (:parse-yyMMddHHmm-as-epoch-seconds
   :reason "technically numeric but semantically wrong for legacy timestamp strings")

  (:change-real-source-file
   :reason "source repair is not part of model-first reader boundary")

  (:write-sqlite-during-timestamp-probing
   :reason "source reader remains an adapter returning import records only")

  (:surface-diagnostics-in-ui
   :reason "UI remains downstream projection"))

 :open-questions
 ((:timezone
   "Which timezone should normalize yyMMddHHmm legacy timestamps? Existing data was likely local user time, but the import model must be deterministic.")

  (:century
   "Legacy Tools.getProperDate assumes 20yy. Confirm whether all source timestamps are 2000-2099 before normalization.")

  (:raw-value-patterns
   "The real-source smoke currently reports source ids, not raw timestamp values. A diagnostic report must inspect raw ts_created/ts_edited patterns without changing acceptance.")

  (:empty-edited
   "Should empty ts_edited mean no edit yet, unknown modified time, or modifiedAt equals createdAt?"))

 :proposed-next-executable-slice
 (!implement-zkn3-timestamp-diagnostic-report
  :module zk-source-zkn3
  :mode :diagnostic-report-only
  :source-fields (ts_created ts_edited)
  :real-source-smoke-property "zkn3.real.source"
  :group-errors-by-field-and-pattern t
  :must-not-change-import-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-slice-if-raw-preservation-requires-core-change
 (!design-zkn3-note-raw-timestamp-preservation
  :module zk-core
  :mode :core-vocabulary-design-only
  :record Zkn3NoteRecord
  :source-fields (ts_created ts_edited)
  :requires-timestamp-compatibility-policy t
  :must-not-implement-record-change-yet t)

 :later-slice-if-legacy-format-is-safely-recognized
 (!implement-zkn3-legacy-timestamp-normalization
  :module zk-source-zkn3
  :mode :parser-compatibility-only
  :source-fields (ts_created ts_edited)
  :requires-timestamp-diagnostic-report t
  :requires-raw-preservation-policy t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :confirmed-non-changes
 (:reader-parser-changed-p nil
  :real-source-acceptance-changed-p nil
  :source-repair-p nil
  :sqlite-import-logic-added-p nil
  :ui-wiring-changed-p nil
  :persistence-schema-changed-p nil
  :attachment-record-added-p nil
  :jaxb-dependency-added-p nil
  :Zkn3SourceReader-changed-p nil
  :import-record-classes-changed-p nil
  :FedWikiPane-changed-p nil
  :legacy-swing-repo-edited-p nil
  :repomix-files-edited-or-committed-p nil
  :real-rgb-zkn3-copied-or-committed-p nil))
