package practice.spark.streaming;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.api.java.function.Function3;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;


public class JavaStatefulNetworkWordCount {
  private static final Pattern SPACE = Pattern.compile(" ");

  public static void main(String[] args) throws Exception {

    // Create the context with a 1 second batch size
    SparkConf sparkConf = new SparkConf().setAppName("JavaStatefulNetworkWordCount").setMaster("local[2]");
    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(5));
    ssc.checkpoint("./checkpoint");

    // Initial state RDD input to mapWithState
    @SuppressWarnings("unchecked")
    List<Tuple2<String, Integer>> tuples = Arrays.asList(
    		new Tuple2<>("hello", 1), new Tuple2<>("world", 1));

    JavaPairRDD<String, Integer> initialRDD = ssc.sparkContext().parallelizePairs(tuples);

    JavaReceiverInputDStream<String> lines = ssc.socketTextStream("localhost", 9998, StorageLevels.MEMORY_AND_DISK_SER_2);

    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
//    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(x.split(" ")).iterator());

    JavaPairDStream<String, Integer> wordsDstream = words.mapToPair(s -> new Tuple2<>(s, 1));

    // Update the cumulative count function
    Function3<String, Optional<Integer>, State<Integer>, Tuple2<String, Integer>> mappingFunc =
        (word, one, state) -> {
	    	if(state.isTimingOut()){
	            System.out.println(word +" is timingout. num:" + one + " state:" + state);
	            return null;
	        }

	        int sum = one.orElse(0) + (state.exists() ? state.get() : 0);
	        Tuple2<String, Integer> output = new Tuple2<>(word, sum);
	        state.update(sum);
	        System.out.println(word + " is not time out. sum:" + sum + " num:" + one + " state:" + state);
	        return output;
        };

    // DStream made of get cumulative counts that get updated in every batch
    JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> stateDstream =
        wordsDstream.mapWithState(StateSpec.function(mappingFunc).timeout(Durations.seconds(2)));
//    wordsDstream.mapWithState(StateSpec.function(mappingFunc).initialState(initialRDD).timeout(Durations.seconds(10)));

//    JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> stateDstream =
//            wordsDstream.mapWithState(StateSpec.function(mappingFunc).initialState(initialRDD));

    stateDstream.print();
    ssc.start();
    ssc.awaitTermination();
  }
}