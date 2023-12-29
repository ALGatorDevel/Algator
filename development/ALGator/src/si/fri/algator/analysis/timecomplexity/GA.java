package si.fri.algator.analysis.timecomplexity;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class GA {
    Kromosom[] population;
    int kromosom_length;
    int n;
    int[] array;

    private static java.util.Random random = new java.util.Random();

    public GA(int population_size, Data measurment, int func_number){
        this.n = population_size;
        this.population = new Kromosom[population_size];
        this.kromosom_length = func_number * 16 + (func_number -1) * 2;
        for (int i=0; i<population_size; i++){
            this.population[i]= new Kromosom(kromosom_length,16,2, measurment);
        }
        this.array = new int[kromosom_length];
        for (int i=0; i< kromosom_length; i++){
            this.array[i] = i;
        }
        Arrays.sort(this.population);
    }

    public static int getIndexRank(int n){
        double r = random.nextDouble();
        double a = -1.0/2;
        double b = n - 1.0/2;
        double c = n - r * n*(n+1.0)/2.0;
        double x = (-b + Math.sqrt(b*b-4*a*c))/(2*a);
        int i = (int)Math.ceil(x);
        return i;
    }

    public int[] getRandomNumbers(int k){
        int n = this.array.length;
        int[] result = new int[k];
        for (int i=0; i < k; i++){
            int index = ThreadLocalRandom.current().nextInt(0, n-i);
            int temp =this.array[index];
            result[i]=temp;
            this.array[index] = this.array[n-i-1];
            this.array[n-i-1] = temp;
        }
        return result;
    }

    public static int[] getRandomNumbers(int k, int[] arr){
        int n = arr.length;
        int[] result = new int[k];
        for (int i=0; i < k; i++){
            int index = ThreadLocalRandom.current().nextInt(0, n-i);
            int temp =arr[index];
            result[i]=temp;
            arr[index] = arr[n-i-1];
            arr[n-i-1] = temp;
        }
        return result;
    }

    public static int getIndexTournament(Kromosom[] population, int k){
        int n = population.length;
        double maxi = 1E20;
        int index = 0;
        for (int i=0; i < k; i++){
            int j = ThreadLocalRandom.current().nextInt(0, n);
            if (population[j].fitness < maxi){
                index = j;
                maxi = population[j].fitness;
            }
        }
        return index;
    }

    public void selection(){
        int k = 10; // tournament selection parameter
        double alpha = 0.04;
        int c = (int)(this.n - alpha*this.n/2);
        Kromosom[] new_population = new Kromosom[2*this.n];
        // use elitistic approach, to preserve best individuals
        for (int i=0; i < alpha*this.n; i++){
            if (i==0){
                new_population[i]=this.population[i];
            } else {
                new_population[i] = small_mutation(this.population[i]);
            }
        }

        int indexAll =(int)(alpha*this.n);
        for (int i=0; i < c; i++){
            for (int j=0; j<1; j++){
                int ii = getIndexTournament(this.population, k);
                int jj = getIndexTournament(this.population, k);
                while (jj==ii){
                    jj = getIndexTournament(this.population, k);
                }
                Kromosom k1 = this.population[ii];
                Kromosom k2 = this.population[jj];
                int[] r = this.getRandomNumbers(2);
                int index = r[0];
                Kromosom new_k1 = Kromosom.crossover(index,k1,k2);
                Kromosom new_k2 = Kromosom.crossover(index,k2,k1);
                index = r[1];
                new_k1 = Kromosom.crossover(index,new_k1,new_k2);
                new_k2 = Kromosom.crossover(index,new_k2,new_k1);
                mutation(new_k1);
                mutation(new_k2);
                new_population[indexAll] = new_k1;
                indexAll++;
                new_population[indexAll] = new_k2;
                indexAll++;

            }
        }

        for (int i=0; i < new_population.length; i++){
            new_population[i].set_fitness();
        }

        Arrays.sort(new_population);

        for (int i=0; i < this.n; i++){
            this.population[i]=new_population[i];
        }
    }


    public void mutation(Kromosom kr){
        int n = ThreadLocalRandom.current().nextInt(1, 15);
        int[] r = this.getRandomNumbers(n);

        for (int i : r){
            kr.encoding[i] = !(kr.encoding[i]);
        }

    }

    public Kromosom small_mutation(Kromosom kr){
        Kromosom res = new Kromosom(kr.length, kr.func_length,kr.op_length,kr.measurment,true);
        for (int i=0; i < kr.encoding.length; i++){
            res.encoding[i] = kr.encoding[i];
        }

        int n = ThreadLocalRandom.current().nextInt(1, 5);
        int[] r = this.getRandomNumbers(n);

        for (int i : r){
            res.encoding[i] = !(res.encoding[i]);
        }
        return res;
    }

    /**
     * finds symbolic equation that describes data, sets data.function value as result
     * runs iter*onePopulationIter iterations
     * @param data
     * @param number_of_all_populations in multipopulation genetic algorithm
     * @param population_size of each population
     * @param iter
     * @param onePopulationIter number of iterations before switching best organisms
     * @param func_number number of func parts in encoding
     */
    public static void GAPredictor(Data data, int number_of_all_populations, int population_size, int iter, int onePopulationIter,
                            int func_number){

        int[] populations_arr = new int[number_of_all_populations];
        for (int i=0; i < number_of_all_populations; i++){
            populations_arr[i] = i;
        }
        GA[] all_populations = new GA[number_of_all_populations];

        for (int i=0; i < number_of_all_populations; i++){
            all_populations[i] = new GA(population_size, data, func_number);
        }
        for (int i=0; i< iter; i++){
            for (GA pop : all_populations){
                for (int j=0; j<onePopulationIter; j++){
                    pop.selection();
                }
            }
            int[] permutation_array = GA.getRandomNumbers(number_of_all_populations,populations_arr);
            for (int j=0; j < number_of_all_populations; j++){
                GA from = all_populations[permutation_array[j]];
                int indeks_to;
                if (j+1 == number_of_all_populations){
                    indeks_to = permutation_array[0];
                } else {
                    indeks_to = permutation_array[j+1];
                }
                GA to = all_populations[indeks_to];
                for (int k=0; k < 2; k++){
                    to.population[population_size-1-k] = from.population[k];
                }
            }
        }
        String result;
        double bestFitnes = 1E200;
        Kromosom bestKromosom = all_populations[0].population[0];
        for (int i=0; i < number_of_all_populations; i++){
            Kromosom best = all_populations[i].population[0];
            if (best.fitness() < bestFitnes){
                bestFitnes = best.fitness();
                bestKromosom = best;
            }
        }
        result = bestKromosom.getString(0);
        data.function = result;
    }

}
