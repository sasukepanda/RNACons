package problemSolving;

import com.sun.istack.internal.NotNull;
import util.Pair;
import util.RngStream;

import java.util.ArrayList;


/**
 * Generic solution object used for optimization in inheriting solvers.
 */
class Solution implements Comparable<Solution> {


    private final ArrayList<Integer> genes;
    private Double score;


    public void setGene(final int index, final int newValue) {
        genes.set(index, newValue);
    }

    /**
     * Constructor.
     *
     * @param genes index of the chosen genes in a table
     * @param score score of the current solution configuration (based on genes)
     */
    Solution(final ArrayList<Integer> genes, final double score) {
        this.genes = genes;
        this.score = score;
    }


    /**
     * Copy constructor
     *
     * @param other solution to copy
     */
    public Solution(Solution other) {
        this(other.getGenes(), other.getScore());
    }


    public void setScore(double score) {
        this.score = score;
    }

    public Double getScore() {

        return score;
    }

    public ArrayList<Integer> getGenes() {
        return new ArrayList<>(genes);
    }

    public String toString() {
        String stringRepresentation = "[";
        for (int i = 0; i != getGenes().size(); ++i) {
            stringRepresentation += getGenes().get(i) + " ";
        }
        stringRepresentation += " : " + getScore() + "]" + System.lineSeparator();
        return stringRepresentation;
    }


    @Override
    public int compareTo(@NotNull Solution other) {
        return getScore().compareTo(other.getScore());
    }


    @Override
    public boolean equals(Object obj) {
        // basic checks
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        // solution comparison
        final Solution other = (Solution) obj;
        return score.compareTo(other.getScore()) == 0 && genes.equals(other.genes);
    }
}


public abstract class AbstractStrategy {


    /**
     * calculates and assigns the sum of pairwise cost as the new score of the given solution
     *
     * @param solution       solution to evaluate
     * @param distanceMatrix pre-calculated get matrix
     */
    public static void assignPairwiseDistanceScore(Solution solution, double[][] distanceMatrix) {
        double score = 0.;
        ArrayList<Integer> genes = solution.getGenes();
        for (int gene1 : genes) {
            for (int gene2 : genes) {
                score += distanceMatrix[gene1][gene2];
            }
        }
        solution.setScore(score);
    }


    /**
     * Randomly select one gene per interval (range) and return a list of it
     *
     * @param ranges intervals from which to select from
     * @param stream pseudo-random number generator stream (L'Ecuyer)
     * @return list, one gene per interval
     */
    private static ArrayList<Integer> selectRandomGenes(ArrayList<Pair<Integer, Integer>> ranges,
                                                        RngStream stream) {
        ArrayList<Integer> genes = new ArrayList<>();
        for (Pair<Integer, Integer> range : ranges) {
            genes.add(stream.randInt(range.getFirst(), range.getSecond() - 1));
        }
        return genes;
    }


    /**
     * Using the get matrix, find the change of gene that brings the most improvement (greedy)
     *
     * @param genes               current genes chosen
     * @param replacementPosition index of the gene list to investigate
     * @param distanceMatrix      pre-calculated matrix of cost
     * @param ranges              list of intervals from which to select new genes from
     * @return index of the new gene (-1 if no better) and difference in score
     */
    public static Pair<Integer, Double> findBestSubstitution(ArrayList<Integer> genes,
                                                             int replacementPosition,
                                                             double[][] distanceMatrix,
                                                             ArrayList<Pair<Integer, Integer>> ranges) {
        // find the best replacement for the allele at position
        double originalCost, currentCost, bestCost;
        int originalGene, bestGene;
        int geneLength = genes.size();

        // calculate the original cost (without change)
        originalCost = 0.;
        originalGene = genes.get(replacementPosition);
        for (int i = 0; i != geneLength; ++i) {
            originalCost += distanceMatrix[genes.get(i)][originalGene];
        }

        // for every possible substitution of the gene at replacement position
        // check if improvement is possible and remember the best
        bestGene = genes.get(replacementPosition);
        bestCost = Double.POSITIVE_INFINITY;

        int begin = ranges.get(replacementPosition).getFirst();
        int end = ranges.get(replacementPosition).getSecond();
        for (int current_gene = begin; current_gene != end; ++current_gene) {
            // calculate the cost with the current replacement
            genes.set(replacementPosition, current_gene);
            currentCost = 0.;
            for (int i = 0; i != geneLength; ++i) {
                currentCost += distanceMatrix[genes.get(i)][current_gene];
            }

            // verify if best found yet, if so, remember the new gene
            if (currentCost <= bestCost) {
                bestGene = current_gene;
                bestCost = currentCost;
            }
        }
        return new Pair<>(bestGene, bestCost - originalCost);
    }


    /**
     * applies the steepest descent procedure until either
     * a local minima is found or maxNumIterations iterations of the substitution are done
     *
     * @param solution         solution to improve
     * @param distanceMatrix   pre-calculated matrix of cost
     * @param ranges           list of intervals from which to select from
     * @param maxNumIterations maximum number of iterations to perform steepest descent
     */
    public static void steepestDescent(Solution solution,
                                       double[][] distanceMatrix,
                                       ArrayList<Pair<Integer, Integer>> ranges,
                                       int maxNumIterations) {
        // forward declarations
        int best_position, best_substitution;
        double best_score;
        Pair<Integer, Double> substitution;
        int iteration = 0;

        while (iteration < maxNumIterations) {
            iteration += 1;
            best_position = -1;
            best_substitution = -1;
            best_score = 0.;
            for (int replacement_position = 0; replacement_position != solution.getGenes().size(); ++replacement_position) {
                substitution = findBestSubstitution(solution.getGenes(), replacement_position, distanceMatrix, ranges);
                if (substitution.getSecond() < best_score) {
                    best_position = replacement_position;
                    best_substitution = substitution.getFirst();
                    best_score = substitution.getSecond();
                }
            }
            // check if a local minima is reached
            if (best_substitution == -1) {
                break;
            } else {
                solution.setGene(best_position, best_substitution);
            }
        }

        // recalculate the score
        assignPairwiseDistanceScore(solution, distanceMatrix);
    }


    /**
     * create a list of Solutions generated randomly
     *
     * @param ranges list of (begin, end) indices, for each gene choices
     * @param size   size of the output list
     * @param stream pseudo random number generator stream
     * @return list of mccons2.Solution randomly generated
     */
    public static ArrayList<Solution> initializeRandomSolutions(ArrayList<Pair<Integer, Integer>> ranges, int size, RngStream stream) {
        {
            // initialize the population
            assert (size > 0);
            ArrayList<Solution> solutions = new ArrayList<>();

            // create random initial, until the population is filled
            for (int iteration = 0; iteration != size; ++iteration) {
                solutions.add(new Solution(selectRandomGenes(ranges, stream), 0.));
            }
            return solutions;
        }
    }


    /**
     * solver interface for the consensus problem
     *
     * @param problem instance of a consensus problem
     * @return list of the best(s) solution(s) found
     */
    public abstract ArrayList<Solution> solve(Problem problem);

    /**
     * gets the verbosity setting of the solver
     *
     * @return verbose (yes or no)
     */
    public abstract boolean isVerbose();
}
