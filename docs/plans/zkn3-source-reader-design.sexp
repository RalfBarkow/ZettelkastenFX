(:id zkn3-source-reader-design
 :kind :design-inspection
 :scope AC-06
 :task (!inspect-zkn3-source-reader-design
        :source #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
        :mode :design-only
        :must-not-implement-yet t)
 :status :design-only
 :source-authority "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :completed-boundary-commit
 "84c5927d65d4cfd1db8b2458d9adf3e4f356bdd0 test(zettelkasten): guard model-first core boundary"

 :constraints
 (:must-not-implement Zkn3SourceReader
  :must-not-add-java-source t
  :must-not-edit-ui t
  :must-not-edit-fedwikipane t
  :must-not-repair-javafx-web-blocker t
  :must-not-change-persistence-schema t
  :must-not-edit-generated-jaxb t
  :must-not-edit-repomix-output t)

 :observed-source-container
 (:type zip-archive
  :members
  ((metaInformation.xml :role :metadata)
   (zknFile.xml :role :primary-note-records)
   (authorFile.xml :role :author-dictionary)
   (keywordFile.xml :role :keyword-dictionary)
   (bookmarks.xml :role :legacy-projection)
   (searchrequests.xml :role :legacy-projection)
   (synonyms.xml :role :legacy-auxiliary-data)
   (references.bib :role :bibtex-bridge)
   (desktop.xml :role :legacy-workspace-projection)
   (desktopme.xml :role :legacy-workspace-projection)
   (desktopnt.xml :role :legacy-workspace-projection)))

 :observed-legacy-extraction-path
 ((ZipFileProcessor
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/zip/ZipFileProcessor.java"
   :finding "Opens the .zkn3 ZipFile, locates zknFile.xml, and unmarshals it into ch.dreyeck.zettelkasten.xml.Zettelkasten using JAXB.")
  (Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root for zettelkasten with zettel children and firstzettel/lastzettel attributes.")
  (Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB element with title, content, author, keywords, manlinks, links, misc, luhmann, zknid, timestamps, and rating attributes.")
  (Links
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Links.java"
   :finding "Generated JAXB collection of attachment/link strings under links/link.")
  (Daten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
   :finding "Documents that author and keyword fields contain comma-separated index numbers into authorFile.xml and keywordFile.xml; manlinks and luhmann contain entry numbers.")
  (XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "DOM parsing and Swing tree rendering are coupled in a JFrame/JTree object and must not be copied into zk-core."))

 :target-core-concepts-today
 (:model (NoteDTO NoteId)
  :ports (NoteRepository KeywordRepository LinkRepository SequenceRepository))

 :proposed-future-port-shape
 (:name Zkn3SourceReader
  :module zk-core
  :package zk.core.ports
  :mode :future-interface-only
  :inputs (:source-path)
  :emits (:note-records :keyword-records :link-records :sequence-records :import-diagnostics)
  :must-not-depend-on (javax.swing java.awt javafx zk.storage zk.ui)
  :should-not-depend-on (:generated-jaxb-classes :sqlite :javafx-runtime :swing-runtime)
  :note "This slice does not create the port. A later slice should define records/interfaces only after resolving identity and diagnostics shape.")

 :mapping-from-zkn-file-to-core-concepts
 ((zettelkasten
   :attributes ((firstzettel :legacy-display-order-first :target :open-question)
                (lastzettel :legacy-display-order-last :target :open-question))
   :children zettel)
  (zettel
   :legacy-identity ((ordinal-position :source "1-based position in zknFile.xml")
                     (zknid :source "@zknid"))
   :target-note-id "Open question: NoteId is currently int; first import can use ordinal position, but durable zknid preservation needs an explicit design."
   :fields
   ((title :target NoteDTO.title)
    (content :target NoteDTO.body :note "Keep legacy UBB/HTML-like text unchanged in the reader; rendering/normalization remains projection/application work.")
    (ts_created :target NoteDTO.createdAt :open-question "Legacy timestamp parsing and timezone policy.")
    (ts_edited :target NoteDTO.modifiedAt :open-question "May be empty; define fallback to createdAt or diagnostic.")
    (rating :target NoteDTO.rating :open-question "Legacy value is optional/simple text; define parse/default/diagnostic rules.")
    (keywords :target KeywordRepository.add :mapping "Comma-separated keywordFile.xml entry numbers.")
    (author :target :no-current-core-port :mapping "Comma-separated authorFile.xml entry numbers; retain as diagnostics or defer until an author/source port exists.")
    (manlinks :target LinkRepository.add :mapping "Comma-separated destination entry numbers; source is current zettel ordinal.")
    (links :target :not-core-note-link :mapping "Attachment/file/web links under links/link; do not map to LinkRepository without a separate attachment port.")
    (misc :target :open-question :note "Could remain out of NoteDTO or be merged into body only by explicit later design.")
    (luhmann :target SequenceRepository.insertChild :mapping "Comma-separated child entry numbers; CSV order is child order.")))
  (authorFile.xml
   :root authors
   :entry-attributes (f authid authts bibkey)
   :target :open-question
   :note "Needed to resolve author indexes, but there is no AuthorRepository in zk-core today.")
  (keywordFile.xml
   :root keywords
   :entry-attributes (f keywid keywts)
   :target KeywordRepository
   :note "Entry position is the numeric key referenced by zettel/keywords.")
  (desktop.xml
   :target :defer
   :note "Contains workspace/projection ordering. Do not treat as core sequence unless a later design explicitly chooses that mapping."))

 :future-reader-output-records
 ((note-record
   :fields (source-ordinal source-zknid title body created-at modified-at rating))
  (keyword-record
   :fields (note-source-ordinal keyword-source-ordinal keyword-text))
  (link-record
   :fields (source-note-ordinal target-note-ordinal :kind manual-link))
  (sequence-record
   :fields (parent-note-ordinal child-note-ordinal order-index :kind luhmann-trail))
  (import-diagnostic
   :fields (severity source-member source-ordinal message)))

 :rejected-designs
 ((:design "Reuse XMLViewer for parsing"
   :reason "It couples XML parsing to Swing JFrame/JTree rendering.")
  (:design "Put the reader in zk-storage-sqlite"
   :reason "ZKN3 source reading is input adaptation, not SQLite storage; zk-core must not depend on storage.")
  (:design "Have zk-core read ZipFile with UI callbacks"
   :reason "Core ports must stay UI-free and callback-free from Swing/JavaFX views.")
  (:design "Map links/link attachments into LinkRepository"
   :reason "Current LinkRepository is note-to-note; attachments need a separate concept.")
  (:design "Implement against generated JAXB classes in zk-core"
   :reason "The generated legacy classes carry javax.xml.bind-era assumptions and should remain evidence, not core dependency."))

 :open-questions
 ((:id identity
   :question "Should NoteId use legacy ordinal numbers, persisted zknid values, or a two-level source identity plus assigned storage ID?")
  (:id timestamps
   :question "What exact timestamp format and timezone should ts_created and ts_edited use, and how should empty ts_edited be represented?")
  (:id authors
   :question "Should authors remain diagnostics/source metadata, or does zk-core need an AuthorRepository/source-reference port before import?")
  (:id attachments
   :question "Should links/link become a future AttachmentRepository port, remain source metadata, or stay out of the first import?")
  (:id misc
   :question "Should misc remain separate source metadata or be appended/projected into note body?")
  (:id display-order
   :question "Do firstzettel/lastzettel encode meaningful order in current files, or is luhmann the only core sequence candidate?")
  (:id diagnostics
   :question "Should malformed references fail the read, be skipped with diagnostics, or produce placeholder records?"))

 :later-implementation-acceptance-criteria
 ((:criterion "A core port/interface can be introduced without imports of javax.swing, java.awt, javafx, zk.storage, or zk.ui.")
  (:criterion "A source-reader adapter can identify zknFile.xml and required dictionary files from the .zkn3 ZIP without UI classes.")
  (:criterion "Reading emits note, keyword, note-link, sequence, and diagnostic records without writing to SQLite.")
  (:criterion "Keyword references resolve through keywordFile.xml entry positions.")
  (:criterion "Manual links and Luhmann trails resolve through source note ordinals and report diagnostics for missing targets.")
  (:criterion "Attachment links are not conflated with note-to-note links.")
  (:criterion "Legacy Swing and generated JAXB code remain evidence unless an explicit later slice chooses an adapter boundary."))

 :next-task-candidate
 (!define-zkn3-source-reader-port
  :mode :core-interface-only
  :requires-design-artifact docs/plans/zkn3-source-reader-design.sexp))
