package si.fri.algator.analysis.timecomplexity;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.HashSet;

public class OutlierDetector {

    public static class OutlierDetection{
        public HashSet<Double> outlierXValues;
        public HashSet<Double> outlierIndex;
        public Data cleanData;
        public Data originalData;

        public OutlierDetection(HashSet<Double> outlierXValues, HashSet<Double> outlierIndex, Data cleanData, Data originalData){
            this.outlierIndex = outlierIndex;
            this.outlierXValues = outlierXValues;
            this.cleanData = cleanData;
            this.originalData = originalData;

        }
    }


    public static OutlierDetection detectOutliers(Data measurment, double sigma){

        HashSet<Double> outlier_index = new HashSet<>();
        HashSet<Double> outlier_x_values = new HashSet<>();
        HashMap<Double, Double> mapping = new HashMap<>();
        for (int i=0; i < measurment.Length; i++){
            mapping.put(measurment.X[i], (double)i);
        }
        Data originalData = measurment;

        while (true) {
            Data powerFitMemory = PowerLawFunction.getOptimalFittedData(measurment,-1,-1);
            Data d = diference(measurment);
            double[] z = zscore(d.Y);
            int i = outlierIndex(z, measurment, powerFitMemory, sigma);
            if (i < 0){
                break;
            }
            outlier_x_values.add(measurment.X[i]);
            outlier_index.add(mapping.get(measurment.X[i]));
            measurment = removePoint(measurment,i);
        }
        return new OutlierDetection(outlier_x_values, outlier_index, measurment, originalData);
    }

    public static Data diference(Data measurement){
        int n = measurement.Length;
        double[] y = new double[n-1];
        double[] x = new double[n-1];
        for (int i=0; i < n-1; i++){
            x[i] = i;
            double n1 = measurement.X[i];
            double n2 = measurement.X[i+1];
            double e_n1 = measurement.Y[i];
            double e_n2 = measurement.Y[i+1];
            y[i] = Math.log(e_n1/e_n2)/Math.log(n1/n2);
        }
        return new Data("alpha",x,y);
    }



    public static int outlierIndex(double[] d, Data data, Data powerFit, double sigma){
        for (int i=0; i < d.length; i++){
            if (Math.abs(d[i]) > sigma){
                if (i==0){
                    return 0;
                }
                double dif1 = Math.abs(data.Y[i] - powerFit.Y[i]);
                double dif2 = Math.abs(data.Y[i+1] - powerFit.Y[i+1]);
                if (dif1 > dif2){
                    return i;
                }
                return i+1;
            }
        }
        return -1;
    }

    public static int subintervalIndex(double[] d, Data data, double sigma){
        for (int i=0; i < d.length; i++){
            if (Math.abs(d[i]) > sigma){
                if (i==0){
                    return 0;
                }
                return i+1;
            }
        }
        return -1;
    }



    public static double[] zscore(double[] y){
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        for (double v : y){
            statistics.addValue(v);
        }
        double sd = statistics.getStandardDeviation();
        double mean = statistics.getMean();
        double[] zscore = new double[y.length];
        for (int i=0; i < y.length; i++){
            zscore[i] = (y[i]-mean)/sd;
        }
        return zscore;
    }

    public static Data removePoint(Data d, int i){
        int n = d.Length;
        double[] x = new double[n-1];
        double[] y = new double[n-1];
        for (int j=0; j < n; j++){
            if (j==i){
                continue;
            } else if (j > i){
                x[j-1] = d.X[j];
                y[j-1] = d.Y[j];
            } else {
                x[j] = d.X[j];
                y[j] = d.Y[j];
            }

        }
        return new Data("cleanData",x,y);
    }
}
