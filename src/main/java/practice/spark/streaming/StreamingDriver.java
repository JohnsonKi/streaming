package practice.spark.streaming;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import com.google.common.primitives.Bytes;

import practice.spark.base.AnnotatedMessageBean;


public class StreamingDriver {

	public static void main(String[] args) throws Exception {
		SparkConf conf = new SparkConf().setAppName("StreamingDriver").setMaster("local[1]");
		JavaSparkContext sc = new JavaSparkContext(conf);

		AnnotatedMessageBean bean11 = new AnnotatedMessageBean();

		Map<String ,Object> annotetion = new HashMap();
		annotetion.put("vin", "11111");
		annotetion.put("model", "model_b");
		bean11.setAnnotetion(annotetion);
		byte[] message = new byte[] {0x00,0x01};

		byte[] blockInfo1 = Bytes.concat(
				getCan0x0E((byte)0x00),getCan0x01((byte)0x00),
				getCan0x0E((byte)0x03),
				getCan0x22((byte)0x05),
				getCan0x0E((byte)0x09),
				getCan0x0E((byte)0x99)
				);

		bean11.setMessage(message);


		//data 準備
		//JavaRDD<Tuple2<TelegramHash, List<CanUnitBean>>> rdd


		JavaStreamingContext jssc = new JavaStreamingContext(sc, Durations.milliseconds(15));
		JavaRDD<AnnotatedMessageBean> unpackMessageRDD = jssc.sparkContext().parallelize(Arrays.asList(bean11),1);

		Queue<JavaRDD<AnnotatedMessageBean>> queue = new ArrayDeque<>();
			queue.add(unpackMessageRDD);
			Thread.sleep(2);
			queue.add(unpackMessageRDD);
			Thread.sleep(2);
			queue.add(unpackMessageRDD);

		JavaDStream<AnnotatedMessageBean> inputJavaDStream = jssc.queueStream(queue);

		inputJavaDStream.foreachRDD(rdd -> rdd.foreach(new VoidFunction<AnnotatedMessageBean>() {

			@Override
			public void call(AnnotatedMessageBean tp2) throws Exception {
				System.out.println("" + tp2.getAnnotetion().get("vin") + ":" +  tp2.getMessage().toString());

			}
		}));
		inputJavaDStream.dstream().saveAsTextFiles("target/data/output/streaming" +System.currentTimeMillis() , "");
		jssc.start();
	}

	public static byte[] getCan0x22(byte cantime) {
		byte[] canIdAndCanDataSize = new byte[] {0x02, 0x28};
		byte[] cantimeMill = new byte[]{cantime};
		byte[] canData = new byte[] {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		return Bytes.concat(canIdAndCanDataSize, cantimeMill, canData);
	}

	public static byte[] getCan0x0E(byte cantime) {
		byte[] canIdAndCanDataSize = new byte[] {0x00, (byte)0xE8};
		byte[] cantimeMill = new byte[]{cantime};
		byte[] canData = new byte[] {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		return Bytes.concat(canIdAndCanDataSize, cantimeMill, canData);
	}

	public static byte[] getCan0x01(byte cantime) {
		byte[] canIdAndCanDataSize = new byte[] {0x00, (byte)0x18};
		byte[] cantimeMill = new byte[]{cantime};
		byte[] canData = new byte[] {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		return Bytes.concat(canIdAndCanDataSize, cantimeMill, canData);
	}
}
