package si.fri.algator.analysis.timecomplexity;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import si.fri.algator.analysis.TableData;

import java.util.ArrayList;

public class Data {
    public double[] X;
    public double[] Y;
    String Name;
    int Length;
    String ekvivalencniRazred;
    String function;

    public Data(String name, double[] X, double[] Y){
        this.X = X;
        this.Y = Y;
        this.Name = name;
        this.Length = X.length;
    }

    public ArrayList<WeightedObservedPoint> ObservedPoints(int q, int k){
        ArrayList<WeightedObservedPoint> result = new ArrayList();
        int n = this.Length;
        if (k < 0){
            k = n;
        }
        if (q < 0 && k >= 0){
            for (int i=0; i < k; i++){
                result.add(new WeightedObservedPoint(1,this.X[i], this.Y[i]));
            }
            return result;
        }

        double sliceLength = n/10;
        double a = q*sliceLength;
        double b = (q+1)*sliceLength;
        for (int i=1; i< this.Length; i++){
            if (q >= 0 && i >=a && i < b ){
                continue;
            }
            result.add(new WeightedObservedPoint(1,this.X[i], this.Y[i]));
        }
        return result;
    }

    public double NIntegrate(){
        double sum = 0;
        int n = this.Length;
        double t = (this.Y[n-1]-this.Y[0])/this.Y[0];
        for (int i=1; i < n; i++){
            sum+= ((this.Y[i-1]+this.Y[i])/2)*(this.X[i]-this.X[i-1])*(1+((double)i/n)*t);
        }
        return sum;
    }


    public ArrayList<Double> findSubintervals(double M, int m, double treshold, int offset){
        Data dif = OutlierDetector.diference(this);
        double[] zscores = OutlierDetector.zscore(dif.Y);

        int kk = OutlierDetector.subintervalIndex(zscores, this, M);
        if (kk==-1 || kk + m >= this.Length){
            return new ArrayList<>();
        }
        Data powerFit = PowerLawFunction.getOptimalFittedData(this, -1, kk);
        double err = 0;
        for (int i = 0; i < kk; i++) {
            err += Math.abs(powerFit.Y[i] - this.Y[i]);
        }
        double avg_err = err / kk;
        err = 0;
        for (int i = kk; i < kk + m; i++) {
            err += Math.abs(powerFit.Y[i] - this.Y[i]);
        }
        Data newData = this.copyFrom(kk);



        ArrayList<Double> res = newData.findSubintervals(M, m, treshold, offset+kk);
        if (offset == 0){
            Data newDataBefore = this.copyTo(kk);
            res.addAll(newDataBefore.findSubintervals(M,m,treshold,0));
        }


        double predict_err = err / m;
        if (predict_err > treshold * avg_err) {
            res.add(this.X[kk]);
            return res;
        }

        return res;
    }

    public Data copyFrom(int k){
        int n = this.Length-k;
        double[] x = new double[n];
        double[] y = new double[n];
        int j=0;
        for (int i=k; i< this.Length; i++){
            x[j] = this.X[i];
            y[j] = this.Y[i];
            j++;
        }
        return new Data("data",x,y);
    }

    public Data copyTo(int k){ ;
        double[] x = new double[k];
        double[] y = new double[k];
        for (int i=0; i< k; i++){
            x[i] = this.X[i];
            y[i] = this.Y[i];
        }
        return new Data("data",x,y);
    }

    public Data LeastSquares(){
        String[] func = new String[]{"const","logarithm","linear","linear*log","quadratic","qub","exp"};
        double nrmseBest = 1E100;
        String best = "const";

        for (int k =0; k < func.length; k++){
            double len = this.Length;
            double sliceLength = len/10;

            double nrmseCross = 0;
            for (int q=0; q<10; q++){
                Data prediction = predict(new Data(func[k], this.X, this.Y), func[k],q,-1);
                nrmseCross+= nrmse(prediction.Y, this.Y,(int)Math.round(q*sliceLength), (int)Math.round((q+1)*sliceLength)-1);
            }
            if (nrmseCross < nrmseBest){
                best = func[k];
                nrmseBest = nrmseCross;
            }
            if (Double.isNaN(nrmseCross)){
                best = "exp";
            }
        }
        return predict(new Data(best,this.X, this.Y), best, -1,-1);
    }

