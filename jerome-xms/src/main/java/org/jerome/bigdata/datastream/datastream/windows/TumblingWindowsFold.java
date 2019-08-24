package org.jerome.bigdata.datastream.datastream.windows;

import org.apache.flink.api.common.functions.FoldFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.jerome.bigdata.datastream.datastream.trigger.CustomProcessingTimeTrigger;
import org.jerome.bigdata.datastream.datastream.util.Split2KV;


import java.util.Properties;

public class TumblingWindowsFold {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置最少一次处理语义和恰一次处理语义
        env.enableCheckpointing(20000,CheckpointingMode.EXACTLY_ONCE);
//		checkpoint 也可以分开设置
        env.enableCheckpointing(20000);
//		env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);

//		设置checkpoint目录
//		env.setStateBackend(new FsStateBackend("/hdfs/checkpoint"));
        env.getCheckpointConfig() // checkpoint的清楚策略
                .enableExternalizedCheckpoints(CheckpointConfig.
                        ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

//		设置重启策略
        env.setRestartStrategy(RestartStrategies.
                fixedDelayRestart(5,//5次尝试
                        50000)); //每次尝试间隔50s

        // 选择设置事件事件和处理事件
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
        properties.setProperty("group.id", "TumblingWindowsFold");

        FlinkKafkaConsumer010<String> kafkaConsumer010 = new FlinkKafkaConsumer010<>("KV",
                new SimpleStringSchema(),
                properties);

        SingleOutputStreamOperator<String> fold = env
                .addSource(kafkaConsumer010)
                .map(new Split2KV())
                .keyBy(0)
//                .windowAll(SlidingEventTimeWindows.of(Time.seconds(10),Time.seconds(10)))
                .windowAll(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(10)))
                .trigger(CustomProcessingTimeTrigger.create())
                .fold("", new FoldFunction<Tuple2<String, Long>, String>() {

                    // 字符串拼接功能，初始字符串为空，然后将数字追加其后
                    @Override
                    public String fold(String acc, Tuple2<String, Long> value) throws Exception {
                        return acc + value.f1;
                    }
                });

        fold.print();

        env.execute(TumblingWindowsFold.class.getCanonicalName());
    }
}
