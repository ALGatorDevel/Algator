package si.fri.algator.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import si.fri.algator.entities.EResult;
import si.fri.algator.entities.Variables;

/**
 *
 * @author ernest, judita
 */
public class ReadResultsInitData {

    public ArrayList<String> resultOrder = new ArrayList<>();

    public ArrayList<String> testOrder = new ArrayList<>();

    public Variables resultPS;

    public EResult eResultDesc;

    public ReadResultsInitData(String[] rOrder, String[] tOrder, Variables resultVS, EResult resultDesc) {
        resultOrder.addAll(Arrays.asList(rOrder));
        testOrder.addAll(Arrays.asList(tOrder));
        resultPS = resultVS;
        eResultDesc = resultDesc;
    }

}
