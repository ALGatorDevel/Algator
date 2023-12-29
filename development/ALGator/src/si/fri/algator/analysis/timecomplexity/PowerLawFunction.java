package si.fri.algator.analysis.timecomplexity;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

import java.util.ArrayList;

public class PowerLawFunction implements ParametricUnivariateFunction {
    double[] params;
    String name;

    @Override
    public double value(double x, double... params) {
        double a = params[0];
        double b = params[1];
        double c = params[2];
        return c + a * Math.pow(x, b);
    }

    @Override
    public double[] gradient(double x, double... params) {
        double a = params[0];
        double b = params[1];
        return new double[]{Math.pow(x, b), a * Math.pow(x, b) * Math.log(x), 1};
    }


    public static Data getOptimalFittedData(Data measurement, int q, int k){
        double[] y = new double[measurement.Length];
        ParametricUnivariateFunction func = new PowerLawFunction();
        SimpleCurveFitter scf = SimpleCurveFitter.create(func, new double[]{5,1,100});
        scf = scf.withMaxIterations(12000);
        ArrayList<WeightedObservedPoint> t = measurement.ObservedPoints(q,k);
        double[] params;
        try {
            params = scf.fit(t);
        } catch (Exception e){
            // if for some reason fit failed to converge or returned error,
            // then fit data with constant, average value
            double avg = 0;
            for (int i=0; i < measurement.Length; i++){
                avg+= measurement.Y[i];
            }
            avg = avg / measurement.Length;
            for (int i=0; i < measurement.Length; i++){
                y[i] = avg;
            }
            Data res = new Data("potenčna", measurement.X, y);
            return res;

        }

        for (int i=0; i < measurement.Length; i++){
            y[i] = func.value(measurement.X[i], params);
        }
        Data res = new Data("potenčna", measurement.X, y);
        res.function = String.format("%s + %s*x^%s", params[2], params[0], params[1]);
        return res;

    }
}
