package org.jerome.bigdata.datastream.datastream.flow.window;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
 
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
 
/**
 *
 * @author : xiaojun
 * @since 9:47 2018/4/2
 */
public class WatermarkTest {
    public static void main(String[] args) throws Exception {
 
        //2018/3/3 3:30:0
        Long baseTimestamp = 1520019000000L;
 
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.getConfig().setAutoWatermarkInterval(2000);
        env.setParallelism(1);
 
        DataStream<Tuple2<String, Long>> raw = env.socketTextStream("localhost", 9999, "\n").map(new MapFunction<String, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(String value) throws Exception {
                //每行输入数据形如: key1@0,key1@13等等，即在baseTimestamp的基础上加多少秒，作为当前event time
                String[] tmp = value.split("@");
                Long ts = baseTimestamp + Long.parseLong(tmp[1]) * 1000;
                return Tuple2.of(tmp[0], ts);
            }
        }).assignTimestampsAndWatermarks(new MyTimestampExtractor(Time.seconds(10))); //允许10秒乱序，watermark为当前接收到的最大事件时间戳减10秒
 
        DataStream<String> window = raw.keyBy(0)
                //窗口都为自然时间窗口，而不是说从收到的消息时间为窗口开始时间来进行开窗，比如3秒的窗口，那么窗口一次是[0,3),[3,6)....[57,0),如果10秒窗口，那么[0,10),[10,20),...
                .window(TumblingEventTimeWindows.of(Time.seconds(3)))
                // 允许5秒延迟
                //比如窗口[2018-03-03 03:30:00,2018-03-03 03:30:03),如果没有允许延迟的话，那么当watermark到达2018-03-03 03:30:03的时候，将会触发窗口函数并移除窗口，这样2018-03-03 03:30:03之前的数据再来，将被丢弃
                //在允许5秒延迟的情况下，那么窗口的移除时间将到watermark为2018-03-03 03:30:08,在watermark没有到达这个时间之前，你输入2018-03-03 03:30:00这个时间，将仍然会触发[2018-03-03 03:30:00,2018-03-03 03:30:03)这个窗口的计算
                .allowedLateness(Time.seconds(5))
                .apply(new WindowFunction<Tuple2<String, Long>, String, Tuple, TimeWindow>() {
                    @Override
                    public void apply(Tuple tuple, TimeWindow window, Iterable<Tuple2<String, Long>> input, Collector<String> out) throws Exception {
                        LinkedList<Tuple2<String, Long>> data = new LinkedList<>();
                        for (Tuple2<String, Long> tuple2 : input) {
                            data.add(tuple2);
                        }
                        data.sort(new Comparator<Tuple2<String, Long>>() {
                            @Override
                            public int compare(Tuple2<String, Long> o1, Tuple2<String, Long> o2) {
                                return o1.f1.compareTo(o2.f1);
                            }
                        });
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String msg = String.format("key:%s,  window:[ %s  ,  %s ), elements count:%d, elements time range:[ %s  ,  %s ]", tuple.getField(0)
                                , format.format(new Date(window.getStart()))
                                , format.format(new Date(window.getEnd()))
                                , data.size()
                                , format.format(new Date(data.getFirst().f1))
                                , format.format(new Date(data.getLast().f1))
                        );
                        out.collect(msg);
 
                    }
                });
        window.print();
 
        env.execute();
 
    }
 
 
    public static class MyTimestampExtractor implements AssignerWithPeriodicWatermarks<Tuple2<String, Long>> {
 
 
        private static final long serialVersionUID = 1L;
 
        /**
         * The current maximum timestamp seen so far.
         */
        private long currentMaxTimestamp;
 
        /**
         * The timestamp of the last emitted watermark.
         */
        private long lastEmittedWatermark = Long.MIN_VALUE;
 
        /**
         * The (fixed) interval between the maximum seen timestamp seen in the records
         * and that of the watermark to be emitted.
         */
        private final long maxOutOfOrderness;
 
 
        private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 
        public MyTimestampExtractor(Time maxOutOfOrderness) {
            if (maxOutOfOrderness.toMilliseconds() < 0) {
                throw new RuntimeException("Tried to set the maximum allowed " +
                        "lateness to " + maxOutOfOrderness + ". This parameter cannot be negative.");
            }
            this.maxOutOfOrderness = maxOutOfOrderness.toMilliseconds();
            this.currentMaxTimestamp = Long.MIN_VALUE + this.maxOutOfOrderness;
        }
 
        public long getMaxOutOfOrdernessInMillis() {
            return maxOutOfOrderness;
        }
 
 
        @Override
        public final Watermark getCurrentWatermark() {
            // this guarantees that the watermark never goes backwards.
            long potentialWM = currentMaxTimestamp - maxOutOfOrderness;
            if (potentialWM >= lastEmittedWatermark) {
                lastEmittedWatermark = potentialWM;
            }
            System.out.println(String.format("call getCurrentWatermark======currentMaxTimestamp:%s  , lastEmittedWatermark:%s", format.format(new Date(currentMaxTimestamp)), format.format(new Date(lastEmittedWatermark))));
            return new Watermark(lastEmittedWatermark);
        }
 
        @Override
        public final long extractTimestamp(Tuple2<String, Long> element, long previousElementTimestamp) {
            long timestamp = element.f1;
            if (timestamp > currentMaxTimestamp) {
                currentMaxTimestamp = timestamp;
            }
            return timestamp;
        }
    }
}
