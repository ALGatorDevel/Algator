package org.asql;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import si.fri.algotest.analysis.DataAnalyser;
import si.fri.algotest.analysis.TableData;
import si.fri.algotest.entities.EQuery;
import si.fri.algotest.entities.Project;
import si.fri.algotest.tools.ATTools;

/**
 *
 * @author ernest
 */
public class ASqlObject {

    private String _vsebina;
    private String description;

    public ASqlObject(String vsebina) {
        if (vsebina != null) {
            _vsebina = vsebina.trim();
            if (_vsebina.startsWith("/*")) {
                description = vsebina.substring(2, _vsebina.indexOf("*/") - 2);
                _vsebina = _vsebina.substring(_vsebina.indexOf("*/") + 2).trim();
            }
        }
    }

    public JSONObject getJSONObject(Project project) {
        JSONObject json = new JSONObject();
        json = json.put("Query", new JSONObject());
        String name = "";
        json.getJSONObject("Query").put("Description", description);
        if (_vsebina.trim().startsWith("@")) {
            name = _vsebina.substring(1, _vsebina.trim().indexOf("=") - 1);
        }
        json.getJSONObject("Query").put("Name", name);
        String[] splitFrom = _vsebina.split("FROM", 2);
        String ostanek = splitFrom[1];
        String[] splitWhere = ostanek.split("WHERE", 2);
        if (splitWhere.length == 1) {
            ostanek = splitWhere[0];
        } else {
            ostanek = splitWhere[1];
        }
        String[] splitSelect = ostanek.split("SELECT", 2);
        ostanek = splitSelect[1];
        if (splitSelect[1].contains("GROUPBY")) {
            String[] splitGroupBy = ostanek.split("GROUPBY", 2);
            ostanek = splitGroupBy[1];
            int groupByEndIndex = ostanek.indexOf("\n");
            if (groupByEndIndex < 0) {
                groupByEndIndex = ostanek.length();
            }
            String groupBy = ostanek.substring(0, groupByEndIndex);
            json.getJSONObject("Query").append("GroupBy", groupBy);
            if (groupBy.trim().length() > 0) {
                json.getJSONObject("Query").put("Count", "1");
            }
        } else {
            json.getJSONObject("Query").append("GroupBy", "");
        }
        if (splitSelect[1].contains("ORDERBY")) {
            String[] splitOrderBy = ostanek.split("ORDERBY", 2);
            int orderByEndIndex = splitOrderBy[1].indexOf("\n");
            if (orderByEndIndex < 0) {
                orderByEndIndex = splitOrderBy[1].length();
            }
            json.getJSONObject("Query").append("SortBy", splitOrderBy[1].substring(0, orderByEndIndex).trim());
        } else {
            json.getJSONObject("Query").append("SortBy", "");
        }
        String fromStr[] = splitFrom[1].substring(0, splitFrom[1].indexOf("\n")).split(",");
        for (String fromStr1 : fromStr) {
            json.getJSONObject("Query").append("TestSets", fromStr1.trim());
        }
        int endIndexSelect = splitSelect[1].indexOf("\n");
        if (endIndexSelect < 0) {
            endIndexSelect = splitSelect[1].indexOf(";");
        }
        if (endIndexSelect < 0) {
            endIndexSelect = splitSelect[1].length();
        }
        String selectClause = splitSelect[1].substring(0, endIndexSelect);
        if (selectClause.endsWith(";")) {
            selectClause = selectClause.substring(0, selectClause.length() - 1);
        }
        String selectStr[] = selectClause.split(",");
        String[] projectParameters = Project.getTestParameters(project.getResultDescriptions());
        for (String selectStr1 : selectStr) {
            boolean parameter = false;
            String paramTrimmed = selectStr1.trim();
            if (paramTrimmed.endsWith(";")) {
                paramTrimmed = paramTrimmed.substring(0, paramTrimmed.length() - 1);
            }
            for (String projectParameter : projectParameters) {
                if (projectParameter.equals(paramTrimmed)) {
                    json.getJSONObject("Query").append("Parameters", paramTrimmed);
                    parameter = true;
                    break;
                }
            }
            if (!parameter) {
                if (paramTrimmed.equals("COUNT(*)")) {
                    json.getJSONObject("Query").put("Count", "1");
                } else {
                    json.getJSONObject("Query").append("Indicators", paramTrimmed);
                }
            }
        }
        if (splitWhere.length > 1) {
            boolean existsFilter = false;
            String[] asqlFilter = splitWhere[1].substring(splitWhere[1].indexOf(")") + 1, splitWhere[1].indexOf("SELECT")).replace("\n", " ").split(" AND ");
            for (String filter : asqlFilter) {
                if (!filter.toUpperCase().contains("COMPUTERID=")) {
                    json.getJSONObject("Query").append("Filter", filter.trim());
                    existsFilter = true;
                } else {
                    String computerIdSplit[] = filter.split("ComputerID=");
                    if (computerIdSplit.length == 2) {
                        json.getJSONObject("Query").put("ComputerID", computerIdSplit[1].trim());
                    }
                }
            }
            if (!existsFilter) {
                json.getJSONObject("Query").append("Filter", "");
            }
            String algorithmStr[] = splitWhere[1].substring(splitWhere[1].indexOf("(") + 1, splitWhere[1].indexOf(")")).split("OR");
            for (String algorithmStr1 : algorithmStr) {
                if (algorithmStr1.contains("algorithm=")) {
                    json.getJSONObject("Query").append("Algorithms", algorithmStr1.split("algorithm=")[1].trim());
                }
            }
        }
        return json;
    }

