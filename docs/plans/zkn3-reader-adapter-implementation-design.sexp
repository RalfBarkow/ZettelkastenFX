(:id zkn3-reader-adapter-implementation-design
 :kind :adapter-design-only
 :scope AC-06
 :task
 (!design-zkn3-reader-adapter-implementation
  :mode :adapter-design-only
  :requires-port zk.core.ports.Zkn3SourceReader
  :requires-records zk.core.importing.Zkn3ImportBatch
  :must-not-implement-reader-yet t)
 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority
 #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "84c5927d65d4cfd1db8b2458d9adf3e4f356bdd0"
   :subject "test(zettelkasten): guard model-first core boundary")
  (:commit "017cf074dfd3333dfae9ddb771eebb565402b537"
   :subject "docs(zettelkasten): design zkn3 source reader boundary")
  (:commit "13181b1fb0e0dd5b21c8e584f759adcb70c1f14d"
   :subject "feat(zettelkasten): define zkn3 source reader port")
  (:commit "fb928e098d9af481cf48ee5fc22b17d6e6a20c15"
   :subject "docs(zettelkasten): design zkn3 import record types")
  (:commit "59a49000a7135ec10f019f5cb933761da223d9d8"
   :subject "feat(zettelkasten): define zkn3 import record types")
  (:commit "d3d8665e274fb5d4649a8848810f9dda38542d10"
   :subject "refactor(zettelkasten): return zkn3 import batch from reader port"))

 :current-core-contract
 (:port zk.core.ports.Zkn3SourceReader
  :method "Zkn3ImportBatch read(Path zkn3File) throws IOException"
  :batch zk.core.importing.Zkn3ImportBatch
  :records (Zkn3NoteRecord
            Zkn3KeywordRecord
            Zkn3LinkRecord
            Zkn3SequenceRecord
            Zkn3ImportDiagnostic)
  :link-kinds (NORMAL MANUAL)
  :must-not-import (javax.swing java.awt javafx zk.storage zk.ui))

 :observed-legacy-extraction-path
 ((ZipFileProcessor
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/zip/ZipFileProcessor.java"
   :finding "Owns ZipFile access, locates zknFile.xml, and unmarshals it into the generated Zettelkasten JAXB root.")
  (Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root for zettelkasten with zettel children plus firstzettel and lastzettel attributes.")
  (Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB record for title, content, author, keywords, manlinks, links, misc, luhmann, zknid, timestamps, rating, ratingcount, and fromBibTex.")
  (Links
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Links.java"
   :finding "Generated JAXB collection of links/link string values.")
  (XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "Combines DOM parsing with Swing JFrame, JTree, DefaultMutableTreeNode, and JScrollPane rendering. The future adapter must not copy this structure."))

 :proposed-module
 (:name zk-source-zkn3
  :package-root zk.source.zkn3
  :depends-on (zk-core)
  :implements zk.core.ports.Zkn3SourceReader
  :justification "ZKN3 is a source file format adapter. The module name keeps source reading distinct from core model ownership, SQLite persistence, and UI projection."
  :must-not-depend-on (zk-storage-sqlite zk-ui-javafx javax.swing java.awt javafx)
  :must-not-write-to-sqlite t
  :must-not-update-ui t)

 :rejected-module-name
 (:name zk-import-zkn3
  :reason "Import can imply repository writes or orchestration. The first adapter should only read a source file into a neutral Zkn3ImportBatch.")

 :proposed-adapter
 (:class zk.source.zkn3.Zkn3ZipSourceReader
  :implements zk.core.ports.Zkn3SourceReader
  :method "Zkn3ImportBatch read(Path zkn3File) throws IOException"
  :role "Open a .zkn3 source archive, inspect source members, map XML facts to core import records, and return an immutable batch without side effects.")

 :adapter-boundary
 (:may-depend-on (zk-core java.base java.xml)
  :may-depend-on-conditionally (jakarta.xml.bind javax.xml.bind)
  :must-not-depend-on (zk-storage-sqlite zk-ui-javafx javax.swing java.awt javafx)
  :must-not-use-legacy-swing-viewers t
  :must-not-write-to-sqlite t
  :must-not-update-ui t
  :must-not-own-schema-migrations t
  :must-not-change-core-port-shape t)

 :future-extraction-steps
 ((!open-zkn3-zip :path ?zkn3-file)
  (!locate-zknFile.xml :inside ?zip)
  (!parse-zknFile.xml :as :document-or-jaxb)
  (!build-source-note-index :by (:source-ordinal :zknid))
  (!map-zettel-elements-to-Zkn3NoteRecord)
  (!resolve-keyword-dictionary :from keywordFile.xml)
  (!map-keywords-to-Zkn3KeywordRecord)
  (!map-links-to-Zkn3LinkRecord :kind NORMAL)
  (!map-manlinks-to-Zkn3LinkRecord :kind MANUAL)
  (!map-luhmann-to-Zkn3SequenceRecord)
  (!collect-diagnostics)
  (!return-Zkn3ImportBatch)))

 :mapping
 ((zknid -> Zkn3NoteRecord.sourceId)
  (title -> Zkn3NoteRecord.title)
  (content -> Zkn3NoteRecord.body)
  (ts_created -> Zkn3NoteRecord.createdAt)
  (ts_edited -> Zkn3NoteRecord.modifiedAt)
  (rating -> Zkn3NoteRecord.rating)
  (keywords -> Zkn3KeywordRecord)
  (links -> Zkn3LinkRecord :kind NORMAL :open-question "links/link may be attachment or external-link data rather than current note-to-note LinkRepository data.")
  (manlinks -> Zkn3LinkRecord :kind MANUAL)
  (luhmann -> Zkn3SequenceRecord)
  (misc -> :open-question)
  (author -> :open-question)
  (fromBibTex -> :open-question)
  (ratingcount -> :open-question)))

 :record-level-notes
 ((Zkn3ImportBatch
   :note "The adapter should construct the existing neutral batch and should not call repositories.")
  (Zkn3NoteRecord
   :note "sourceId preserves zknid. Timestamp and rating parsing errors should produce diagnostics according to a later policy.")
  (Zkn3KeywordRecord
   :note "Existing record stores noteSourceId and keyword text; keywordFile.xml index resolution belongs inside the source adapter.")
  (Zkn3LinkRecord
   :note "Existing record stores fromSourceId, toSourceId, and kind. The adapter must not invent SQLite IDs.")
  (Zkn3SequenceRecord
   :note "Existing record stores parentSourceId, childSourceId, and order. luhmann CSV order is the likely order source.")
  (Zkn3ImportDiagnostic
   :note "Existing diagnostic shape can report severity, sourceId, field, and message. Broader source-member diagnostics are a later record-design slice."))

 :rejected-placements
 ((:inside-zk-core
   :reason "ZIP/XML file-format handling is adapter behavior, not core model or port ownership.")
  (:inside-zk-storage-sqlite
   :reason "Source reading is not persistence and must not be coupled to repository writes.")
  (:inside-zk-ui-javafx
   :reason "Source reading must be available without JavaFX or a running UI.")
  (:inside-legacy-swing-repo
   :reason "The legacy repo is behavioral evidence only for this refactor.")
  (:reuse-legacy-XMLViewer
   :reason "XMLViewer couples parsing to Swing rendering. It is anti-pattern evidence, not an adapter basis."))

 :parser-design-options
 ((:option dom
   :benefit "Uses java.xml with no JAXB dependency and is enough for the current small source vocabulary."
   :risk "Can become ad hoc unless field extraction is isolated and covered by fixtures.")
  (:option stax
   :benefit "Streaming java.xml parser with lower memory use for large archives."
   :risk "More bookkeeping for cross-reference resolution and diagnostics.")
  (:option jaxb
   :benefit "Matches the legacy extraction path and generated XML shape."
   :risk "Adds JAXB dependency decisions and risks copying generated legacy representation into adapter code.")
  (:recommendation :defer-parser-choice
   :reason "This slice records the adapter boundary only. The implementation slice should choose DOM, StAX, or JAXB with fixtures and diagnostics policy."))

 :open-decisions
 ((:id parser
   :question "Should the parser use DOM, StAX, or JAXB?")
  (:id jaxb-source
   :question "If JAXB is used, should generated classes be copied into the adapter module, regenerated from schema, or replaced by a small hand-written parser?")
  (:id malformed-links
   :question "Should malformed links become diagnostics or fail the whole import?")
  (:id luhmann-encoding
   :question "How exactly is luhmann encoded, including empty values, duplicates, missing children, and cycles?")
  (:id manlinks-semantics
   :question "Are manlinks semantically a distinct link kind or a legacy UI distinction?")
  (:id source-metadata
   :question "Should author, misc, fromBibTex, and ratingcount be modeled now or preserved as diagnostics/metadata?")
  (:id normal-links
   :question "Does links/link describe note-to-note links, attachments, external URLs, or multiple legacy concepts?")
  (:id dictionary-files
   :question "Which dictionary members are required for a successful import, and which can be missing with diagnostics?"))

 :later-implementation-acceptance-criteria
 ((:criterion "A new adapter module is created outside zk-core, zk-storage-sqlite, and zk-ui-javafx.")
  (:criterion "The adapter module depends on zk-core and implements zk.core.ports.Zkn3SourceReader.")
  (:criterion "The adapter returns zk.core.importing.Zkn3ImportBatch and does not write to repositories.")
  (:criterion "The adapter has no imports of javax.swing, java.awt, javafx, zk.storage, or zk.ui.")
  (:criterion "The adapter locates zknFile.xml inside the authoritative .zkn3 source archive.")
  (:criterion "The adapter maps notes, keywords, manual links, sequence relations, and diagnostics into existing core import records.")
  (:criterion "The adapter keeps SQLite import/application logic in a later orchestration slice.")
  (:criterion "The adapter does not edit or depend on legacy Swing XMLViewer behavior.")
  (:criterion "The implementation slice includes fixtures or tests proving malformed source facts become the chosen diagnostics/failure behavior."))

 :later-implementation-task
 (!create-zkn3-reader-adapter-module
  :module zk-source-zkn3
  :implements zk.core.ports.Zkn3SourceReader
  :returns zk.core.importing.Zkn3ImportBatch
  :must-not-depend-on (zk-storage-sqlite zk-ui-javafx javax.swing java.awt javafx)
  :mode :module-scaffold-only))
