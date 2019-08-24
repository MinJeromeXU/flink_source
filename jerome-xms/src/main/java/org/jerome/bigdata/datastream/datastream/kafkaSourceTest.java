package org.jerome.bigdata.datastream.datastream;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

import java.util.Properties;
/*
	trigger 测试
	滚动窗口，20s
	然后是trigger内部技术，10个元素输出一次。

 */
public class kafkaSourceTest {

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
		properties.setProperty("bootstrap.servers", "localhost:9092");
		properties.setProperty("group.id", "test");

		FlinkKafkaConsumer010<String> kafkaConsumer010 = new FlinkKafkaConsumer010<>("jsontest",
				new SimpleStringSchema(),
				properties);

		DataStream<String> stream = env
				.addSource(kafkaConsumer010)
				.map(new String2Integer());
				/*.timeWindowAll(org.apache.flink.streaming.api.windowing.time.Time.seconds(20))
				.trigger(CustomProcessingTimeTrigger.create());*/
//		stream.sum(0)
				stream.print()
				;

		Iterable<JobVertex> vertices = env.getStreamGraph().getJobGraph().getVertices();
		for (JobVertex vertex :vertices){
			System.out.println("=====>"+vertex.getName());
			System.out.println("=====>"+vertex.getID());
		}

		env.execute(kafkaSourceTest.class.getCanonicalName());
	}
	private static class String2Integer extends RichMapFunction<String, String> {
		private static final long serialVersionUID = 1180234853172462378L;
		@Override
		public String map(String event) throws Exception {

			return event;
		}
		@Override
		public void open(Configuration parameters) throws Exception {
		}
	}

}
