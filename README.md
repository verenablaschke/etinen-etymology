# Word/morpheme-level etymology inference

This repository contains the backend code for the etymology inference component for the Etymological Inference Engine (EtInEn).
Running it requires access to the other modules in https://github.com/jdellert/etinen-full (all of them for the GUI version; everything but `etinen` for the command line version).

The most important classes are:
- [EtymologyProblem](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/problems/EtymologyProblem.java): defines the Probabilistic Soft Logic (PSL) rules and predicates that encode what kinds of phonetic and semantic changes are to be expected for loanwords and inherited words; runs the PSL inference
- [EtymologyIdeaGenerator](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/ideas/EtymologyIdeaGenerator.java): given a set of word forms or language and semantic concept combinations, the idea generator creates the corresponding PSL atoms that can be inserted into the EtymologyProblem's PSL rules
- [EtymologyRagFilter](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/filter/EtymologyRagFilter.java): determines which of the inferred results are relevant enough to be should be shown to the user in the Rule Atom Graph (RAG)
- The classes in the [`talk`](https://github.com/verenablaschke/etinen-etymology/tree/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/talk) package contain classes for many of the PSL rules and predicates used in EtymologyProblem that can also generate explanations for the inference outcome.

The code for the etymology component is currently slightly outdated, after several major changes to the `etinen-shared` module.

The GUI classes for this component can be found in https://github.com/jdellert/etinen/tree/master/src/main/java/de/tuebingen/sfs/eie/gui/etymology.

# Predicates

## Closed predicates

(F = form; S = semantics; X = exists; T = tree)

About language forms:
- **Flng**(form ID, language): form--language pairs
- **Fufo**(form ID, phon. form): underlying form
- **Fsem**(form ID, concept): semantics of a form
- **XFufo**(form ID): helper predicate: an underlying form for this ID exists

Similarity measures:
- **Fsim**(phon. form 1, phon. form 2): phonetic similarity
- **Ssim**(concept 1, concept 2): semantic similarity

Phylogenetic information:
- **Tanc**(language 1, language 2): language 2 is an ancestor of language 1
- **Tcnt**(language 1, language 2): contact between the two languages

## Open predicates
- **Einh**(form ID 1, form ID 2): form 1 is inherited from form 2
- **Eloa**(form ID 1, form ID 2): form 1 was loaned from form 2
- **Eunk**(form ID): unknown etymology
