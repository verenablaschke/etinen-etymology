# Word/morpheme-level etymology inference

This repository contains the backend code for the etymology inference component for the Etymological Inference Engine (EtInEn).
For a standalone version with some sample data see [etinen-etymology-standalone](https://github.com/verenablaschke/etinen-etymology-standalone).

The most important classes are:

- [EtymologyProblem](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/problems/EtymologyProblem.java):
  defines the Probabilistic Soft Logic (PSL) rules and predicates that encode what kinds of phonetic and semantic
  changes are to be expected for loanwords and inherited words; runs the PSL inference
- [EtymologyIdeaGenerator](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/ideas/EtymologyIdeaGenerator.java):
  given a set of word forms or language and semantic concept combinations, the idea generator creates the corresponding
  PSL atoms that can be inserted into the EtymologyProblem's PSL rules
- [EtymologyRagFilter](https://github.com/verenablaschke/etinen-etymology/blob/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/filter/EtymologyRagFilter.java):
  determines which of the inferred results are relevant enough to be should be shown to the user in the Rule Atom
  Graph (RAG)
- The package [`talk/rule`](https://github.com/verenablaschke/etinen-etymology/tree/master/src/main/java/de/tuebingen/sfs/eie/components/etymology/talk/rule)
  contains classes for the PSL rules used in EtymologyProblem that can also generate explanations for the inference
  outcome. (The relevant talking predicate classes are
  in [jdellert/etinen-shared/.../talk/pred](https://github.com/jdellert/etinen-shared/tree/master/src/main/java/de/tuebingen/sfs/eie/shared/talk/pred)
  .)

# Predicates

## Closed predicates

These are either set to 1 or, if not applicable for a pair of arguments, simply not added.

- **Xinh**(form1, form2): an inheritance relation is possible (the language of form 2 is direct ancestor of language of form 1)
- **Xloa**(form1, form2): a contact relation is possible (the language of form 1 has an incoming contact link from language 2)

## Open predicates

- **Einh**(form1, form2): form 1 is inherited from form 2
- **Eloa**(form1, form2): form 1 was borrowed from form 2
- **Eunk**(form): the form's etymology is unknown (outside the scope of the input data)
- **Fhom**(form, homset): the form belongs to the given homologue set;* fixed to 1 or 0 for attested data, to 1 for confirmed reconstructions, open otherwise
- **Fsim**(form1, form2): phonetic similarity; fixed to [1 - IWSA distance] for attested data and reconstructions, open otherwise

\* Homologue set = set of words with the same origin; a cognate set with the relevant loanwords added

# PSL Rules

## Constraints:

These mainly ensure symmetry and a certain degree of transitivity, 
and that each form's homologue membership judgments and potential etymologies form distributions:

| Name                 | Rule                                                                     | Explanation                                                                                                                                                                                 |
|----------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **EinhOrEloaOrEunk** | Einh(X, +Y) + Eloa(X, +Z) + Eunk(X) = 1 .                                | The possible explanations for a word's origin must follow a probability distribution.                                                                                                       |
| **EloaPlusEloa**     | Eloa(X, Y) + Eloa(Y, X) <= 1 .                                           | Borrowing cannot happen in a circular fashion.                                                                                                                                              |
| **FsimSymmetry**     | Fsim(X, Y) = Fsim(Y, X) .                                                | Form similarity is symmetric.                                                                                                                                                               |
| **FsimTransitivity** | Fsim(X, Y) & Fsim(Y, Z) & (X != Y) & (X != Z) & (Y != Z) -> Fsim(X, Z) . | Form similarity is transitive: if a form is similar to two other forms, those should also be similar to one another.                                                                        |
| **FhomDistribution** | Fhom(X, +H) = 1.                                                         | Every word must belong to exactly one homologue set.                                                                                                                                        |
| **EinhToFhom**       | Einh(X, Y) & Fhom(Y, H) -> Fhom(X, H) .                                  | When we reconstruct an inheritance and assign some belief to the homologue status of the parent, we must assign at least as much belief to the child's inclusion in the same homologue set. |
| **EloaToFhom**       | Eloa(X, Y) & Fhom(Y, H) -> Fhom(X, H) .                                  | When we reconstruct a borrowing and assign some belief to the homologue status of the donor, we must assign at least as much belief to the recipient's inclusion in the same homologue set. |

## Priors:

| Name          | Rule             | Explanation                                                                                                 |
|---------------|------------------|-------------------------------------------------------------------------------------------------------------|
| **EunkPrior** | 2.5: ~Eunk(X)    | By default, we prefer available explanations within the scope of the forms the inference is performed over. |
| **EloaPrior** | 0.5: ~Eloa(X, Y) | By default, we do not assume that a word is a loanword.                                                     |

## Other weighted rules:

| Name                  | Rule                                                                  | Explanation                                                                                                                                        |
|-----------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| **EinhToFsim**        | 2.0: Einh(X, Z) & Einh(Y, Z) & (X != Y) -> Fsim(X, Y)                 | If two forms are inherited from the same form, they should be similar.                                                                             |
| **FsimToFsim**        | 1.0: Fsim(X, Y) & Einh(X, W) & Einh(Y, Z) & (W != Z) -> Fsim(W, Z)    | If two forms are similar and inherited from different sources, those source words should be similar to one another too.                            |
| **EloaToFsim**        | 1.0: Eloa(X, Y) & Fsim(X, Z) & (Y != Z) & (X != Z) -> Fsim(X, Y)      | If a form was inherited, then any similar form should also be similar to the donor form.                                                           |
| **FhomChildToParent** | 0.6: Fhom(X, H) & Xinh(X, Z) -> Fhom(Z, H)                            | If a homologue of H in unlikely to exist in a parent language, that makes it less likely for a homologue to exist in the child language.           |
| **FhomParentToChild** | 0.2: Fhom(Z, H) & Xinh(X, Z) -> Fhom(X, H)                            | If a homologue of H in unlikely to exist in a child language, that makes it less likely for a homologue to exist in the parent language.           |
| **FhomToEinh**        | 0.4: Fhom(X, H) & Fhom(Y, H) & Xinh(X, Y) -> Einh(X, Y)               | If the forms in a language and its parent are assigned to the same homologue set, this suggests that the form in the child language was inherited. |
| **FhomToEloa**        | 1.0: Fhom(X, H) & ~Fhom(Y, H) & Xinh(X, Y) & Xloa(X, Z) -> Eloa(X, Z) | If there is any doubt about the reconstructability of a homologue set in the parent, an available loanword etymology becomes much more likely.     |