    public String getJSONString(Project project) {
        return getJSONObject(project).get("Query").toString();
    }

    public String getASQLString() {
        return _vsebina;
    }

    public static ASqlObject initFromJSON(JSONObject json) {
        ArrayList<String> algs = new ArrayList<>();
        JSONArray algJSON = json.getJSONObject("Query").getJSONArray("Algorithms");
        for (int i = 0; i < algJSON.length(); i++) {
            algs.add("algorithm=" + algJSON.getString(i));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("FROM " + json.getJSONObject("Query").getJSONArray("TestSets").join(","));
        sb.append("\n");
        sb.append("WHERE (" + ATTools.stringJoin(" OR ", algs));
        sb.append(")\n");
        sb.append("AND " + json.getJSONObject("Query").getJSONArray("Filter").join(" AND "));
        sb.append("AND ComputerID=" + json.getJSONObject("Query").getString("ComputerID"));
        sb.append("\n");
        sb.append("SELECT ");
        JSONArray indicators = json.getJSONObject("Query").getJSONArray("Indicators");
        JSONArray parameters = json.getJSONObject("Query").getJSONArray("Parameters");
        sb.append(indicators.join(", "));
        if (indicators.length() > 0 && parameters.length() > 0) {
            sb.append(", ");
        }
        sb.append(parameters.join(", "));
        if (json.getJSONObject("Query").getString("Count").equals("1")) {
            if (indicators.length() > 0 && parameters.length() > 0) {
                sb.append(", ");
            }
            sb.append("COUNT(*)");
        }
        sb.append("\n");
        JSONArray groupBy = json.getJSONObject("Query").getJSONArray("GroupBy");
        JSONArray sortBy = json.getJSONObject("Query").getJSONArray("SortBy");
        if (groupBy.length() > 0) {
            sb.append("GROUPBY " + groupBy.join(", "));
            sb.append("\n");
        }
        if (sortBy.length() > 0) {
            sb.append("ORDERBY " + sortBy.join(", "));
            sb.append("\n");
        }
        return new ASqlObject(sb.toString());
    }

    public TableData runQuery(Project project) {
        String[] queries = getASQLString().split(";\n");
        if (queries == null || queries.length == 1) {
            EQuery eQuery = new EQuery();
            eQuery.initFromJSON(getJSONString(project));
            return DataAnalyser.runQuery(project.getEProject(), eQuery, (String) eQuery.get(EQuery.ID_ComputerID));
        } else {
            HashMap<String, TableData> results = new HashMap<>();
            TableData currentTd = null;
            for (String fullQuery : queries) {
                String[] queryParts = fullQuery.split("\n")[0].split("=", 2);
                String varName = "";
                String query = "";
                if (queryParts.length > 1) {
                    varName = queryParts[0].trim();
                    query = fullQuery.split("=", 2)[1];
                } else {
                    query = fullQuery;
                }
                ASqlObject lo = new ASqlObject(query);
                EQuery eQuery = new EQuery();
                eQuery.initFromJSON(lo.getJSONString(project));
                currentTd = DataAnalyser.runQuery(project.getEProject(), eQuery, (String) eQuery.get(EQuery.ID_ComputerID), results);
                results.put(varName, currentTd);
            }
            return currentTd;
        }
    }

}
