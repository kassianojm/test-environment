import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.Duration;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;

import java.io.File;
import java.io.FileWriter;


import java.util.Map;

import java.io.DataOutputStream;
import java.io.File;


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.spark.streaming.api.java.JavaStreamingContext;

import org.apache.spark.streaming.State;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.AbstractJavaRDDLike;
import org.apache.spark.api.java.JavaRDD;

import org.apache.spark.api.java.Optional;
import scala.Tuple2;
import spark.AbstractClusterServer;
import spark.ResultSender;
import utils.ByteArrayUtils;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.scheduler.RateController;
import org.yggdrasil.receivers.SparkClusterMPIByteArrayReceiver;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import java.util.HashMap;

public class SUMServer extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;
    //to use just one file
	private HashMap<Integer, Double> mapValues = new HashMap<Integer, Double>();
	
	
	public synchronized void gravarArquivo(int valInt, double valDobro, Thread thread) throws Exception {
				FileOutputStream fos = null;
				DataOutputStream dos = null;
				String entrada = String.valueOf(valInt).concat(",").concat(String.valueOf(valDobro)).concat("\n");
				File arquivo = new File("/tmp/file_"+thread.getId()+".txt");
				arquivo.createNewFile();
				fos = new FileOutputStream(arquivo, true);
				dos = new DataOutputStream(fos);
				dos.writeBytes(entrada);		
				dos.flush();
				dos.close();
				fos.close();	
	}
	
    public static void main(String[] args) throws Exception {(new SUMServer()).run(args);}
	
    protected String getName() {
        return "SUMServer";
    }
    
    
    public int inteiro;
	public double dobro;
	
	

    @SuppressWarnings("serial")
    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);
        

        /**
          * NFS: ssc.checkpoint("./SUMServer");
        **/
		/**
		 * nbPart represents the number of mpi process
		 * **/
        int nbPart = clients;
        
        /**
         * Custom partition scheme
         * **/
        Partitioner partitioner = 
                new Partitioner() {
                    @Override
                    public int numPartitions() {
                        return nbPart;
                    }
                    @Override
                    public int getPartition(Object arg0) {
                        @SuppressWarnings("unchecked")
                        Tuple2<Integer,Integer> x = (Tuple2<Integer,Integer>)arg0;
                        //Returns the rank and step values
						//System.out.println("Rank: "+x._1+" Step: "+x._2);
                        return x._1;
                    }
                };
                
		//it is an array to save the streams (sums) in an ordered way
        ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> to_union =
            new ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>>();
            
        for (int i=0; i<senders.length; i++){
			//in_port=9999;
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(new SparkClusterMPIByteArrayReceiver(in_port, sender,TH));
                in_port+=2;
                JavaPairDStream<Tuple2<Integer, Integer>, Tuple2<Integer,Double>> sums = lines.mapToPair(new PairFunction<byte[], Tuple2<Integer,Integer>, Tuple2<Integer,Double>>() {
                    @Override
                    public Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> call(byte[] t) throws Exception {
                        int values = t.length -8;
                        double sum = 0;
                        for(int i=8; i<t.length; i+=4) {
                            sum += (double) ByteArrayUtils.byteArrayToFloat(t, i);
                        }
                         return new Tuple2<>(new Tuple2<>(ByteArrayUtils.byteArrayToInt(t, 0), ByteArrayUtils.byteArrayToInt(t, 4)), new Tuple2<>(values/4, sum));
                    }
                }).reduceByKey((gauche, droite) -> new Tuple2<Integer, Double>(gauche._1+droite._1, gauche._2 + droite._2));
                //grouped receives the RDD sums with the records gruped by rank and step
       	//	JavaPairDStream<Tuple2<Integer, Integer>, Iterable<Tuple2<Integer, Double>>> grouped = sums.groupByKey();
      		JavaPairDStream<Tuple2<Integer, Integer>, Iterable<Tuple2<Integer, Double>>> grouped = sums.groupByKey(partitioner);
                sums = grouped.mapValues(new Function<Iterable<Tuple2<Integer,Double>>, Tuple2<Integer, Double>>() {
                    @Override
                    public Tuple2<Integer, Double> call(Iterable<Tuple2<Integer, Double>> v1) throws Exception {
                        Tuple2<Integer, Double> res = null;
                        for(Tuple2<Integer, Double> v : v1) {
                            if(res == null) {
                                res = v;
                              }else {
                                res = new Tuple2<Integer,Double>(res._1 + v._1, res._2 + v._2);
                            }
                        }
                        return res;
                    }
                });
                to_union.add(sums);
            }
        }
        /*
         * Assemble every JavaPaitDStream together
         */
        JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>> together = null;
        for (JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>> joined : to_union){
            if (together == null){
                together = joined;
            }else{
                together = together.union(joined);
            }
        }

        /*
         * Now we need to reducebykey the together stream.
         */
        JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>> together_reduced = together.reduceByKey(new Function2<Tuple2<Integer,Double>, Tuple2<Integer,Double>, Tuple2<Integer,Double>>() {
            
            @Override
            public Tuple2<Integer, Double> call(Tuple2<Integer, Double> v1, Tuple2<Integer, Double> v2) throws Exception {
                return new Tuple2<Integer, Double>(v1._1+v2._1, v1._2+v2._2);
            }
        });

        Function3<Tuple2<Integer,Integer>, Optional<Tuple2<Integer, Double>>, State<Tuple2<Integer, Double>>, Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> mappingFunction =
            (identificator, local_sums, state) -> {
                Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>> output = null;
                Tuple2<Integer, Double> new_state = null;

                if(local_sums.isPresent()){
                    /*
                     * If local sums are present, it means that, we got
                     * something for this round
                     */
                    if(state.exists()){
                        /*
                         * If there is a state about this key, we can sum the
                         * values and output an updated tuple
                         */
                        new_state = new Tuple2<Integer, Double>(
                                        local_sums.get()._1()+state.get()._1(),
                                        local_sums.get()._2()+state.get()._2()
                                    );
                        output = new Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>(
                                identificator,
                                new_state
                                );
                    }else{
                        /*
                         * Otherwise, it means that, we have to initialise the
                         * state
                         */
                        new_state = new Tuple2<Integer, Double>(
                                        local_sums.get()._1(),
                                        local_sums.get()._2()
                                    );
                        output = new Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>(
                                identificator,
                                new_state
                                );
                    }
		if (!state.isTimingOut()){
                    state.update(new_state);
		}
                }else{
                    /*
                     * If the local sums are not present it means that Spark
                     * that nothing was received during this time window about
                     * the key.
                     * We should return the original state.
                     */
                    if(state.exists()){
                        new_state = new Tuple2<Integer, Double>(
                                        state.get()._1(),
                                        state.get()._2()
                                    );
                        output = new Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>(
                                identificator,
                                new_state
                                );
			if (!state.isTimingOut()){
	                        state.update(new_state);
        		}
	            }else{
                        /* If the state does not exists also, I do not
                         * understand why we are executing this piece of code.
                         * */
                    }
                }
                return output;
            };

