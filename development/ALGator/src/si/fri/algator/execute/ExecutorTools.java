package si.fri.algator.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author tomaz
 */
public class ExecutorTools {

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

  public static void main(String[] args) {
    List<String> data = new ArrayList<>(Arrays.asList(
      "$for{i, 1, 5, 1}:Type0:test${i}:${1000*i}:RND",
      "Some other line",
      "$for{y,10, 15, 2}:Result is ${y} and ${i}",
      "$for{i, 10, 20, 5}:Type0:test${i}:${1000*i}:RND"
    ));
    expandLoops(data).forEach(System.out::println);
  }
  
}
