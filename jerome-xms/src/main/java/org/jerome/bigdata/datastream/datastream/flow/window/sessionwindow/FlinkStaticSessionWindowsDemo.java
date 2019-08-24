package org.jerome.bigdata.datastream.datastream.flow.window.sessionwindow;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.IterableIterator;
import org.apache.flink.util.OutputTag;


public class FlinkStaticSessionWindowsDemo {

    public static void main(String[] args) throws Exception {
        long delay = 5000L;
        long windowGap = 10000L;

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);

        // 设置数据源
        DataStream<Tuple3<String, String, Long>> source = env.addSource(new StreamDataSource()).name("Demo Source");

        OutputTag outputTag = new OutputTag<Tuple3<String, String, Long>>("late-data") {
        };
        // 设置水位线
        DataStream<Tuple3<String, String, Long>> stream = source
                .assignTimestampsAndWatermarks(
                        new BoundedOutOfOrdernessTimestampExtractor<Tuple3<String, String, Long>>(Time.milliseconds(delay)) {
                            @Override
                            public long extractTimestamp(Tuple3<String, String, Long> element) {
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                System.out.println(element.f0 + "\t" + element.f1 + " watermark -> " + format.format(getCurrentWatermark().getTimestamp()) + " timestamp -> " + format.format(element.f2));
                                //数据是否丢失看事件时间+超市时间是否小雨水位线时间
                                return element.f2;
                            }
                        }
                );

        // 窗口聚合
        stream.keyBy(0)
                .window(EventTimeSessionWindows.withGap(Time.milliseconds(windowGap)))
                .sideOutputLateData(outputTag)
                .process(new ProcessWindowFunction<Tuple3<String, String, Long>, Object, Tuple, TimeWindow>() {
                    @Override
                    public void process(Tuple tuple, Context context, Iterable<Tuple3<String, String, Long>> iterable, Collector<Object> collector) throws Exception {
                        Tuple3<String, String, Long> result = new Tuple3<>();
                        Iterator<Tuple3<String, String, Long>> iterable1 = iterable.iterator();
                        result.f0="0";
                        result.f1="1";
                        result.f2=12L;
                        while (iterable1.hasNext()) {
                            Tuple3<String, String, Long> tmp = iterable1.next();
                            result.f1 = result.f1 + tmp.f1;
                            result.f0 = result.f0 + tmp.f0;
                            result.f2 = result.f2 + tmp.f2;
                        }
                        collector.collect(result.toString());
                    }
                }).getSideOutput(outputTag).map(new MapFunction<Tuple3<String,String,Long>,String>() {
            @Override
            public String map(Tuple3<String,String,Long> o) throws Exception {

                return o.f0+"out";
            }
        }).print();
////
//                .reduce(
//            new ReduceFunction<Tuple3<String, String, Long>>() {
//                @Override
//                public Tuple3<String, String, Long> reduce(Tuple3<String, String, Long> value1, Tuple3<String, String, Long> value2) throws Exception {
//                    return Tuple3.of(value1.f0, value1.f1 + "1" + value2.f1, 1L);
//                }
//            }
//        ).print();

//        stream.keyBy(0)
//                .window(EventTimeSessionWindows.withGap(Time.milliseconds(windowGap)))
//                .sideOutputLateData(outputTag)
//                .reduce(
//                        new ReduceFunction<Tuple3<String, String, Long>>() {
//                            @Override
//                            public Tuple3<String, String, Long> reduce(Tuple3<String, String, Long> value1, Tuple3<String, String, Long> value2) throws Exception {
//                                return Tuple3.of(value1.f0, value1.f1 + "1" + value2.f1, 1L);
//                            }
//                        }
//                ).getSideOutput(outputTag).print();

        env.execute("TimeWindowDemo");
    }

}
