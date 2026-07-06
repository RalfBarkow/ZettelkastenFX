(:id zkn3-attachment-metadata-policy-design
 :kind :metadata-policy-design-only
 :scope AC-06

 :task
 (!design-zkn3-attachment-metadata-policy
  :module zk-source-zkn3
  :mode :metadata-policy-design-only
  :source-field links
  :must-not-map-to-Zkn3LinkRecord t
  :must-not-implement-attachment-import-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :requires
 ((:commit "615ae0b4c48a0bbdb7de9dae51989a2d7709afc0"
   :subject "docs(zettelkasten): design zkn3 link extraction"))

 :current-invariant
 (:no-incomplete-imports t
  :no-per-note-skipping t
  :no-partial-successful-batches t
  :no-silent-source-data-loss-once-in-scope t
  :unrepresentable-source-feature-policy-required t
  :sqlite-write-allowed-p nil)

 :source-semantic-decision
 (:source-field links
  :source-shape "links/link"
  :decision :attachment-or-hyperlink-metadata
  :not-Zkn3LinkRecord t
  :reason "legacy evidence treats links/link as attachments, files, or websites; note-reference graph edges are represented separately by manlinks")

 :preservation-problem
 (:current-core-import-records
  (Zkn3NoteRecord Zkn3KeywordRecord Zkn3LinkRecord Zkn3SequenceRecord Zkn3ImportDiagnostic)

  :missing-record
  Zkn3AttachmentRecord

  :problem
  "If links/link source data is ignored during a complete import, the import is incomplete.")

 :policy-question
 "Should attachment/hyperlink metadata be represented as a new core import record, rejected until supported, or explicitly excluded from the supported import profile?"

 :legacy-evidence
 ((:zettel-schema
   (:class ch.dreyeck.zettelkasten.xml.Zettel
    :field links
    :type ch.dreyeck.zettelkasten.xml.Links
    :required-p t
    :finding "Each zettel has a required links element in the generated JAXB shape."))

  (:links-schema
   (:class ch.dreyeck.zettelkasten.xml.Links
    :root "links"
    :child "link"
    :child-cardinality "zero or more"
    :child-type String
    :labels-p nil
    :path-field-p nil
    :finding "Attachment metadata is stored as raw link string values, not structured label/path records."))

  (:object-factory
   (:link-element-type String
    :manlinks-element-type String
    :finding "link and manlinks are separate string-bearing XML elements."))

  (:daten-constants
   (:ELEMENT_ATTACHMENTS "links"
    :ELEMENT_ATTCHILD "link"
    :ELEMENT_MANLINKS "manlinks"
    :ELEMENT_ATTACHMENT_PATH "attachmentpath"
    :ELEMENT_IMAGE_PATH "imagepath"))

  (:daten-format-comment
   (:links-meaning "list of links (attachments) to files or websites"
    :link-meaning "single entry (attachment) of the links"
    :manlinks-meaning "manual links from the user, referring to other entries, i.e. their index numbers"))

  (:per-zettel-storage
   (:addEntry
    :finding "creates a links element on the zettel and adds each supplied string as a link child")
   (:changeEntry
    :finding "removes existing link children from the zettel links element and rewrites supplied link strings"))

  (:attachment-api
   (:getAttachments
    :finding "returns link child elements from a single entry after separator conversion")
   (:getAttachmentsAsString
    :finding "documents links/hyperlinks, existing local files, file:// conversion, and links/attachments return values")
   (:setAttachments
    :finding "sets links (attachments) from a string array containing hyperlinks, attachment paths, etc.")
   (:addAttachments
    :finding "adds hyperlink or attachment path strings under the entry links element")
   (:deleteAttachment
    :finding "deletes a raw attachment value for a specific entry"))

  (:local-file-path-evidence
   (:EditorFrame.insertAttachment
    :finding "file chooser inserts hard-disk attachments; file paths are added to the attachment list")
   (:FileOperationsUtil.getFileExtension
    :finding "non-hyperlink attachment strings are resolved as linked files"))

  (:url-evidence
   (:FileOperationsUtil.isHyperlink
    :finding "recognizes http, https, ftp, webdav, news, and outlook prefixes as hyperlinks")
   (:HtmlUbbUtil.getEntryAttachments
    :finding "renders attachment strings as href targets")
   (:SearchResultsFrame.openAttachment
    :finding "opens a clicked attachment as either file or website hyperlink")))

 :policy-options
 ((:add-core-attachment-record
   :record Zkn3AttachmentRecord
   :meaning "preserve per-note attachment or hyperlink metadata as source-reader output"
   :pros ("no silent data loss" "keeps source reading UI-free and SQLite-free")
   :cons ("requires core import vocabulary extension"))

  (:reject-when-attachments-present
   :meaning "if any links/link value is present, reject complete import until attachment model exists"
   :pros ("preserves no-incomplete-import invariant")
   :cons ("blocks importing zettelkasten files that use attachments"))

  (:supported-profile-excludes-attachments
   :meaning "declare attachments out of supported import scope"
   :pros ("simpler importer")
   :cons ("violates full-fidelity import unless the operator explicitly accepts a lossy profile")))

 :recommended-policy
 (:decision :reject-when-attachments-present-until-Zkn3AttachmentRecord-exists
  :reason "No silent data loss. links/link is source data but currently has no neutral core import record."
  :empty-links-field "batch remains valid"
  :nonempty-links-field "reject complete batch with ERROR diagnostic until attachment metadata record is designed")

 :future-core-record-proposal
 (:record Zkn3AttachmentRecord
  :package zk.core.importing
  :fields
  ((noteSourceId :type String)
   (value        :type String)
   (kind         :type Zkn3AttachmentKind :values (:UNKNOWN :FILE :URL))
   (sourceField  :type String :value "links/link")))

 :future-kind-classification-policy
 (:must-preserve-original-value t
  :classification-is-advisory t
  :url-prefixes-observed ("http://" "https://" "ftp://" "webdav://" "news:" "outlook:")
  :file-path-policy "non-URL values may be local or relative file paths; do not require filesystem existence while reading source metadata"
  :label-policy "legacy links/link has no separate label field; use value as the only preserved source value until richer evidence exists")

 :attachment-completeness-policy
 (:missing-links-field
  (:severity INFO
   :effect "note has no attachment metadata; batch remains valid")

  :empty-links-element
  (:severity INFO
   :effect "note has no attachment metadata; batch remains valid")

  :nonempty-link-value-before-attachment-record-exists
  (:severity ERROR
   :effect "reject complete batch; do not silently discard attachment metadata")

  :blank-link-value
  (:severity WARNING
   :effect "ignore only if legacy permits placeholder entries; otherwise reject when attachment import is in scope")

  :local-file-path
  (:severity ERROR
   :effect "reject until attachment preservation record exists")

  :url
  (:severity ERROR
   :effect "reject until attachment preservation record exists"))

 :rejected-behaviors
 ((:map-links-link-to-Zkn3LinkRecord
   :reason "links/link is attachment or hyperlink metadata, not internal note graph edge")

  (:silently-drop-links-link
   :reason "would create incomplete import")

  (:store-attachment-as-diagnostic-only
   :reason "diagnostics are not durable imported metadata")

  (:write-attachments-to-sqlite-from-source-reader
   :reason "source reader returns import records only")

  (:surface-attachments-in-ui-from-source-reader
   :reason "source reader must remain UI-free"))

 :relationship-to-manlinks
 (:links
  (:meaning "attachment/hyperlink metadata"
   :maps-to-Zkn3LinkRecord-p nil)

  :manlinks
  (:meaning "manual note-reference indexes"
   :later-task !design-zkn3-manlink-extraction
   :candidate-target Zkn3LinkRecord))

 :implementation-constraints
 (:must-not-implement-attachment-import t
  :must-not-add-Zkn3AttachmentRecord t
  :must-not-map-links-link-to-Zkn3LinkRecord t
  :must-not-create-Zkn3LinkRecord-in-production-code t
  :must-not-populate-Zkn3ImportBatch.links t
  :must-not-change-Zkn3DomSourceReader.java t
  :must-not-change-Zkn3SourceReader.java t
  :must-not-change-import-record-classes t
  :must-not-add-SQLite-import-logic t
  :must-not-add-UI-wiring t
  :must-not-add-JAXB t
  :must-not-edit-FedWikiPane.java t
  :must-not-repair-JavaFX-Web-blocker t
  :must-not-edit-generated-JAXB-files t
  :must-not-edit-legacy-Swing-repo t
  :must-not-edit-Repomix-files t)

 :next-executable-task
 (!implement-zkn3-attachment-presence-guard
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :guard-only
  :source-field links
  :must-not-map-to-Zkn3LinkRecord t
  :must-not-add-Zkn3AttachmentRecord-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-core-vocabulary-task
 (!design-zkn3-attachment-record
  :module zk-core
  :mode :core-vocabulary-design-only
  :record Zkn3AttachmentRecord
  :source-field links
  :must-not-implement-record-yet t))
