package si.fri.algator.analysis.timecomplexity;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

import java.util.HashSet;
import java.util.Random;


public class SimpleFunction implements ParametricUnivariateFunction {
    double[] params;
    String name;


    public SimpleFunction(String name) {
        this.name = name;
    }

    @Override
    public double value(double x, double... params) {
        double a = params[0];
        double b = params[1];
        switch (this.name) {
            case "const":
                return a;
            case "logarithm":
                return a + b * Math.log(x);
            case "linear":
                return a + b * x;
            case "linear*log":
                return a + b * x * Math.log(x);
            case "quadratic":
                return a + b * x * x;
            case "qub":
                return a + b * x * x * x;
            case "exp":
                return a +b*Math.exp(x);
        }
        return 1;
    }

    @Override
    public double[] gradient(double x, double... params) {
        double[] grad;
        switch (this.name) {
            case "const":
                grad = new double[]{1, 0};
                break;
            case "logarithm":
                grad = new double[]{1, Math.log(x)};
                break;
            case "linear":
                grad = new double[]{1, x};
                break;
            case "linear*log":
                grad = new double[]{1, x*Math.log(x)};
                break;
            case "quadratic":
                grad = new double[]{1, x*x};
                break;
            case "exp":
                grad = new double[]{1, Math.exp(x)};
                break;
            default:
                grad = new double[]{1, x*x*x};

        }
        return grad;
    }
}



