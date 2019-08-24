package org.jerome.bigdata.datastream.datastream.Broadcast;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.util.Collector;
import org.jerome.bigdata.datastream.datastream.Broadcast.Util.FileUtil;
import org.jerome.bigdata.datastream.datastream.Broadcast.Util.PrepareBroadCastData;
import org.jerome.bigdata.datastream.datastream.util.Split2KV;

import java.util.*;

public class BroadcastNoneKeyed {
    public static void main(String[] args) throws Exception {
        FileUtil.delFile("/Users/meitu/Desktop/1");

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
        properties.setProperty("group.id", "BroadcastKeyed");

        FlinkKafkaConsumer010<String> kafkaConsumer010 = new FlinkKafkaConsumer010<>("KV",
                new SimpleStringSchema(),
                properties);

        kafkaConsumer010.setStartFromLatest();

        SingleOutputStreamOperator<Tuple2<String, Long>> noneKeyed = env
                .addSource(kafkaConsumer010)
                .map(new Split2KV());

        MapStateDescriptor<String, String> ruleStateDescriptor = new MapStateDescriptor<>(
                "RulesBroadcastState",
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO
                );

        BroadcastStream<Tuple2<String, String>> broadcast = env.fromCollection(PrepareBroadCastData.getBroadcastData()).broadcast(ruleStateDescriptor);


        SingleOutputStreamOperator<Tuple3<String, String, Long>> res = noneKeyed.connect(broadcast).process(new BroadcastProcessFunction<Tuple2<String,Long>, Tuple2<String,String>, Tuple3<String,String,Long>>() {
            MapStateDescriptor<String, String> ruleStateDescriptor = new MapStateDescriptor<>(
                    "RulesBroadcastState",
                    BasicTypeInfo.STRING_TYPE_INFO,
                    BasicTypeInfo.STRING_TYPE_INFO
            );
            @Override
            public void processElement(Tuple2<String, Long> value, ReadOnlyContext ctx, Collector<Tuple3<String, String, Long>> out) throws Exception {
                out.collect(new Tuple3<>(value.f0,ctx.getBroadcastState(ruleStateDescriptor).get(value.f0),value.f1));

            }

            @Override
            public void processBroadcastElement(Tuple2<String, String> value, Context ctx, Collector<Tuple3<String, String, Long>> out) throws Exception {
                ctx.getBroadcastState(ruleStateDescriptor).put(value.f0,value.f1);

            }
        });

        res.writeAsText("/Users/meitu/Desktop/1");

        // execute the program
        env.execute("Iterative Pi Example");
    }
}
