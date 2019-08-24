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
import net.sf.json.JSONObject;
import org.apache.flink.api.java.tuple.Tuple3;
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
public class AsyncIOSideTableJoinMysqlTest {
    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
        properties.setProperty("group.id", "AsyncIOSideTableJoinMysql");

        FlinkKafkaConsumer010<JSONObject> kafkaConsumer010 = new FlinkKafkaConsumer010<>("jsontest",
                new KafkaEventSchema(),
                properties);

        DataStreamSource<Tuple3<String, String, Long>> source = env
                .addSource(new StreamDataSource());

        SampleAsyncFunction asyncFunction = new SampleAsyncFunction();

        DataStream<Object> result;
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

        env.execute(AsyncIOSideTableJoinMysqlTest.class.getCanonicalName());
    }

    private static class SampleAsyncFunction extends RichAsyncFunction<Tuple3<String, String, Long>, Object> {
        private transient SQLClient mySQLClient;
        private Cache<String, String> Cache;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
//            Cache = Caffeine
//                    .newBuilder()
//                    .maximumSize(1025)
//                    .expireAfterAccess(10, TimeUnit.MINUTES)
//                    .build();
//
//            JsonObject mySQLClientConfig = new JsonObject();
//            mySQLClientConfig.put("url", "jdbc:mysql://localhost:3306/mysql")
//                    .put("driver_class", "com.mysql.jdbc.Driver")
//                    .put("max_pool_size", 20)
//                    .put("user", "root")
////                    .put("max_idle_time",1000)
//                    .put("password", "123456");
//
//            VertxOptions vo = new VertxOptions();
//            vo.setEventLoopPoolSize(10);
//            vo.setWorkerPoolSize(20);
//
//            Vertx vertx = Vertx.vertx(vo);
//            mySQLClient = JDBCClient.createNonShared(vertx, mySQLClientConfig);

        }

        @Override
        public void close() throws Exception {
            super.close();
            if(mySQLClient!=null)
                mySQLClient.close();
            if(Cache!=null)
                Cache.cleanUp();
        }

        @Override
        public void asyncInvoke(final Tuple3<String, String, Long> input, final ResultFuture<Object> resultFuture) {

//            String key = input.getString("fruit");
//            String cacheIfPresent = Cache.getIfPresent(key);
//            if (cacheIfPresent != null) {
//
//                input.put("docs", cacheIfPresent);
//
//                resultFuture.complete(Collections.singleton(input));
//                return;
//            }
            mySQLClient.getConnection(conn -> {
                if (conn.failed()) {
                    //Treatment failures
                    resultFuture.completeExceptionally(conn.cause());
                    return;
                }

                final SQLConnection connection = conn.result();
            /*
                结合自己的查询逻辑，拼凑出相应的sql，然后返回结果。
             */
                String querySql = "SELECT Host ,User FROM user ";
                connection.query(querySql, res2 -> {
                    if(res2.failed()){
                        resultFuture.complete(null);
                        return;
                    }

                    if (res2.succeeded()) {
                        ResultSet rs = res2.result();
                        List<JsonObject> rows = rs.getRows();
                        System.out.println(rows.size());
                        if(rows.size() <= 0){
                            resultFuture.complete(null);
                            return;
                        }else{
                            for(JsonObject jsonObject : rows){
                                System.out.println(jsonObject.toBuffer());
                            }
                        }
//                        for (JsonObject json : rows) {
//                            String desc = json.getString("docs");
//                            input.put("docs", desc);
//                            Cache.put(key,desc);
//                            resultFuture.complete(Collections.singleton(input));
//                        }
                        // Do something with results
                    } else {
                        resultFuture.complete(null);
                    }
                });

                connection.close(done -> {
                    if (done.failed()) {
                        throw new RuntimeException(done.cause());
                    }
                });

            });
        }

    }
}
