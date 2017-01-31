package org.lep.senate.model;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class ReportRow {
    private Map<Pair<Integer, Step>, Double> row;

    public ReportRow() {
        row = new HashMap<>();
        for(int i = 1; i < 4; i++) {
            for(Step s: Step.values()) {
                Pair<Integer, Step> col = new Pair<>(i, s);
                row.put(col, 0.0);
            }
        }
    }

    public void incrementScore(int importance, Step step) {
        incrementScore(importance, step, 1.0);
    }

    public void incrementScore(int importance, Step step, double increment) {
        Pair<Integer, Step> key = new Pair<>(importance, step);
        Double value = row.get(key);
        row.put(key, value + increment);
    }

    public Double getScore(int importance, Step step) {
        return row.get(new Pair<>(importance, step));
    }
}
