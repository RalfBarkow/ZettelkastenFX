(:id zkn3-luhmann-sequence-extraction-design
 :kind :mapping-design-only
 :scope AC-06

 :task
 (!design-zkn3-luhmann-sequence-extraction
  :module zk-source-zkn3
  :mode :mapping-design-only
  :source-field luhmann
  :target-record Zkn3SequenceRecord
  :requires-note-keyword-and-manlink-batch-policy t
  :must-not-implement-sequence-extraction-yet t)

 :status :design-only
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"
 :source-authority #P"/Users/rgb/rgb~Zettelkasten/Zettelkasten-Dateien/rgb.zkn3"

 :completed-prerequisites
 ((:commit "7c4db4ddcb425da518c9aea04703c7d7281c5a85"
   :subject "feat(zettelkasten): extract zkn3 manual link records"))

 :requires
 (:complete-note-keyword-manlink-and-attachment-policy
  (:no-incomplete-imports t
   :no-per-note-skipping t
   :no-partial-successful-batches t
   :invalid-note-field-rejects-batch t
   :invalid-keyword-reference-rejects-batch t
   :nonempty-unsupported-attachment-rejects-batch t
   :invalid-manlink-reference-rejects-batch t
   :sqlite-write-allowed-p nil))

 :current-importer-state
 (:notes-extracted-p t
  :keywords-extracted-p t
  :attachment-link-guard-p t
  :manual-links-extracted-as-Zkn3LinkRecord-MANUAL-p t
  :backlinks-synthesized-by-reader-p nil
  :luhmann-extracted-p nil
  :sqlite-write-allowed-p nil)

 :target-record
 (:class zk.core.importing.Zkn3SequenceRecord
  :fields ((parentSourceId String)
           (childSourceId String)
           (order int))
  :target-port zk.core.ports.SequenceRepository
  :target-port-methods (childrenOf parentOf insertChild reorder detach)
  :order-observation "Existing tests use order 0 for first inserted child; preserve source token order and map first child to order 0 unless later repository evidence changes this.")

 :legacy-evidence
 ((:constant
   (:class de.danielluedecke.zettelkasten.database.Daten
    :name ELEMENT_TRAILS
    :value "luhmann"))

  (:format-comment
   (:class de.danielluedecke.zettelkasten.database.Daten
    :finding "luhmann is used to indicate follower-entries (trails), displayed as sub-entries of an entry"))

  (:zettel-schema
   (:class ch.dreyeck.zettelkasten.xml.Zettel
    :field luhmann
    :type String
    :position "distinct required child element after misc"))

  (:object-factory
   (:class ch.dreyeck.zettelkasten.xml.ObjectFactory
    :element luhmann
    :type String))

  (:source-fixture
   (:class de.danielluedecke.zettelkasten.database.DatenTestNG
    :example "<luhmann>4,10,61,161,1771,3622</luhmann>"
    :meaning "comma-separated entry references")
   (:example "<luhmann>1</luhmann>"
    :finding "findParentLuhmann(1, true) returns 2 when zettel 2 has luhmann 1"))

  (:read-methods
   (:method Daten.getSubEntriesCsv
    :finding "returns raw ELEMENT_TRAILS text for a zettel")
   (:method Daten.getLuhmannNumbersAsString
    :finding "splits luhmann follower numbers on comma")
   (:method Daten.getLuhmannNumbersAsInteger
    :finding "parses split luhmann tokens as integers")
   (:method Daten.getAllLuhmannNumbers
    :finding "collects notes with sequence children and their child ids"))

  (:mutation-methods
   (:method Daten.appendSubEntryToEntry
    :finding "appends a new sub-entry to a parent entry's luhmann list")
   (:method Daten.addSubEntryToEntryAtPosition
    :finding "inserts newSubEntry into parentEntry ELEMENT_TRAILS at an explicit sibling position")
   (:method Daten.addEntry
    :finding "when a new follower entry is added, appendSubEntryToEntry(luhmann, activatedEntryNumber) stores the new entry under the related parent entry")
   (:method Daten.deleteLuhmannNumber
    :finding "removes entryToRemove from parentEntry ELEMENT_TRAILS")
   (:method Daten.setLuhmann
    :finding "generated JAXB setter exists on Zettel; Daten writes ELEMENT_TRAILS directly rather than using this setter"))

  (:navigation-methods
   (:method Daten.findParentLuhmann
    :finding "finds a parent by scanning all entries and checking whether their luhmann CSV contains the requested entry number")
   (:method Daten.isTopLevelLuhmann
    :finding "a top-level luhmann entry has followers but findParentLuhmann returns -1")
   (:method Daten.retrieveAllLuhmannNumbers
    :finding "recursively descends through child luhmann entries")
   (:method Daten.findChildrenLuhmann
    :finding "requested method name was not found; child lookup behavior is represented by getSubEntriesCsv/getLuhmannNumbers* and recursive retrieval"))

  (:cycle-and-self-guards
   (:method Daten.addSubEntryToEntryAtPosition
    :self-link "returns false when parentEntry equals newSubEntry")
   (:method Daten.firstEntryIsDescendantOfSecondEntry
    :cycle "prevents adding an ancestor as a descendant because it would create a cycle")
   (:method Daten.getSubEntryPosition
    :self-link "returns -1 when parentEntry equals subEntry")))

 :semantic-candidates
 ((:luhmann-field-lists-parents
   :meaning "current zettel is a child/follow-up of each referenced zettel")

  (:luhmann-field-lists-children
   :meaning "current zettel has each referenced zettel as a child/follow-up")

  (:luhmann-field-lists-trail-neighbors
   :meaning "references are trail relations requiring additional direction policy")))

 :semantic-decision
 (:evidence-backed-candidate :luhmann-field-lists-children
  :meaning "the zettel containing the luhmann field is the parent; each token is a child/sub-entry position"
  :evidence
  ((Daten.getSubEntriesCsv "returns follower/sub-entry numbers for the current entry")
   (Daten.addSubEntryToEntryAtPosition "stores newSubEntry under parentEntry ELEMENT_TRAILS")
   (Daten.findParentLuhmann "finds parents by scanning other entries whose luhmann CSV contains the child entry number")
   (DatenTestNG "entry 2 with <luhmann>1</luhmann> is the parent of entry 1"))
  :implementation-decision :defer-until-evidence-only-confirmation
  :reason "Direction evidence is strong, but the next executable slice should record it independently before production mapping.")

 :source-model
 (:file "zknFile.xml"
  :source-element zettel
  :source-id-field zknid
  :source-field luhmann
  :reference-format "comma-separated integer entry positions"
  :reference-target "other zettel entry positions"
  :entry-position-index-base :one-based
  :relation-kind :sequence-or-folgezettel)

 :identity-resolution-policy
 (:source-note-id
  (:from zettel.zknid
   :target "Zkn3SequenceRecord parentSourceId or childSourceId depending on direction")

  :luhmann-token
  (:meaning "legacy zknFile entry position"
   :index-base :one-based
   :conversion "targetElementIndex = token - 1")

  :target-note-id
  (:from "resolved target zettel.zknid"
   :target "Zkn3SequenceRecord childSourceId if luhmann lists children")

  :reason
  "Core import records should use stable source ids, not positional references, after resolution.")

 :candidate-field-mapping-if-luhmann-lists-parents
 ((zettel.luhmann.token             -> :legacy-parent-entry-position)
  (zknFile.zettel[token - 1].zknid  -> Zkn3SequenceRecord.parentSourceId)
  (zettel.zknid                     -> Zkn3SequenceRecord.childSourceId)
  (:token-order                     -> Zkn3SequenceRecord.order))

 :candidate-field-mapping-if-luhmann-lists-children
 ((zettel.zknid                     -> Zkn3SequenceRecord.parentSourceId)
  (zettel.luhmann.token             -> :legacy-child-entry-position)
  (zknFile.zettel[token - 1].zknid  -> Zkn3SequenceRecord.childSourceId)
  (:token-order                     -> Zkn3SequenceRecord.order))

 :recommended-candidate-field-mapping
 (:direction :luhmann-field-lists-children
  :fields
  ((zettel.zknid                     -> Zkn3SequenceRecord.parentSourceId)
   (zettel.luhmann.token             -> :legacy-child-entry-position)
   (zknFile.zettel[token - 1].zknid  -> Zkn3SequenceRecord.childSourceId)
   (:token-order-zero-based          -> Zkn3SequenceRecord.order))
  :implementation-status :not-yet-approved
  :required-before-implementation "Run evidence-only direction semantics slice and then implement a resolution probe before populating sequence records.")

 :ordering-policy
 (:preserve-token-order t
  :meaning "comma-separated luhmann references are imported in source order"
  :order-base :zero-based
  :evidence ((Daten.addSubEntryToEntryAtPosition "inserts at explicit position")
             (Daten.getSubEntryPosition "returns zero-based sibling position")
             (SequenceRepository.insertChild "accepts int order")
             (SQLiteRepositoriesTest "uses insertChild(parent, child, 0) for first child")))

 :luhmann-completeness-policy
 (:missing-luhmann-field
  (:severity INFO
   :effect "note has no sequence references; batch remains valid")

  :blank-luhmann-field
  (:severity INFO
   :effect "note has no sequence references; batch remains valid")

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

  :self-sequence-reference
  (:severity ERROR
   :effect "reject complete batch unless later evidence proves self-trails are meaningful"
   :legacy-evidence "Daten.addSubEntryToEntryAtPosition returns false when parentEntry equals newSubEntry")

  :duplicate-sequence-reference-same-source
  (:severity INFO
   :effect "deduplicate exact duplicate relation, preserving first occurrence order")

  :cycle
  (:severity WARNING
   :effect "design open: direct insertion prevents cycles, but import should define whether detected source cycles reject batch or emit warnings before SQLite application"))

 :directionality-and-multiplicity-policy
 (:direction
  (:decision :evidence-backed-candidate-lists-children
   :methods-reviewed (findParentLuhmann getSubEntriesCsv getLuhmannNumbersAsString getLuhmannNumbersAsInteger appendSubEntryToEntry addSubEntryToEntryAtPosition deleteLuhmannNumber)
   :missing-requested-methods (findChildrenLuhmann)
   :requires-next-slice !inspect-zkn3-luhmann-direction-semantics))

  :multiple-references
  (:allowed-p t
   :reason "fixtures and legacy methods allow multiple comma-separated follower entries in a single luhmann field")

  :multiple-parents
  (:allowed-by-source-p :possible
   :core-port-note "SequenceRepository.parentOf(child) suggests one parent in storage; importer must decide whether duplicate child under multiple parents rejects batch before repository application")

  :backlinks-synthesized-by-reader-p nil
  :reason "source reader imports stored source relations; it does not reproduce legacy mutation behavior")

 :batch-ordering-policy
 (:requires-complete-notes t
  :requires-complete-keywords t
  :requires-complete-manlinks t
  :requires-attachment-presence-guard t
  :if-note-batch-rejected "return zero records; preserve note ERROR diagnostics"
  :if-keyword-batch-rejected "return zero records; preserve keyword ERROR diagnostics"
  :if-manlink-batch-rejected "return zero records; preserve manlink ERROR diagnostics"
  :if-attachment-guard-rejected "return zero records; preserve attachment ERROR diagnostics"
  :if-luhmann-resolution-rejected "return zero records for notes, keywords, links, and sequences; emit ERROR diagnostics and an ERROR import summary")

 :rejected-behaviors
 ((:merge-luhmann-with-manlinks
   :reason "manual cross-references and Folgezettel/trail relations have different semantics")

  (:use-raw-position-as-core-note-id
   :reason "source ids should be resolved to zknid where possible")

  (:silently-drop-invalid-luhmann-token
   :reason "would create incomplete sequence import")

  (:invent-sequence-direction
   :reason "direction must come from legacy method behavior, not field name alone")

  (:synthesize-backlinks
   :reason "source reader must not reproduce legacy mutation behavior")

  (:write-sequences-to-sqlite
   :reason "source reader returns import records only")

  (:surface-sequences-in-ui
   :reason "source reader must remain UI-free"))

 :confirmed-non-changes
 (:implements-luhmann-extraction-p nil
  :creates-Zkn3SequenceRecord-in-production-p nil
  :populates-Zkn3ImportBatch.sequences-p nil
  :changes-Zkn3DomSourceReader-p nil
  :changes-tests-p nil
  :adds-new-link-extraction-p nil
  :adds-attachment-record-p nil
  :adds-jaxb-dependency-p nil
  :adds-sqlite-import-logic-p nil
  :changes-Zkn3SourceReader-p nil
  :changes-import-record-classes-p nil
  :changes-ui-wiring-p nil
  :changes-persistence-schema-p nil
  :edits-legacy-swing-repo-p nil
  :edits-repomix-files-p nil)

 :next-executable-task
 (!inspect-zkn3-luhmann-direction-semantics
  :module zk-source-zkn3
  :mode :evidence-only
  :source-field luhmann
  :legacy-methods (findParentLuhmann findChildrenLuhmann getLuhmannNumbers)
  :must-not-implement-sequence-extraction-yet t)

 :later-implementation-probe
 (!implement-zkn3-luhmann-resolution-probe
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :resolution-probe-only
  :source-field luhmann
  :index-base :one-based
  :target-record Zkn3SequenceRecord
  :must-not-populate-sequence-records-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :later-mapping-task
 (!implement-zkn3-luhmann-sequence-record-mapping
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :sequence-records-only
  :source-field luhmann
  :target-record Zkn3SequenceRecord
  :requires-luhmann-direction-policy t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
