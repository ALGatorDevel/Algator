package si.fri.algator.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author tomaz
 */
public class ExecutorTools {

/*
  // method iterates over the data in list and expands every loop data with 
  // several lines as defined by loop. For example, line 
  //    $for{i, 10, 20, 5}:Type0:test${i}:${1000*i}:RND
  // is replaced by lines
  //   Type0:test10:10000:RND
  //   Type0:test15:15000:RND
  //   Type0:test20:20000:RND
  public static List<String> expandLoops(List<String> data) {
    return data.stream().flatMap(item -> {
      // Check if the string starts with "$for{"
      Pattern pattern = Pattern.compile("\\$for\\{([a-zA-Z]+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\}:(.*)");
      Matcher matcher = pattern.matcher(item);

      if (matcher.matches()) {
        // Extract start, stop, step, and value
        String var   = matcher.group(1);  
        int start    = Integer.parseInt(matcher.group(2));
        int stop     = Integer.parseInt(matcher.group(3));
        int step     = Integer.parseInt(matcher.group(4));
        String value = matcher.group(5);

        // Generate the loop and replace ${smth} with evaluated result
        List<String> resultLines = new ArrayList<>();
        for (int i = start; i <= stop; i += step) {
          // Process each expression inside ${} and replace it with evaluated result
          String replacedValue = value;
          
          // Find all occurrences of ${smth} and replace them
          Pattern evalPattern = Pattern.compile("\\$\\{(.*?)\\}");
          Matcher evalMatcher = evalPattern.matcher(replacedValue);

          StringBuffer sb = new StringBuffer();
          while (evalMatcher.find()) {
              String expression = evalMatcher.group(1);
              
              // Evaluate the expression
              String evalResult = expression;
              try {
                Expression expr = new ExpressionBuilder(expression)
                  .variable(var)
                  .build()
                  .setVariable(var, i);
                evalResult = String.valueOf((int) expr.evaluate());
              } catch (Exception e) {}

              evalMatcher.appendReplacement(sb, evalResult); // Replace it with the evaluated result
          }
          evalMatcher.appendTail(sb);  // Append the remaining part of the string

          resultLines.add(sb.toString());        
        }
        return resultLines.stream(); // Return the generated lines as a stream
      } else {
        // If the string doesn't match the pattern, return the original item as-is
        return Stream.of(item);
      }
    })
    .collect(Collectors.toList());
  }
*/
 
 /* 
 private static final Pattern FOR = Pattern.compile("\\$for\\{([a-zA-Z]+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\}:(.*)");
    private static final Pattern EXPR = Pattern.compile("\\$\\{(.*?)\\}");

    public static List<String> expandLoops(List<String> lines) {
        return lines.stream().flatMap(line -> expand(line, new HashMap<>())).collect(Collectors.toList());
    }

    // Rekurzivna funkcija, ki vzdržuje vse spremenljivke v "variables"
    private static Stream<String> expand(String line, Map<String, Double> variables) {
        Matcher m = FOR.matcher(line);
        if (m.matches()) {
            String var = m.group(1);
            int start = Integer.parseInt(m.group(2));
            int stop  = Integer.parseInt(m.group(3));
            int step  = Integer.parseInt(m.group(4));
            String rest = m.group(5);

            return IntStream.iterate(start, i -> i <= stop, i -> i + step).boxed()
                    .flatMap(i -> {
                        Map<String, Double> newVars = new HashMap<>(variables);
                        newVars.put(var, (double)i);
                        return expand(rest, newVars);
                    });
        } else {
            // Ni več $for, izračunamo vse ${…} izraze z vsemi spremenljivkami
            Matcher em = EXPR.matcher(line);
            StringBuffer sb = new StringBuffer();
            while (em.find()) {
                String expr = em.group(1);
                String eval;
                try {
                    ExpressionBuilder builder = new ExpressionBuilder(expr);
                    variables.keySet().forEach(builder::variable);
                    double value = builder.build().setVariables(variables).evaluate();
                    eval = String.valueOf((int)value);
                } catch (Exception e) {
                    eval = em.group(0); // pusti original, če ni možno
                }
                em.appendReplacement(sb, Matcher.quoteReplacement(eval));
            }
            em.appendTail(sb);
            return Stream.of(sb.toString());
        }
    }

    public static void mmain(String[] args) {
        List<String> data = List.of(
            "$for{i, 10, 20, 5}:$for{j, 1, 3, 1}:Type0:test${i}:${1000*i+j}:RND"
        );
        expandLoops(data).forEach(System.out::println);
    }
  
  */
  
