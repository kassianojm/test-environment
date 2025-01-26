import org.apache.spark.api.java.Optional;
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
import scala.Tuple3;
import spark.AbstractClusterServer;
import spark.ResultSender;
import utils.ByteArrayUtils;

import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.yggdrasil.receivers.SparkClusterMPIByteArrayReceiver;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.yggdrasil.receivers.CoMDReceiver;


public class COMDHistogram extends AbstractClusterServer implements Serializable{

    public static int[] mergeHashMap(int[] v1, int[] v2) throws Exception {
        for(int i = 0; i<v1.length; i++) {
            v2[i] = v1[i] + v2[i];
        }
        return v2;
    }
    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new COMDHistogram()).run(args);}

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

        ArrayList<JavaPairDStream<Tuple3<Integer,Integer,Integer>,int[]>> to_union = new ArrayList<>();

        int modulo = 10;

        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                CoMDReceiver receiver = new CoMDReceiver(in_port, sender);
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(receiver);
                in_port+=2;
                /*
                 * Produce a value count for the key :
                 * rank
                 * step
                 */
                JavaPairDStream<Tuple3<Integer, Integer, Integer>, int[]> prim_distrib = lines.mapToPair(new PairFunction<byte[], 
                        Tuple3<Integer,Integer,Integer>, int[]>() {

                            @Override
                            public Tuple2<Tuple3<Integer,Integer, Integer>, int[]> call(byte[] t) throws Exception {
                                int position = 0;
                                int step           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int rank           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nLocal         = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nGlobal        = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int size_of_result = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int[] value = new int[modulo];
                                for (int j = 0; j<modulo; j++) {
                                    value[j] = 0;
                                }
                                for(int i=0; i<nLocal; i++) {
                                    int gid      = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                    /*int iSpecies = ByteArrayUtils.byteArrayToInt(t, position);*/ position +=4;
                                    double r1;
                                    double r2;
                                    double r3;
                                    //double p1;
                                    //double p2;
                                    //double p3;
                                    //double f1;
                                    //double f2;
                                    //double f3;
                                    //double u;
                                    if(size_of_result == 4) {
                                        r1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        r2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        r3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        /*p1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*p2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*p3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*f1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*f2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*f3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                        /*u  = (double) ByteArrayUtils.byteArrayToFloat(t, position); */position+=size_of_result;
                                    }else {
                                        r1 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        r2 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        r3 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        /*p1 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*p2 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*p3 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*f1 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*f2 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*f3 = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                        /*u  = ByteArrayUtils.byteArrayToDouble(t, position); */position+=size_of_result;
                                    }
                                    value[(int)r1%modulo]+=1;
                                }
                                return new Tuple2<>(new Tuple3<>(rank, step, nGlobal), value);
                            }
                });
                to_union.add(prim_distrib);
            }
        }
        /*
         * Assemble every JavaPaitDStream together
         */
        JavaPairDStream<Tuple3<Integer,Integer, Integer>, int[]> together = null;
        for (JavaPairDStream<Tuple3<Integer,Integer, Integer>, int[]> joined : to_union){
            if (together == null){
                together = joined;
            }else{
                together = together.union(joined);
            }
        }
        
        /*
         * Now we need to reducebykey the together stream.
         */
        JavaPairDStream<Tuple3<Integer,Integer,Integer>, int[]> together_reduced = together;
        
        int inipc     = TH;
        String network   = this.network;
        
        together_reduced.foreachRDD(new VoidFunction<JavaPairRDD<Tuple3<Integer,Integer,Integer>,int[]>>() {
            
            @Override
            public void call(JavaPairRDD<Tuple3<Integer, Integer,Integer>, int[]> t) throws Exception {
                // TODO Auto-generated method stub
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple3<Integer,Integer,Integer>,int[]>>>() {
                    
                    @Override
                    public void call(Iterator<Tuple2<Tuple3<Integer, Integer, Integer>, int[]>> records) throws Exception {
                        // TODO Auto-generated method stub
                        //ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                        while (records.hasNext()) {
                            Tuple2<Tuple3<Integer,Integer, Integer>, int[]> record = records.next();
                            int rank  = record._1._1();
                            int step  = record._1._2();
                            int[] distrib = record._2;
                            int global_count = 0;
                            for (int value : distrib) {
                                global_count+= value;
                            }
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
                                //queue.put(element);
                            }
                        }
                    }
                });
            }
        });

        JavaPairDStream<Tuple2<Integer,Integer>, int[]> global = together_reduced.mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,int[]>, Tuple2<Integer,Integer>, int[]>() {

            @Override
            public Tuple2<Tuple2<Integer,Integer>, int[]> call(Tuple2<Tuple3<Integer,Integer, Integer>, int[]> t) throws Exception {
                return new Tuple2<>(new Tuple2<>(t._1._2(),t._1._3()), t._2);
            }
        }).reduceByKey(new Function2<int[], int[], int[]>() {
            @Override
            public int[] call(int[] v1, int[] v2) throws Exception {
                return mergeHashMap(v1, v2);
            }
        });


        Function3<Tuple2<Integer,Integer>, Optional<int[]>, State<int[]>, Tuple2<Tuple2<Integer,Integer>, int[]>> mappingFunction2 =
            (identificator, local_sums, state) -> {
                Tuple2<Tuple2<Integer,Integer>, int[]> output = null;
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
                        output = new Tuple2<>(
                                identificator,
                                new_state
                                );
                    }else{
                        /*
                         * Otherwise, it means that, we have to initialise the
                         * state
                         */
                        new_state = local_sums.get();
                        output = new Tuple2<>(
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
                        output = new Tuple2<>(
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


        JavaMapWithStateDStream<Tuple2<Integer,Integer>, int[], int[], Tuple2<Tuple2<Integer,Integer>, int[]>> mapWithStateDStream2 =
                global.mapWithState(StateSpec.function(mappingFunction2));


        mapWithStateDStream2.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Tuple2<Integer,Integer>,int[]>>>() {
            
            @Override
            public void call(JavaRDD<Tuple2<Tuple2<Integer,Integer>, int[]>> t) throws Exception {
                // TODO Auto-generated method stub
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,int[]>>>() {
                    
                    @Override
                    public void call(Iterator<Tuple2<Tuple2<Integer,Integer>, int[]>> records) throws Exception {
                        StringBuilder b = new StringBuilder();
                        while (records.hasNext()) {
                            Tuple2<Tuple2<Integer,Integer>, int[]> record = records.next();
                            int step  = record._1._1;
                            int[] distrib = record._2;
                            int global_count = 0;
                            for (int value : distrib) {
                                global_count+= value;
                            }
                            int trigger_size2 = record._1()._2();
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
}
