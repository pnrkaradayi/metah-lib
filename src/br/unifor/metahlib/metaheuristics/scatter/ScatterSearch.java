package br.unifor.metahlib.metaheuristics.scatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.unifor.metahlib.base.Heuristic;
import br.unifor.metahlib.base.Problem;
import br.unifor.metahlib.base.Solution;
import br.unifor.metahlib.base.Utils;
import br.unifor.metahlib.heuristics.FirstImprovement;
import br.unifor.metahlib.problems.tsp.neighborhood.KOpt;

/**
 * An implementation of scatter search metaheuristic.
 * 
 * @author Nathanael de Castro Costa
 */
public abstract class ScatterSearch extends Heuristic {

	/**
	 * The reference set size
	 */
	protected int refSetSize;

	/**
	 * Candidade set size used in diversification generator method
	 */
	private int candidateSetSize;

	/**
	 * Used to know the number of solutions chosen by the best cost
	 */
	private int numBestElements;

	/**
	 * Constructor of the class.
	 * 
	 * @param problem
	 *            the problem to be solved
	 * @param refSetSize
	 *            the reference set (or solutions) size
	 */
	public ScatterSearch(Problem problem, int refSetSize) {

		super(problem);

		this.refSetSize = refSetSize;

		this.candidateSetSize = 4 * refSetSize;

		numBestElements = refSetSize / 2;

	}

	@Override
	public Solution execute() {

		List<Solution> refSet = initialPhase();

		return scatterSearchPhase(refSet);

	}

	/**
	 * The initial phase of the scatter search. A set of solutions (the
	 * reference set) are generated to start the scatter search. The size of
	 * solutions set should be equals of refSetSize variable.
	 * 
	 * @return the reference set
	 */
	protected List<Solution> initialPhase() {

		Solution seed = seedGeneration();

		List<Solution> refSet = new ArrayList<Solution>(refSetSize);

		do {
			List<Solution> candidateSet = diversificationGenerator(seed);
			candidateSet = initialImprovement(candidateSet);
			referenceSetUpdate(refSet, candidateSet);
		} while (refSet.size() == refSetSize);

		return refSet;
	}

	/**
	 * Creates a solution which it is used to generated others trials solutions
	 * at the initial phase
	 * 
	 * @return the seed solution
	 */
	protected Solution seedGeneration() {

		return problem.getInitialSolution();
	}

	/**
	 * Produces trial solutions from seed solution
	 * 
	 * @param seed
	 *            the seed solution which is used to produce the trial solutions
	 * @return a list of trial solutions
	 */
	protected List<Solution> diversificationGenerator(Solution seed) {

		final List<Solution> solutions = new ArrayList<Solution>(
				candidateSetSize);

		for (int i = 0; i < candidateSetSize; i++) {

			Solution solution = new Solution(problem);

			int n = problem.getDimension();

			Object[] values = new Object[n];

			// h > 0 and h < n
			int h = random.nextInt(n - 1) + 1;

			for (int s = h, pos = 0; s > 0; s--) {

				int rLim = (n - s) / h;

				for (int r = 0; r <= rLim; r++, pos++) {

					values[pos] = seed.getValues()[s + (r * h)];
				}
			}

			solution.setValues(values);

			solutions.add(solution);
		}

		return solutions;
	}

	/**
	 * Makes an improvement over the candidate set of trial solutions
	 * 
	 * @param candidateSet
	 *            trial solutions to be improved
	 * @return candidate set improved
	 */
	protected List<Solution> initialImprovement(List<Solution> candidateSet) {

		final List<Solution> solutions = new ArrayList<Solution>(candidateSet
				.size());

		for (int i = 0; i < candidateSet.size(); i++) {

			Solution solution = candidateSet.get(i);

			FirstImprovement fi = new FirstImprovement(problem, new KOpt(
					problem, 2), solution, 100);

			solution = fi.execute();

			solutions.add(solution);

		}

		return solutions;
	}

	/**
	 * Updates the reference set
	 * 
	 * @param refSet
	 *            reference set to be updated
	 * @param solution
	 *            solution to be added into reference set
	 */
	protected void referenceSetUpdate(final List<Solution> refSet,
			final List<Solution> candidateSet) {

		double[] costs = new double[candidateSet.size()];

		for (int i = 0; i < candidateSet.size(); i++) {
			costs[i] = problem.getCostEvaluator().eval(candidateSet.get(i));
		}

		int[] origPos = Utils.sort(costs);

		// best solutions (intensification)
		int r1 = numBestElements;

		// number of solutions for diversification
		int r2 = refSetSize - r1;

		int[] intensificationSet = Arrays.copyOf(origPos, r1);

		// candidate diversification set
		int[] candDiverSet = new int[r2];
		System.arraycopy(origPos, r1, candDiverSet, 0, r2);

		int[] diversificationSet = getDiversificationSet(candidateSet,
				intensificationSet, candDiverSet);

		for (int i = 0; i < intensificationSet.length
				&& refSet.size() < refSetSize; i++) {
			refSet.add(candidateSet.get(intensificationSet[i]));
		}

		for (int i = 0; i < diversificationSet.length
				&& refSet.size() < refSetSize; i++) {
			refSet.add(candidateSet.get(diversificationSet[i]));
		}

		removeEqualsSolutions(refSet);

	}

