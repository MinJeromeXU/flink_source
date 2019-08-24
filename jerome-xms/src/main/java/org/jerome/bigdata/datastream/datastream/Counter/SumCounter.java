package org.jerome.bigdata.datastream.datastream.Counter;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.SimpleCounter;

import java.util.ArrayList;
import java.util.List;

public class SumCounter extends SimpleCounter {
    private final List<Counter> internalCounters = new ArrayList<>();
    public SumCounter() {
    }

    public void addCounter(Counter toAdd) {
        internalCounters.add(toAdd);
    }

    @Override
    public long getCount() {
        long sum = super.getCount();
        for (Counter counter : internalCounters) {
            sum += counter.getCount();
        }
        return sum;
    }
}
