(:id zkn3-manlink-self-link-policy-design
 :kind :policy-design-only
 :scope AC-06

 :task
 (!design-zkn3-manlink-self-link-policy
  :module zk-source-zkn3
  :mode :policy-design-only
  :source-field manlinks
  :target-record Zkn3LinkRecord
  :requires-manlink-resolution-probe t
  :must-not-map-manlinks-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"

 :completed-prerequisites
 ((:commit "88d8a927e08e686f365311f9b6158023c585aa04"
   :subject "docs(zettelkasten): design zkn3 manual link extraction")
  (:commit "f257421d9013b3e425c9842dc9cb9bfaaca80475"
   :subject "feat(zettelkasten): probe zkn3 manual link resolution"))

 :manual-link-model
 (:source-field manlinks
  :reference-format "comma-separated one-based zettel entry positions"
  :source-id zettel.zknid
  :target-id "resolved target zettel.zknid"
  :candidate-record Zkn3LinkRecord
  :candidate-kind MANUAL
  :backlinks-synthesized-by-reader-p nil)

 :self-link-definition
 (:condition (= sourceNoteId targetNoteId)
  :source "manlinks token resolves to the source zettel itself")

 :legacy-evidence
 ((:manual-link-addition
   (:method Daten.addManLink
    :evidence "returns false when entry == addvalue"
    :interpretation "legacy interactive mutation prevents creating self-links through this method"))

  (:backreference-addition
   (:method Daten.addManualLink
    :evidence "calls addManLink(addvalue, entry), then addManLink(entry, addvalue)"
    :interpretation "legacy double-link behavior would invoke the same self-link guard twice for entry == addvalue"))

  (:persisted-manlinks-field
   (:method Daten.getManualLinksAsString
    :evidence "reads the raw comma-separated ELEMENT_MANLINKS text")
   (:method Daten.setManualLinks
    :evidence "stores supplied comma-separated text in ELEMENT_MANLINKS")
   (:interpretation "persisted source data can contain values not created through addManLink"))

  (:content-extraction
   (:method Daten.extractManualLinksFromContent
    :evidence "extracts integer values from [z #number] tags and deduplicates by number")
   (:method Daten.retrievePreparedManualLinksFromContent
    :evidence "sorts extracted integer values into comma-separated text")
   (:interpretation "content extraction does not establish source-reader UI or graph semantics"))

  (:deleted-entry-behavior
   (:method Daten.deleteZettel
    :evidence "clears zknid, title, content, manlinks, trails, and timestamps while keeping the zettel position")
   (:method Daten.isDeleted
    :evidence "delegates to isEmpty")
   (:method Daten.isEmpty
    :evidence "treats missing entry or empty content as empty/deleted")))

 :current-resolution-probe
 (:manlinks-parsed-p t
  :index-base :one-based
  :target-zknid-resolution-p t
  :invalid-or-dangling-reference-severity ERROR
  :self-link-severity WARNING
  :self-link-batch-effect :valid
  :duplicate-source-target-resolution "deduplicate exact source-target pair for probe count"
  :creates-Zkn3LinkRecord-p nil
  :populates-Zkn3ImportBatch.links-p nil
  :sqlite-write-allowed-p nil)

 :policy-options
 ((:preserve-with-warning
   :meaning "create Zkn3LinkRecord even when sourceNoteId equals targetNoteId, and emit WARNING diagnostic"
   :pros ("full-fidelity source preservation" "no silent data loss" "self-loop is structurally representable")
   :cons ("downstream graph views may need to display or filter self-loops"))

  (:reject-batch
   :meaning "treat self-link as invalid and reject the complete batch"
   :pros ("avoids odd graph edges")
   :cons ("rejects otherwise complete source data without proof that self-links are invalid"))

  (:drop-with-warning
   :meaning "do not create edge but emit warning"
   :pros ("keeps graph simpler")
   :cons ("violates no-silent-loss/full-fidelity import policy"))))

 :recommended-policy
 (:decision :preserve-with-warning
  :reason "A resolved self-link is not dangling or malformed; it is explicit source data. Dropping it would be lossy, and rejecting it would require stronger legacy evidence."
  :legacy-evidence-note "Daten.addManLink prevents interactive self-link creation, but persisted ELEMENT_MANLINKS text can still contain a self-reference."
  :mapping-effect "future Zkn3LinkRecord may have identical source and target note ids"
  :diagnostic "emit WARNING for self-link"
  :downstream-note "UI or graph projection may choose to visually filter self-loops later, but source import should preserve them")

 :self-link-completeness-policy
 (:self-link
  (:severity WARNING
   :effect "preserve as MANUAL link record in mapping slice unless later evidence forbids")

  :duplicate-self-link
  (:severity INFO
   :effect "deduplicate exact duplicate source-target-kind edge")

  :invalid-or-dangling-manlink
  (:severity ERROR
   :effect "reject complete batch")

  :silent-drop
  (:allowed-p nil))

 :backreference-policy
 (:synthesize-backlinks-p nil
  :reason "source reader imports persisted source relations and must not reproduce legacy mutation/backreference side effects"
  :self-link-effect "do not create an extra reverse edge for a self-link")

 :rejected-behaviors
 ((:silently-drop-self-link
   :reason "would lose explicit source data")

  (:synthesize-reverse-self-link
   :reason "source reader must not reproduce legacy mutation/backreference behavior")

  (:reject-self-link-without-evidence
   :reason "self-link is resolvable and structurally representable")

  (:hide-self-link-in-ui-from-source-reader
   :reason "source reader remains UI-free"))

 :acceptance-criteria-for-mapping-slice
 (:creates-manual-link-records-p t
  :self-link-record
  (:fromSourceId "same source note id"
   :toSourceId "same target note id"
   :kind MANUAL
   :diagnostic-severity WARNING)
  :duplicate-self-link-deduplication "one source-target-kind MANUAL record"
  :invalid-or-dangling-manlink "ERROR diagnostics and zero-record rejected batch"
  :backlinks-synthesized-by-reader-p nil
  :sqlite-write-allowed-p nil
  :ui-dependency-allowed-p nil)

 :confirmed-non-changes
 (:implements-manlink-record-mapping-p nil
  :creates-Zkn3LinkRecord-in-production-p nil
  :populates-Zkn3ImportBatch.links-p nil
  :implements-luhmann-extraction-p nil
  :adds-attachment-record-p nil
  :adds-jaxb-dependency-p nil
  :adds-sqlite-import-logic-p nil
  :changes-Zkn3SourceReader-p nil
  :changes-import-record-classes-p nil
  :changes-Zkn3DomSourceReader-p nil
  :changes-tests-p nil
  :changes-ui-wiring-p nil
  :changes-persistence-schema-p nil
  :edits-legacy-swing-repo-p nil
  :edits-repomix-files-p nil)

 :future-mapping-task
 (!implement-zkn3-manlink-record-mapping
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :manual-link-records-only
  :source-field manlinks
  :target-record Zkn3LinkRecord
  :kind MANUAL
  :self-link-policy :preserve-with-warning
  :requires-manlink-resolution t
  :must-not-map-luhmann-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
