package com.rackspacecloud.metrics.queryservice.domains;

import com.rackspacecloud.metrics.queryservice.exceptions.InfluxDbQueryResultException;
import lombok.Data;

import java.util.*;

@Data
public class QueryDomainOutput {
    private String name;
    private List<String> columns;
    private List<List<Object>> valuesCollection;

    public List<Map<String, Object>> getQueryResponse(){
        List<Map<String, Object>> records = new ArrayList<>();

        String[] cols = columns.toArray(new String[columns.size()]);

        valuesCollection.forEach(
                value -> {
                    if(value.size() != cols.length) {
                        throw new InfluxDbQueryResultException(String.format(
                                "Bad data in InfluxDB query result. Columns [%s]; values are [%s]",
                                Arrays.toString(cols), Arrays.toString(new List[]{value})
                        ));
                    }
                    Map<String, Object> record = new HashMap<>();
                    for(int i = 0; i < cols.length; i++) {
                        record.put(cols[i], value.get(i));
                    }
                    records.add(record);
                }
        );

        return records;
    }

    public List<String> getValuesAsCollection(){
        List<String> records = new ArrayList<>();

        // for two level deep arrays (array of arrays), I have two forEach calls (one for each level)
        valuesCollection.forEach(value -> value.forEach(item -> records.add(item.toString())));

        return records;
    }
}
