package mccons.solvers;

import java.util.ArrayList;
import java.util.Collections;

import mccons.util.Pair;
import mccons.util.ProgressBar;
import mccons.util.RngStream;
import mccons.util.Util;


public class SolverHeuristic extends Solver {
    // misc parameters
    private boolean verbose;
    private double tolerance;
    private long[] seeds;

    // GA settings
    private int populationSize;
    private int numGenerations;
    private int eliteSize;

    private double improvementProbability;
    private int improvementDepth;

    private double crossoverProbability;
    private double crossoverMixingRatio;

    private double mutationProbability;
    private double mutationStrength;


    public SolverHeuristic(// misc parameters
                           boolean verbose_,
                           double tolerance_,
                           long[] seeds_,

                           // GA settings
                           int populationSize_,
                           int numGenerations_,
                           int eliteSize_,
                           double improvementProbability_,
                           int improvementDepth_,
                           double crossoverProbability_,
                           double crossoverMixingRatio_,
                           double mutationProbability_,
                           double mutationStrength_) {
        assert (tolerance_ >= 0);
        assert (seeds.length == 6);

        verbose = verbose_;
        tolerance = tolerance_;
        seeds = seeds_;
        populationSize = populationSize_;
        numGenerations = numGenerations_;
        eliteSize = eliteSize_;
        improvementProbability = improvementProbability_;
        improvementDepth = improvementDepth_;
        crossoverProbability = crossoverProbability_;
        crossoverMixingRatio = crossoverMixingRatio_;
        mutationProbability = mutationProbability_;
        mutationStrength = mutationStrength_;

    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * uniform crossover operator over the genes of two solutions
     *
     * @param parent1 first parent solution
     * @param parent2 second parent solution
     * @param stream  pseudo-random generator stream
     * @return a crossover between both parents :P
     */
    public static Solution uniformCrossover(Solution parent1, Solution parent2, double mixingRatio, RngStream stream) {

        ArrayList<Integer> newGenes = parent1.getGenes();
        int size = parent1.getGenes().size();

        for (int index = 0; index != size; ++index) {
            if (stream.randU01() < mixingRatio) {
                newGenes.set(index, parent2.getGenes().get(index));
            }
        }
        return new Solution(newGenes, Double.POSITIVE_INFINITY);
    }


    /**
     * @param sol
     * @param ranges
     * @param mutation_probability
     * @param stream
     * @return
     */
    public Solution uniformMutate(Solution sol,
                                  ArrayList<Pair<Integer, Integer>> ranges,
                                  double mutation_probability,
                                  RngStream stream) {
        // mutate the solution by simply swapping with a probability
        ArrayList<Integer> mutated_genes = sol.getGenes();
        int gene_size = mutated_genes.size();
        for (int index = 0; index != gene_size; ++index) {
            if (stream.randU01() < mutation_probability) {
                // exchange for a random gene within the same range
                mutated_genes.set(index, stream.randInt(ranges.get(index).getFirst(), ranges.get(index).getSecond() - 1));
            }
        }
        return new Solution(mutated_genes, Double.POSITIVE_INFINITY);
    }


    Pair<Integer, Integer> select2(int low, int high, RngStream stream) {
        // select 2 different random integers in the interval
        assert (high - 1 > low);
        int first = stream.randInt(low, high - 1);
        int second = stream.randInt(low, high - 1);

        while (first == second) {
            second = stream.randInt(low, high - 1);
        }
        return new Pair(first, second);
    }


    /**
     * @param population
     * @param numToSelect
     * @param stream
     * @return
     */
    ArrayList<Solution> binaryTournamentSelection(ArrayList<Solution> population,
                                                  int numToSelect,
                                                  RngStream stream) {
        // classical binary tournament selection
        assert (numToSelect > 0);
        Pair<Integer, Integer> indices;
        ArrayList<Solution> selected = new ArrayList<>();
        int popSize = population.size() - 1;

        for (int index = 0; index != numToSelect; ++index) {
            indices = select2(0, popSize, stream);
            Solution first = population.get(indices.getFirst());
            Solution second = population.get(indices.getSecond());

            if (first.compareTo(second) < 0) {
                selected.add(new Solution(first));
            } else {
                selected.add(new Solution(second));
            }
        }
        return selected;
    }


    /**
     * solve the consensus problem
     * using an hybrid strategy (genetic algorithm + steepest descent)
     *
     * @param distanceMatrix
     * @param ranges
     * @return
     */
    public ArrayList<Solution> solve(double[][] distanceMatrix,
                                     ArrayList<Pair<Integer, Integer>> ranges) {
        assert (populationSize > 0);
        assert (numGenerations > 0);
        assert (eliteSize >= 0 && eliteSize < populationSize);
        assert (0. <= crossoverProbability && crossoverProbability <= 1.);
        assert (0. <= crossoverMixingRatio && crossoverMixingRatio <= 1.);
        assert (0. <= mutationProbability && mutationProbability <= 1.);

        // seed the pseudorandom generator (MRG32k3a from L'Ecuyer)
        RngStream prng = new RngStream();
        prng.setSeed(seeds);

        // some declarations for later
        ArrayList<ArrayList<Solution>> best_solutions = new ArrayList<>();
        double currentBestScore = Double.POSITIVE_INFINITY;
        double bestScoreEver = Double.POSITIVE_INFINITY;
        double scaledThreshold = tolerance * ranges.size() * (ranges.size() - 1);

        // start the progress meter
        ProgressBar bar = new ProgressBar("", 40);

        // initialize the population
        ArrayList<Solution> population = initializeRandomSolutions(ranges, populationSize, prng);

        // main loop
        for (int generation_index = 0; generation_index != numGenerations; ++generation_index) {

            // output the progress bar if not silent
            if (verbose) {
                bar.update(((float) generation_index) / numGenerations);
            }

            // score the solutions and sort the population by score
            for (Solution solution : population) {
                assignPairwiseDistanceScore(solution, distanceMatrix);
            }
            Collections.sort(population);
            assert Util.isSorted(population);

            // remember the best solutions of the current generation
            ArrayList<Solution> currentBestSolutions = new ArrayList<>();
            currentBestScore = population.get(0).getScore(); // because it is sorted

            if (currentBestScore < bestScoreEver) {
                bestScoreEver = currentBestScore;
            }

            for (Solution solution : population) {
                if (solution.getScore() <= currentBestScore + scaledThreshold) {
                    // if not elite, put it there
                    if (!currentBestSolutions.contains(solution)) {
                        currentBestSolutions.add(new Solution(solution));
                    }
                } else {
                    break;
                }
            }

            best_solutions.add(currentBestSolutions);

            // elitist selection with only unique individuals, no repetition
            ArrayList<Solution> elite = new ArrayList<>();

            for (Solution solution : population) {
                if (elite.size() >= eliteSize) {
                    break;
                } else {
                    if (!elite.contains(solution)) {
                        elite.add(new Solution(solution));
                    }
                }
            }

            // selection process
            ArrayList<Solution> parents = binaryTournamentSelection(population, ((populationSize - elite.size()) * 2), prng);
            ArrayList<Solution> children = new ArrayList<>();
            for (int i = 0; i != populationSize - eliteSize; ++i) {
                Solution parent1 = parents.get(i * 2);
                Solution parent2 = parents.get((i * 2) + 1);
                Solution child;

                // crossover
                if (prng.randU01() < crossoverProbability) {
                    child = uniformCrossover(parent1, parent2, crossoverMixingRatio, prng);
                } else {
                    child = new Solution(parent1);
                }

                // mutation
                if (prng.randU01() < mutationProbability) {
                    uniformMutate(child, ranges, mutationStrength, prng);
                }


                // improvement
                if (prng.randU01() < improvementProbability)
                    steepestDescent(child, distanceMatrix, ranges, improvementDepth);

                // children is complete
                children.add(child);
            }

            // replace the population by its children and the previous elite
            for (Solution solution : elite) {
                children.add(new Solution(solution));
            }

            // swap the two populations
            population = children;
        }

        // clean the progress bar
        if (verbose)
            bar.clean();


        System.out.println("best score seen = " + bestScoreEver);
        // keep all the unique best solutions up to a specified suboptimal threshold
        ArrayList<Solution> suitableSolutions = new ArrayList<>();
        double scoreThreshold = bestScoreEver + scaledThreshold;

        for (ArrayList<Solution> bestSolutions : best_solutions) {
            for (Solution solution : bestSolutions) {
                if ((solution.getScore() <= scoreThreshold) &&
                        (!suitableSolutions.contains(solution))) {
                    suitableSolutions.add(new Solution(solution));
                }
            }
        }
        return suitableSolutions;
    }
}
