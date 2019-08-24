package org.jerome.bigdata.datastream.datastream.trigger;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

import java.util.Properties;

/*
	trigger 测试
	滚动窗口，20s
	然后是trigger内部技术，10个元素输出一次。

	原理文章可以参考：
	https://zhuanlan.zhihu.com/p/57939498
	https://zhuanlan.zhihu.com/p/57939828
 */
public class kafkaSourceTriggerTest {

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", "mt-mdh.local:9093");
		properties.setProperty("group.id", "test");

		FlinkKafkaConsumer010<String> kafkaConsumer010 = new FlinkKafkaConsumer010<>("test",
				new SimpleStringSchema(),
				properties);

		AllWindowedStream<Integer, TimeWindow> stream = env
				.addSource(kafkaConsumer010)
				.map(new String2Integer())
				.timeWindowAll(org.apache.flink.streaming.api.windowing.time.Time.seconds(20))
				.trigger(CustomProcessingTimeTrigger.create());
		stream.sum(0)
				.print()
				;

		env.execute(kafkaSourceTriggerTest.class.getCanonicalName());
	}
	private static class String2Integer extends RichMapFunction<String, Integer> {
		private static final long serialVersionUID = 1180234853172462378L;
		@Override
		public Integer map(String event) throws Exception {

			return Integer.valueOf(event);
		}
		@Override
		public void open(Configuration parameters) throws Exception {
		}
	}

}
