// MCCONS using a genetic algorithm solver

#include "../include/MCCONS.h"
#include "../include/SolverGA.h"
#include "../include/OptionParser.h"


using optparse::OptionParser;


int main(int argc, char *argv[])
{

    // create the command line parser
    OptionParser parser = OptionParser().description("MC-Cons Consensus Optimizer Using a Genetic Algorithm");
    parser.add_option("-f", "--data").dest("dataFile").help("path to MARNA-like input file");
    parser.add_option("-p", "--popsize").dest("popSize").help("genetic algorithm population size").type("size_t");
    parser.add_option("-n", "--numgen").dest("numGenerations").type("size_t").help("genetic algorithm number of generations");
    parser.add_option("-s", "--silent").action("store_true").dest("silent").help("don't display status to stderr");
    optparse::Values options = parser.parse_args(argc, argv);


    // GLOBAL SETTINGS (MODIFY AT YOUR OWN RISKS (WHICH ARE MINIMAL))
    unsigned long SEEDS[6] = {42, 42, 42, 42, 42, 42};
//     assert((SEEDS != {0, 0, 0, 0, 0, 0}) &&
//            "Invalid values for the seeds of the Pseudo-Random Number Generator");

    int IMPROVEMENT_DEPTH= 2;
    int ELITE_SIZE = 30;
    double CROSSOVER_PROBABILITY = 0.5;
    double MUTATION_PROBABILITY = 0.05;
    double IMPROVEMENT_PROBABILITY = 0.05;
    size_t POPULATION_SIZE = 250;
    size_t NUM_GENERATIONS = 250;
    bool silent = false;


    if (options.is_set("dataFile"))
    {
      // extract command line parameters
      if (options["silent"] == "1")
      {
          silent = true;
      }

      if (options.is_set("popSize"))
      {
          POPULATION_SIZE = atoi(options["popSize"].c_str());
      }

      if (options.is_set("numGenerations"))
      {
          NUM_GENERATIONS = atoi(options["numGenerations"].c_str());
      }


      std::string path = options["dataFile"];

      // instantiate the genetic algorithm solver
      Solver* solver = new SolverGA(silent,
                                    POPULATION_SIZE,
                                    NUM_GENERATIONS,
                                    IMPROVEMENT_DEPTH,
                                    ELITE_SIZE,

                                    CROSSOVER_PROBABILITY,
                                    MUTATION_PROBABILITY,
                                    IMPROVEMENT_PROBABILITY);

      // execute and cleanup
      MCCONS(path, solver, SEEDS);
      delete solver;
    } else
    {
      // something went wrong with the arguemnts, print error message and exit
      std::cerr << "Error: something went wrong, please check usage -h or --help" << std::endl;
      std::exit(0);
    }
}
