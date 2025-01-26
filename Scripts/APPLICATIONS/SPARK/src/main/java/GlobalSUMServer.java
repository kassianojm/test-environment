import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.Duration;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.spark.streaming.State;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.AbstractJavaRDDLike;
import org.apache.spark.api.java.JavaPairRDD;
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


public class GlobalSUMServer extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new GlobalSUMServer()).run(args);}

    protected String getName() {
        return "GlobalSUMServer";
    }

    @SuppressWarnings("serial")
    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);

        int nbPart = clients;
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
                        return x._1;
                    }
                };

        ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> to_union =
            new ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>>();
        for (int i=0; i<senders.length; i++){
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
   		JavaPairDStream<Tuple2<Integer, Integer>, Iterable<Tuple2<Integer, Double>>> grouped = sums.groupByKey(partitioner);

   //		JavaPairDStream<Tuple2<Integer, Integer>, Iterable<Tuple2<Integer, Double>>> grouped = sums.groupByKey();
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


		double aux = Math.sqrt((tempon-8)/4);
		int d = (int)Math.floor(aux);
        int trigger_size = d*d;

        JavaMapWithStateDStream<Tuple2<Integer,Integer>, Tuple2<Integer, Double>, Tuple2<Integer,Double>, Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> mapWithStateDStream =
                together_reduced.filter(new Function<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>, Boolean>() {
                    
                    @Override
                    public Boolean call(Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> v1) throws Exception {
                        return !(v1._2._1().equals(trigger_size));
                    }
                }).mapWithState(StateSpec.function(mappingFunction).partitioner(partitioner).timeout(new Duration(20000)));
    //}).mapWithState(StateSpec.function(mappingFunction).timeout(new Duration(20000)));

        together_reduced = together_reduced.filter(new Function<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>, Boolean>() {
            
            @Override
            public Boolean call(Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> v1) throws Exception {
                return (v1._2._1().equals(trigger_size));
            }
        });
        together_reduced = together_reduced.union(mapWithStateDStream.mapToPair(
                new PairFunction<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>, 
                    Tuple2<Integer,Integer>, Tuple2<Integer,Double>>() {

                        @Override
                        public Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer,Double>> call(
                                Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> t) throws Exception {
                            // TODO Auto-generated method stub
                            return t;
                        }
        }));

        //int outipc = out_ipc;
        //int inipc  = TH;
        //String network = this.network;

        together_reduced.foreachRDD(
                new VoidFunction<JavaPairRDD<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>() {
                    @Override
                    public void call(JavaPairRDD<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> t) throws Exception {
                        //this code is driver side
                        t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>>>() {
                            @Override
                            public void call(Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>>> records) throws Exception {
                                //this code is executor side
                               // ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                                while (records.hasNext()) {
                                    Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> record = records.next();
                                    int rank  = record._1._1;
                                    int step  = record._1._2;
                                    int count = record._2._1;
                                    double sum= record._2._2;
                                    if(count == trigger_size){
                                        byte[] element = new byte[16];
                                        double mean = sum / count;
                                        System.arraycopy(ByteArrayUtils.intToByteArray(rank), 0, element, 0, 4);
                                        System.arraycopy(ByteArrayUtils.intToByteArray(step), 0, element, 4, 4);
                                        System.arraycopy(ByteArrayUtils.floatToByteArray((float)mean), 0, element, 8, 4);
                                        //queue.put(element);
                                    }
                                }
                            }
                        });
                    }
                });

        JavaPairDStream<Integer, Tuple2<Integer,Double>> global = together_reduced.mapToPair(new PairFunction<Tuple2<Tuple2<Integer,Integer>,Tuple2<Integer,Double>>, Integer, Tuple2<Integer,Double>>() {
            @Override
            public Tuple2<Integer, Tuple2<Integer, Double>> call(
                    Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> t) throws Exception {
                return new Tuple2<>(t._1._2, t._2);
            }
        }).reduceByKey(new Function2<Tuple2<Integer,Double>, Tuple2<Integer,Double>, Tuple2<Integer,Double>>() {
            
            @Override
            public Tuple2<Integer, Double> call(Tuple2<Integer, Double> v1, Tuple2<Integer, Double> v2) throws Exception {
                return new Tuple2<Integer, Double>(v1._1+v2._1, v1._2+v2._2);
            }
        });

        int nb_mpi_rank = super.clients;
        int trigger_size2 = trigger_size  * super.clients;

        Function3<Integer, Optional<Tuple2<Integer, Double>>, State<Tuple2<Integer, Double>>, Tuple2<Integer, Tuple2<Integer, Double>>> mappingFunction2 =
            (identificator, local_sums, state) -> {
                Tuple2<Integer, Tuple2<Integer, Double>> output = null;
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
                        output = new Tuple2<Integer, Tuple2<Integer, Double>>(
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
                        output = new Tuple2<Integer, Tuple2<Integer, Double>>(
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
                        output = new Tuple2<Integer, Tuple2<Integer, Double>>(
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

        JavaMapWithStateDStream<Integer, Tuple2<Integer, Double>, Tuple2<Integer,Double>, Tuple2<Integer, Tuple2<Integer, Double>>> mapWithStateDStream2 =
            global.filter(new Function<Tuple2<Integer,Tuple2<Integer,Double>>, Boolean>() {
                
                @Override
                public Boolean call(Tuple2<Integer, Tuple2<Integer, Double>> v1) throws Exception {
                    return !(v1._2()._1().equals(trigger_size2));
                }
            }).mapWithState(StateSpec.function(mappingFunction2).timeout(new Duration(20000)));
        global = global.filter(new Function<Tuple2<Integer,Tuple2<Integer,Double>>, Boolean>() {
            
            @Override
            public Boolean call(Tuple2<Integer, Tuple2<Integer, Double>> v1) throws Exception {
                    return (v1._2()._1().equals(trigger_size2));
            }
        });
        global = global.union(mapWithStateDStream2.mapToPair(
                new PairFunction<Tuple2<Integer,Tuple2<Integer,Double>>, Integer, Tuple2<Integer,Double>>() {

                    @Override
                    public Tuple2<Integer, Tuple2<Integer, Double>> call(Tuple2<Integer, Tuple2<Integer, Double>> t)
                            throws Exception {
                        return t;
                    }
        }));

        //mapWithStateDStream2.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Integer,Tuple2<Integer,Double>>>>() {
        //    
        //    @Override
        //    public void call(JavaRDD<Tuple2<Integer, Tuple2<Integer, Double>>> t) throws Exception {
        //        t.foreach(new VoidFunction<Tuple2<Integer,Tuple2<Integer,Double>>>() {
        //            
        //            @Override
        //            public void call(Tuple2<Integer, Tuple2<Integer, Double>> record) throws Exception {
        //                    int step  = record._1;
        //                    int count = record._2._1;
        //                    double sum= record._2._2;
        //                    System.out.println("Step "+step+" : "+count+" "+trigger_size2);
        //                    if(count == trigger_size2){
        //                        double mean = sum / count;
        //                        System.out.println("Step "+step+" : "+mean);
        //                    }
        //            }
        //        });

        //    }
        //});
        global.foreachRDD(new VoidFunction<JavaPairRDD<Integer,Tuple2<Integer,Double>>>() {
            
            @Override
            public void call(JavaPairRDD<Integer, Tuple2<Integer, Double>> t) throws Exception {
                //this code is driver side
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Integer,Tuple2<Integer,Double>>>>() {
                    @Override
                    public void call(Iterator<Tuple2<Integer, Tuple2<Integer, Double>>> records) throws Exception {
                        //this code is executor side
                        while (records.hasNext()) {
                            Tuple2<Integer, Tuple2<Integer, Double>> record = records.next();
                            int step  = record._1;
                            int count = record._2._1;
                            double sum= record._2._2;
                            if(count == trigger_size2){
                                double mean = sum / count;
                                System.out.println("Step "+ step +" : " + mean);
                            }else{
								 System.out.println("trigger_size2 " + trigger_size2 + "count "+ count);

								}
                        }
                    }
                });
            }
        });
    }
}
