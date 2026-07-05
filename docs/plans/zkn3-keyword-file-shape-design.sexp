(:id zkn3-keyword-file-shape-design
 :kind :shape-design-only
 :scope AC-06
 :task
 (!design-zkn3-keyword-file-shape
  :module zk-source-zkn3
  :mode :shape-design-only
  :entry "keywordFile.xml"
  :requires-root-observation t
  :must-not-map-keywords-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority
 #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "855fefffb18813288ae765c0f3538e2ce4a0a759"
   :subject "docs(zettelkasten): refine zkn3 keyword index design")
  (:commit "df4b3963733e670e98789e2040849e7e4c96b2cb"
   :subject "feat(zettelkasten): probe zkn3 keyword archive entry")
  (:commit "7ca08bc748c1b4a148481b57cb38615ffe9dca15"
   :subject "feat(zettelkasten): probe zkn3 keyword xml root"))

 :slice-boundary
 (:records-shape-only t
  :implements-keyword-file-shape-validation-p nil
  :implements-keyword-extraction-p nil
  :implements-keyword-index-resolution-p nil
  :writes-sqlite-p nil
  :touches-ui-p nil)

 :legacy-evidence
 ((:filename-constant
   (:constant Constants.keywordFileName
    :value "keywordFile.xml"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/util/Constants.java"))

  (:document-root-constant
   (:constant Daten.DOCUMENT_KEYWORDS
    :value "keywords"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"))

  (:entry-element-constant
   (:constant Daten.ELEMENT_ENTRY
    :value "entry"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"))

  (:zettel-keywords-field
   (:constant Daten.ELEMENT_KEYWORD
    :value "keywords"
    :meaning "comma-separated keyword index references into keywordFile.xml"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"))

  (:frequency-attribute
   (:constant Daten.ATTRIBUTE_FREQUENCIES
    :value "f"
    :meaning "legacy frequency metadata; not mapped in this slice"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"))

  (:format-comment
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "legacy comments describe zettel/keywords as index numbers into keywordFile, separated by commas, and show keywordFile.xml as a keywords root with entry children"))

  (:legacy-accessors
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :getKeyword "retrieves a keywordFile entry by 1-based legacy position"
    :getKeywords "splits zettel keywords on comma and resolves each token through getKeyword"
    :getKeywordIndexNumbers "splits zettel keywords on comma and parses integer index tokens")))

 :current-target-record
 (:class zk.core.importing.Zkn3KeywordRecord
  :fields ((noteSourceId String)
           (keyword String))
  :frequency-field-p nil)

 :expected-keyword-file-shape
 (:archive-entry "keywordFile.xml"
  :root-element "keywords"
  :child-elements
  ((:name "entry"
    :text "keyword value"
    :attributes
    ((:name "f"
      :meaning "frequency metadata"
      :required-p nil
      :mapped-in-this-slice-p nil)))))

 :index-resolution-model
 (:source-reference zettel.keywords
  :reference-format "comma-separated integer tokens"
  :target-file "keywordFile.xml"
  :target-root "keywords"
  :target-entry "entry"
  :index-base :legacy-position-based
  :open-question "Confirm whether indexes are strictly 1-based by implementation test before resolving real keywords.")

 :keyword-file-completeness-policy
 (:missing-keywordFile.xml
  (:severity ERROR
   :effect "reject complete batch")

  :malformed-keywordFile.xml
  (:severity ERROR
   :effect "reject complete batch")

  :wrong-root
  (:severity ERROR
   :expected-root "keywords"
   :effect "reject complete batch")

  :non-entry-child-under-keywords
  (:severity WARNING
   :effect "ignore only if clearly non-semantic whitespace/comment; otherwise reject in later implementation")

  :blank-entry-text
  (:severity ERROR
   :effect "reject complete batch if referenced by any zettel keyword token")

  :missing-frequency-attribute
  (:severity INFO
   :effect "batch remains valid; frequency not mapped")

  :malformed-frequency-attribute
  (:severity WARNING
   :effect "batch remains valid; frequency not mapped"))

 :import-completeness-policy-preserved
 (:no-incomplete-imports t
  :no-per-note-skipping t
  :no-partial-successful-batches t
  :wrong-or-malformed-keywordFile.xml "rejects keyword-aware import batch"
  :rejected-batch-returns-zero-records t
  :rejected-batch-diagnostics ERROR
  :sqlite-writes-remain-impossible t)

 :rejected-assumptions
 ((:keywordFile-root-is-unknown
   :reason "legacy constants and format comment identify root as keywords")

  (:keyword-values-come-directly-from-zettel-keywords
   :reason "zettel keywords contains keyword-file index references")

  (:frequency-is-core-keyword-data
   :reason "frequency appears to be legacy metadata, not required for Zkn3KeywordRecord")

  (:resolve-keywords-before-validating-file-shape
   :reason "would combine shape validation and graph mapping in one slice"))

 :next-executable-task
 (!implement-zkn3-keyword-file-shape-validation
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :shape-validation-only
  :entry "keywordFile.xml"
  :expected-root "keywords"
  :expected-child "entry"
  :must-not-map-keywords-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-task-not-next
 (!implement-zkn3-keyword-index-resolution
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :resolution-only
  :source-field zettel.keywords
  :keyword-index-file "keywordFile.xml"
  :keyword-root "keywords"
  :keyword-entry "entry"
  :must-not-map-links-manlinks-or-sequences-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
