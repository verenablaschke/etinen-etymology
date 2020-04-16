package de.tuebingen.sfs.eie.components.etymology.eval;

import de.tuebingen.sfs.eie.components.etymology.ideas.EtymologyIdeaGenerator;
import de.tuebingen.sfs.eie.components.etymology.problems.EtymologyProblem;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class EtymologyInferenceTest {

	public static void main(String[] args) {
		// For debugging with a GUI, use the EtymologyFactViewer in the etinen repository.
		ProblemManager problemManager = ProblemManager.defaultProblemManager();
		EtymologyProblem problem = new EtymologyProblem(problemManager.getDbManager(), "EtymologyProblem");
		EtymologyIdeaGenerator ideaGen = EtymologyIdeaGenerator.getIdeaGeneratorForTesting(problem, false, false);
		ideaGen.generateDataAtoms();
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		RuleAtomGraph rag = result.getRag();
		rag.printToStream(System.out);
		result.printInferenceValues();
	}
}
