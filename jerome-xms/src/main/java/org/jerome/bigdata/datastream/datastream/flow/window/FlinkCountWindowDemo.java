package org.jerome.bigdata.datastream.datastream.flow.window;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public class FlinkCountWindowDemo {

    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);
        env.setParallelism(1);
        final int windowSize = params.getInt("window", 3);
        final int slideSize = params.getInt("slide", 2);
        // read source data
        DataStreamSource<Tuple2<String, String>> inStream = env.addSource(new StreamDataSource());

        // calculate
        DataStream<Tuple2<String, String>> outStream = inStream
                                                           .keyBy(0)
                                                           .countWindow(windowSize)
                                                           .reduce(
                                                               new ReduceFunction<Tuple2<String, String>>() {
                                                                   @Override
                                                                   public Tuple2<String, String> reduce(Tuple2<String, String> value1, Tuple2<String, String> value2) throws Exception {
                                                                       return Tuple2.of(value1.f0, value1.f1 + "" + value2.f1);
                                                                   }
                                                               }
                                                           );
        /**滑动窗口 没进来2两个元素 将最近三个元素计算一边*/
        // calculate
//        DataStream<Tuple2<String, String>> outStream = inStream
//                .keyBy(0)
//                .countWindow(windowSize, slideSize)
//                .reduce(
//                        new ReduceFunction<Tuple2<String, String>>() {
//                            @Override
//                            public Tuple2<String, String> reduce(Tuple2<String, String> value1, Tuple2<String, String> value2) throws Exception {
//                                return Tuple2.of(value1.f0, value1.f1 + "" + value2.f1);
//                            }
//                        }
//                );

        outStream.print();
        env.execute("WindowWordCount");
    }
}
