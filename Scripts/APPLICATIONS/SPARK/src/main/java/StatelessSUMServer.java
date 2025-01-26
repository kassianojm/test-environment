import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import scala.Tuple2;
import spark.AbstractClusterServer;
import spark.ResultSender;
import utils.ByteArrayUtils;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.yggdrasil.receivers.SparkClusterMPIByteArrayReceiver;


public class StatelessSUMServer extends AbstractClusterServer implements Serializable{

    public static void main(String[] args) throws Exception {(new StatelessSUMServer()).run(args);}

    protected String getName() {
        return "StatelessSUMServer";
        /**
         * This application presents the mean of each msg by dividing the sum of all element by the array size
         **/
    }

    protected void register_spark_application() throws IOException {
        /**
          * NFS: ssc.checkpoint("./data_SUMServer");
        **/
        ssc.checkpoint(hdfs_root);
        
        /**
         * senders: # MQ's instances
         * receivers_by_sender: # receivers per node (greather data ingestion)
         * 
         * **/
         
        for (int i=0; i<senders.length; i++){
//			in_port=9999;
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                System.out.println("connected on "+sender+" PULL "+in_port+" PUSH "+(in_port+1));
                System.out.println(" port "+in_port);

					
					/**
					 * a) DStream represents the basic abstraction in Spark Streaming. It is comprised by a continuous sequence of RDDs (micro-batches), where each RDD contains a distributed collection of data from a certain interval
					 * * An RDD can be represented "like" a set of pointers to where the actual data is in a cluster.
					 * b) Each DStream is associated with a Receiver running on a worker machine that receives the data from multiple sources and stores it in Spark’s memory for processing.
					 * b.1) More receivers increases overall throughput, but it can becomes a bottleneck.    
					 * ---APP---
					 * c) JavaReceiverInputDStream (lines): Creates an input stream (receiverStream) from network source hostname:port. The data is received as serialized blocks.
					 * d) In this case, the data is received by the ZMQ-based API ovevr the SparkClusterMPIByteArrayReceiver method.                 * 
					 * **/
					JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(new SparkClusterMPIByteArrayReceiver(in_port, sender, TH));
					in_port+=2;
					/**
					 * JavaPairDStream is an interface to a DStream of key-value pairs, which provides extra methods * like `reduceByKey` and `join`.
					 * In this case, sums stores every records(events) - MPI-based msgs - into multiple RDDs (a microbatch or a simple RDD).
					 * As result of a mapToPair transformation, a new DStream for all elements of this DStream will be created (using two T2 tuples). 
					 * The created values are rank, step, size and a sum (containing a sum of the content's of every position of the array for a given msg).
					 * */
					JavaPairDStream<Tuple2<Integer, Integer>, Tuple2<Integer,Double>> sums = lines.mapToPair(new PairFunction<byte[], Tuple2<Integer,Integer>, Tuple2<Integer,Double>>() {
						/**
						* Retrieving data of each msg to sum the content of every position of the array into sum
						**/
						@Override
						public Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> call(byte[] t) throws Exception {
							int values = t.length -8;
							double sum = 0;
							for(int i=8; i<t.length; i+=4) {
								sum += (double) ByteArrayUtils.byteArrayToFloat(t, i);
							}
							/**
							*There is a msg.
							**/
							if(t.length > 8) {
								//System.out.println("----");
								System.out.println("Rank: " + ByteArrayUtils.byteArrayToInt(t, 0) + "\n Step: " + ByteArrayUtils.byteArrayToInt(t, 4) + "\n Array Size: " + values/4 + "\n SUM: "+ sum);
								//System.out.println("----");
								return new Tuple2<>(new Tuple2<>(ByteArrayUtils.byteArrayToInt(t, 0), ByteArrayUtils.byteArrayToInt(t, 4)), new Tuple2<>(values/4, sum));
							}else {
								return null;
							}
						}
					});
									   

					/**
					 * DStream sums.foreachRDD is an "output operator" in Spark Streaming. It allows you to access the underlying RDDs of the DStream to execute:
					 * -> actions (our case - functions to return some value)
					 * -> to perform some transformations (new RDDs) with the data. 
					 * foreachPartitions create a single connection object and send all the records in a RDD partition using that connection, achieving some level of parallelism (it changes the ideia of one record at time)
					**/
					sums.foreachRDD( new  VoidFunction<JavaPairRDD<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>() {
						private static final long serialVersionUID = -7988279685523690596L;
						
						@Override
						public void call(JavaPairRDD<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> t) throws Exception {
							//this code is driver side
							t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>>() {
								private static final long serialVersionUID = -5728589411881723382L;
								@Override
								public void call(Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>>> records) throws Exception {
									//this code is executor side
									while (records.hasNext()) {
										Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> record = records.next();
										if(record != null) {
											int rank  = record._1._1;
											int step  = record._1._2;
											int count = record._2._1;
											double sum = record._2._2;
											double mean = sum / count;
											System.out.println("Rank: "+rank+" Step: "+step+ " count: "+count+" sum: "+sum+"\n mean: "+mean);
										   
										}
									}
								}
							});
						}
					});
                
            }
        }
    }
}