  /*
      private static final Pattern FOR =
        Pattern.compile("\\$for\\{([a-zA-Z]+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\}:(.*)");

    // ${...} ali $int{...} ali $double{...}
    private static final Pattern EXPR =
        Pattern.compile("\\$(?:(int|double))?\\{(.*?)\\}");

    public static List<String> expandLoops(List<String> lines) {
        return lines.stream()
                .flatMap(line -> expand(line, new HashMap<>()))
                .collect(Collectors.toList());
    }

    private static Stream<String> expand(String line, Map<String, Double> variables) {
        Matcher m = FOR.matcher(line);

        if (m.matches()) {
            String var = m.group(1);
            int start = Integer.parseInt(m.group(2));
            int stop  = Integer.parseInt(m.group(3));
            int step  = Integer.parseInt(m.group(4));
            String rest = m.group(5);

            return IntStream.iterate(start, i -> i <= stop, i -> i + step)
                    .boxed()
                    .flatMap(i -> {
                        Map<String, Double> newVars = new HashMap<>(variables);
                        newVars.put(var, (double) i);
                        return expand(rest, newVars);
                    });
        }

        // Ni več $for → obdelaj izraze
        Matcher em = EXPR.matcher(line);
        StringBuffer sb = new StringBuffer();

        while (em.find()) {
            String type = em.group(1);  // int ali double ali null
            String expr = em.group(2);

            String eval;
            try {
                ExpressionBuilder builder = new ExpressionBuilder(expr);
                variables.keySet().forEach(builder::variable);

                double value = builder.build()
                        .setVariables(variables)
                        .evaluate();

                if ("double".equals(type)) {
                    eval = String.valueOf(value);
                } else { // default = int
                    eval = String.valueOf((int) value);
                }

            } catch (Exception e) {
                eval = em.group(0); // če eval ne uspe, pusti original
            }

            em.appendReplacement(sb, Matcher.quoteReplacement(eval));
        }

        em.appendTail(sb);
        return Stream.of(sb.toString());
    }

    public static void pmain(String[] args) {

        List<String> data = List.of(
            "$for{i, 1, 10, 1}:$for{j, 1, 10, 1}:int=${1000*i+j}, double=$double{1000*i+j/10.0}"
        );

        expandLoops(data).forEach(System.out::println);
    }
  */
  
   private static final Pattern FOR =
        Pattern.compile("\\$for\\{([a-zA-Z]+),\\s*([-+]?[0-9]*\\.?[0-9]+),\\s*([-+]?[0-9]*\\.?[0-9]+),\\s*([-+]?[0-9]*\\.?[0-9]+)\\}:(.*)");

    // ${...} ali $int{...} ali $double{...}
    private static final Pattern EXPR =
        Pattern.compile("\\$(?:(int|double))?\\{(.*?)\\}");

    public static List<String> expandLoops(List<String> lines) {
        return lines.stream()
                .flatMap(l -> expand(l, new HashMap<>()))
                .collect(Collectors.toList());
    }
    static double clean(double v) {
      return Math.round(v * 1_000_000_000d) / 1_000_000_000d;
    }
    private static Stream<String> expand(String line, Map<String, Double> vars) {
        Matcher m = FOR.matcher(line);

        if (m.matches()) {

            String var = m.group(1);
            double start = Double.parseDouble(m.group(2));
            double stop  = Double.parseDouble(m.group(3));
            double step  = Double.parseDouble(m.group(4));
            String rest  = m.group(5);

            if (step == 0)
                throw new IllegalArgumentException("Step cannot be 0");

            List<String> result = new ArrayList<>();

            for (double i = start;
                 step > 0 ? i <= stop : i >= stop;
                 i += step) {

                Map<String, Double> newVars = new HashMap<>(vars);
                newVars.put(var, i);

                expand(rest, newVars).forEach(result::add);
            }

            return result.stream();
        }

        // ni več $for → evaluiraj izraze
        Matcher em = EXPR.matcher(line);
        StringBuffer sb = new StringBuffer();

        while (em.find()) {

            String type = em.group(1);   // int, double ali null
            String expr = em.group(2);

            String eval;

            try {
                ExpressionBuilder builder = new ExpressionBuilder(expr);
                vars.keySet().forEach(builder::variable);

                double value = builder.build()
                        .setVariables(vars)
                        .evaluate();

                if ("double".equals(type))
                    eval = String.valueOf(clean(value));
                else
                    eval = String.valueOf((int) value);

            } catch (Exception e) {
                eval = em.group(0);
            }

            em.appendReplacement(sb, Matcher.quoteReplacement(eval));
        }

        em.appendTail(sb);
        return Stream.of(sb.toString());
    }

    // TEST
    public static void amain(String[] args) {

        List<String> data = List.of(
            "$for{i, 10, 0, -5}:$for{j, 1, 2, 0.5}:"
          + "i=${i}, j=$double{j}, sum=$double{i+j}"
        );

        expandLoops(data).forEach(System.out::println);
    }
  
  
  public static void main(String[] args) {
    List<String> data = new ArrayList<>(Arrays.asList(
      "$for{m,1,10,1}:$for{i, 0, 10, 1}:Type0:test${11*(m-1)+i}:$int{100*m}:$double{i/10}:RND",
      "$for{y, 15, 16,0.1}:Result is $double{y} and ${i}",
      "$for{i, 10, 20, 5}:Type0:test${i}:${1000*i}:RND"
    ));
    expandLoops(data).forEach(System.out::println);
  }   


}
