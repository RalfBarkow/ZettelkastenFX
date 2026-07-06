(:id zkn3-keyword-index-resolution-design
 :kind :resolution-design-only
 :scope AC-06
 :task
 (!design-zkn3-keyword-index-resolution
  :module zk-source-zkn3
  :mode :resolution-design-only
  :source-field zettel.keywords
  :keyword-index-file "keywordFile.xml"
  :keyword-root "keywords"
  :keyword-entry "entry"
  :requires-keyword-file-shape-validation t
  :must-not-map-keywords-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority
 #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "855fefffb18813288ae765c0f3538e2ce4a0a759"
   :subject "docs(zettelkasten): refine zkn3 keyword index design")
  (:commit "efc1b8912d8ad9724d309fb8fdcc5b845bbdfbd2"
   :subject "docs(zettelkasten): design zkn3 keyword file shape")
  (:commit "c56c607b278d262759a881cfdc21550de5e2d67c"
   :subject "feat(zettelkasten): validate zkn3 keyword file shape"))

 :slice-boundary
 (:design-only t
  :implements-keyword-index-resolution-p nil
  :implements-keyword-extraction-p nil
  :creates-Zkn3KeywordRecord-in-production-code-p nil
  :writes-sqlite-p nil
  :touches-ui-p nil)

 :resolution-source-model
 (:note-source
  (:file "zknFile.xml"
   :element zettel
   :id-field zknid
   :keyword-reference-field keywords
   :reference-format "comma-separated integer tokens")

  :keyword-source
  (:file "keywordFile.xml"
   :root "keywords"
   :entry "entry"
   :entry-text "keyword value"))

 :legacy-evidence
 ((:keyword-file-name
   (:constant Constants.keywordFileName
    :value "keywordFile.xml"
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/util/Constants.java"))

  (:keyword-root-and-entry
   (:constants ((Daten.DOCUMENT_KEYWORDS "keywords")
                (Daten.ELEMENT_ENTRY "entry")
                (Daten.ELEMENT_KEYWORD "keywords"))
    :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"))

  (:format-comment
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "zettel keywords are one or more index numbers indicating entries of keywordFile, separated by commas"))

  (:getKeyword
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "getKeyword documents keyword positions from 1 to getCount(KWCOUNT), retrieves keywordFile by that position, and returns empty string if no such keyword exists"))

  (:retrieveElement
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "retrieveElement accepts positions from 1 to getCount and indexes the XML content list with pos - 1"))

  (:getKeywords
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "getKeywords returns null for blank zettel keywords; otherwise splits on comma, parses each token as integer, and calls getKeyword(token)"))

  (:getKeywordIndexNumbers
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "getKeywordIndexNumbers returns null for blank zettel keywords; otherwise splits on comma and parses each token as integer"))

  (:getKeywordFrequencies
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :finding "legacy frequency counting breaks after the first matching keyword index per zettel, so repeated same-note references are not counted multiple times")))

 :index-base
 (:decision :one-based
  :meaning "zettel keyword token 1 resolves to the first keywordFile.xml entry"
  :conversion "entryIndex = token - 1"
  :zero-valid-p nil
  :evidence "getKeyword documents one-based keyword positions and retrieveElement reads elementList.get(pos - 1)")

 :tokenization-policy
 (:source-field zettel.keywords
  :delimiter ","
  :trim-whitespace t
  :drop-empty-tokens t
  :token-type integer
  :preserve-original-token-in-diagnostics t)

 :resolution-algorithm
 ((!collect-keyword-entries :from "keywordFile.xml" :root "keywords" :entry "entry")
  (!for-each-zettel :from "zknFile.xml")
  (!read-note-source-id :field zknid)
  (!read-keyword-index-tokens :field keywords)
  (!parse-each-token-as-integer)
  (!resolve-index-to-keyword-entry-text)
  (!deduplicate-resolved-keywords-per-note)
  (!prepare-Zkn3KeywordRecord-values-later))

 :keyword-index-completeness-policy
 (:blank-keywords-field
  (:severity INFO
   :effect "note has no keyword references; batch remains valid")

  :empty-token-after-splitting
  (:severity INFO
   :effect "ignore empty token only if caused by extra delimiter or whitespace; preserve diagnostic if useful")

  :non-integer-token
  (:severity ERROR
   :effect "reject complete batch; do not drop token")

  :zero-index-when-one-based
  (:severity ERROR
   :effect "reject complete batch")

  :negative-index
  (:severity ERROR
   :effect "reject complete batch")

  :index-out-of-range
  (:severity ERROR
   :effect "reject complete batch")

  :referenced-blank-keyword-entry
  (:severity ERROR
   :effect "reject complete batch")

  :duplicate-reference-same-note
  (:severity INFO
   :effect "deduplicate exact same resolved keyword for same note")

  :duplicate-keyword-text-different-entries
  (:severity INFO
   :effect "preserve resolved keyword text; deduplication per note happens after resolution"))

 :future-target-mapping
 ((zettel.zknid                      -> Zkn3KeywordRecord.noteSourceId)
  (keywordFile.entry[resolved-index] -> Zkn3KeywordRecord.keyword))

 :batch-interaction-policy
 (:requires-complete-note-batch t
  :requires-valid-keyword-file-shape t
  :if-note-batch-rejected "return zero notes and zero keywords; preserve note ERROR diagnostics"
  :if-keyword-index-resolution-rejected "return zero records for notes, keywords, links, and sequences; emit ERROR diagnostics identifying zettel keywords or keywordFile.xml and an ERROR import summary"
  :reason "Keyword records attach to note source IDs and resolved dictionary text; unresolved references would create an incomplete import graph.")

 :rejected-behaviors
 ((:treat-zettel-keywords-as-literal-keyword-text
   :reason "legacy source model uses keyword index references")

  (:silently-drop-non-integer-token
   :reason "would create incomplete keyword graph")

  (:silently-drop-out-of-range-index
   :reason "would create incomplete keyword graph")

  (:fallback-to-token-as-keyword
   :reason "would mix reference syntax with keyword value semantics")

  (:create-keyword-record-before-note-batch-valid
   :reason "keyword records require complete valid note substrate")

  (:write-keywords-to-sqlite
   :reason "source reader returns import records only")

  (:surface-keywords-in-ui
   :reason "source reader must remain UI-free"))

 :future-tests
 ((:one-based-resolution
   "token 1 resolves to first entry if one-based decision is confirmed")

  (:blank-keywords
   "blank keywords field produces no keyword records and does not reject batch")

  (:multiple-keywords
   "comma-separated tokens resolve to multiple keyword values")

  (:whitespace-around-tokens
   "tokens are trimmed before integer parsing")

  (:non-integer-token
   "rejects entire batch with ERROR diagnostics")

  (:out-of-range-index
   "rejects entire batch with ERROR diagnostics")

  (:duplicate-reference
   "deduplicates repeated resolved keyword for same note")

  (:blank-referenced-entry
   "rejects entire batch if referenced entry text is blank"))

 :next-executable-task
 (!implement-zkn3-keyword-index-resolution-probe
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :resolution-probe-only
  :source-field zettel.keywords
  :keyword-index-file "keywordFile.xml"
  :keyword-root "keywords"
  :keyword-entry "entry"
  :must-not-create-Zkn3KeywordRecord-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-mapping-task-not-next
 (!implement-zkn3-keyword-record-mapping
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :keyword-records-only
  :source-field zettel.keywords
  :keyword-index-file "keywordFile.xml"
  :target-record Zkn3KeywordRecord
  :requires-keyword-index-resolution t
  :must-not-map-links-manlinks-or-sequences-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
