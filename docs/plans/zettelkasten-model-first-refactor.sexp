(:id zettelkasten-model-first-refactor
 :kind :shop3-htn-plan
 :topic "Zettelkasten Model-First Refactor"
 :scope AC-06
 :slice boundary-guards-and-plan
 :slice-intent "Record the model-first architecture and install boundary guards only."
 :claim "The Zettelkasten model is UI-free; Swing, JavaFX, FedWiki, DMX, and HyperDoc are projections."
 :source-authority "/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
 :target-core zk-core
 :legacy-ui zettelkasten-swing
 :ports (NoteRepository KeywordRepository LinkRepository SequenceRepository)
 :adapters (zk-storage-sqlite zk-ui-javafx legacy-swing fedwiki dmx hyperdoc)

 :selected-plan
 ((!record-plan-artifact
   :id zettelkasten-model-first-refactor
   :kind :shop3-htn-plan
   :topic "Zettelkasten Model-First Refactor")

  (!inspect-target-architecture
   :repo "/Users/rgb/workspace/ZettelkastenFX"
   :modules
   ((zk-core
     :owns (:model NoteDTO NoteId)
     :ports (NoteRepository KeywordRepository LinkRepository SequenceRepository)
     :must-not-import (javax.swing java.awt javafx zk.storage zk.ui))
    (zk-storage-sqlite
     :implements (NoteRepository KeywordRepository LinkRepository SequenceRepository)
     :adapts-to zk-core)
    (zk-ui-javafx
     :role :projection-runtime-adapter
     :uses (zk-core zk-storage-sqlite))))

  (!inspect-legacy-swing-couplings
   :repo "/Users/rgb/workspace/Zettelkasten"
   :classes
   ((XMLViewer
     :file "src/main/java/ch/dreyeck/zettelkasten/xml/XMLViewer.java"
     :finding "XML parsing and Swing tree rendering are one object: JFrame/JTree/DefaultMutableTreeNode are built directly while parsing DOM nodes.")
    (ZettelController
     :file "src/main/java/ch/dreyeck/zettelkasten/ZettelController.java"
     :finding "Controller holds AttachmentsView and refreshes concrete Swing view state after data changes.")
    (AttachmentsView
     :file "src/main/java/ch/dreyeck/zettelkasten/attachments/AttachmentsView.java"
     :finding "Attachment projection is a JPanel with JList/DefaultListModel state instead of a UI-free model projection.")
    (ZettelkastenView
     :file "src/main/java/de/danielluedecke/zettelkasten/ZettelkastenView.java"
     :finding "Legacy main view owns Daten, DesktopData, Bookmarks, BibTeX, TasksData, Swing widgets, and UI callback adapters; it is behavioral evidence, not model authority.")))

  (!define-boundary-rules
   :rules (core-must-not-import-swing
           core-must-not-import-javafx
           core-must-not-import-sqlite
           core-must-not-import-ui
           storage-must-implement-core-ports))

  (!add-boundary-tests
   :tests ((core-must-not-import-swing
            :module zk-core
            :test zk.core.architecture.CoreBoundaryTest
            :forbidden-imports (javax.swing java.awt))
           (core-must-not-import-javafx
            :module zk-core
            :test zk.core.architecture.CoreBoundaryTest
            :forbidden-imports (javafx))
           (core-must-not-import-sqlite
            :module zk-core
            :test zk.core.architecture.CoreBoundaryTest
            :forbidden-imports (zk.storage))
           (core-must-not-import-ui
            :module zk-core
            :test zk.core.architecture.CoreBoundaryTest
            :forbidden-imports (zk.ui))
           (storage-must-implement-core-ports
            :module zk-storage-sqlite
            :test zk.storage.sqlite.SQLiteRepositoryPortContractTest
            :contracts ((NoteRepositorySQLite NoteRepository)
                        (KeywordRepositorySQLite KeywordRepository)
                        (LinkRepositorySQLite LinkRepository)
                        (SequenceRepositorySQLite SequenceRepository)))))

  (!record-next-extraction-plan
   :next ((!inspect-zkn3-source-reader-design
           :source #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"
           :mode :design-only
           :must-not-implement-yet t
           :must-not-import javax.swing
           :outputs (:note-records :keyword-records :link-records :sequence-records))))

  (!close-plan-artifact
   :id zettelkasten-model-first-refactor))

 :validation
 ((:command "mvn -pl zk-core test"
   :result PASS)
  (:command "mvn -pl zk-storage-sqlite -am test"
   :result PASS)
  (:command "mvn test"
   :result BLOCKED
   :blocked-by "zk-ui-javafx/src/main/java/zk/ui/javafx/FedWikiPane.java requires JavaFX Web classes that are unavailable"
   :blocker-touched-by-slice no))

 :status :closed)
