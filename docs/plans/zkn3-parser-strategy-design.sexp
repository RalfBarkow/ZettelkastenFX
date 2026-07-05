(:id zkn3-parser-strategy-design
 :kind :parser-design-only
 :scope AC-06
 :task
 (!design-zkn3-parser-strategy
  :module zk-source-zkn3
  :mode :parser-design-only
  :choices (dom stax jaxb)
  :must-not-implement-parser-yet t)
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
   :subject "build(zettelkasten): scaffold zkn3 source adapter module"))

 :current-module-boundary
 (:module zk-source-zkn3
  :depends-on (zk-core)
  :test-dependencies (junit-jupiter)
  :current-parser-dependencies nil
  :current-reader-implementation nil
  :boundary-test zk.source.zkn3.Zkn3SourceModuleBoundaryTest)

 :legacy-evidence
 ((ZipFileProcessor
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/zip/ZipFileProcessor.java"
   :finding "Legacy path opens the .zkn3 ZipFile, finds zknFile.xml, and unmarshals through javax.xml.bind JAXB.")
  (Zettelkasten
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettelkasten.java"
   :finding "Generated JAXB root captures zettel children and firstzettel/lastzettel attributes.")
  (Zettel
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Zettel.java"
   :finding "Generated JAXB element captures title, content, author, keywords, manlinks, links, misc, luhmann, zknid, timestamps, rating, ratingcount, and fromBibTex.")
  (Links
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/Links.java"
   :finding "Generated JAXB collection shows links are nested link elements, not only a scalar text field.")
  (XMLViewer
   :file "/Users/rgb/workspace/Zettelkasten/src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
   :anti-pattern "Uses DOM but couples parsing to Swing JFrame/JTree/JScrollPane rendering. DOM can be reused as a parser strategy only without this UI shape.")
  (legacy-pom
   :file "/Users/rgb/workspace/Zettelkasten/pom.xml"
   :finding "Legacy build carries JAXB dependencies and Swing libraries; those dependencies are not appropriate for the first zk-source-zkn3 parser slice."))

 :parser-options
 ((:dom
   :pros ("JDK built-in" "simple field extraction" "no JAXB dependency" "easy fixture inspection")
   :cons ("loads whole XML document" "manual mapping required" "needs explicit secure parser configuration")
   :boundary-fit :good)

  (:stax
   :pros ("JDK built-in" "streaming" "works for large XML" "lower peak memory")
   :cons ("more state-machine code" "harder to inspect initially" "cross-reference diagnostics need more bookkeeping")
   :boundary-fit :good)

  (:jaxb
   :pros ("legacy repo already has generated JAXB-like classes" "schema-shaped mapping" "less manual field walking")
   :cons ("adds dependency or generated class question" "risks importing legacy representation into adapter" "heavier first slice" "javax.xml.bind is not part of modern JDKs")
   :boundary-fit :acceptable-but-heavier))

 :recommended-strategy
 (:parser :dom
  :module zk-source-zkn3
  :reason "JDK built-in, smallest dependency surface, adequate for first adapter implementation, keeps parser code in adapter module."
  :implementation-note "Map DOM elements directly into Zkn3ImportBatch records; do not reuse XMLViewer and do not copy generated JAXB classes into zk-core."
  :revisit-when (:large-zkn3-files :memory-pressure :performance-evidence))

 :dependency-boundary
 (:module zk-source-zkn3
  :may-use (java.util.zip java.xml zk-core)
  :must-not-use (javax.swing java.awt javafx zk-storage-sqlite zk-ui-javafx)
  :must-not-add-dependencies (jaxb sqlite javafx)
  :must-not-copy-generated-jaxb-classes-into-zk-core t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :future-implementation-outline
 ((!open-zip-file :path ?zkn3-file)
  (!find-entry "zknFile.xml")
  (!parse-entry-with-dom)
  (!extract-zettel-elements)
  (!map-note-fields-to-Zkn3NoteRecord)
  (!map-keywords-to-Zkn3KeywordRecord)
  (!map-links-to-Zkn3LinkRecord :kind NORMAL)
  (!map-manlinks-to-Zkn3LinkRecord :kind MANUAL)
  (!map-luhmann-to-Zkn3SequenceRecord)
  (!collect-diagnostics)
  (!return-Zkn3ImportBatch)))

 :dom-implementation-guidance
 (:must-configure-parser-securely t
  :must-disable-external-entities t
  :must-not-render-xml-tree t
  :should-keep-field-extractors-small t
  :should-test-with-minimal-fixtures-before-authoritative-file t
  :should-collect-diagnostics-instead-of-printing-stack-traces t)

 :rejected-strategies
 ((:copy-XMLViewer
   :reason "Couples XML parsing to Swing rendering.")

  (:put-parser-in-zk-core
   :reason "File-format parsing is adapter behavior.")

  (:put-parser-in-zk-storage-sqlite
   :reason "Source parsing is not persistence.")

  (:put-parser-in-zk-ui-javafx
   :reason "Source parsing must be UI-free.")

  (:start-with-jaxb
   :reason "Too much dependency/design surface for first implementation slice.")

  (:copy-generated-jaxb-to-zk-core
   :reason "Would bind the core model to a generated legacy XML representation."))

 :open-questions
 ((:id keywords-delimiter
   :question "What delimiter syntax is used inside keywords?")
  (:id manlinks-delimiter
   :question "What delimiter syntax is used inside manlinks?")
  (:id links-shape
   :question "Are links nested elements or text payloads in all real files?")
  (:id luhmann-encoding
   :question "How exactly is luhmann encoded?")
  (:id timestamps
   :question "Are timestamps always parseable as epoch seconds/milliseconds, or are there mixed formats?")
  (:id malformed-link-policy
   :question "Should malformed link targets become diagnostics or fail the import?")
  (:id source-metadata
   :question "Should author, misc, fromBibTex, and ratingcount be preserved as metadata records later?"))

 :later-implementation-acceptance-criteria
 ((:criterion "The first parser implementation uses DOM in zk-source-zkn3 without adding JAXB, SQLite, or JavaFX dependencies.")
  (:criterion "The parser opens the .zkn3 archive and locates zknFile.xml without touching UI or repositories.")
  (:criterion "The parser maps source fields into Zkn3ImportBatch records and reports malformed source facts as the chosen diagnostics policy.")
  (:criterion "The parser uses secure java.xml configuration and does not resolve external entities.")
  (:criterion "The parser does not copy XMLViewer or generated JAXB classes into zk-core."))

 :next-implementation-task
 (!implement-zkn3-dom-reader-skeleton
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :implements zk.core.ports.Zkn3SourceReader
  :returns zk.core.importing.Zkn3ImportBatch
  :mode :skeleton-with-empty-batch-or-fixture-only
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
