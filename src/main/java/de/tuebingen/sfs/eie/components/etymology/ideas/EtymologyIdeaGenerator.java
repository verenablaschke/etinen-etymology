package de.tuebingen.sfs.eie.components.etymology.ideas;

import java.util.*;

import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblemConfig;
import de.tuebingen.sfs.eie.shared.core.EtymologicalTheory;
import de.tuebingen.sfs.eie.shared.core.IndexedObjectStore;
import de.tuebingen.sfs.eie.shared.core.LanguagePhylogeny;
import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.eie.shared.util.Pair;
import de.tuebingen.sfs.eie.shared.util.PhoneticSimilarityHelper;
import de.tuebingen.sfs.psl.engine.IdeaGenerator;
import de.tuebingen.sfs.psl.util.data.Multimap;
import de.tuebingen.sfs.psl.util.data.Multimap.CollectionType;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

public class EtymologyIdeaGenerator extends IdeaGenerator {

    public static boolean PRINT_LOG = true;

    public static final int CTRL_ARG = -3;
    public static final String TMP_SFX = "_form";

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
        if (PRINT_LOG) {
            config.logSettings();
        }
        logger.displayln("Finished setting up the Etymology Idea Generator.");
    }

    public void generateAtoms() {
        IndexedObjectStore objectStore = theory.getIndexedObjectStore();
        LanguagePhylogeny phylo = theory.getLanguagePhylogeny();

        Multimap<String, Form> langsToForms = new Multimap<>(CollectionType.SET);
        Set<Integer> homPegs = new HashSet<>();
        for (int formId : config.getFormIds()) { // TODO min 2 forms!
            String lang = objectStore.getLangForForm(formId);
            langsToForms.put(lang, new Form(formId));
            int peg = objectStore.getPegForFormIdIfRegistered(formId);
            if (peg > -1) {
                // TODO if this is a modern form w/o homologue set: warn user
                homPegs.add(peg);
            }
            if (PRINT_LOG) {
                System.err.println(
                        formId + " " + theory.normalize(formId) + " -- " + objectStore.getLangForForm(formId) +
                                " -- peg: " + peg);
            }
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
            // TODO If the selection contains languages from multiple different families,
            // add languages + word forms from up to the earliest established contact.
            // TODO warn user if there are no relevant contacts
            for (Collection<String> relatedLangs : familyAncestorToLangs.values()) {
                addMissingLangsForBranch(phylo, phylo.lowestCommonAncestor(new ArrayList<>(relatedLangs)), relatedLangs,
                        langsMissing, langsGiven);
            }
            // TODO try to connect the different branches via contact links and go up further in the tree if necessary
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
        for (String lang : langsMissing) {
            // TODO discuss this (int vs str)
//            Form form = new Form(lang + TMP_SFX);
            int formId = objectStore.createFormId();
            objectStore.addFormIdWithLanguage(formId, lang);
            Form form = new Form(formId);
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
                        if (!langsToForms.containsKey(contact)){
                            continue;
                        }
                        for (Form contactForm : langsToForms.get(contact)) {
                            pslProblem.addObservation("Xloa", 1.0, form.toString(), contactForm.toString());
                            pslProblem.addTarget("Eloa", form.toString(), contactForm.toString());
                            if (PRINT_LOG) {
                                System.err.println(
                                        "Observation: Xloa(" + form.prettyPrint() + ", " + contactForm.prettyPrint() +
                                                ") 1.0");
                                System.err.println(
                                        "Target: Eloa(" + form.prettyPrint() + ", " + contactForm.prettyPrint() + ")");
                            }
                        }
                    }
                }

                String parent = phylo.parents.get(lang);
                if (PRINT_LOG) System.err.println(
                        lang + " <- " + parent + " " + langsToForms.containsKey(parent) + " " + langsToForms.keySet());
                if (langsToForms.containsKey(parent)) {
                    for (Form parentForm : langsToForms.get(parent)) {
                        pslProblem.addObservation("Xinh", 1.0, form.toString(), parentForm.toString());
                        pslProblem.addTarget("Einh", form.toString(), parentForm.toString());
                        if (PRINT_LOG) {
                            System.err.println(
                                    "Observation: Xinh(" + form.prettyPrint() + ", " + parentForm.prettyPrint() +
                                            ") 1.0");
                            System.err.println(
                                    "Target: Einh(" + form.prettyPrint() + ", " + parentForm.prettyPrint() + ")");
                        }
                    }
                }
            }
            for (String lang2 : langsToForms.keySet()) {
                int dist = phylo.distance(lang, lang2);
                if (dist > maxDist) {
                    maxDist = dist;
                }
                for (Form form1 : langsToForms.get(lang)) {
                    for (Form form2 : langsToForms.get(lang2)) {
                        pslProblem.addObservation("Xdst", 1.0, form1.toString(), form2.toString(), dist + "");
                        if (PRINT_LOG) {
                            System.err.println(
                                    "Observation: Xdst(" + form1.prettyPrint() + ", " + form2.prettyPrint() + ", " +
                                            dist + ")");
                        }
                    }
                }
            }
        }
        for (int i = maxDist; i > 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                // "smaller than"
                pslProblem.addObservation("Xsth", 1.0, j + "", i + "");
                if (PRINT_LOG) {
                    System.err.println("Observation: Xsth(" + j + ", " + i + ") 1.0");
                }
            }
        }

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
                    if (PRINT_LOG) {
                        System.err.println("Target: Fsim(" + formI.prettyPrint() + ", " + formJ.prettyPrint() + ")");
                        System.err.println("Target: Fsim(" + formJ.prettyPrint() + ", " + formI.prettyPrint() + ")");
                    }
                } else {
                    double fSim = phonSim.similarity(formI.id, formJ.id);
                    pslProblem.addObservation("Fsim", fSim, formI + "", formJ + "");
                    pslProblem.addObservation("Fsim", fSim, formJ + "", formI + "");
                    ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", formI + "", formJ + "");
                    ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", formJ + "", formI + "");
                    if (PRINT_LOG) {
                        System.err.println("Observation: Fsim(" + formI.prettyPrint() + ", " + formJ.prettyPrint() +
                                ") %.2f".formatted(fSim));
                        System.err.println("Observation: Fsim(" + formJ.prettyPrint() + ", " + formI.prettyPrint() +
                                ") %.2f".formatted(fSim));
                    }
                }
            }
        }
        addAtomsForSingleForm(objectStore, allForms.get(nForms - 1), homPegs);

        if (PRINT_LOG) {
            super.pslProblem.printAtomsToConsole();
        }
    }

    private void addAtomsForSingleForm(IndexedObjectStore objectStore, Form form, Set<Integer> homPegs) {
        pslProblem.addObservation("Fsim", 1.0, form.toString(), form.toString());
        ((EtymologyProblem) pslProblem).addFixedAtom("Fsim", form.toString(), form.toString());
        if (PRINT_LOG) {
            System.err.println("Fsim: Xloa(" + form.prettyPrint() + ", " + form.prettyPrint() + ") 1.0");
        }

        addHomsetInfo(objectStore, form, homPegs);

        // Make sure the EinhOrEloaOrEunk rule always gets grounded:
        pslProblem.addObservation("Eloa", 0.0, form.toString(), CTRL_ARG + "");
        // TODO this one should actually probably be properly excluded from the sidebar:
        ((EtymologyProblem) pslProblem).addFixedAtom("Eloa", form.toString(), CTRL_ARG + "");
        if (PRINT_LOG) {
            System.err.println("Observation: Eloa(" + form.prettyPrint() + ", CTRL_ARG) 0.0");
        }
    }

    private void addMissingLangsForBranch(LanguagePhylogeny phylo, String lca, Collection<String> relatedLangs,
                                          Set<String> langsMissing, List<String> langsGiven) {
        for (String inputLang : relatedLangs) {
            for (String ancLang : phylo.pathToRoot(inputLang)) {
                if (langsGiven.contains(ancLang)) {
                    continue;
                }
                langsMissing.add(ancLang);
                if (ancLang.equals(lca)) {
                    break;
                }
            }
        }
    }

    private void addHomsetInfo(IndexedObjectStore objectStore, Form form, Set<Integer> homPegs) {
        int pegForForm = -1;
        if (form.hasId()) {
            objectStore.getPegForFormIdIfRegistered(form.id);
        }

        if (pegForForm == -1) {
            // Unknown homologue set -> infer set membership.
            for (int homPeg : homPegs) {
                pslProblem.addTarget("Fhom", form.toString(), homPeg + "");
                ((EtymologyProblem) pslProblem).addFixedAtom("Fhom", form.toString(), homPeg + "");
                if (PRINT_LOG) {
                    System.err.println("Target: Fhom(" + form.prettyPrint() + ", " + theory.normalize(homPeg) + ")");
                }
            }
            return;
        }

        // TODO make sure homPegs won't be empty
        for (int homPeg : homPegs) {
//            if (homPeg == pegForForm) {
            pslProblem.addObservation("Fhom", 1.0, form.toString(), homPeg + "");
//            } else {
//                pslProblem.addObservation("Fhom", 0.0, form.toString(), homPeg + "");
//            }
            ((EtymologyProblem) pslProblem).addFixedAtom("Fhom", form.toString(), homPeg + "");
            if (PRINT_LOG) {
                System.err.println(
                        "Observation: Fhom(" + form.prettyPrint() + ", " + theory.normalize(homPeg) + ") 1.0");
            }
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
