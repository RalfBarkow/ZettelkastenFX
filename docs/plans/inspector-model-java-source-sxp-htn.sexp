(:task
 (!extend-htn-for-java-source-model-inspector
  :context inspector-model-bug
  :legacy-repo #P"/Users/rgb/workspace/Zettelkasten/"
  :implementation-repo #P"/Users/rgb/workspace/ZettelkastenFX/"
  :focus "How are manlinks created, stored, resolved, maintained, deleted and projected?"))

(:problem
 (:name inspector-model-bug)
 (:symptom "grep output was inspected as if it were a model")
 (:failure "the object shape did not match the reading task")
 (:example "PATTERN getManualLinks is a search probe, not a lifecycle operation")
 (:required-correction "parse Java source into a repo model, then derive lifecycle views"))

(:operational-definition
 (:explanation
  (:is "an inspectable derivation object connecting a question to model facts, source spans, role assignments, and decision impact")
  (:must-contain
   (:question
    :model-objects-used
    :source-spans-used
    :derivation-steps
    :uncertainties
    :decision-impact))
  (:benefits
   (:auditable true)
   (:navigable true)
   (:replannable true)
   (:non-terminal true))))

(:revised-htn
 (:method inspect-manlinks-lifecycle-through-java-model
  :task
  (inspect-manlinks-lifecycle
   :repo #P"/Users/rgb/workspace/Zettelkasten/"
   :focus "How are manlinks created, stored, resolved, maintained, deleted and projected?")

  :subtasks
  (ordered
   (!define-java-source-model-vocabulary)
   (!parse-java-repo-to-sxp-model)
   (!derive-java-call-and-reference-index)
   (!derive-manlink-lifecycle-model)
   (!inspect-manlink-lifecycle-model)
   (!ask-model-bounded-questions)
   (!use-grep-only-as-targeted-sensor-if-model-has-gap))))

(:search-policy
 (:grep-first false)
 (:grep-status :raw-sensor-data)
 (:grep-allowed-when
  ((:missing-model-fact true)
   (:target-symbol-known true)
   (:expected-result-type declared)
   (:post-processing-target declared)))
 (:not-acceptable-as-final-model true))

(:manlink-lifecycle-question
 (:created "Where are manual link tokens extracted from content?")
 (:stored "Where are tokens serialized into ELEMENT_MANLINKS?")
 (:resolved "Where are tokens parsed or resolved to integer references?")
 (:maintained "Where are backreferences added or adjusted?")
 (:deleted "Where are references removed or rewritten when a zettel is deleted?")
 (:projected "Where are manual links shown or exported?"))

(:next-task
 (!design-zkn3-unresolved-reference-record
  :module zk-core
  :mode :core-vocabulary-design-only
  :records (Zkn3UnresolvedReferenceRecord
            Zkn3UnresolvedReferenceKind
            Zkn3UnresolvedReferenceReason)
  :batch-field unresolvedReferences
  :source-fields (manlinks luhmann)
  :first-use-case (:manlinks :out-of-range-reference)
  :must-not-implement-record-yet t
  :must-not-change-source-reader-acceptance-yet t
  :must-not-write-to-sqlite t
  :must-not-touch-ui t))
