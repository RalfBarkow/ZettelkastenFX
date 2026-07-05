(:id zkn3-note-field-extraction-design
 :kind :mapping-design-only
 :scope AC-06
 :task
 (!design-zkn3-note-field-extraction
  :module zk-source-zkn3
  :mode :mapping-design-only
  :source-fields (zknid title content ts_created ts_edited rating)
  :target-record Zkn3NoteRecord
  :must-not-implement-field-extraction-yet t)
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
   :subject "refactor(zettelkasten): return zkn3 import batch from reader port")
  (:commit "6f3672fc6cf31970dfeaf2ec062f8e265dacce7d"
   :subject "docs(zettelkasten): design zkn3 reader adapter")
  (:commit "6acf90a72ff6ac5f767b9a7edcb4bb2c00a4a2ee"
   :subject "build(zettelkasten): scaffold zkn3 source adapter module")
  (:commit "0f62fe5173878c5f13a8a51ca75be63f5f68e4c0"
   :subject "docs(zettelkasten): choose zkn3 parser strategy")
  (:commit "0042e9b130a80ff2aaaedf79e9c006e56755251f"
   :subject "feat(zettelkasten): add zkn3 reader skeleton")
  (:commit "cd67052179ad0d4992050203d4276b6a2f20d171"
   :subject "feat(zettelkasten): probe zkn3 archive entry")
  (:commit "efd7d4e4853bbf0473d62db816ef27b2bda2bdd5"
   :subject "feat(zettelkasten): probe zkn3 xml root")
  (:commit "091535d2fdf98bbdb8a5f02c0061e58cfd7be331"
   :subject "feat(zettelkasten): count zkn3 zettel elements"))

 :mapping-scope
 (:source-container zknFile.xml
  :source-root zettelkasten
  :source-element zettel
  :target-record zk.core.importing.Zkn3NoteRecord
  :target-fields (sourceId title body createdAt modifiedAt rating)
  :out-of-scope (keywords links manlinks luhmann author misc fromBibTex ratingcount))

 :current-target-record
 (:class zk.core.importing.Zkn3NoteRecord
  :fields ((sourceId String)
           (title String)
           (body String)
           (createdAt java.time.Instant)
           (modifiedAt java.time.Instant)
           (rating java.util.OptionalInt)))

 :legacy-evidence
 ((Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB zettel has child elements title and content, required attributes zknid, ts_created, ts_edited, and optional/simple rating.")
  (Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root has zettel children under zettelkasten.")
  (Links
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Links.java"
   :finding "links/link is separate relation or attachment evidence and is excluded from this note-record slice.")
  (XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "Combines DOM parsing with Swing JFrame/JTree/JScrollPane rendering; do not copy its architecture."))

 :current-adapter-state
 (:module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :implemented-so-far (:zip-entry-probe :xml-root-probe :zettel-count-probe)
  :does-not-create (Zkn3NoteRecord Zkn3KeywordRecord Zkn3LinkRecord Zkn3SequenceRecord)
  :does-not-write-to-sqlite t
  :does-not-touch-ui t)

 :field-mapping
 ((zknid      -> Zkn3NoteRecord.sourceId)
  (title      -> Zkn3NoteRecord.title)
  (content    -> Zkn3NoteRecord.body)
  (ts_created -> Zkn3NoteRecord.createdAt)
  (ts_edited  -> Zkn3NoteRecord.modifiedAt)
  (rating     -> Zkn3NoteRecord.rating))

 :extraction-policy
 (:element-selection
  (:source "direct zettel element currently being mapped"
   :must-not-read-sibling-zettel-fields-for-record-values t
   :duplicate-detection :batch-level-later)
  :text-elements
  ((title
    :source "first direct child element named title"
    :whitespace-policy "preserve text content except XML parser normalization; trimming policy remains open")
   (content
    :source "first direct child element named content"
    :whitespace-policy "preserve body text; do not normalize markup in the source reader")))

 :timestamp-strategy
 (:fields (ts_created ts_edited)
  :target (createdAt modifiedAt)
  :preferred-type java.time.Instant
  :rules
  ((:numeric-epoch-millis "parse as Instant.ofEpochMilli")
   (:numeric-epoch-seconds "parse as Instant.ofEpochSecond only if evidence confirms seconds")
   (:blank-or-malformed "emit diagnostic; do not silently invent value"))
  :implementation-note "Use an explicit helper so timestamp policy is testable without touching UI or SQLite."
  :open-question "Which timestamp unit is used consistently in real rgb.zkn3?")

 :rating-strategy
 (:source-field rating
  :target-field Zkn3NoteRecord.rating
  :target-type OptionalInt
  :rules
  ((:blank OptionalInt.empty)
   (:missing OptionalInt.empty)
   (:integer OptionalInt.of)
   (:malformed "emit WARNING diagnostic and use OptionalInt.empty"))
  :open-question "Whether rating has a documented range, such as 0..5, needs evidence before enforcing a range.")

 :diagnostics
 ((:missing-source-id
   :severity ERROR
   :field zknid
   :effect "do not create Zkn3NoteRecord for this zettel")

  (:duplicate-source-id
   :severity ERROR
   :field zknid
   :effect "batch-level validation later; do not rely on last-write-wins")

  (:missing-title
   :severity WARNING
   :field title
   :effect "create record with empty title or synthesized title only if policy is accepted later")

  (:missing-content
   :severity WARNING
   :field content
   :effect "create record with empty body")

  (:malformed-created-timestamp
   :severity ERROR
   :field ts_created
   :effect "open question: skip note or require fallback policy")

  (:malformed-edited-timestamp
   :severity WARNING
   :field ts_edited
   :effect "open question: use createdAt or skip note")

  (:malformed-rating
   :severity WARNING
   :field rating
   :effect "use OptionalInt.empty"))

 :record-creation-policy
 (:create-note-record-when
  (:required-fields-valid (zknid ts_created)
   :title-policy :open
   :content-policy :open
   :modified-timestamp-policy :open)
  :do-not-create-note-record-when
  (:missing-source-id t
   :malformed-created-timestamp :open)
  :must-report-diagnostics t)

 :excluded-from-this-slice
 ((keywords   :later !design-zkn3-keyword-extraction)
  (links      :later !design-zkn3-link-extraction)
  (manlinks   :later !design-zkn3-manlink-extraction)
  (luhmann    :later !design-zkn3-sequence-extraction)
  (author     :later !design-zkn3-note-metadata-policy)
  (misc       :later !design-zkn3-note-metadata-policy)
  (fromBibTex :later !design-zkn3-bibliography-reference-policy)
  (ratingcount :later !design-zkn3-rating-count-policy))

 :implementation-acceptance-criteria-for-later-slice
 ((:criterion "Zkn3DomSourceReader creates Zkn3NoteRecord only from zknid, title, content, ts_created, ts_edited, and rating.")
  (:criterion "The implementation produces diagnostics for missing source IDs, malformed timestamps, malformed ratings, and unresolved timestamp policy cases.")
  (:criterion "The implementation does not map keywords, links, manlinks, luhmann sequences, author, misc, fromBibTex, or ratingcount.")
  (:criterion "The implementation does not add JAXB, SQLite, JavaFX, Swing, or generated legacy XML classes.")
  (:criterion "Tests use temporary XML fixtures and do not read the authoritative rgb.zkn3 file.")
  (:criterion "The reader continues to return a neutral Zkn3ImportBatch without writing to repositories."))

 :future-implementation-task
 (!implement-zkn3-note-field-extraction
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :note-records-only
  :source-fields (zknid title content ts_created ts_edited rating)
  :target-record Zkn3NoteRecord
  :must-not-map-keywords-links-or-sequences-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
