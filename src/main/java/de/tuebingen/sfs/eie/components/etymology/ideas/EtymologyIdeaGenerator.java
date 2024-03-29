/*
 * Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.*;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.core.LanguagePhylogeny;
import de.tuebingen.sfs.eie.shared.core.LanguageTree;
import de.tuebingen.sfs.eie.shared.talk.HypotheticalForm;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.util.data.Multimap;
import de.tuebingen.sfs.psl.util.data.Multimap.CollectionType;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EtymologyIdeaGenerator extends IdeaGenerator {

    private static final Logger systemLogger = LogManager.getLogger();
    public static final int CTRL_ARG = -3;

    private EtymologicalTheory theory;
    protected EtymologyProblemConfig config;
    protected InferenceLogger logger;

    public EtymologyIdeaGenerator(EtymologyProblem problem, EtymologicalTheory theory) {
        super(problem);
        this.theory = theory;
        this.logger = problem.getLogger();
        logger.displayln("...Creating EtymologyIdeaGenerator.");
        logger.displayln("...Working with the following idea generation configuration:");
        config = (EtymologyProblemConfig) problem.getConfig();
        if (systemLogger.isTraceEnabled()) {
            config.logSettings();
        }
        logger.displayln("Finished setting up the Etymology Idea Generator.");
    }

    public void generateAtoms() {
        IndexedObjectStore objectStore = theory.getIndexedObjectStore();
        LanguagePhylogeny phylo = theory.getLanguagePhylogeny();

        Multimap<String, Form> langsToForms = new Multimap<>(CollectionType.SET);
        Set<Integer> homPegs = new HashSet<>();
        Set<String> conceptsInConfig = new HashSet<>();
        for (int formId : config.getFormIds()) { // TODO min 2 forms!
            String lang = objectStore.getLangForForm(formId);
            langsToForms.put(lang, new Form(formId));
            conceptsInConfig.addAll(objectStore.getConceptsForForm(formId));
            int peg = objectStore.getPegForFormIdIfRegistered(formId);
            if (peg > -1) {
                // TODO if this is a modern form w/o homologue set: warn user
                homPegs.add(peg);
            }
            systemLogger.trace(
                    formId + " " + theory.normalize(formId) + " -- " + objectStore.getLangForForm(formId) +
                            " -- peg: " + peg);
        }

        // Are there any languages missing?
        List<String> langsGiven = new ArrayList<>(langsToForms.keySet());
        Set<String> langsMissing = new HashSet<>();
        String lca = phylo.lowestCommonAncestor(langsGiven);
        if (lca.equals(LanguagePhylogeny.root)) {
            Multimap<String, String> familyAncestorToLangs = new Multimap<>(CollectionType.SET);
            for (String lang : langsGiven) {
                familyAncestorToLangs.put(phylo.getPathFor(lang).get(0), lang);
            }
            // TODO warn user if there are no relevant contacts
            getLangsForBranches(phylo, langsMissing, langsGiven, familyAncestorToLangs);
        } else {
            // If there is a (non-root) common ancestor,
            // add languages + word forms up to the lowest common ancestor.
            addMissingLangsForBranch(phylo, lca, langsGiven, langsMissing, langsGiven);
        }

        // Retrieve proto forms in same homologue sets, if available
        List<Form> allForms = new ArrayList<>();
        langsToForms.values().forEach(allForms::addAll);
        for (int peg : homPegs) {
            for (int formId : objectStore.getFormsForPeg(peg)) {
                Form form = new Form(formId);
                if (allForms.contains(form)) {
                    continue;
                }
                String lang = objectStore.getLangForForm(formId);
                if (langsMissing.contains(lang)) {
                    allForms.add(form);
                    langsToForms.put(lang, form);
                    langsMissing.remove(lang);
                }
            }
        }
        // Dummy values for any remaining proto forms
        List<String> concepts = new ArrayList<>(conceptsInConfig);
        Collections.sort(concepts);
        for (String lang : langsMissing) {
            Form form = new Form(new HypotheticalForm(lang, concepts).toString());
            langsToForms.put(lang, form);
            allForms.add(form);
        }

        // Form atoms
        // TODO check EtymologicalTheory to see if confirmed Einh/Eloa/Eunk belief values
        // from previous inferences can be used here
        int maxDist = -1;
        for (String lang : langsToForms.keySet()) {
            for (Form form : langsToForms.get(lang)) {
                pslProblem.addTarget("Eunk", form.toString());

                if (phylo.hasIncomingInfluences(lang)) {
                    for (String contact : phylo.getIncomingInfluences(lang)) {
                        if (!langsToForms.containsKey(contact)) {
                            continue;
                        }
                        for (Form contactForm : langsToForms.get(contact)) {
                            pslProblem.addObservation("Xloa", 1.0, form.toString(), contactForm.toString());
                            pslProblem.addTarget("Eloa", form.toString(), contactForm.toString());
                            systemLogger.trace(
                                    "Observation: Xloa(" + form.prettyPrint() + ", " + contactForm.prettyPrint() +
                                            ") 1.0");
                            systemLogger.trace(
                                    "Target: Eloa(" + form.prettyPrint() + ", " + contactForm.prettyPrint() + ")");
                        }
                    }
                }

                String parent = phylo.parents.get(lang);
                if (parent == null || parent.equals(LanguageTree.root)) {
                    continue;
                }
                systemLogger.trace(
                        lang + " <- " + parent + " " + langsToForms.containsKey(parent) + " " + langsToForms.keySet());
                if (langsToForms.containsKey(parent)) {
                    for (Form parentForm : langsToForms.get(parent)) {
                        pslProblem.addObservation("Xinh", 1.0, form.toString(), parentForm.toString());
                        pslProblem.addTarget("Einh", form.toString(), parentForm.toString());
                        systemLogger.trace(
                                "Observation: Xinh(" + form.prettyPrint() + ", " + parentForm.prettyPrint() +
                                        ") 1.0");
                        systemLogger.trace(
                                "Target: Einh(" + form.prettyPrint() + ", " + parentForm.prettyPrint() + ")");
                    }
                }
            }
//            for (String lang2 : langsToForms.keySet()) {
//                int dist = phylo.distance(lang, lang2);
//                if (dist > maxDist) {
//                    maxDist = dist;
//                }
//                for (Form form1 : langsToForms.get(lang)) {
//                    for (Form form2 : langsToForms.get(lang2)) {
//                        pslProblem.addObservation("Xdst", 1.0, form1.toString(), form2.toString(), dist + "");
//                        if (PRINT_LOG) {
//                            System.err.println(
//                                    "Observation: Xdst(" + form1.prettyPrint() + ", " + form2.prettyPrint() + ", " +
//                                            dist + ")");
//                        }
//                    }
//                }
//            }
        }
//        for (int i = maxDist; i > 0; i--) {
//            for (int j = i - 1; j >= 0; j--) {
//                // "smaller than"
//                pslProblem.addObservation("Xsth", 1.0, j + "", i + "");
//                if (PRINT_LOG) {
//                    System.err.println("Observation: Xsth(" + j + ", " + i + ") 1.0");
//                }
//            }
//        }

        PhoneticSimilarityHelper phonSim = new PhoneticSimilarityHelper(objectStore.getCorrModel(), theory);
        int nForms = allForms.size();
        for (int i = 0; i < nForms - 1; i++) {
            Form formI = allForms.get(i);
            addAtomsForSingleForm(objectStore, formI, homPegs);

            // Compare phonetic forms.
            for (int j = i + 1; j < nForms; j++) {
                Form formJ = allForms.get(j);
                if (!formI.hasId() || !formJ.hasId() || theory.tokenize(formI.id) == null ||
                        theory.tokenize(formJ.id) == null) {
                    // If at least one of the forms is unknown, we have to infer the similarity scores.
                    pslProblem.addTarget("Fsim", formI + "", formJ + "");
                    pslProblem.addTarget("Fsim", formJ + "", formI + "");
                    systemLogger.trace("Target: Fsim(" + formI.prettyPrint() + ", " + formJ.prettyPrint() + ")");
                    systemLogger.trace("Target: Fsim(" + formJ.prettyPrint() + ", " + formI.prettyPrint() + ")");
                } else {
                    double fSim = phonSim.similarity(formI.id, formJ.id);
                    pslProblem.addObservation("Fsim", fSim, formI + "", formJ + "");
                    pslProblem.addObservation("Fsim", fSim, formJ + "", formI + "");
                    ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", formI + "", formJ + "");
                    ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", formJ + "", formI + "");
                    systemLogger.trace("Observation: Fsim(" + formI.prettyPrint() + ", " + formJ.prettyPrint() +
                            ") %.2f".formatted(fSim));
                    systemLogger.trace("Observation: Fsim(" + formJ.prettyPrint() + ", " + formI.prettyPrint() +
                            ") %.2f".formatted(fSim));
                }
            }
        }
        addAtomsForSingleForm(objectStore, allForms.get(nForms - 1), homPegs);

        if (systemLogger.isTraceEnabled()) {
            super.pslProblem.printAtomsToConsole();
        }
    }

    private void getLangsForBranches(LanguagePhylogeny phylo, Set<String> langsMissing, List<String> langsGiven,
                                     Multimap<String, String> familyAncestorToLangs) {
        List<String>[] families = (ArrayList<String>[]) new ArrayList[familyAncestorToLangs.keySet().size()];
        String[] curLcas = new String[families.length];
        int i = 0;
        for (Collection<String> relatedLangs : familyAncestorToLangs.values()) {
            String branchLca = phylo.lowestCommonAncestor(new ArrayList<>(relatedLangs));
            systemLogger.trace("Branch under " + branchLca);
            systemLogger.trace("- given: " + relatedLangs);
            Set<String> newLangs = addMissingLangsForBranch(phylo, branchLca, relatedLangs, langsMissing, langsGiven);
            curLcas[i] = branchLca;
            families[i] = new ArrayList<>(relatedLangs);
            families[i].addAll(newLangs);
            i++;
        }
        // Try to connect the different branches via contact links and go up further in the tree if necessary
        String[] contactAncestors = phylo.oldestContactLanguages(families, curLcas);
        for (i = 0; i < contactAncestors.length; i++) {
            if (contactAncestors[i] == null) {
                systemLogger.trace("No relevant contact links involving the ancestors of " + curLcas[i]);
                continue;
            }
            systemLogger.trace("Adding ancestors of " + curLcas[i]);
            for (String anc : phylo.pathToRoot(curLcas[i])) {
                langsMissing.add(anc);
                systemLogger.trace("- " + anc);
                if (contactAncestors[i].equals(anc)) {
                    break;
                }
            }
        }
    }

    private Set<String> addMissingLangsForBranch(LanguagePhylogeny phylo, String lca, Collection<String> relatedLangs,
                                                 Set<String> langsMissing, List<String> langsGiven) {
        Set<String> langsMissingInBranch = new HashSet<>();
        for (String inputLang : relatedLangs) {
            if (inputLang.equals(lca)) {
                continue;
            }
            for (String ancLang : phylo.pathToRoot(inputLang)) {
                if (langsGiven.contains(ancLang)) {
                    continue;
                }
                if (!langsMissing.contains(ancLang)) {
                    systemLogger.trace("- adding " + ancLang + " (> " + inputLang + ")");
                }
                langsMissing.add(ancLang);
                langsMissingInBranch.add(ancLang);
                if (ancLang.equals(lca)) {
                    break;
                }
            }
        }
        return langsMissingInBranch;
    }

    private void addAtomsForSingleForm(IndexedObjectStore objectStore, Form form, Set<Integer> homPegs) {
        pslProblem.addObservation("Fsim", 1.0, form.toString(), form.toString());
        ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", form.toString(), form.toString());
        systemLogger.trace("Fsim: Xloa(" + form.prettyPrint() + ", " + form.prettyPrint() + ") 1.0");

        addHomsetInfo(objectStore, form, homPegs);

        // Make sure the EinhOrEloaOrEunk rule always gets grounded:
        pslProblem.addObservation("Eloa", 0.0, form.toString(), CTRL_ARG + "");
        ((EtymologyProblem) pslProblem).addFixedAtom("Eloa", form.toString(), CTRL_ARG + "");
        ((EtymologyProblem) pslProblem).addHiddenAtom("Eloa", form.toString(), CTRL_ARG + "");
        systemLogger.trace("Observation: Eloa(" + form.prettyPrint() + ", CTRL_ARG) 0.0");
    }

    private void addHomsetInfo(IndexedObjectStore objectStore, Form form, Set<Integer> homPegs) {
        int pegForForm = -1;
        if (form.hasId()) {
            pegForForm = objectStore.getPegForFormIdIfRegistered(form.id);
        }

        if (pegForForm == -1) {
            // Unknown homologue set -> infer set membership.
            for (int homPeg : homPegs) {
                pslProblem.addTarget("Fhom", form.toString(), homPeg + "");
                systemLogger.trace("Target: Fhom(" + form.prettyPrint() + ", " + theory.normalize(homPeg) + ")");
            }
            return;
        }

        // TODO make sure homPegs won't be empty
        // (display warning if no input forms belong to any homsets)
        for (int homPeg : homPegs) {
            if (homPeg == pegForForm) {
                pslProblem.addObservation("Fhom", 1.0, form.toString(), homPeg + "");
            } else {
                pslProblem.addObservation("Fhom", 0.0, form.toString(), homPeg + "");
            }
            ((EtymologyProblem) pslProblem).addFixedAtom("Fhom", form.toString(), homPeg + "");
            systemLogger.trace(
                        "Observation: Fhom(" + form.prettyPrint() + ", " + theory.normalize(homPeg) + ") 1.0");
        }
    }

    class Form {
        Integer id = null;
        String str = null;

        Form(int id) {
            this.id = id;
        }

        Form(String str) {
            this.str = str;
        }

        boolean hasId() {
            return id != null;
        }

        public String prettyPrint() {
            return str == null ? theory.normalize(id) : str;
        }

        @Override
        public String toString() {
            return id == null ? str : id + "";
        }
    }

}
