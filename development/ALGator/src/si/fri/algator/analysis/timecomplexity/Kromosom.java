package si.fri.algator.analysis.timecomplexity;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
//import org.jetbrains.annotations.NotNull;

public class Kromosom implements Comparable<Kromosom> {

    boolean[] encoding;
    int length;
    int func_length;
    int op_length;
    Data measurment;
    double fitness;

    private static java.util.Random random = new java.util.Random();

    public Kromosom(int length, int func_length, int op_length, Data measurment){
        this.length = length;
        this.func_length = func_length;
        this.op_length = op_length;
        this.measurment = measurment;
        this.encoding = new boolean[length];
        for (int i=0; i<length; i++){
            this.encoding[i] = random.nextBoolean();
        }
        this.set_fitness();
    }

    public Kromosom(int length, int func_length, int op_length, Data measurment, boolean empty){
        this.length = length;
        this.func_length = func_length;
        this.op_length = op_length;
        this.measurment = measurment;
        this.encoding = new boolean[length];
    }

    public String getString(int i){
        if (i + this.func_length + this.op_length >= this.length){
            // last
            int fun = readInt(this.encoding, i ,2);
            double alpha = readDouble(this.encoding, i+2);
            if (fun < 2){
                if (fun == 0){
                    return String.format("%f", alpha);
                }
                return String.format("x^ %f", alpha);
            }
            return String.format("%f", alpha);
        }
        int fun = readInt(this.encoding, i ,2);
        double alpha = readDouble(this.encoding, i+2);
        int op = readInt(this.encoding,i+this.func_length,2);
        if (op == 3) {
            // compositum
            if (fun == 0) {
                // const
                return String.format("%f", alpha);
            } else if (fun == 1) {
                // x^n
                return String.format("(%s)^%f", this.getString(i+this.func_length+this.op_length), alpha);
            } else if (fun == 2){
                // exp
                return String.format("exp(%s)", this.getString(i+this.func_length+this.op_length), alpha);
            } else {
                // log
                return String.format("log(%s)", this.getString(i+this.func_length+this.op_length), alpha);
            }
        }
        String opStr = "+";
        if (op==1){
            opStr = "-";
        } else if (op==2){
            opStr = "*";
        }
        if (fun == 0){
            // const
            return String.format("%s %s %s", alpha, opStr, this.getString(i+this.func_length+this.op_length));
        } else if (fun == 1){
            // x^n
            return String.format("x^%s %s %s", alpha, opStr, this.getString(i+this.func_length+this.op_length));
        } else {
            return this.getString(i+this.func_length+this.op_length);
        }
    }


    public static int readInt(boolean[] arr, int i, int step){
        int res = 0;
        for (int j=i; j<(i+step); j++){
            if (arr[j]){
                res = 2*res +1;
            } else {
                res = 2*res;
            }
        }
        return res;
    }

    public static double readDouble(boolean[] arr, int i){
        // func length is 16, 2 bits are reserved
        // 14 bits left for floating point representnation
        // 1 bit sign
        // 5 bits eksponent
        // 8 bits mantisa
        int eksp = 0;
        for (int j=i+1; j<(i+6); j++){
            if (arr[j]){
                eksp= 2*eksp+1;
            } else {
                eksp = 2*eksp;
            }
        }
        double mantisa = 1;
        double q = 1.0/2;
        for (int j=i+6; j<(i+14); j++){
            if (arr[j]) {
                mantisa = mantisa + q;
            }
            q = q * 1/2;
        }
        double res = mantisa * Math.pow(2,eksp-11);
        if (arr[i]){
            return -res;
        }
        return res;
    }

    public static Kromosom crossover(int index, Kromosom k1, Kromosom k2){
        Kromosom res = new Kromosom(k1.length,k1.func_length,k1.op_length, k1.measurment, true);
        for (int i=0; i < index; i++){
            res.encoding[i] = k1.encoding[i];
        }
        for (int i=index; i < k1.length; i++){
            res.encoding[i] = k2.encoding[i];
        }
        return res;
    }

    public double fitness(){
        Expression e = new ExpressionBuilder(this.getString(0))
                .variables("x")
                .build();
        double[] predict = new double[this.measurment.Length];
        for (int i=0; i<this.measurment.Length; i++){
            predict[i] = e.setVariable("x", this.measurment.X[i]).evaluate();
       }
        return Data.rmse(this.measurment.Y, predict);
    }


    public void set_fitness(){
        this.fitness = this.fitness();
    }

    @Override
    public int compareTo(Kromosom other) {
        return Double.compare(this.fitness, other.fitness);
    }

}
