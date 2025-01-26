import org.apache.spark.api.java.Optional;
import org.apache.spark.streaming.State;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import spark.ResultSender;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.spark.api.java.JavaPairRDD;
import scala.Tuple2;
import scala.Tuple5;
import spark.AbstractClusterServer;
import utils.ByteArrayUtils;

import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.yggdrasil.receivers.CoMDReceiver;


public class COMDMean extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new COMDMean()).run(args);}

    protected String getName() {
        return "SimpleApp";
    }


    @SuppressWarnings("serial")
    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);
        
        ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple5<Integer,Integer, Double,Double,Double>>> to_union =
            new ArrayList<JavaPairDStream<Tuple2<Integer,Integer>, Tuple5<Integer,Integer, Double,Double,Double>>>();
        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                CoMDReceiver receiver = new CoMDReceiver(in_port, sender);
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(receiver);
                in_port+=2;
                JavaPairDStream<
                    Tuple2<
                        Integer,//Step
                        Integer//Rank
                        >
                    ,
                    Tuple5<
                        Integer,
                        Integer,//Count
                        Double,// r1
                        Double,// r2
                        Double// r3
                    >
                    > atomsPerStep_map = lines.flatMapToPair(
                            new PairFlatMapFunction<byte[], 
                    Tuple2< Integer, Integer >
                    ,
                        Tuple5<
                            Integer,
                            Integer,//Step
                            Double,// r1
                            Double,// r2
                            Double// r3
                        >
                    > () {
                            @Override
                            public Iterator<Tuple2<
                                Tuple2<
                                    Integer, Integer
                                    >
                                ,
                                    Tuple5<
                                        Integer,
                                        Integer,//Step
                                        Double,// r1
                                        Double,// r2
                                        Double// r3
                                    >
                                >
                            > call(
                                    byte[] t) throws Exception {
                                int position = 0;
                                int step           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int rank           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nLocal         = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nGlobal        = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int size_of_result = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                ArrayList<Tuple2<
                                    Tuple2<
                                        Integer,//Step
                                        Integer//Rank
                                        >
                                    ,
                                    Tuple5<
                                        Integer,//Global
                                        Integer,//local
                                        Double,// r1
                                        Double,// r2
                                        Double// r3
                                    >
                                >
                                >  my_list = new ArrayList<>();
                                double gr1 = 0;
                                double gr2 = 0;
                                double gr3  = 0;
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
                                    gr1 += r1;
                                    gr2 += r2;
                                    gr3 += r3;
                            }
                                my_list.add(new Tuple2<Tuple2<Integer,Integer>, Tuple5<Integer,Integer,Double,Double,Double>>(
                                        new Tuple2<>(step, rank), new Tuple5<>(nGlobal,nLocal, gr1, gr2, gr3)));
                        return my_list.iterator();
                    }
                });
                to_union.add(atomsPerStep_map);
                int inipc  = TH;
                String network = this.network;

                atomsPerStep_map.foreachRDD(
                        new VoidFunction<JavaPairRDD<Tuple2<Integer,Integer>,Tuple5<Integer,Integer,Double,Double,Double>>>() {
                            
                            @Override
                            public void call(JavaPairRDD<Tuple2<Integer, Integer>, Tuple5<Integer, Integer, Double, Double, Double>> t)
                                    throws Exception {
                                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,Tuple5<Integer,Integer,Double,Double,Double>>>>() {
                                    
                                    @Override
                                    public void call(Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple5<Integer, Integer, Double, Double, Double>>> records)
                                            throws Exception {
                                        //ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                                        while (records.hasNext()) {
                                            Tuple2<Tuple2<Integer, Integer>, Tuple5<Integer, Integer, Double, Double, Double>> record = records.next();
                                            int rank  = record._1._1;
                                            int step  = record._1._2;
                                            int trigger_size = record._2._1();
                                            int count = record._2._2();
                                            double sum= record._2._3();
                                            byte[] element = new byte[16];
                                            double mean = sum / count;
                                            System.arraycopy(ByteArrayUtils.intToByteArray(rank), 0, element, 0, 4);
                                            System.arraycopy(ByteArrayUtils.intToByteArray(step), 0, element, 4, 4);
                                            System.arraycopy(ByteArrayUtils.floatToByteArray((float)mean), 0, element, 8, 4);
                                            //queue.put(element);
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
        /*
         * Assemble every JavaPaitDStream together
         */
        JavaPairDStream<Tuple2<Integer,Integer>, Tuple5<Integer,Integer, Double,Double,Double>> together = null;
        for (JavaPairDStream<Tuple2<Integer,Integer>, Tuple5<Integer,Integer, Double,Double,Double>> joined : to_union){
            if (together == null){
                together = joined;
            }else{
                together = together.union(joined);
            }
        }

        /*
         * Now we need to reducebykey the together stream.
         */
        JavaPairDStream<Tuple2<Integer,Integer>, Tuple5<Integer,Integer, Double,Double,Double>> together_reduced = together;


        JavaPairDStream<Integer, Tuple5<Integer,Integer,Double,Double,Double>> global = together_reduced.mapToPair(
                new  PairFunction<Tuple2<Tuple2<Integer,Integer>,Tuple5<Integer,Integer,Double,Double,Double>>, 
                Integer, Tuple5<Integer,Integer,Double,Double,Double>>() {

            @Override
            public Tuple2<Integer, Tuple5<Integer, Integer, Double, Double, Double>> call(
                    Tuple2<Tuple2<Integer, Integer>, Tuple5<Integer, Integer, Double, Double, Double>> t)
                    throws Exception {
                return new Tuple2<>(t._1._1, t._2);
            }
        }).reduceByKey(new Function2<Tuple5<Integer,Integer,Double,Double,Double>, Tuple5<Integer,Integer,Double,Double,Double>, Tuple5<Integer,Integer,Double,Double,Double>>() {
            
            @Override
            public Tuple5<Integer, Integer, Double, Double, Double> call(Tuple5<Integer, Integer, Double, Double, Double> v1,
                    Tuple5<Integer, Integer, Double, Double, Double> v2) throws Exception {
                return new Tuple5<Integer,Integer, Double, Double, Double>( v1._1(),v1._2()+v2._2(), v1._3()+v2._3(), v1._4()+v2._4(), v1._5()+v2._5());
            }
        });

        Function3<Integer, Optional<Tuple5<Integer,Integer, Double,Double,Double>>, 
                 State<Tuple5<Integer,Integer, Double,Double,Double>>, Tuple2<Integer,
                 Tuple5<Integer,Integer, Double,Double,Double>>> mappingFunction2 =
            (identificator, local_sums, state) -> {
                Tuple2<Integer, Tuple5<Integer,Integer, Double,Double,Double>> output = null;
                Tuple5<Integer,Integer, Double,Double,Double> new_state = null;

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
                        new_state = new Tuple5<Integer,Integer, Double,Double,Double>(
                                        local_sums.get()._1(),
                                        local_sums.get()._2()+state.get()._2(),
                                        local_sums.get()._3()+state.get()._3(),
                                        local_sums.get()._4()+state.get()._4(),
                                        local_sums.get()._5()+state.get()._5()
                                    );
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

        JavaMapWithStateDStream<Integer, Tuple5<Integer,Integer,Double,Double, Double>, 
        Tuple5<Integer,Integer,Double,Double,Double>, Tuple2<Integer, Tuple5<Integer,Integer,Double,Double, Double>>> mapWithStateDStream2 =
            global.filter(new Function<Tuple2<Integer,Tuple5<Integer,Integer,Double,Double,Double>>, Boolean>() {
                
                @Override
                public Boolean call(Tuple2<Integer, Tuple5<Integer,Integer, Double,Double,Double>> v1) throws Exception {
                    return !(v1._2()._1().equals(v1._2()._2()));
                }
            }).mapWithState(StateSpec.function(mappingFunction2));
        global = global.filter(new Function<Tuple2<Integer,Tuple5<Integer,Integer,Double,Double,Double>>, Boolean>() {
            
            @Override
            public Boolean call(Tuple2<Integer, Tuple5<Integer, Integer,Double,Double,Double>> v1) throws Exception {
                    return (v1._2()._1().equals(v1._2()._2()));
            }
        });
        global = global.union(mapWithStateDStream2.mapToPair(
                new PairFunction<Tuple2<Integer,Tuple5<Integer,Integer,Double,Double,Double>>, Integer, Tuple5<Integer,Integer,Double,Double,Double>>() {

                    @Override
                    public Tuple2<Integer, Tuple5<Integer,Integer,Double,Double, Double>> call(
                            Tuple2<Integer, Tuple5<Integer,Integer, Double,Double,Double>> t)
                            throws Exception {
                        return t;
                    }
        }));

        global.foreachRDD(new VoidFunction<JavaPairRDD<Integer,Tuple5<Integer,Integer,Double,Double,Double>>>() {
            
            @Override
            public void call(JavaPairRDD<Integer, Tuple5<Integer, Integer, Double, Double, Double>> t) throws Exception {
                t.foreachPartition(new VoidFunction<Iterator<Tuple2<Integer,Tuple5<Integer,Integer,Double,Double,Double>>>>() {
                    
                    @Override
                    public void call(Iterator<Tuple2<Integer, Tuple5<Integer, Integer, Double, Double, Double>>> records) throws Exception {
                        //this code is executor side
                        while (records.hasNext()) {
                            Tuple2<Integer, Tuple5<Integer, Integer, Double, Double, Double>> record = records.next();
                            int step  = record._1;
                            int trigger = record._2()._1();
                            int count = record._2()._2();
                            double sum= record._2._3();
                            if(count == trigger){
                                double mean = sum / count;
                                System.out.println("Step "+step+" : "+mean);
                            }else {
                                System.out.println("Cont "+count+" trigger "+trigger);
                            }
                        }
                    }
                });
            }
        });
    }
}
