package org.jerome.bigdata.datastream.datastream.AsyncIO;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.json.JSONObject;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.util.Collector;
import org.jerome.bigdata.datastream.datastream.watermark.KafkaEventSchema;
import redis.clients.jedis.Jedis;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class FlatmapJoin {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置最少一次处理语义和恰一次处理语义
//		env.enableCheckpointing(20000,CheckpointingMode.EXACTLY_ONCE);
//		checkpoint 也可以分开设置
//		env.enableCheckpointing(20000);
//		env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);
//		设置checkpoint目录
//		env.setStateBackend(new FsStateBackend("/hdfs/checkpoint"));
//        env.getCheckpointConfig() // checkpoint的清楚策略
//                .enableExternalizedCheckpoints(CheckpointConfig.
//                        ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);


//		设置重启策略
        env.setRestartStrategy(RestartStrategies.
                fixedDelayRestart(5,//5次尝试
                        50000)); //每次尝试间隔50s


        // 选择设置事件事件和处理事件
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
        properties.setProperty("group.id", "FlatmapJoin");

        FlinkKafkaConsumer010<JSONObject> kafkaConsumer010 = new FlinkKafkaConsumer010<>("jsontest",
                new KafkaEventSchema(),
                properties);

        SingleOutputStreamOperator<JSONObject> flatMap = env
                .addSource(kafkaConsumer010)

                .keyBy(new KeySelector<JSONObject, String>() { // 注意 keyby的目的将相同key分配到相同的并行度
                    //这样就可以实现利用缓存高校处理了
                    @Override
                    public String getKey(JSONObject value) throws Exception {
                        return value.getString("fruit");
                    }
                })
                .flatMap(new JoinWithMysql());

        flatMap.print();

        env.execute(FlatmapJoin.class.getCanonicalName());
    }

    public static class JoinWithMysql extends RichFlatMapFunction<JSONObject, JSONObject> {
        private Jedis jedis = null;
        private Cache<String, String> Cache;
        // 打开数据库链接
        @Override
        public void open(Configuration parameters) throws Exception {
            jedis = new Jedis("localhost", 6379);
            Cache = Caffeine
                    .newBuilder()
                    .maximumSize(1025)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build();
        }

        // 关闭数据库链接
        @Override
        public void close() throws Exception {
            if (jedis != null&&jedis.isConnected()){
                jedis.close();
            }
            if(Cache != null)
                Cache.cleanUp();
        }

        @Override
        public void flatMap(JSONObject value, Collector<JSONObject> out) throws Exception {
            if(jedis!=null && (!jedis.isConnected())){

                /*
                我这里只采用了kv类型的结构，假如存的是list可以遍历然后输出。
                for(String val : vals){
                    out.collect(val);
                }

                Cache就是本地缓存，缓存超时时间是10min
                 */

                String fruit = value.getString("fruit");
                String cache_data = Cache.getIfPresent(fruit);
                if(null != cache_data){
                    value.put("docs",cache_data);
                    out.collect(value);
                }else{
                    String s = jedis.get(fruit);
                    if(s != null){
                        Cache.put(fruit,s);
                        value.put("docs",s);
                        out.collect(value);
                    }
                }
            }

        }
    }
}