    public void SetBest(String best){
        this.ekvivalencniRazred = best;
    }

    public String GetBest(){
        return this.ekvivalencniRazred;
    }

    public static double rmse(double[] series1, double[] series2){
        double err = 0;
        int n = series1.length;
        for (int i=0; i < n; i++){
            err+= Math.pow(series1[i]-series2[i], 2);
        }
        return Math.sqrt(err/n);
    }

    public static double nrmse(double[] series1, double[] series2, int i, int j){
        double err = 0;
        for (int k=i; k < j; k++){
            err+= Math.pow(series1[k]-series2[k], 2);
        }
        return Math.sqrt(err/(j-i));
    }

    public static Data predict(Data measurement, String method, int q, int k){
        int n = measurement.Length;
        double[] x = measurement.X.clone();
        double[] y = new double[n];
        ParametricUnivariateFunction func = new SimpleFunction(method);
        SimpleCurveFitter scf = SimpleCurveFitter.create(func, new double[]{1,1});
        if (Double.isInfinite(measurement.Y[measurement.Y.length-1])){
            for (int i=0; i < n; i++){
                if (method=="exp"){
                    y[i] = Double.POSITIVE_INFINITY;
                } else {
                    y[i] = 0;
                }

            }
            Data ret = new Data(method, x,y);
            ret.SetBest("exp");
            ret.function = String.format("exp(x)");
            return ret;
        }
        if (method=="exp"){
            ArrayList al = measurement.ObservedPoints(q, k);
            double d = Math.exp(measurement.ObservedPoints(q, k).get(al.size()-1).getX());
            if (Double.isInfinite(d)){
                for (int i=0; i < n; i++){
                    y[i] = 1E200;
                }
                Data ret = new Data(method, x,y);
                ret.SetBest("exp");
                ret.function = String.format("exp(x)");
                return ret;
            }
        }

        double[] params = scf.fit(measurement.ObservedPoints(q, k));
        double a = params[0];
        double b = params[1];
        String str;
        switch (method) {
            case "const":
                str = String.format("%s", a);
                break;
            case "logarithm":
                str = String.format("%s + %s*log(x)",a,b);
                break;
            case "linear":
                str = String.format("%s + %s*x",a,b);
                break;
            case "linear*log":
                str = String.format("%s + %s*x*log(x)", a, b);
                break;
            case "quadratic":
                str = String.format("%s + %s*x^2", a, b);
                break;
            case "qub":
                str = String.format("%s + %s*x^3", a, b);
                break;
            default:
                str = String.format("%s + %s*exp(x)", a, b);
        }
        for (int i=0; i < n; i++){
            y[i] = func.value(x[i], params);
        }
        Data ret = new Data(method, x,y);
        ret.SetBest(method);
        ret.function = str;
        return ret;
    }

    public static Data dataFromTableData(String name, TableData td, int xIndex, int yIndex){
        int n = td.data.size();
        int j = 0;
        while (j < n){
            if (td.data.get(j).get(yIndex).toString() == "?"){
                break;
            }
            j++;
        }
        double[] x = new double[j];
        double[] y = new double[j];
        for (int i=0; i < j; i++){
            try {
                x[i] = ((Number)td.data.get(i).get(xIndex)).doubleValue();
            } catch (Exception e){
                x[i] = Double.parseDouble((String)td.data.get(i).get(xIndex));
            }
            try {
                y[i] = ((Number)td.data.get(i).get(yIndex)).doubleValue();
            } catch (Exception e){
                y[i] = Double.parseDouble((String)td.data.get(i).get(yIndex));
            }
        }
        return new Data(name, x, y);
    }

    public String GetFunction(){
        return this.function;
    }

    public int GetIndex(double xValue){
        // array is sorted, use binary search for speed
        return java.util.Arrays.binarySearch(this.X, xValue);
    }

}
