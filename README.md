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
- **Xinh**(form1, form2): an inheritance relation is possible, i.e. language of form 2 is direct ancestor of language of form 1
- **Xloa**(form1, form2): a contact relation is possible, i.e. language of form 1 has an incoming contact link from language 2

## Open predicates
- **Fhom**(form, homset): the (hypothetical) form exists for the homologue set; fixed to 1 or 0 for attested data, to 1 for confirmed reconstructions, loose otherwise
- **Fsim**(form1, form2): phonetic similarity; fixed to (1 - IWSA distance) for attested data and reconstructions, loose otherwise
- **Einh**(form1, form2): form 1 is inherited from form 2
- **Eloa**(form1, form2): form 1 was borrowed from form 2
- **Eunk**(form): unknown etymology

# PSL Rules

| Name | Rule | Explanation |
|---|---|---|
| EunkPrior | ~Eunk(X) | By default, we do not assume that words are of unknown origin. |
| EloaPrior | ~Eloa(X, Y) | By default, we do not assume that a word is a loanword. |
| EinhOrEloaOrEunk | Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 . | The possible explanations for a word's origin follow a probability distribution. |
| EloaPlusEloa | Eloa(X, Y) + Eloa(Y, X) <= 1 . | Borrowing cannot happen in a circular fashion. |
| FhomToFhom | Fhom(X,H) & Fhom(Y,H) & Xinh(X,Z) & Xinh(Y,Z) -> Fhom(Z,H) | |
| FsimToEinh | Fsim(X,Y) & Xinh(X,Z) & Xinh(Y,Z) -> Einh(X,Z) | |
| FsimToFsim | Fsim(X,Y) & Einh(X,W) & Einh(Y,Z) & W != Z -> Fsim(W,Z) | |
| FsimPlusFsim | Fsim(X,Y) = Fsim(Y,X) . | |
