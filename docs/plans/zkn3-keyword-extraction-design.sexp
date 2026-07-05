(:id zkn3-keyword-extraction-design
 :kind :mapping-design-only
 :scope AC-06
 :task
 (!repair-zkn3-keyword-extraction-design-for-indexed-keyword-file
  :module zk-source-zkn3
  :previous-design ca96bdfb1c34b6fb94733250ac72199ae14f3fdd
  :mode :mapping-design-only
  :source-field keywords
  :keyword-index-file "keywordFile.xml"
  :target-record Zkn3KeywordRecord
  :requires-complete-note-batch-policy t
  :must-not-implement-keyword-extraction-yet t)
 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority
 #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "b0ffe4f618fa954679ffd3ca899422318edd4278"
   :subject "feat(zettelkasten): extract zkn3 note records")
  (:commit "c02fe4dc4cb0f88cd3c560cea327b9bb0bdb160e"
   :subject "fix(zettelkasten): reject incomplete zkn3 note batches")
  (:commit "ca96bdfb1c34b6fb94733250ac72199ae14f3fdd"
   :subject "docs(zettelkasten): design zkn3 keyword extraction"))

 :requires
 (:complete-note-batch-policy
  (:no-incomplete-imports t
   :no-per-note-skipping t
   :any-required-note-error-invalidates-batch t
   :rejected-batch-returns-zero-records t
   :sqlite-writes-remain-impossible t))

 :current-target-record
 (:class zk.core.importing.Zkn3KeywordRecord
  :fields ((noteSourceId String)
           (keyword String)))

 :corrected-source-model
 (:zknFile.xml
  (:contains zettel
   :zettel-field keywords
   :field-meaning "comma-separated keyword index references")

  :keywordFile.xml
  (:contains keyword-entries
   :indexing "legacy appears to use 1-based positions"
   :entry-text "keyword value"))

 :mapping-scope
 (:source-container zknFile.xml
  :source-element zettel
  :source-field keywords
  :target-record zk.core.importing.Zkn3KeywordRecord
  :requires-existing-note-source-id zknid
  :requires-keyword-dictionary keywordFile.xml
  :out-of-scope (links manlinks luhmann sqlite ui graph-materialization))

 :observed-legacy-evidence
 ((Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB zettel has a required child element keywords represented as String.")
  (Daten-storage-comment
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
   :finding "Legacy data comments describe zettel/keywords as one or more index numbers into keywordFile.xml, separated by commas.")
  (Daten-getKeywordIndexNumbers
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
   :finding "getKeywordIndexNumbers reads the zettel keywords child text and splits it on comma.")
  (Daten-getKeywords
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
   :finding "getKeywords converts each parsed keyword index number through getKeyword, which resolves the value in keywordFile.xml.")
  (Daten-setKeywordIndexNumbers
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
   :finding "setKeywordIndexNumbers stores a comma-separated string of keyword index numbers back into the zettel keywords child.")
  (Constants
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/util/Constants.java"
   :finding "keywordFile.xml is the legacy keyword dictionary member.")
  (ObjectFactory
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/ObjectFactory.java"
   :finding "Generated JAXB factory declares keywords as a string element, not a nested keyword-value collection.")
  (ZipFileProcessor
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/zip/ZipFileProcessor.java"
   :finding "Legacy probe evidence currently locates zknFile.xml; keywordFile.xml probing must be added deliberately in a later slice.")
  (Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root has zettel children under zettelkasten.")
  (XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "Combines DOM parsing with Swing JFrame/JTree/JScrollPane rendering; do not copy this architecture."))

 :field-mapping
 ((zettel.zknid -> Zkn3KeywordRecord.noteSourceId)
  (zettel.keywords.index-token -> keywordFile.xml.entry[index-token].text)
  (keywordFile.xml.entry.text -> Zkn3KeywordRecord.keyword))

 :corrected-field-mapping
 ((zettel.zknid -> Zkn3KeywordRecord.noteSourceId)
  (zettel.keywords.index-token -> :keyword-index-reference)
  (keywordFile.xml.entry[index].text -> Zkn3KeywordRecord.keyword))

 :required-archive-members-for-keyword-extraction
 ("zknFile.xml"
  "keywordFile.xml")

 :keyword-tokenization-policy
 (:source-field keywords
  :token-kind "1-based keywordFile.xml entry index"
  :delimiter ","
  :candidate-delimiters (",")
  :rejected-candidate-delimiters
  ((";" :reason "No legacy zknFile.xml keyword-index evidence found for semicolon-separated keyword references.")
   ("\n" :reason "Observed newline handling is UI drag/drop text behavior, not zknFile.xml persistence."))
  :normalization
  ((:trim-token-whitespace t)
   (:drop-empty-tokens t)
   (:preserve-keyword-case t)
   (:preserve-keyword-text t)
   (:do-not-stem t)
   (:do-not-translate t)
   (:do-not-normalize-case t))
  :open-question "Confirm the authoritative rgb.zkn3 uses the same comma-separated index convention and keywordFile.xml member.")

 :keyword-resolution-policy
 (:keyword-file
  (:member keywordFile.xml
   :role "dictionary mapping keyword index numbers to keyword text"
   :required-when "any valid zettel has a nonblank keywords field")

  :index-token
  (:parse-as positive-integer
   :index-base "legacy accessors treat persisted positions as 1-based"
   :blank-token "treat blank token inside a nonblank keywords field as malformed and reject the complete batch")

  :dictionary-entry
  (:missing-index
   (:severity ERROR
    :effect "reject keyword extraction for the whole batch; do not create a partial keyword graph"))
  (:blank-keyword-text
   (:severity ERROR
    :effect "reject complete batch if referenced by any note"))
  (:duplicate-keyword-for-same-note
   (:severity INFO
    :effect "deduplicate within note after index resolution and trimming"))
  (:duplicate-index-for-same-note
   (:severity INFO
    :effect "deduplicate exact repeated index token for the same note")))

 :keyword-completeness-policy
 (:missing-keywordFile.xml
  (:severity ERROR
   :effect "reject keyword extraction and reject complete import batch once keywords are in scope")

  :malformed-keyword-index-token
  (:severity ERROR
   :effect "reject complete batch; do not silently drop token")

  :keyword-index-out-of-range
  (:severity ERROR
   :effect "reject complete batch; do not create partial keyword graph")

  :blank-keywords-field
  (:severity INFO
   :effect "note has no keyword references; batch remains valid")

  :duplicate-keyword-reference-for-same-note
  (:severity INFO
   :effect "deduplicate exact same resolved keyword for same note")

  :blank-keywordFile-entry
  (:severity ERROR
   :effect "reject complete batch if referenced by any note")

  :keyword-for-invalid-note
  (:severity ERROR
   :effect "impossible after complete-note-batch policy; reject entire batch if encountered"))

 :batch-interaction-policy
 (:depends-on-complete-notes t
  :if-note-batch-rejected "return zero notes and zero keywords; preserve note ERROR diagnostics"
  :if-keyword-batch-rejected "return zero records for notes, keywords, links, and sequences; emit ERROR diagnostics identifying keyword source fields and an ERROR summary"
  :reason "Keyword records attach to note source IDs; partial notes or partial keyword graph would create an incomplete import.")

 :rejected-behaviors
 ((:create-keyword-without-note-source-id
   :reason "keyword records must remain attached to a source note")
  (:treat-zettel-keywords-as-literal-keyword-text
   :reason "legacy evidence says zettel/keywords stores keyword index numbers into keywordFile.xml")
  (:silently-drop-ambiguous-keyword-data
   :reason "would create incomplete graph import")
  (:normalize-case
   :reason "case normalization is a later semantic policy, not source reading")
  (:write-keywords-to-sqlite
   :reason "source reader returns import records only")
  (:render-keywords-in-ui
   :reason "source reader must remain UI-free")
  (:copy-XMLViewer-architecture
   :reason "XMLViewer couples parser concerns to Swing rendering"))

 :rejected-assumptions
 ((:keywords-field-contains-literal-keyword-values
   :reason "legacy evidence indicates zettel keywords are comma-separated index references into keywordFile.xml")

  (:silently-drop-unresolvable-keyword-index
   :reason "would create incomplete graph import")

  (:implement-keywords-from-zknFile-only
   :reason "keyword values require keywordFile.xml resolution"))

 :diagnostics
 ((:missing-keywords-field
   :severity INFO
   :field keywords
   :effect "no Zkn3KeywordRecord values for that note")
  (:blank-keywords-field
   :severity INFO
   :field keywords
   :effect "no Zkn3KeywordRecord values for that note")
  (:malformed-keyword-index
   :severity ERROR
   :field keywords
   :effect "reject whole import batch")
  (:missing-keywordFile
   :severity ERROR
   :field keywordFile.xml
   :effect "reject whole import batch when any nonblank keywords field requires dictionary resolution")
  (:keyword-index-out-of-range
   :severity ERROR
   :field keywordFile.xml
   :effect "reject whole import batch")
  (:blank-keywordFile-entry
   :severity ERROR
   :field keywordFile.xml
   :effect "reject whole import batch if referenced by any note")
  (:missing-dictionary-entry
   :severity ERROR
   :field keywordFile.xml
   :effect "reject whole import batch")
  (:duplicate-keyword-for-note
   :severity INFO
   :field keywords
   :effect "deduplicate exact duplicate for that note"))

 :excluded-from-this-slice
 ((links    :later !design-zkn3-link-extraction)
  (manlinks :later !design-zkn3-manlink-extraction)
  (luhmann  :later !design-zkn3-sequence-extraction)
  (sqlite   :later !design-zkn3-import-application-to-repositories)
  (ui       :later !design-zkn3-import-ui-projection)
  (graph-materialization :later !design-zkn3-import-application-to-repositories))

 :constraints
 (:must-not-implement-keyword-extraction t
  :must-not-create-Zkn3KeywordRecord-in-production-code t
  :must-not-change-Zkn3DomSourceReader t
  :must-not-change-Zkn3SourceReader t
  :must-not-change-import-record-classes t
  :must-not-add-jaxb t
  :must-not-add-zip-probing-for-keywordFile.xml t
  :must-not-add-sqlite-import-logic t
  :must-not-touch-ui t
  :must-not-edit-FedWikiPane t
  :must-not-edit-legacy-swing-repo t
  :must-not-edit-repomix-files t)

 :later-implementation-acceptance-criteria
 ((:criterion "Zkn3DomSourceReader maps keyword records only after note extraction has produced a complete valid note batch.")
  (:criterion "The implementation resolves comma-separated zettel/keywords index tokens through keywordFile.xml before constructing Zkn3KeywordRecord.")
  (:criterion "Each Zkn3KeywordRecord uses zettel.zknid as noteSourceId and resolved dictionary text as keyword.")
  (:criterion "Malformed keyword index tokens or missing dictionary entries reject the whole import batch rather than producing partial keyword records.")
  (:criterion "Missing or blank keywords produces no keyword records for that note and does not reject the batch.")
  (:criterion "The implementation does not map links, manlinks, luhmann sequences, SQLite writes, or UI behavior.")
  (:criterion "Tests use temporary ZIP/XML fixtures and do not read the authoritative rgb.zkn3 file."))

 :next-executable-probe-before-implementation
 (!implement-zkn3-keyword-file-entry-probe
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :probe-only
  :entry "keywordFile.xml"
  :requires-complete-note-batch-policy t
  :must-not-map-keywords-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-implementation-task
 (!implement-zkn3-keyword-extraction
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :keyword-records-only
  :source-field keywords
  :keyword-index-file "keywordFile.xml"
  :target-record Zkn3KeywordRecord
  :requires-complete-note-batch-policy t
  :must-not-map-links-manlinks-or-sequences-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
