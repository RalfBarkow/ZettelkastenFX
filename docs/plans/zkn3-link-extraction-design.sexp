(:id zkn3-link-extraction-design
 :kind :mapping-semantics-design-only
 :scope AC-06

 :task
 (!design-zkn3-link-extraction
  :module zk-source-zkn3
  :mode :mapping-design-only
  :source-field links
  :target-record Zkn3LinkRecord
  :requires-complete-note-and-keyword-batch-policy t
  :must-not-implement-link-extraction-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "c02fe4d"
   :subject "fix(zettelkasten): reject incomplete zkn3 note batches")
  (:commit "697b81e"
   :subject "feat(zettelkasten): extract zkn3 keyword records"))

 :requires
 (:complete-note-and-keyword-batch-policy
  (:no-incomplete-imports t
   :no-per-note-skipping t
   :no-partial-successful-batches t
   :invalid-note-field-rejects-batch t
   :invalid-keyword-reference-rejects-batch t
   :sqlite-write-allowed-p nil))

 :target-record
 (:class zk.core.importing.Zkn3LinkRecord
  :fields ((fromSourceId String)
           (toSourceId String)
           (kind Zkn3LinkKind))
  :kind-values (NORMAL MANUAL)
  :current-use "not populated from zettel links/link in this design")

 :legacy-evidence
 ((:zettel-schema
   (:field links
    :shape "links element containing zero or more link elements"))

  (:links-class
   (:class ch.dreyeck.zettelkasten.xml.Links
    :root "links"
    :children "link"
    :child-type String
    :finding "Generated JAXB links element contains a live List<String> of link values."))

  (:object-factory
   (:element link
    :type String)
   (:element manlinks
    :type String))

  (:daten-constants
   (:file "/Users/rgb/workspace/Zettelkasten/src/main/java/de/danielluedecke/zettelkasten/database/Daten.java"
    :evidence ((ELEMENT_MANLINKS "manlinks")
               (ELEMENT_ATTACHMENTS "links")
               (ELEMENT_ATTCHILD "link")
               (ELEMENT_TRAILS "luhmann"))))

  (:daten-format-comment
   (:links-meaning "list of links (attachments) to files or websites, separated by new sub-elements called link"
    :manlinks-meaning "manual links from the user, referring to other entries, i.e. their index numbers"
    :luhmann-meaning "follower-entries (trails) displayed as sub-entries"))

  (:daten-semantics
   (:links-meaning :attachment-or-hyperlink-metadata
    :evidence ((getAttachments
                :finding "returns child elements from ELEMENT_ATTACHMENTS and converts separator chars")
               (getAttachmentsAsString
                :finding "documents hyperlinks and attachments and reads ELEMENT_ATTACHMENTS children")
               (setAttachments
                :finding "sets links (attachments) from strings containing hyperlinks or attachment paths")
               (addAttachments
                :finding "adds hyperlink or attachment path strings as ELEMENT_ATTCHILD children"))))

  (:manual-links
   (:manlinks-meaning :manual-note-reference-indexes
    :evidence ((getManualLinks
                :finding "returns int entry numbers parsed from ELEMENT_MANLINKS")
               (getManualLinksAsString
                :finding "splits comma-separated ELEMENT_MANLINKS text")
               (setManualLinks
                :finding "stores comma-separated entry numbers in ELEMENT_MANLINKS")
               (retrievePreparedManualLinksFromContent
                :finding "prepares extracted manual link numbers for ELEMENT_MANLINKS storage"))))

  (:luhmann
   (:meaning :sequence-or-trail-relation
    :evidence ((getSubEntriesCsv
                :finding "reads ELEMENT_TRAILS as comma-separated follower or sub-entry numbers")
               (getLuhmannNumbersAsString
                :finding "splits luhmann follower numbers")
               (addSubEntryToEntryAtPosition
                :finding "stores ordered child entries in the luhmann tag")))))

 :semantic-decision
 (:source-field links
  :decision :defer-as-attachment-metadata
  :reason "legacy constants, comments, and methods treat links/link as attachment or hyperlink strings, while manlinks carries manual note-reference indexes"
  :do-not-map-to Zkn3LinkRecord
  :graph-link-source-field-candidate manlinks)

 :candidate-internal-link-mapping
 (:not-applicable-p t
  :reason "links/link values are attachment or hyperlink strings, not proven internal note source IDs"
  :would-require-source-field manlinks
  :actual-Zkn3LinkRecord-fields ((fromSourceId String)
                                 (toSourceId String)
                                 (kind Zkn3LinkKind))
  :actual-Zkn3LinkKind-values (NORMAL MANUAL)
  :possible-later-manlinks-mapping
  ((source zettel.zknid)
   (target manlinks.comma-separated-entry-number-resolved-to-target-zknid)
   (kind MANUAL)))

 :attachment-deferral-policy
 (:source-field links
  :reason "legacy links appear to be hyperlinks/attachments rather than note-to-note graph edges"
  :do-not-map-to Zkn3LinkRecord
  :later-task !design-zkn3-attachment-metadata-policy)

 :link-completeness-policy
 (:decision :deferred-attachment-metadata
  :attachment-data-in-import-batch-p nil
  :must-not-silently-discard-in-final-importer t
  :current-slice-effect "do not populate Zkn3ImportBatch.links from links/link"
  :if-attachments-enter-scope-later "define core attachment metadata records or diagnostics before import can be complete"
  :internal-note-link-policy-not-active-p t
  :once-note-graph-links-enter-scope
  (:requires-complete-notes t
   :requires-complete-keywords t
   :no-dangling-graph-edges t
   :unresolved-target-rejects-batch t
   :rejected-batch-records (notes 0 keywords 0 links 0 sequences 0)
   :rejected-batch-diagnostics "ERROR diagnostics plus an ERROR import summary"
   :sqlite-write-allowed-p nil))

 :rejected-assumptions
 ((:links-always-mean-note-graph
   :reason "legacy code must distinguish attachments/hyperlinks from internal note references before mapping")
  (:silently-drop-links
   :reason "would create incomplete import once links are in scope")
  (:map-external-url-to-Zkn3LinkRecord
   :reason "Zkn3LinkRecord is for source note relations, not external attachments, unless core model says otherwise")
  (:merge-links-and-manlinks
   :reason "normal links and manual links may have distinct legacy semantics and should be designed separately")
  (:write-links-to-sqlite
   :reason "source reader returns import records only")
  (:surface-links-in-ui
   :reason "source reader must remain UI-free"))

 :excluded-from-this-slice
 ((manlinks :later !design-zkn3-manlink-extraction)
  (luhmann  :later !design-zkn3-sequence-extraction)
  (sqlite   :later !design-zkn3-import-application-to-repositories)
  (ui       :later !design-zkn3-import-ui-projection))

 :confirmed-non-changes
 (:implements-link-extraction-p nil
  :creates-Zkn3LinkRecord-p nil
  :populates-Zkn3ImportBatch.links-p nil
  :implements-manlinks-extraction-p nil
  :implements-luhmann-extraction-p nil
  :changes-Zkn3SourceReader-p nil
  :changes-import-record-classes-p nil
  :changes-sqlite-p nil
  :changes-ui-wiring-p nil
  :changes-persistence-schema-p nil)

 :acceptance-criteria-for-later-implementation-slice
 ((:criterion "links/link is not mapped to Zkn3LinkRecord unless new evidence proves it is an internal note relation.")
  (:criterion "Attachment or hyperlink data is either represented by a future core metadata record or reported by diagnostics; it is not silently discarded in final import semantics.")
  (:criterion "manlinks is designed separately before any manual note-reference records are emitted.")
  (:criterion "Any later Zkn3LinkRecord creation rejects dangling source targets and returns zero successful records for a rejected batch.")
  (:criterion "No SQLite writes, UI wiring, JAXB generated edits, or persistence schema changes are introduced by source reading."))

 :next-task
 (!design-zkn3-attachment-metadata-policy
  :module zk-source-zkn3
  :mode :metadata-policy-design-only
  :source-field links
  :must-not-map-to-Zkn3LinkRecord t
  :must-not-implement-attachment-import-yet t))
