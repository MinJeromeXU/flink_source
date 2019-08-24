package org.jerome.bigdata.datastream.datastream.AsyncIO;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public class StreamDataSource extends RichParallelSourceFunction<Tuple3<String, String, Long>> {

    private volatile boolean running = true;

    @Override
    public void run(SourceContext<Tuple3<String, String, Long>> sourceContext) throws Exception {
        Tuple3[] elements = new Tuple3[]{
                Tuple3.of("a","21",282742347L),
                Tuple3.of("f","34",282333347L),
                Tuple3.of("d","34",282732347L),
                Tuple3.of("a","234",282237347L),
                Tuple3.of("c","23",282742347L),
                Tuple3.of("a","3",282733487L),
                Tuple3.of("e","89",282372347L)
        };
        int count = 0;
        while (running && count <elements.length){
            sourceContext.collect(new Tuple3<String, String, Long>((String)elements[count].f0, (String)elements[count].f1, (Long)elements[count].f2));
            count++;
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}
