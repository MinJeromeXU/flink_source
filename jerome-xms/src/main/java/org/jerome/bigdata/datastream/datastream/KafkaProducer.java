package org.jerome.bigdata.datastream.datastream;

import net.sf.json.JSONObject;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.Thread.sleep;

public class KafkaProducer {
	public static void main(String [] args)  {

		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
		props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());

		Producer<String,String> producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
		java.util.Random points =new java.util.Random(2);

		for(int i = 0; i < 10000; i++){

//			producer.send(new ProducerRecord<>("side",String.valueOf(points.nextInt(10)) ));
			SendMsg_Json(producer);
//			SendMsg_KV(producer);
//			SendMsg_KafkaEvent(producer);
//			SendMsg_FlinkEngine(producer);
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		producer.close();
	}

	public static void SendMsg_Json(Producer<String, String> producer ){
		java.util.Random points =new java.util.Random(2);
		List list = new ArrayList<String>();
		list.add("apple");
		list.add("pear");
		list.add("nut");
		list.add("grape");
		list.add("banana");
		list.add("pineapple");
		list.add("pomelo");
		list.add("orange");


		for(int i = 0; i < 3000; i++){
//			for(int j =0;j < 10;j++){
				JSONObject json = new JSONObject();
				json.put("fruit",list.get(points.nextInt(8)));
				json.put("number",points.nextInt(4));
				json.put("time",System.currentTimeMillis());
				json.put("number",points.nextInt(4));
			producer.send(new ProducerRecord<String, String>("jsontest",String.valueOf(i), json.toString()));
//			}

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void SendMsg_KV(Producer<String, String> producer ){
		java.util.Random points =new java.util.Random(2);
		List list = new ArrayList<String>();
		list.add("apple");
		list.add("pear");
		list.add("nut");
		list.add("grape");
		list.add("banana");
		list.add("pineapple");
		list.add("pomelo");
		list.add("orange1");

		for(int i = 0; i < 3000; i++){
			String str = list.get(points.nextInt(8))+" "+points.nextInt(5);
			producer.send(new ProducerRecord<>("KV",String.valueOf(i), str));
//			str = list.get(points.nextInt(8))+" "+points.nextInt(5);
//			producer.send(new ProducerRecord<>("KV1",String.valueOf(i), str));
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void SendMsg_KafkaEvent(Producer<String, String> producer ){
		java.util.Random points =new java.util.Random(2);
		List list = new ArrayList<String>();
		list.add("apple");
		list.add("pear");
		list.add("nut");
		list.add("grape");
		list.add("banana");
		list.add("pineapple");
		list.add("pomelo");
		list.add("orange");


		for(int i = 0; i < 3000; i++){
			String str = list.get(points.nextInt(8))+","+points.nextInt(5)+","+System.currentTimeMillis();
			producer.send(new ProducerRecord<>("test",String.valueOf(i), str));

			try {
				sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public static void SendMsg_FlinkEngine(Producer<String, String> producer ){
		java.util.Random points =new java.util.Random(2);
		List list = new ArrayList<String>();
		list.add("apple");
		list.add("nut");
		list.add("banana");
		list.add("orange");


		for(int i = 0; i < 3000; i++){
			JSONObject json = new JSONObject();
			json.put("fruit",list.get(points.nextInt(4)));
			json.put("number",points.nextInt(4));
			json.put("time",System.currentTimeMillis());
			producer.send(new ProducerRecord<String, String>("FlinkEngine",String.valueOf(i), json.toString()));

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
