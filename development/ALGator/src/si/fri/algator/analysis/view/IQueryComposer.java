package si.fri.algator.analysis.view;

import java.awt.event.ActionListener;
import si.fri.algator.analysis.TableData;
import si.fri.algator.entities.EPresenter;
import si.fri.algator.entities.EQuery;
import si.fri.algator.entities.Project;

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
