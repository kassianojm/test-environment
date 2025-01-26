import org.apache.spark.streaming.StateSpec;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.Partitioner;
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
import org.yggdrasil.receivers.SparkClusterMPIByteArrayReceiver;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;


public class OpitGlobalHistogramServer extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new OpitGlobalHistogramServer()).run(args);}

    protected String getName() {
        return "SimpleApp";
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
                        //rank step
                        Tuple2<Integer,Integer> x = (Tuple2<Integer,Integer>)arg0;
                        return x._1();
                    }
                };

        ArrayList<JavaPairDStream<Tuple2<Integer,Integer>,int[]>> to_union = new ArrayList<>();

        int modulo = 10;

        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(new SparkClusterMPIByteArrayReceiver(in_port, sender,TH));
                in_port+=2;
                /*
                 * Produce a value count for the key :
                 * rank
                 * step
                 */
                JavaPairDStream<Tuple2<Integer, Integer>, int[]> prim_distrib = lines.flatMapToPair(
                        new PairFlatMapFunction<byte[], Tuple2<Integer, Integer>, int[]>() {
                            @Override
                            public Iterator<Tuple2<Tuple2<Integer, Integer>, int[]>> call(
                                    byte[] t) throws Exception {
                                int rank = ByteArrayUtils.byteArrayToInt(t);
                                int step = ByteArrayUtils.byteArrayToInt(t, 4);
                                ArrayList<Tuple2<Tuple2<Integer, Integer>, int[]>> ml = new ArrayList<>();
                                int nbval = (t.length-8)/4;
                                Tuple2<Integer, Integer> key = new Tuple2<>(rank, step);
                                int[] value = new int[modulo];
                                for (int j = 0; j<modulo; j++) {
                                    value[j] = 0;
                                }
                                for (int i = 0; i <nbval; i++) {
                                    value[((int) (ByteArrayUtils.byteArrayToFloat(t, i*4))%modulo)] += 1;
                                }
                                ml.add(new Tuple2<Tuple2<Integer,Integer>, int[]>(key, value));
                                return ml.iterator();
                            }
                        });
                        int trigger_size = tempon;
                        int inipc     = TH;
                        String network   = this.network;
                        prim_distrib.foreachRDD(new VoidFunction<JavaPairRDD<Tuple2<Integer,Integer>,int[]>>() {
                            
                            @Override
                            public void call(JavaPairRDD<Tuple2<Integer, Integer>, int[]> t) throws Exception {
                                // TODO Auto-generated method stub
                                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,int[]>>>() {
                                    
                                    @Override
                                    public void call(Iterator<Tuple2<Tuple2<Integer, Integer>, int[]>> records) throws Exception {
                                        // TODO Auto-generated method stub
                                        //ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                                        while (records.hasNext()) {
                                            Tuple2<Tuple2<Integer, Integer>, int[]> record = records.next();
                                            int rank  = record._1._1;
                                            int step  = record._1._2;
                                            int[] distrib = record._2;
                                            int global_count = 0;
                                            for (int value : distrib) {
                                                global_count+= value;
                                            }
                                            if(global_count == trigger_size){
                                                byte[] element = new byte[8+8*distrib.length];
                                                System.arraycopy(ByteArrayUtils.intToByteArray(rank), 0, element, 0, 4);
                                                System.arraycopy(ByteArrayUtils.intToByteArray(step), 0, element, 4, 4);
                                                int position=8;
                                                for (int i=0; i<distrib.length; i++) {
                                                    int key = i;
                                                    int val = distrib[i];
                                                    System.arraycopy(ByteArrayUtils.floatToByteArray(key), 0, element, position, 4);
                                                    position+=4;
                                                    System.arraycopy(ByteArrayUtils.floatToByteArray(((float)val)*(float)100/(float)global_count), 0, element, 8, 4);
                                                    position+=4;
                                                }
                                               // queue.put(element);
                                            }
                                        }
                                    }
                                });
                            }
                        });
                to_union.add(prim_distrib);
                
                
            }
        }
        /*
         * Assemble every JavaPaitDStream together
         */
        JavaPairDStream<Tuple2<Integer, Integer>, int[]> together = null;
        for (JavaPairDStream<Tuple2<Integer, Integer>, int[]> joined : to_union){
            if (together == null){
                together = joined;
            }else{
                together = together.union(joined);
            }
        }
        

        JavaPairDStream<Integer, int[]> global = together.mapToPair(new PairFunction<Tuple2<Tuple2<Integer,Integer>,int[]>, Integer, int[]>() {
            @Override
            public Tuple2<Integer, int[]> call(Tuple2<Tuple2<Integer, Integer>, int[]> t) throws Exception {
                return new Tuple2<>(t._1._2, t._2);
            }
        }).reduceByKey(new Function2<int[], int[], int[]>() {
            
            @Override
            public int[] call(int[] v1, int[] v2) throws Exception {
                return mergeHashMap(v1, v2);
            }
        });


        Function3<Integer, Optional<int[]>, State<int[]>, Tuple2<Integer, int[]>> mappingFunction2 =
            (identificator, local_sums, state) -> {
                Tuple2<Integer, int[]> output = null;
                int[] new_state = null;

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
                        new_state = mergeHashMap(local_sums.get(),state.get());
                        output = new Tuple2<Integer, int[]>(
                                identificator,
                                new_state
                                );
                    }else{
                        /*
                         * Otherwise, it means that, we have to initialise the
                         * state
                         */
                        new_state = local_sums.get();
                        output = new Tuple2<Integer, int[]>(
                                identificator,
                                new_state
                                );
                    }
                    state.update(new_state);
                }else{
                    /*
                     * If the local sums are not present it means that Spark
                     * that nothing was received during this time window about
                     * the key.
                     * We should return the original state.
                     */
                    if(state.exists()){
                        new_state = state.get();
                        output = new Tuple2<Integer, int[]>(
                                identificator,
                                new_state
                                );
                        state.update(new_state);
                    }else{
                        /* If the state does not exists also, I do not
                         * understand why we are executing this piece of code.
                         * */
                    }
                }
                return output;
            };


        JavaMapWithStateDStream<Integer, int[], int[], Tuple2<Integer, int[]>> mapWithStateDStream2 =
                global.mapWithState(StateSpec.function(mappingFunction2));

        int trigger_size2 = tempon * super.clients;

        mapWithStateDStream2.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Integer,int[]>>>() {
            
            @Override
            public void call(JavaRDD<Tuple2<Integer, int[]>> t) throws Exception {
                // TODO Auto-generated method stub
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Integer,int[]>>>() {
                    
                    @Override
                    public void call(Iterator<Tuple2<Integer, int[]>> records) throws Exception {
                        StringBuilder b = new StringBuilder();
                        while (records.hasNext()) {
                            Tuple2<Integer, int[]> record = records.next();
                            int step  = record._1;
                            int[] distrib = record._2;
                            int global_count = 0;
                            for (int value : distrib) {
                                global_count+= value;
                            }
                            if(global_count == trigger_size2){
                                b.append("distrib for ");
                                b.append(step);
                                b.append(" :\n");
                                for (int i=0; i<distrib.length; i++) {
                                    int key = i;
                                    int val = distrib[i];
                                    b.append(key+" "+val+"\n");
                                }
                            }
                        }
                        System.out.println(b.toString());
                    }
                });
            }
        });

    }
    public static int[] mergeHashMap(int[] v1, int[] v2) throws Exception {
        for(int i = 0; i<v1.length; i++) {
            v2[i] = v1[i] + v2[i];
        }
        return v2;
    }
}