	/**
	 * returns the indices of solutions for diversification set
	 */
	private int[] getDiversificationSet(List<Solution> candidateSet,
			int[] intensSet, int[] candDiverSet) {

		double[][] diffMatrix = new double[candDiverSet.length][intensSet.length];

		for (int i = 0; i < candDiverSet.length; i++) {
			for (int j = 0; j < intensSet.length; j++) {

				diffMatrix[i][j] = -getSolutionsDifference(candidateSet
						.get(candDiverSet[i]), candidateSet.get(intensSet[j]));
			}
		}

		double[] meanDiff = new double[candDiverSet.length];

		for (int i = 0; i < meanDiff.length; i++) {
			meanDiff[i] = Utils.mean(diffMatrix[i]);
		}

		return Utils.sort(meanDiff);
	}

	/**
	 * Returns the difference between two solutions.
	 * 
	 * @param solution
	 * @param solution2
	 * @return the difference between solutions passed by arguments
	 */
	protected abstract double getSolutionsDifference(Solution solution,
			Solution solution2);

	private void removeEqualsSolutions(List<Solution> refSet) {

		Set<Integer> hashCodes = new HashSet<Integer>(refSet.size());

		for (int i = 0; i < refSet.size(); i++) {

			int hashCode = Arrays.hashCode(refSet.get(i).getValues());

			if (hashCodes.contains(hashCode)) {
				refSet.remove(i);
				i--;
			}

			hashCodes.add(hashCode);
		}
	}

	/**
	 * Starts the scatter search phase
	 * 
	 * @param refSet
	 *            the initial reference set
	 * @return the best solution found
	 */
	protected Solution scatterSearchPhase(List<Solution> refSet) {

		List<Solution> solutions = null;

		for (int i = 0; i < max_it; i++) {
			Subsets subsets = subsetGeneration(refSet);
			solutions = solutionCombination(subsets, refSet);
			improvement(solutions);
			referenceSetUpdate(refSet, solutions);
		}

		double bestCost = Double.POSITIVE_INFINITY;
		int bestSolutionIndex = -1;

		for (int i = 0; i < solutions.size(); i++) {

			double cost = solutions.get(i).getCost();
			int solutionIndex = i;

			if (cost < bestCost) {
				bestCost = cost;
				bestSolutionIndex = solutionIndex;
			}
		}

		return solutions.get(bestSolutionIndex);
	}

	/**
	 * Generates subsets of solutions
	 * 
	 * @param refSet
	 *            the reference set which is used for generates de subsets
	 * @return the subsets generated
	 */
	protected Subsets subsetGeneration(List<Solution> refSet) {

		double[] costs = new double[refSet.size()];

		for (int i = 0; i < costs.length; i++) {
			costs[i] = refSet.get(i).getCost();
		}

		Subsets subsets = new Subsets();

		addTwoElementsSubset(subsets, refSet);

		addThreeElementsSubset(subsets, costs.clone());

		addFourElementsSubset(subsets, costs.clone());

		addBestElementsSubset(subsets, costs.clone());

		return subsets;

	}

	private void addTwoElementsSubset(Subsets subsets, List<Solution> refSet) {

		for (int i = 0; i < refSet.size() - 1; i++) {

			for (int j = i + 1; j < refSet.size(); j++) {

				List<Integer> list = new ArrayList<Integer>(2);
				list.add(i);
				list.add(j);

				subsets.addSubset(list);
			}
		}
	}

	private void addThreeElementsSubset(Subsets subsets, double[] costs) {

		List<List<Integer>> twoElementsSubset = subsets.getSubset(2);

		int[] sortedCostsIndices = Utils.sort(costs);

		for (List<Integer> list : twoElementsSubset) {

			int best = -1;

			for (int i = 0; i < costs.length; i++) {

				if (!list.contains(sortedCostsIndices[i])) {
					best = sortedCostsIndices[i];
					break;
				}
			}

			list.add(best);
			subsets.addSubset(list);
		}
	}

	private void addFourElementsSubset(Subsets subsets, double[] costs) {

		List<List<Integer>> twoElementsSubset = subsets.getSubset(3);

		int[] sortedCostsIndices = Utils.sort(costs);

		for (List<Integer> list : twoElementsSubset) {

			int best = -1;

			for (int i = 0; i < costs.length; i++) {

				if (!list.contains(sortedCostsIndices[i])) {
					best = sortedCostsIndices[i];
					break;
				}
			}

			list.add(best);
			subsets.addSubset(list);
		}
	}

