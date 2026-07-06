(:id zkn3-manlink-extraction-design
 :kind :mapping-design-only
 :scope AC-06

 :task
 (!design-zkn3-manlink-extraction
  :module zk-source-zkn3
  :mode :mapping-design-only
  :source-field manlinks
  :target-record Zkn3LinkRecord
  :requires-attachment-presence-guard t
  :must-not-implement-manlink-extraction-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :requires
 (:complete-note-keyword-and-attachment-policy
  (:no-incomplete-imports t
   :no-per-note-skipping t
   :no-partial-successful-batches t
   :invalid-note-field-rejects-batch t
   :invalid-keyword-reference-rejects-batch t
   :nonempty-unsupported-attachment-rejects-batch t
   :sqlite-write-allowed-p nil))

 :completed-prerequisites
 ((:commit "615ae0b4c48a0bbdb7de9dae51989a2d7709afc0"
   :subject "docs(zettelkasten): design zkn3 link extraction")
  (:commit "d1e976c3dbebae167192a8bdb3e4efa1d81fae9b"
   :subject "docs(zettelkasten): design zkn3 attachment metadata policy")
  (:commit "260d33431b79e0f074b603b272e9173b0ce983e2"
   :subject "feat(zettelkasten): guard unsupported zkn3 attachments"))

 :target-record
 (:class zk.core.importing.Zkn3LinkRecord
  :fields ((fromSourceId String)
           (toSourceId String)
           (kind Zkn3LinkKind))
  :kind-values (NORMAL MANUAL)
  :candidate-kind MANUAL)

 :legacy-evidence
 ((:constant
   (:name Daten.ELEMENT_MANLINKS
    :value "manlinks"
    :meaning "manual link child element"))

  (:constants
   (:name Constants.FORMAT_MANLINK_OPEN
    :value "[z")
   (:name Constants.FORMAT_MANLINK_CLOSE
    :value "[/z]"))

  (:zettel-schema
   (:class ch.dreyeck.zettelkasten.xml.Zettel
    :field manlinks
    :type String))

  (:object-factory
   (:class ch.dreyeck.zettelkasten.xml.ObjectFactory
    :element manlinks
    :type String))

  (:storage-format
   (:method Daten.setManualLinks
    :format "comma-separated integer entry numbers")
   (:method Daten.retrievePreparedManualLinksFromContent
    :format "sorted comma-separated integer entry numbers"))

  (:read-format
   (:method Daten.getManualLinks
    :returns "int[] of entry numbers")
   (:method Daten.getManualLinksAsString
    :returns "String[] from splitting comma-separated ELEMENT_MANLINKS text")
   (:method Daten.getManualLinksAsSingleString
    :returns "single comma-separated manlinks string"))

  (:content-extraction
   (:method Daten.extractManualLinksFromContent
    :syntax "[z #number]text[/z]"
    :observed-pattern "\\[z ([^\\[]*)\\](.*?)\\[/z\\]"
    :result "integer list of referenced entries"))

  (:index-base
   (:method Daten.retrieveElement
    :evidence "pos is documented as 1 to getCount(); XML element access uses pos - 1")
   (:method Daten.getEntryAsHtml
    :evidence "displayed entry number is from 1 to size of zknfile; retrieveElement handles pos - 1"))

  (:backreference-behavior
   (:method Daten.addManualLink
    :behavior "adds current entry as manual link to the referred entry, then adds the referred entry to the current entry")
   (:method Daten.addManLink
    :behavior "stores one directed integer reference and rejects entry == addvalue")
   (:method Daten.deleteManualLinks
    :behavior "removes corresponding backlink from referred entries when manual links are deleted"))

  (:deleted-entry-behavior
   (:method Daten.deleteZettel
    :behavior "deleted entries remain in zknFile positions with empty attributes and child text")
   (:method Daten.isDeleted
    :behavior "delegates to isEmpty")
   (:method Daten.isEmpty
    :behavior "treats missing entry or empty content as deleted/empty")))

 :source-model
 (:file "zknFile.xml"
  :source-element zettel
  :source-id-field zknid
  :source-field manlinks
  :reference-format "comma-separated integer entry numbers"
  :reference-target "other zettel entry positions"
  :entry-position-index-base :one-based)

 :identity-resolution-policy
 (:source-note-id
  (:from zettel.zknid
   :target Zkn3LinkRecord.fromSourceId)

  :manlink-token
  (:meaning "legacy zknFile entry position"
   :index-base :one-based
   :conversion "targetElementIndex = token - 1")

  :target-note-id
  (:from "resolved target zettel.zknid"
   :target Zkn3LinkRecord.toSourceId)

  :reason
  "Core import records should not expose fragile positional references when stable source ids are available.")

 :directionality-policy
 (:stored-manlinks-are-read-as-authored-source-data t
  :do-not-synthesize-backlinks-during-import t
  :reason "The source file may already contain backreferences; source reader should import what is present, not reproduce legacy mutation behavior."

  :edge-kind
  (:target Zkn3LinkKind
   :value MANUAL
   :meaning "manual note-reference relation"))

 :field-mapping
 ((zettel.zknid                -> Zkn3LinkRecord.fromSourceId)
  (zettel.manlinks.token       -> :legacy-target-entry-position)
  (zknFile.zettel[token].zknid -> Zkn3LinkRecord.toSourceId)
  (:constant MANUAL            -> Zkn3LinkRecord.kind))

 :manlink-completeness-policy
 (:missing-manlinks-field
  (:severity INFO
   :effect "note has no manual links; batch remains valid")

  :blank-manlinks-field
  (:severity INFO
   :effect "note has no manual links; batch remains valid")

  :empty-token-after-splitting
  (:severity INFO
   :effect "ignore only if caused by extra delimiter or whitespace; preserve diagnostic if useful")

  :non-integer-token
  (:severity ERROR
   :effect "reject complete batch")

  :zero-index
  (:severity ERROR
   :effect "reject complete batch because entry positions are one-based")

  :negative-index
  (:severity ERROR
   :effect "reject complete batch")

  :target-entry-out-of-range
  (:severity ERROR
   :effect "reject complete batch")

  :target-zettel-missing-zknid
  (:severity ERROR
   :effect "reject complete batch")

  :target-zettel-deleted-or-empty
  (:severity ERROR
   :effect "reject complete batch unless a deleted-entry policy is designed")

  :self-link
  (:severity WARNING
   :effect "open policy; design must decide preserve or reject before implementation"
   :legacy-evidence "Daten.addManLink returns false when entry == addvalue")

  :duplicate-manlink-same-source-target-kind
  (:severity INFO
   :effect "deduplicate exact duplicate edge"))

 :deleted-entry-policy
 (:problem "Legacy zknFile positions may remain stable by leaving cleared/deleted zettel elements in place."
  :required-before-implementation
  "Define how to detect deleted/empty target zettel entries."
  :observed-legacy-detection
  (:deleted "Daten.isDeleted delegates to isEmpty"
   :empty "Daten.isEmpty returns true when the zettel content child is empty or the entry is missing")
  :recommended-default
  "A manlink to a deleted/empty target rejects the complete batch until deletion semantics are explicitly modeled.")

 :batch-ordering-policy
 (:requires-complete-notes t
  :requires-complete-keywords t
  :requires-attachment-presence-guard t
  :if-note-batch-rejected "return zero records; preserve note ERROR diagnostics"
  :if-keyword-batch-rejected "return zero records; preserve keyword ERROR diagnostics"
  :if-attachment-guard-rejected "return zero records; preserve attachment ERROR diagnostics"
  :if-manlink-resolution-rejected "return zero records for notes, keywords, links, and sequences; emit ERROR diagnostics and an ERROR import summary")

 :rejected-behaviors
 ((:map-links-link-to-manual-note-edge
   :reason "links/link is attachment or hyperlink metadata")

  (:use-raw-position-as-target-note-id
   :reason "core import records should use source note ids where available, not positional references")

  (:silently-drop-invalid-manlink
   :reason "would create incomplete graph import")

  (:synthesize-backlinks
   :reason "source reader should not reproduce legacy mutation behavior during import")

  (:merge-manlinks-with-luhmann
   :reason "manual cross-references and sequence/trail relations have different semantics")

  (:write-manlinks-to-sqlite
   :reason "source reader returns import records only")

  (:surface-manlinks-in-ui
   :reason "source reader must remain UI-free"))

 :confirmed-non-changes
 (:implements-manlink-extraction-p nil
  :creates-Zkn3LinkRecord-p nil
  :populates-Zkn3ImportBatch.links-p nil
  :implements-luhmann-extraction-p nil
  :adds-attachment-record-p nil
  :adds-jaxb-dependency-p nil
  :adds-sqlite-import-logic-p nil
  :changes-Zkn3SourceReader-p nil
  :changes-import-record-classes-p nil
  :changes-ui-wiring-p nil
  :changes-persistence-schema-p nil)

 :future-implementation-probe
 (!implement-zkn3-manlink-resolution-probe
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :resolution-probe-only
  :source-field manlinks
  :index-base :one-based
  :target-record Zkn3LinkRecord
  :kind MANUAL
  :must-not-populate-link-records-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-mapping-task
 (!implement-zkn3-manlink-record-mapping
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :manual-link-records-only
  :source-field manlinks
  :target-record Zkn3LinkRecord
  :kind MANUAL
  :requires-manlink-resolution t
  :must-not-map-luhmann-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