//	JavaMapWithStateDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>, Tuple2<Integer,Double>, Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> mapWithStateDStream = together_reduced.mapWithState(StateSpec.function(mappingFunction).partitioner(partitioner));	
         JavaMapWithStateDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>, Tuple2<Integer,Double>, Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> mapWithStateDStream = together_reduced.mapWithState(StateSpec.function(mappingFunction).partitioner(partitioner).timeout(new Duration(20000)));
//
//	JavaMapWithStateDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>, Tuple2<Integer,Double>, Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> mapWithStateDStream = together_reduced.mapWithState(StateSpec.function(mappingFunction).timeout(new Duration(20000)));

		double aux = Math.sqrt((tempon-8)/4);
		int d = (int)Math.floor(aux);
        int trigger_size = d*d;
 
        // int inipc  = TH;
        //String network = this.network;

       mapWithStateDStream.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>>() {
            @Override
            public void call(JavaRDD<Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>>> t) throws Exception {
                //this code is driver side
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>>() {
                    @Override
                    public void call(Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>>> records) throws Exception {
                        //this code is executor side	
                        while (records.hasNext()) {
                            Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> record = records.next();
                            int rank  = record._1._1;
                            int step  = record._1._2;
                            int count = record._2._1;
                            double sum= record._2._2;
                            if(count == trigger_size){
                                double mean = sum / count;
                                inteiro = step;
                                dobro = mean; 
                                Thread tt = Thread.currentThread();
								//gravarArquivo(inteiro, dobro, tt);
                            }
                        }
                      }
				});
				
            }
        });
    }
}
