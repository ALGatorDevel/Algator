package si.fri.algator.analysis;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import si.fri.algator.global.ATLog;

/**
 *
 * @author ernest, judita
 */
public class AlgInterpreter {

    private static final String[] stringsNotAllowed = new String[]{"{", "}"};
    public static final String[] mathMembers = new String[]{"sin", "cos", "abs", "ceil", "floor", "max", "min", "pow", "random", "round", "signum", "sqr"};

    
    public static String prepareExpression(String expression) {
        for (String str : stringsNotAllowed) {
            if (expression.contains(str)) {
                ATLog.log("Illegal character in expression " + expression, 3);
                return "";
            }
        }
        expression = expression.replace(";", ",");
//        for (String mb : mathMembers) {
//            expression = expression.replace(mb + "(", "Math." + mb + "(");
//        }
        return expression;
    }

    public static Object evalExpression(String expression) {
       Expression e = new ExpressionBuilder(expression).build();
       double result = e.evaluate();
       return result;
    }

}
