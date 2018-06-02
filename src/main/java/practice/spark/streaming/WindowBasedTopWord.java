package practice.spark.streaming;

import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;

//在linux下一个终端 输入 $ nc -L -p 9999
public class WindowBasedTopWord {

    public static void main(String[] args) {
        SparkConf conf = new SparkConf().setAppName("wordcount").setMaster("local[2]");
        JavaStreamingContext jssc = new JavaStreamingContext(conf,Durations.seconds(5));

        // 这里日志简化, yasaka hello, lily world,这里日志简化主要是学习怎么使用Spark Streaming的
        JavaReceiverInputDStream<String> searchLog = jssc.socketTextStream("localhost", 9998);
        // 将搜索日志转换成只有一个搜索词即可
        JavaDStream<String> searchWordDStream = searchLog.map(new Function<String,String>(){
            private static final long serialVersionUID = 1L;
            @Override
            public String call(String searchLog) throws Exception {
                //return searchLog.split(" ")[1];
            	return searchLog;
            }
        });

        // 将搜索词映射为(searchWord, 1)的Tuple格式
        JavaPairDStream<String, Integer> searchWordPairDStream = searchWordDStream.mapToPair(
        		new PairFunction<String,String,Integer>(){
		            private static final long serialVersionUID = 1L;
		            @Override
		            public Tuple2<String, Integer> call(String word) throws Exception {
		                return new Tuple2<String,Integer>(word,1);
		            }});

        System.out.println("0000000000");

        JavaPairDStream<String, Integer> searchWordCountsDStream =
                searchWordPairDStream.reduceByKeyAndWindow(new Function2<Integer,Integer,Integer>(){
		            private static final long serialVersionUID = 1L;
		            @Override
		            public Integer call(Integer v1, Integer v2) throws Exception {
		                return v1+v2;
		            }}, Durations.seconds(60), Durations.seconds(10));

        System.out.println("111111111");

        // 到这里就已经每隔10秒把之前60秒收集到的单词统计计数(Durations.seconds(5),每隔batch的时间间隔为5s, 所以共有12个RDD),执行transform操作因为一个窗口60秒数据会变成一个RDD
        // 然后对这一个RDD根据每个搜索词出现频率进行排序然后获取排名前3热点搜索词,这里不用transform用transformToPair返回就是键值对
        JavaPairDStream<String,Integer> finalDStream = searchWordCountsDStream.transformToPair(
            new Function<JavaPairRDD<String,Integer>,JavaPairRDD<String, Integer>>(){
                private static final long serialVersionUID = 1L;
                @Override
                public JavaPairRDD<String, Integer> call(JavaPairRDD<String, Integer> searchWordCountsRDD) throws Exception {

                    // 反转
                    JavaPairRDD<Integer,String> countSearchWordsRDD = searchWordCountsRDD.mapToPair(
                    		new PairFunction<Tuple2<String,Integer>,Integer,String>(){
		                        private static final long serialVersionUID = 1L;
		                        @Override
		                        public Tuple2<Integer, String> call(
		                                Tuple2<String, Integer> tuple) throws Exception {
		                            return new Tuple2<Integer,String>(tuple._2,tuple._1);
		                        }});
                    //排序
                    JavaPairRDD<Integer,String> sortedCountSearchWordsRDD = countSearchWordsRDD.sortByKey(false);

                    //再次反转
                    JavaPairRDD<String,Integer> sortedSearchWordsRDD = sortedCountSearchWordsRDD.mapToPair(
                    		new PairFunction<Tuple2<Integer,String>,String,Integer>(){
		                        private static final long serialVersionUID = 1L;
		                        @Override
		                        public Tuple2<String,Integer> call(
		                                Tuple2<Integer,String> tuple) throws Exception {
		                            return new Tuple2<String,Integer>(tuple._2,tuple._1);
		                        }});

                    //获取前三个word
                    List<Tuple2<String,Integer>> topSearchWordCounts = sortedSearchWordsRDD.take(3);

                    //打印
                    for(Tuple2<String,Integer> wordcount : topSearchWordCounts){
                        System.out.println("====="+wordcount._1 + " " + wordcount._2);
                    }

                    return searchWordCountsRDD;
                }
            });

        // 这个无关紧要,只是为了触发job的执行,所以必须有action操作
        finalDStream.print();

        jssc.start();
        try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        jssc.close();
    }
}