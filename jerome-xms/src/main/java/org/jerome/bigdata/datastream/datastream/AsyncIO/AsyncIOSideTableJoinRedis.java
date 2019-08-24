package org.jerome.bigdata.datastream.datastream.AsyncIO;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import net.sf.json.JSONObject;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.jerome.bigdata.datastream.datastream.watermark.KafkaEventSchema;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/*
    关于异步IO原理的讲解可以参考浪尖的知乎～：

    https://zhuanlan.zhihu.com/p/48686938
 */
public class AsyncIOSideTableJoinRedis {
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
//        env.setRestartStrategy(RestartStrategies.
//                fixedDelayRestart(5,//5次尝试
//                        50000)); //每次尝试间隔50s

        // 选择设置事件事件和处理事件
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
        properties.setProperty("group.id", "AsyncIOSideTableJoinRedis");

        FlinkKafkaConsumer010<JSONObject> kafkaConsumer010 = new FlinkKafkaConsumer010<>("jsontest",
                new KafkaEventSchema(),
                properties);

        DataStreamSource<JSONObject> source = env
                .addSource(kafkaConsumer010);

        SampleAsyncFunction asyncFunction = new SampleAsyncFunction();

        // add async operator to streaming job
        DataStream<JSONObject> result;
        if (true) {
            result = AsyncDataStream.orderedWait(
                    source,
                    asyncFunction,
                    1000000L,
                    TimeUnit.MILLISECONDS,
                    20).setParallelism(1);
        }
        else {
            result = AsyncDataStream.unorderedWait(
                    source,
                    asyncFunction,
                    10000,
                    TimeUnit.MILLISECONDS,
                    20).setParallelism(1);
        }

        result.print();

        env.execute(AsyncIOSideTableJoinRedis.class.getCanonicalName());
    }

    private static class SampleAsyncFunction extends RichAsyncFunction<JSONObject, JSONObject> {
        private transient RedisClient redisClient;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);

            RedisOptions config = new RedisOptions();
            config.setHost("127.0.0.1");
            config.setPort(6379);

            VertxOptions vo = new VertxOptions();
            vo.setEventLoopPoolSize(10);
            vo.setWorkerPoolSize(20);

            Vertx vertx = Vertx.vertx(vo);

            redisClient = RedisClient.create(vertx, config);
        }

        @Override
        public void close() throws Exception {
            super.close();
            if(redisClient!=null)
                redisClient.close(null);

        }

        @Override
        public void asyncInvoke(final JSONObject input, final ResultFuture<JSONObject> resultFuture) {


            String fruit = input.getString("fruit");

            // 获取hash-key值
//            redisClient.hget(fruit,"hash-key",getRes->{
//            });
            // 直接通过key获取值，可以类比
            redisClient.get(fruit,getRes->{
                if(getRes.succeeded()){
                    String result = getRes.result();
                    if(result== null){
                        resultFuture.complete(null);
                        return;
                    }
                    else {
                        input.put("docs",result);
                        resultFuture.complete(Collections.singleton(input));
                    }
                } else if(getRes.failed()){
                    resultFuture.complete(null);
                    return;
                }
            });
        }

    }
}
