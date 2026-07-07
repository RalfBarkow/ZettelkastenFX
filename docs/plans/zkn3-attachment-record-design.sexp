(:id zkn3-attachment-record-design
 :kind :core-vocabulary-design-only
 :scope AC-06

 :task
 (!design-zkn3-attachment-record
  :module zk-core
  :mode :core-vocabulary-design-only
  :record Zkn3AttachmentRecord
  :source-field links
  :requires-real-source-smoke-diagnostics t
  :must-not-implement-record-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :requires
 ((:commit "d1e976c3dbebae167192a8bdb3e4efa1d81fae9b"
   :subject "docs(zettelkasten): design zkn3 attachment metadata policy")
  (:commit "260d33431b79e0f074b603b272e9173b0ce983e2"
   :subject "feat(zettelkasten): guard unsupported zkn3 attachments")
  (:commit "3ce0157753baeea9104d1545f051fa09a2a2d447"
   :subject "feat(zettelkasten): accept blank zkn3 edited timestamps"))

 :real-source-blocker
 (:status :correctly-rejected
  :first-remaining-error-class :unsupported-links-link-attachment-metadata
  :errors 2
  :reason "nonempty links/link cannot be silently dropped because no core attachment record exists")

 :current-importer-state
 (:notes-extractable-p t
  :raw-timestamp-provenance-represented-p t
  :blank-ts-edited-compatible-p t
  :keywords-extractable-p t
  :manual-links-extractable-as Zkn3LinkRecord
  :luhmann-sequences-extractable-as Zkn3SequenceRecord
  :nonempty-links-link-rejects-complete-batch-p t
  :sqlite-write-allowed-p nil
  :ui-access-allowed-p nil)

 :semantic-separation
 (:manual-note-links
  (:source-field manlinks
   :target-record Zkn3LinkRecord
   :meaning "internal note graph edge")

  :luhmann-sequences
  (:source-field luhmann
   :target-record Zkn3SequenceRecord
   :meaning "Folgezettel parent-child sequence relation")

  :attachments-or-hyperlinks
  (:source-field links/link
   :target-record Zkn3AttachmentRecord
   :meaning "note-attached file, URL, or external reference metadata"))

 :observed-legacy-extraction-path
 ((:zkn3-container
   (:internal-file "zknFile.xml"
    :finding "The source reader inspects zknFile.xml inside the .zkn3 archive for zettel records."))

  (:generated-zettel-shape
   (:class ch.dreyeck.zettelkasten.xml.Zettel
    :field links
    :type ch.dreyeck.zettelkasten.xml.Links
    :ownership "links belongs to the containing zettel"))

  (:generated-links-shape
   (:class ch.dreyeck.zettelkasten.xml.Links
    :root "links"
    :child "link"
    :child-cardinality "zero or more"
    :child-type String
    :finding "The XML model preserves link values as raw strings with no separate label, path, URL, or type field."))

  (:legacy-constants
   (:class de.danielluedecke.zettelkasten.database.Daten
    :ELEMENT_ATTACHMENTS "links"
    :ELEMENT_ATTCHILD "link"
    :ELEMENT_MANLINKS "manlinks"
    :ELEMENT_ATTACHMENT_PATH "attachmentpath"
    :ELEMENT_IMAGE_PATH "imagepath"))

  (:legacy-format-comment
   (:class de.danielluedecke.zettelkasten.database.Daten
    :links-meaning "list of links (attachments) to files or websites"
    :link-meaning "single entry (attachment) of the links"
    :manlinks-meaning "manual links from the user, referring to other entries"))

  (:legacy-write-path
   (:addEntry "creates a links element and appends each supplied attachment string as a link child"
    :changeEntry "removes existing link children and rewrites supplied attachment strings"
    :setAttachments "sets hyperlinks, attachment paths, etc. as link child text"
    :addAttachments "appends hyperlink or attachment path strings to a note's links element"))

  (:legacy-read-path
   (:getAttachments "returns the link child elements for one entry"
    :getAttachmentsAsString "exposes links, hyperlinks, existing files, and optional file:// conversion"))

  (:legacy-url-evidence
   (:class de.danielluedecke.zettelkasten.util.FileOperationsUtil
    :isHyperlink-prefixes ("http://" "https://" "ftp://" "webdav://" "news:" "outlook:")
    :finding "Legacy classification is prefix-based UI/runtime behavior, not source record authority."))

  (:duplicate-evidence
   (:test ch.dreyeck.zettelkasten.ZettelControllerTest
    :finding "Legacy evidence contains duplicate attachment values; reader vocabulary should not deduplicate by default.")))

 :current-core-vocabulary
 (:batch Zkn3ImportBatch
  :records (Zkn3NoteRecord Zkn3KeywordRecord Zkn3LinkRecord Zkn3SequenceRecord Zkn3ImportDiagnostic)
  :missing-record Zkn3AttachmentRecord
  :current-batch-fields (notes keywords links sequences diagnostics)
  :attachment-slot-present-p nil)

 :recommended-record
 (:name Zkn3AttachmentRecord
  :package zk.core.importing
  :fields
  ((sourceNoteId
    :type String
    :meaning "source zettel id owning the attachment metadata"
    :required true)

   (rawValue
    :type String
    :meaning "exact links/link text from zknFile.xml"
    :required true)

   (kind
    :type Zkn3AttachmentKind
    :meaning "advisory classification of raw value"
    :required true)

   (order
    :type int
    :meaning "zero-based order within the source note's links/link list"
    :required true)))
  :validation
  (:sourceNoteId nonblank
   :rawValue nonblank
   :kind nonnull
   :order nonnegative))

 :recommended-kind-enum
 (:name Zkn3AttachmentKind
  :values
  ((URL
    :meaning "raw value parses as an absolute URI or matches legacy hyperlink prefixes")

   (FILE
    :meaning "raw value appears to be a local or relative file reference")

   (UNKNOWN
    :meaning "raw value is preserved but cannot yet be safely classified")))
  :classification-policy "classification must not change full-fidelity preservation; rawValue remains authoritative"
  :decision :include-enum-as-advisory-with-UNKNOWN-fallback
  :reason "The enum is small enough for the core vocabulary and UNKNOWN prevents unsafe semantics from blocking preservation.")

 :minimal-alternative
 (:record Zkn3AttachmentRecord
  :fields (sourceNoteId rawValue order)
  :defer-kind-classification true
  :status :fallback-if-kind-policy-proves-too-ambiguous)

 :design-decision
 (:chosen-record-shape :record-plus-advisory-kind-enum
  :kind-enum Zkn3AttachmentKind
  :raw-value-authoritative-p t
  :classification-required-for-preservation-p nil
  :unknown-kind-valid-p t
  :reader-acceptance-change-in-this-slice-p nil)

 :batch-impact
 (:record Zkn3ImportBatch
  :add-field attachments
  :type "List<Zkn3AttachmentRecord>"
  :default "List.of()"
  :ordering "source zettel order, then links/link order"
  :error-policy "nonempty links/link no longer rejects only after attachment records are populated")

 :attachment-completeness-policy
 (:missing-links
  (:severity INFO
   :effect "note has no attachments; batch remains valid")

  :empty-links
  (:severity INFO
   :effect "note has no attachments; batch remains valid")

  :blank-link
  (:severity WARNING
   :effect "ignore blank attachment value and preserve diagnostic; batch remains valid")

  :nonblank-link
  (:current-effect "reject complete batch because record is not implemented"
   :future-effect "create Zkn3AttachmentRecord and keep batch valid")

  :unclassifiable-nonblank-link
  (:future-effect "preserve as rawValue with kind UNKNOWN; do not reject merely because classification is unknown")

  :duplicate-link-value-same-note
  (:future-effect "preserve order unless exact duplicate policy is deliberately changed in a later slice"))

 :ordering-and-identity-policy
 (:owner "containing zettel source id"
  :order "zero-based occurrence order of nonblank links/link elements within that zettel"
  :deduplication "do not deduplicate by default; attachment lists may intentionally repeat values or order may carry meaning"
  :raw-preservation "rawValue is authoritative")

 :implementation-acceptance-criteria-for-next-slice
 ((:criterion "Add Zkn3AttachmentRecord under zk.core.importing only.")
  (:criterion "Add Zkn3AttachmentKind only if UNKNOWN fallback is preserved.")
  (:criterion "Extend Zkn3ImportBatch with List<Zkn3AttachmentRecord> attachments.")
  (:criterion "Keep record and enum UI-free, SQLite-free, and JAXB-free.")
  (:criterion "Keep Zkn3DomSourceReader acceptance unchanged until a later mapping slice.")
  (:criterion "Core boundary test continues to forbid javax.swing, java.awt, javafx, zk.storage, and zk.ui imports."))

 :rejected-behaviors
 ((:map-links-link-to-Zkn3LinkRecord
   :reason "links/link is attachment or hyperlink metadata, not internal note graph relation")

  (:drop-nonblank-links-link
   :reason "would violate full-fidelity import")

  (:require-safe-kind-classification-before-preserving
   :reason "unknown external-reference metadata must still be preservable")

  (:deduplicate-by-default
   :reason "legacy data can contain repeated values, and order/duplication policy is not safely inferred")

  (:write-attachments-to-sqlite-now
   :reason "repository application is downstream")

  (:surface-attachments-in-ui-now
   :reason "UI projection is downstream")

  (:repair-real-source
   :reason "source repair is not part of reader vocabulary design"))

 :non-scope
 (:do-not-implement-Zkn3AttachmentRecord t
  :do-not-implement-Zkn3AttachmentKind t
  :do-not-change-Zkn3ImportBatch t
  :do-not-change-Zkn3DomSourceReader.java t
  :do-not-change-Zkn3SourceReader.java t
  :do-not-add-SQLite-import-logic t
  :do-not-add-UI-wiring t
  :do-not-normalize-or-classify-attachments-in-code t
  :do-not-weaken-unsupported-links-link-rejection-yet t
  :do-not-add-JAXB t
  :do-not-edit-FedWikiPane.java t
  :do-not-repair-JavaFX-Web-blocker t
  :do-not-edit-legacy-Swing-repo t
  :do-not-edit-Repomix-files t
  :do-not-copy-or-commit-zkn3-files t)

 :next-task-candidate
 (!implement-zkn3-attachment-record
  :module zk-core
  :mode :core-record-change-only
  :records (Zkn3AttachmentRecord Zkn3AttachmentKind)
  :batch-field attachments
  :must-not-change-source-reader-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-source-reader-task
 (!implement-zkn3-attachment-record-mapping
  :module zk-source-zkn3
  :mode :attachment-records-only
  :source-field links
  :target-record Zkn3AttachmentRecord
  :requires-attachment-record t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
