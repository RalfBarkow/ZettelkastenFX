(:id zkn3-reader-record-types-design
 :kind :design-only
 :scope AC-06
 :task (!design-zkn3-reader-record-types
        :mode :design-only
        :requires-port zk.core.ports.Zkn3SourceReader
        :must-not-implement-reader-yet t)
 :status :design-only
 :source-authority
 #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :requires
 ((:commit "84c5927d65d4cfd1db8b2458d9adf3e4f356bdd0"
   :subject "test(zettelkasten): guard model-first core boundary")
  (:commit "017cf074dfd3333dfae9ddb771eebb565402b537"
   :subject "docs(zettelkasten): design zkn3 source reader boundary")
  (:commit "13181b1fb0e0dd5b21c8e584f759adcb70c1f14d"
   :subject "feat(zettelkasten): define zkn3 source reader port"))

 :constraints
 (:must-not-add-java-source t
  :must-not-change Zkn3SourceReader
  :must-not-implement-reader t
  :must-not-parse-zip t
  :must-not-parse-xml-or-jaxb t
  :must-not-import-to-sqlite t
  :must-not-edit-generated-jaxb t
  :must-not-edit-fedwikipane t
  :must-not-repair-javafx-web-blocker t
  :must-not-change-ui-wiring t
  :must-not-change-persistence-schema t
  :must-not-edit-repomix-output t)

 :zkn3-fields
 (zknid title content author keywords manlinks links misc luhmann
  ts_created ts_edited rating ratingcount fromBibTex)

 :observed-evidence
 ((Zkn3SourceReader
   :file "zk-core/src/main/java/zk/core/ports/Zkn3SourceReader.java"
   :finding "Current core port only exposes Stream<NoteDTO> readNotes(Path). Full import records are intentionally not defined yet.")
  (NoteDTO
   :file "zk-core/src/main/java/zk/core/model/NoteDTO.java"
   :fields (id title body createdAt modifiedAt rating)
   :finding "DTO covers note text and scalar timestamps/rating, but not keywords, links, sequences, source identity, or diagnostics.")
  (NoteRepository
   :file "zk-core/src/main/java/zk/core/ports/NoteRepository.java"
   :finding "Owns note persistence operations behind a core port.")
  (KeywordRepository
   :file "zk-core/src/main/java/zk/core/ports/KeywordRepository.java"
   :finding "Owns note-keyword associations behind a separate core port.")
  (LinkRepository
   :file "zk-core/src/main/java/zk/core/ports/LinkRepository.java"
   :finding "Owns note-to-note links behind a separate core port.")
  (SequenceRepository
   :file "zk-core/src/main/java/zk/core/ports/SequenceRepository.java"
   :finding "Owns parent/child ordering behind a separate core port.")
  (legacy-Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root has zettel children and firstzettel/lastzettel attributes.")
  (legacy-Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB zettel has title, content, author, keywords, manlinks, links, misc, luhmann, zknid, timestamps, rating, ratingcount, and fromBibTex.")
  (legacy-Links
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Links.java"
   :finding "Generated JAXB links element contains zero or more link strings.")
  (legacy-ZipFileProcessor
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/zip/ZipFileProcessor.java"
   :finding "Legacy extraction locates zknFile.xml inside the .zkn3 ZipFile and unmarshals JAXB objects.")
  (legacy-XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "Couples DOM parsing to Swing JFrame/JTree rendering; do not copy this architecture."))

 :design-principles
 (:reader-output-is-neutral t
  :reader-output-is-not-sqlite t
  :reader-output-is-not-swing-or-javafx t
  :reader-output-is-not-generated-jaxb t
  :note-dto-remains-small t
  :separate-core-repositories-remain-separate t
  :diagnostics-are-first-class t)

 :proposed-core-import-records
 ((Zkn3ImportBatch
   :kind :aggregate
   :contains (notes keywords links manual-links sequences diagnostics)
   :purpose "One immutable import result for callers that need a complete source snapshot before applying it to repositories.")

  (Zkn3NoteRecord
   :kind :source-record
   :fields (source-id source-ordinal title body created-at modified-at rating)
   :maps-eventually-to NoteRepository
   :notes "source-id should preserve zknid; source-ordinal should preserve the 1-based position used by legacy references.")

  (Zkn3KeywordRecord
   :kind :association-record
   :fields (note-source-id note-source-ordinal keyword-source-ordinal keyword)
   :maps-eventually-to KeywordRepository
   :notes "keyword-source-ordinal is needed because zknFile.xml stores keyword references as positions into keywordFile.xml.")

  (Zkn3LinkRecord
   :kind :relation-record
   :fields (from-note-source-id from-note-source-ordinal to-note-source-id to-note-source-ordinal link-kind)
   :link-kinds (manual-link normal-note-link)
   :maps-eventually-to LinkRepository
   :notes "manlinks are known note-to-note relations. A normal note-link kind is reserved for future parsed note references, not for links/link attachments.")

  (Zkn3AttachmentLinkRecord
   :kind :metadata-record
   :fields (note-source-id note-source-ordinal target link-kind)
   :link-kinds (file web unknown)
   :maps-eventually-to :no-existing-core-port
   :notes "The XML links/link vocabulary describes attachments or external locations, not current LinkRepository note-to-note links.")

  (Zkn3SequenceRecord
   :kind :sequence-record
   :fields (parent-note-source-id parent-note-source-ordinal child-note-source-id child-note-source-ordinal order source)
   :source-values (luhmann firstzettel-lastzettel open-question)
   :maps-eventually-to SequenceRepository
   :notes "luhmann CSV order can become child order; firstzettel/lastzettel meaning remains unresolved.")

  (Zkn3SourceMetadataRecord
   :kind :metadata-record
   :fields (note-source-id note-source-ordinal author misc from-bibtex rating-count)
   :maps-eventually-to :open-question
   :notes "Preserves source facts that lack current core ports without forcing them into NoteDTO.")

  (Zkn3ImportDiagnostic
   :kind :diagnostic-record
   :fields (severity source-member source-id source-ordinal field message)
   :severity-values (info warning error)
   :maps-eventually-to :import-report
   :notes "Malformed timestamps, missing keyword dictionary entries, broken links, and unsupported fields should be reportable without aborting every read."))

 :mapping-to-core-ports
 ((Zkn3NoteRecord maps-to NoteRepository)
  (Zkn3KeywordRecord maps-to KeywordRepository)
  (Zkn3LinkRecord maps-to LinkRepository)
  (Zkn3SequenceRecord maps-to SequenceRepository)
  (Zkn3AttachmentLinkRecord maps-to :no-existing-core-port)
  (Zkn3SourceMetadataRecord maps-to :open-question)
  (Zkn3ImportDiagnostic maps-to :import-report))

 :field-to-record-mapping
 ((zknid -> (Zkn3NoteRecord.source-id
             Zkn3KeywordRecord.note-source-id
             Zkn3LinkRecord.from-note-source-id
             Zkn3SequenceRecord.parent-note-source-id
             Zkn3ImportDiagnostic.source-id))
  (title -> Zkn3NoteRecord.title)
  (content -> Zkn3NoteRecord.body)
  (ts_created -> Zkn3NoteRecord.created-at)
  (ts_edited -> Zkn3NoteRecord.modified-at)
  (rating -> Zkn3NoteRecord.rating)
  (keywords -> Zkn3KeywordRecord)
  (author -> Zkn3SourceMetadataRecord.author)
  (manlinks -> Zkn3LinkRecord)
  (links -> Zkn3AttachmentLinkRecord)
  (misc -> Zkn3SourceMetadataRecord.misc)
  (luhmann -> Zkn3SequenceRecord)
  (fromBibTex -> Zkn3SourceMetadataRecord.from-bibtex)
  (ratingcount -> Zkn3SourceMetadataRecord.rating-count))

 :reader-output-shape-options
 ((:option batch-object
   :shape Zkn3ImportBatch
   :benefit "Allows validation and identity resolution across all notes before repository writes."
   :cost "Requires holding source records in memory.")
  (:option multiple-streams
   :shape (read-note-records read-keyword-records read-link-records read-sequence-records read-diagnostics)
   :benefit "Allows incremental consumption."
   :cost "Harder to preserve cross-record diagnostics and source ordering.")
  (:recommendation batch-object
   :reason "ZKN3 relations use source ordinals and dictionaries, so a complete source snapshot reduces premature coupling to repository write order."))

 :rejected-designs
 ((:make-NoteDTO-own-keywords-links-and-sequences
   :reason "Would collapse separate repository boundaries into one DTO.")
  (:make-Zkn3SourceReader-depend-on-SQLite
   :reason "Would make source reading a persistence adapter instead of a core port.")
  (:reuse-legacy-JAXB-types-as-core-model
   :reason "Would bind core to generated legacy XML representation.")
  (:copy-XMLViewer-parsing-path
   :reason "XMLViewer couples parsing to Swing rendering.")
  (:make-links-link-records-map-directly-to-LinkRepository
   :reason "links/link stores attachments or external locations, while LinkRepository currently models note-to-note relations.")
  (:normalize-source-ids-into-NoteId-inside-reader
   :reason "Would force storage identity assignment into source reading before import policy is defined."))

 :open-questions
 ((:id manlinks-semantics
   :question "Are manlinks semantically distinct from normal note links, or should they become a LinkRepository link kind only during import?")
  (:id luhmann-order
   :question "Does luhmann CSV order fully encode parent/child order, and how should duplicate or cyclic trails be diagnosed?")
  (:id source-metadata
   :question "Should author, misc, fromBibTex, and ratingcount become core model fields, metadata records, or diagnostics?")
  (:id identity
   :question "Should source IDs remain zknid strings through import, or be normalized into NoteId immediately after an identity planning phase?")
  (:id reader-output-api
   :question "Should the future reader emit a batch object, multiple streams, or both?")
  (:id normal-links
   :question "Is there a legacy note-link syntax inside content that should produce normal-note-link records distinct from manlinks?")
  (:id attachments
   :question "Should attachment/external links wait for a dedicated core AttachmentRepository port?")
  (:id timestamps
   :question "What timestamp parser and timezone policy should be used for ts_created and possibly empty ts_edited?"))

 :later-implementation-acceptance-criteria
 ((:criterion "Core record types introduce no imports of javax.swing, java.awt, javafx, zk.storage, or zk.ui.")
  (:criterion "Core record types do not depend on generated legacy JAXB classes.")
  (:criterion "Note, keyword, note-link, sequence, metadata, attachment-link, and diagnostic facts can be represented without SQLite.")
  (:criterion "NoteDTO remains focused on persisted note scalar fields unless a separate model slice changes it deliberately.")
  (:criterion "Manual links and attachment links remain distinguishable.")
  (:criterion "Luhmann sequence records preserve child order from source data.")
  (:criterion "Malformed or unresolved source facts can be represented as diagnostics."))

 :recommended-next-implementation-slice
 (!define-zkn3-import-record-types
  :mode :core-records-only
  :requires-design-artifact docs/plans/zkn3-reader-record-types-design.sexp
  :must-not-implement-reader-yet t))
