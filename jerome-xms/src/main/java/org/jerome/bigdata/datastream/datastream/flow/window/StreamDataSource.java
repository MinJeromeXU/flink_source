package org.jerome.bigdata.datastream.datastream.flow.window;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public  class StreamDataSource extends RichParallelSourceFunction<Tuple2<String, String>> {
        private volatile boolean running = true;

        @Override
        public void run(SourceContext<Tuple2<String, String>> ctx) throws InterruptedException {

            Tuple2[] elements = new Tuple2[]{
                Tuple2.of("a", "1"),
                Tuple2.of("a", "2"),
                Tuple2.of("a", "3"),
                Tuple2.of("a", "4"),
                Tuple2.of("a", "5"),
                Tuple2.of("a", "6"),
                Tuple2.of("b", "7"),
                Tuple2.of("b", "8"),
                Tuple2.of("b", "9"),
                Tuple2.of("b", "0")
            };

            int count = 0;
            while (running && count < elements.length) {
                ctx.collect(new Tuple2<>((String) elements[count].f0, (String) elements[count].f1));
                count++;
                Thread.sleep(1000);
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
