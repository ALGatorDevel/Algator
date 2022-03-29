package si.fri.algotest.analysis.view;

import java.awt.event.ActionListener;
import si.fri.algotest.analysis.TableData;
import si.fri.algotest.entities.EPresenter;
import si.fri.algotest.entities.EQuery;
import si.fri.algotest.entities.Project;

/**
 *
 * @author Ernest
 */
public interface IQueryComposer {

    EQuery getQuery();

    void setOuterChangeListener(ActionListener action);

    void setProject(Project project, String computerID);

    void setQuery(EQuery query);
    
    String getComputerID();

    TableData runQuery();

}