	private void addBestElementsSubset(Subsets subsets, double[] costs) {

		List<Integer> bestElements = new ArrayList<Integer>(numBestElements);

		int[] sorted = Utils.sort(costs);

		for (int i = 0; i < numBestElements; i++) {
			bestElements.add(sorted[i]);
		}

		subsets.addSubset(bestElements);
	}

	/**
	 * Combines solutions in the subsets to generate others solutions
	 * 
	 * @param subsets
	 *            the solutions (represented by theirs indices)
	 * @param refSet
	 *            the reference set with the original solutions
	 * @return a set of new solutions
	 */
	protected List<Solution> solutionCombination(Subsets subsets,
			List<Solution> refSet) {

		List<Solution> result = new ArrayList<Solution>(4);

		for (int i = 0; i < 4; i++) {

			List<Integer> solutionsIndices = subsets
					.split(subsets.getSubset(i));

			double weightsSum = 0;
			for (Integer solutionIndex : solutionsIndices) {

				weightsSum += 1.0 / refSet.get(solutionIndex).getCost();
			}

			List<Double> weights = new ArrayList<Double>(solutionsIndices
					.size());

			for (Integer solutionIndex : solutionsIndices) {

				double weight = refSet.get(solutionIndex).getCost();

				weights.add((1 / weight) / weightsSum);
			}

			List<Solution> solutions = new ArrayList<Solution>(solutionsIndices
					.size());
			for (Integer solutionIndex : solutionsIndices) {
				solutions.add(refSet.get(solutionIndex));
			}

			List<Variable> variables = getVariables(solutions);
			List<Double> scores = new ArrayList<Double>(variables.size());

			for (Variable var : variables) {

				double score = 0.0;

				for (int s = 0; s < solutionsIndices.size(); s++) {

					Solution solution = refSet.get(solutionsIndices.get(s));

					if (solutionContainsVariable(solution, var)) {

						score += weights.get(s);
					}
				}

				score = Math.floor(score + 0.5);

				scores.add(score);
			}

			Solution solution = mountSolution(refSet, variables, scores);

			result.add(solution);
		}
		return result;
	}

	/**
	 * Gets the variables of all solutions
	 * 
	 * @param solutions
	 *            solutions which theirs variables will be extracted
	 * @return a set of variables
	 */
	protected abstract List<Variable> getVariables(List<Solution> solutions);

	/**
	 * Verifies if a solution contains a variable
	 * 
	 * @param solution
	 *            the solution to be verified
	 * @param var
	 *            the variable to be found
	 * @return <code>true</code> if the variable was found in the solution
	 */
	protected abstract boolean solutionContainsVariable(Solution solution,
			Variable var);

	/**
	 * Assembles a solution from the variables and theirs scores
	 * 
	 * @param refSet
	 *            reference set
	 * @param variables
	 *            variables found in solutions of reference set
	 * @param scores
	 *            scores os variables
	 * @return	the assembled solution 
	 */
	protected abstract Solution mountSolution(List<Solution> refSet,
			List<Variable> variables, List<Double> scores);

	protected abstract void improvement(List<Solution> solutions);

	protected class Subsets {

		Map<Integer, List<Integer>> subsets;

		Map<Integer, List<Integer>> subsetsIndex;

		public Subsets() {

			this.subsets = new HashMap<Integer, List<Integer>>();
		}

		public List<Integer> split(List<List<Integer>> subset) {

			Set<Integer> set = new HashSet<Integer>();

			for (List<Integer> integer : subset) {
				for (Integer integer2 : integer) {
					set.add(integer2);
				}
			}

			return new ArrayList<Integer>(set);
		}

		public void addSubset(List<Integer> indices) {

			int[] indices_ = new int[indices.size()];

			for (int i = 0; i < indices_.length; i++) {
				indices_[i] = indices.get(i);
			}

			int[] copy = Arrays.copyOf(indices_, indices_.length);

			Arrays.sort(copy);

			int hashCode = Arrays.hashCode(copy);

			subsets.put(hashCode, indices);

			List<Integer> list = subsetsIndex.get(indices_.length);

			if (list == null) {
				list = new ArrayList<Integer>();
				subsetsIndex.put(indices_.length, list);
			}

			list.add(hashCode);
		}

		public List<List<Integer>> getSubset(int numElements) {

			List<Integer> list = subsetsIndex.get(numElements);

			List<List<Integer>> subset = new ArrayList<List<Integer>>(list
					.size());

			for (Integer integer : list) {

				List<Integer> orig = subsets.get(integer);

				List<Integer> copy = new ArrayList<Integer>(orig);

				subset.add(copy);
			}

			return subset;
		}
	}

	protected interface Variable {

	}
}
