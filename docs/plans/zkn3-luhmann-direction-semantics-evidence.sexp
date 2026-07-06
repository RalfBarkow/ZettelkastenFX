(:id zkn3-luhmann-direction-semantics-evidence
 :kind :evidence-only
 :scope AC-06

 :task
 (!inspect-zkn3-luhmann-direction-semantics
  :module zk-source-zkn3
  :mode :evidence-only
  :source-field luhmann
  :legacy-methods (findParentLuhmann findChildrenLuhmann getLuhmannNumbers)
  :must-not-implement-sequence-extraction-yet t)

 :status :confirmed
 :implementation-repo "/Users/rgb/workspace/ZettelkastenFX"
 :legacy-evidence-repo "/Users/rgb/workspace/Zettelkasten"

 :completed-prerequisites
 ((:commit "08068545ab5c3d51299b5b18c4b709aab47169ab"
   :subject "docs(zettelkasten): design zkn3 luhmann sequence extraction"))

 :candidate-direction
 (:source-field luhmann
  :candidate "containing zettel is parent; luhmann tokens are children"
  :candidate-record-mapping
  ((containing-zettel.zknid -> Zkn3SequenceRecord.parentSourceId)
   (luhmann-token-resolved-zettel.zknid -> Zkn3SequenceRecord.childSourceId)
   (token-order -> Zkn3SequenceRecord.order)))

 :legacy-evidence
 ((:constant ELEMENT_TRAILS
   :file "Daten.java"
   :finding "ELEMENT_TRAILS has value \"luhmann\".")

  (:method retrieveElement
   :file "Daten.java"
   :finding "public positions are documented as 1..getCount(); XML element access uses pos - 1.")

  (:method getSubEntriesCsv
   :file "Daten.java"
   :finding "returns the raw luhmann child text for the given entry; comments call it follower- or sub-entries of an entry.")

  (:method getLuhmannNumbers
   :file "Daten.java"
   :finding "No exact getLuhmannNumbers method exists; getLuhmannNumbersAsString and getLuhmannNumbersAsInteger are the concrete legacy readers.")

  (:method getLuhmannNumbersAsString
   :file "Daten.java"
   :finding "reads getSubEntriesCsv(pos), splits the comma-separated luhmann text, and returns follower-entry numbers for entry pos.")

  (:method getLuhmannNumbersAsInteger
   :file "Daten.java"
   :finding "reads getSubEntriesCsv(pos), splits luhmann text, and parses tokens as integer follower-entry numbers.")

  (:method getAllLuhmannNumbers
   :file "Daten.java"
   :finding "iterates entries with luhmann numbers, adds each listed child id, then adds the current parent note id.")

  (:method findParentLuhmann
   :file "Daten.java"
   :finding "searches every currentEntry, reads currentEntry luhmann CSV, and returns currentEntry when its CSV contains the requested entryNumber.")

  (:method findChildrenLuhmann
   :file "Daten.java"
   :finding "No exact findChildrenLuhmann method exists; child lookup is represented by getSubEntriesCsv/getLuhmannNumbers* and recursive retrieveAllLuhmannNumbers.")

  (:method appendSubEntryToEntry
   :file "Daten.java"
   :finding "delegates to addSubEntryToEntryAtPosition(entry, newSubEntry, -1), appending newSubEntry to entry's sub-entries.")

  (:method addSubEntryToEntryAtPosition
   :file "Daten.java"
   :finding "parameters are parentEntry and newSubEntry; method inserts newSubEntry into parentEntry ELEMENT_TRAILS and rejects parentEntry == newSubEntry.")

  (:method addLuhmann
   :file "Daten.java"
   :finding "No exact addLuhmann method exists; appendSubEntryToEntry and addSubEntryToEntryAtPosition are the concrete add operations.")

  (:method deleteLuhmann
   :file "Daten.java"
   :finding "No exact deleteLuhmann method exists; deleteLuhmannNumber(parentEntry, entryToRemove) removes entryToRemove from parentEntry ELEMENT_TRAILS.")

  (:method firstEntryIsDescendantOfSecondEntry
   :file "Daten.java"
   :finding "prevents cycles by checking whether a proposed ancestor already appears in a proposed child subtree.")

  (:method getSubEntryPosition
   :file "Daten.java"
   :finding "searches a parent entry's luhmann CSV for a subEntry and returns the zero-based sibling position.")

  (:method Zettel.getLuhmann
   :file "Zettel.java"
   :finding "generated JAXB getter returns the luhmann string field without changing semantics.")

  (:method Zettel.setLuhmann
   :file "Zettel.java"
   :finding "generated JAXB setter stores the luhmann string field without changing semantics.")

  (:method ObjectFactory.createLuhmann
   :file "ObjectFactory.java"
   :finding "generated JAXB factory creates the luhmann string element.")

  (:tests
   :file "DatenTestNG.java"
   :finding "fixture puts <luhmann>1</luhmann> on entry 2; test asserts findParentLuhmann(1, true) returns 2, proving containing entry 2 is parent and token 1 is child."))

 :direction-decision
 (:decision :containing-zettel-parent_tokens-children
  :confidence :confirmed
  :mapping
  ((containing-zettel.zknid -> Zkn3SequenceRecord.parentSourceId)
   (token-resolved-zettel.zknid -> Zkn3SequenceRecord.childSourceId)
   (token-order -> Zkn3SequenceRecord.order)))

 :index-base
 (:decision :one-based
  :evidence "Daten.retrieveElement documents positions from 1 to getCount() and accesses the XML element list with pos - 1; luhmann comments repeat that positions are 1..getCount()."
  :conversion "targetElementIndex = token - 1")

 :ordering-decision
 (:preserve-token-order t
  :target-field Zkn3SequenceRecord.order
  :order-origin "comma-separated luhmann token order"
  :evidence "addSubEntryToEntryAtPosition inserts at a sibling position, getSubEntryPosition returns the zero-based position in the parent luhmann CSV, and SequenceRepository.insertChild accepts an int order.")

 :self-and-cycle-evidence
 (:self-sequence-reference
  (:legacy-behavior "addSubEntryToEntryAtPosition returns false when parentEntry equals newSubEntry"
   :import-policy "self sequence references should reject the complete batch unless a later policy deliberately overrides legacy behavior")

  :cycle
  (:legacy-behavior "firstEntryIsDescendantOfSecondEntry prevents adding cycles to the sub-entry tree"
   :import-policy "cycle detection should be designed before repository application; direct source cycles should not be silently imported"))

 :implementation-gate
 (:sequence-extraction-allowed-p true
  :reason "Legacy reader, mutation, navigation, and test behavior confirm direction and one-based index conversion. A later resolution probe may inspect luhmann tokens without populating sequence records.")

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

 :next-task-if-confirmed
 (!implement-zkn3-luhmann-resolution-probe
  :module zk-source-zkn3
  :class zk.source.zkn3.Zkn3DomSourceReader
  :mode :resolution-probe-only
  :source-field luhmann
  :direction :containing-zettel-parent_tokens-children
  :index-base :one-based
  :target-record Zkn3SequenceRecord
  :must-not-populate-sequence-records-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t)

 :next-task-if-blocked
 (!construct-zkn3-luhmann-direction-fixture
  :module zk-source-zkn3
  :mode :fixture-design-only
  :source-field luhmann
  :must-not-implement-sequence-extraction-yet t))
