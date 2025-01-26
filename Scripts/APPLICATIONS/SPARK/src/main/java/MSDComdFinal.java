import org.apache.spark.api.java.Optional;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.State;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;
import scala.Tuple5;
import spark.AbstractClusterServer;
import utils.ByteArrayUtils;

import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.yggdrasil.receivers.CoMDReceiver;


public class MSDComdFinal extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new MSDComdFinal()).run(args);}

    protected String getName() {
        return "SimpleApp";
    }

    public static Tuple2<Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>> merge_computations(
            Tuple2<Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>> v1,
            Tuple2<Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>> v2)
            throws Exception {
                Tuple4<Integer, Double, Double, Double> v1_left = v1._1;
                Tuple4<Integer, Double, Double, Double> v1_right = v1._2;
                Tuple4<Integer, Double, Double, Double> v2_left = v2._1;
                Tuple4<Integer, Double, Double, Double> v2_right = v2._2;
                Tuple4<Integer, Double, Double, Double> left = null;
                Tuple4<Integer, Double, Double, Double> right = null;
                if( v1_left == null && v2_left == null) {
                    left= null;
                }else if(v1_left == null) {
                    left = v2_left;
                }else if(v2_left == null) {
                    left = v1_left;
                }
                if( v1_right == null && v2_right == null) {
                    right= null;
                }else if(v1_right == null) {
                    right = v2_right;
                }else if(v2_right == null) {
                    right = v1_right;
                }
                if(left == null || right == null) {
                    System.out.println("No way 1 ?");
                }
                return new Tuple2<>(left, right);
    }

    @SuppressWarnings("serial")
    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);

        ArrayList<JavaPairDStream<
                    Tuple3<
                        Integer,//Step
                        Integer,//Atom_ID
                        Integer//global wanted
                        >
                    ,
                    Tuple4<
                        Integer,//Merged
                        Double,// r1
                        Double,// r2
                        Double// r3
                    >
            >
        > to_union = new ArrayList<>();
        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                CoMDReceiver receiver = new CoMDReceiver(in_port, sender);
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(receiver);
                in_port+=2;
                JavaPairDStream<
                    Tuple3<
                        Integer,//Step
                        Integer,//Atom_ID
                        Integer//global wanted
                        >
                    ,
                    Tuple4<
                            Integer,//Merged
                            Double,// r1
                            Double,// r2
                            Double// r3
                        >
                > atomsPerStep_map = lines.flatMapToPair(new PairFlatMapFunction<byte[], 
                    Tuple3<
                        Integer,//Step
                        Integer,//Atom_ID
                        Integer//global wanted
                        >
                    ,
                        Tuple4<
                            Integer,//Step
                            Double,// r1
                            Double,// r2
                            Double// r3
                        >
                    > () {
                            @Override
                            public Iterator<Tuple2<
                                Tuple3<
                                    Integer,//Step
                                    Integer,//Atom_ID
                                    Integer//global wanted
                                    >
                                ,
                                    Tuple4<
                                        Integer,//Merged
                                        Double,// r1
                                        Double,// r2
                                        Double// r3
                                    >
                                >
                            > call(
                                    byte[] t) throws Exception {
                                int position = 0;
                                int step           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                /*int rank           = ByteArrayUtils.byteArrayToInt(t, position);*/ position +=4;
                                int nLocal         = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nGlobal        = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int size_of_result = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                ArrayList<Tuple2<
                                    Tuple3<
                                        Integer,//Step
                                        Integer,//Atom_ID
                                        Integer//global wanted
                                        >
                                    ,
                                        Tuple4<
                                            Integer,//Merged
                                            Double,// r1
                                            Double,// r2
                                            Double// r3
                                        >
                                    >
                                >  my_list = new ArrayList<>();
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
                                    my_list.add(new Tuple2<>(new Tuple3<>(step, gid, nGlobal), new Tuple4<>(0, r1,r2,r3)));
                                }
                                return my_list.iterator();
                            }
                });
                to_union.add(atomsPerStep_map);
            }
            /*
             * Assemble every JavaPaitDStream together
             */
            JavaPairDStream<Tuple3<Integer, Integer, Integer>, Tuple4<Integer,Double,Double,Double>> together = null;
            for (JavaPairDStream<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>> joined : to_union){
                if (together == null){
                    together = joined;
                }else{
                    together = together.union(joined);
                }
            }
            //Perform even and odd computations
            JavaPairDStream<Tuple3<Integer,Integer,Integer>, Tuple4<Integer,Double,Double,Double>> even =
                       together.mapToPair(
                               new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,
                                   Tuple4<Integer,Double,Double,Double>>, 
                                   Tuple3<Integer,Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                @Override
                public Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                        Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                        throws Exception {
                    if(t._1()._1()%2 == 0) {
                        return new Tuple2<>(new Tuple3<>(t._1()._1()+1, t._1()._2(), t._1()._3()), t._2());
                    }else {
                        return new Tuple2<>(new Tuple3<>(t._1()._1(), t._1()._2(), t._1()._3()), t._2());
                    }
                }
            }).reduceByKey(new Function2<Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>>() {
                
                @Override
                public Tuple4<Integer, Double, Double, Double> call(Tuple4<Integer, Double, Double, Double> v1,
                        Tuple4<Integer, Double, Double, Double> v2) throws Exception {
                    return new Tuple4<Integer, Double, Double, Double>(1, v1._2()*v2._1(), v1._2()*v2._2(), v1._3()*v2._3());
                }
            });
            JavaPairDStream<
                    Tuple3<
                        Integer,//Step
                        Integer,//Atom_ID
                        Integer//global wanted
                        >
                    ,
                    Tuple4<
                            Integer,//Merged
                            Double,// r1
                            Double,// r2
                            Double//  r3
                        >
            > odd = together.mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Tuple3<Integer,Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                @Override
                public Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                        Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                        throws Exception {
                    if(t._1()._1()%2 == 0) {
                        return new Tuple2<>(new Tuple3<>(t._1()._1(), t._1()._2(), t._1()._3()), t._2());
                    }else {
                        return new Tuple2<>(new Tuple3<>(t._1()._1()+1, t._1()._2(), t._1()._3()), t._2());
                    }
                }
            }).reduceByKey(new Function2<Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>>() {
                
                @Override
                public Tuple4<Integer, Double, Double, Double> call(Tuple4<Integer, Double, Double, Double> v1,
                        Tuple4<Integer, Double, Double, Double> v2) throws Exception {
                    return new Tuple4<Integer, Double, Double, Double>(1, v1._2()*v2._1(), v1._2()*v2._2(), v1._3()*v2._3());
                }
            });
            
            Function3<
                    Tuple3<//key
                            Integer,//computation_id,
                            Integer,//Atom_ID
                            Integer//global wanted
                        >,
                     Optional< // fresh data
                             Tuple4<
                                 Integer,//Step
                                 Double,// r1
                                 Double,// r2
                                 Double// r3
                             >
                     >, 
                     State<    // what to keep in memory
                             Tuple4<
                                 Integer,//Step
                                 Double,// r1
                                 Double,// r2
                                 Double// r3
                             >
                    >, 
                    Tuple2<
                        Tuple3<Integer,Integer,Integer>,
                             Tuple4<
                                 Integer,//Step
                                 Double,// r1
                                 Double,// r2
                                 Double// r3
                            >
                    >
            > mapFunction =  new Function3<
                                            Tuple3<Integer,Integer,Integer>,
                                            Optional<Tuple4<Integer,Double,Double,Double>>,
                                            State<Tuple4<Integer,Double,Double,Double>>,
                                            Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>
            >() {
                
                @Override
                public Tuple2<Tuple3<Integer,Integer,Integer>, Tuple4<Integer, Double, Double, Double>> call(
                        Tuple3<Integer, Integer, Integer> key,
                        Optional<Tuple4<Integer, Double, Double, Double>> input,
                        State<Tuple4<Integer, Double, Double, Double>> state)
                        throws Exception {
                     Tuple4<Integer, Double, Double, Double> new_state;
                     if(input.isPresent()){
                        /*
                         * If local sums are present, it means that, we got
                         * something for this round
                         */
                        if(state.exists()){
                             Tuple4<Integer, Double, Double, Double> v1 = state.get();
                             Tuple4<Integer, Double, Double, Double> v2 = input.get();
                            new_state = new Tuple4<Integer, Double, Double, Double>(1, v1._2()*v2._1(), v1._2()*v2._2(), v1._3()*v2._3());
                            state.remove();
                        }else{
                            new_state = input.get(); 
                            if(input.get()._1().equals(1)){
                                state.remove();
                            }else {
                                state.update(new_state);
                            }
                        }
                        /*
                         * Check if enough data is gathered to release the result
                         */
                        return new Tuple2<>(key,new_state);
                       
                    }else{
                        state.remove();
                    }
                    return null;
                }
            };

            
            JavaMapWithStateDStream<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>, Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>>>
            mapped_even = even.filter(new Function<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Boolean>() {
                
                @Override
                public Boolean call(Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> v1)
                        throws Exception {
                    return v1._2()._1().equals(0);
                }
            }).mapWithState(StateSpec.function(mapFunction));//.partitioner(partitioner).timeout(new Duration(Long.MAX_VALUE)));
            JavaMapWithStateDStream<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>, Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>>>
                mapped_odd = odd.filter(new Function<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Boolean>() {
                
                @Override
                public Boolean call(Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> v1)
                        throws Exception {
                    return v1._2()._1().equals(0);
                }
            }).mapWithState(StateSpec.function(mapFunction));//.partitioner(partitioner).timeout(new Duration(Long.MAX_VALUE)));
            
            
            JavaPairDStream<
                Tuple2<
                    Integer,//Step
                    Integer//global wanted
                >,
                Tuple4<
                    Integer, //count
                    Double,// r1
                    Double,// r2
                    Double// r3
                >> addition_even = even.union(mapped_even.mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Tuple3<Integer,Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                    @Override
                    public Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                            Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                            throws Exception {
                        return t;
                    }
                })).mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Tuple2<Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                    @Override
                    public Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                            Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                            throws Exception {
                        if(t._2()._1().equals(0)) {
                            return new Tuple2<>(new Tuple2<>(t._1()._1(), t._1._3()), new Tuple4<>(0, (double)0, (double)0, (double)0));
                        }else {
                            return new Tuple2<>(new Tuple2<>(t._1()._1(), t._1._3()), t._2());
                        }
                    }
                }).reduceByKey(new Function2<Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>>() {
                    
                    @Override
                    public Tuple4<Integer, Double, Double, Double> call(Tuple4<Integer, Double, Double, Double> v1,
                            Tuple4<Integer, Double, Double, Double> v2) throws Exception {
                        return new Tuple4<Integer, Double, Double, Double>(v1._1()+v2._1(), v1._2()+v2._2(), v1._3()+v2._3(), v1._4()+v2._4());
                    }
                });

            JavaPairDStream<
                Tuple2<
                    Integer,//Step
                    Integer//global wanted
                >,
                Tuple4<
                    Integer, //count
                    Double,// r1
                    Double,// r2
                    Double// r3
                >> addition_odd = odd.union(mapped_odd.mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Tuple3<Integer,Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                    @Override
                    public Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                            Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                            throws Exception {
                        return t;
                    }
                })).mapToPair(new PairFunction<Tuple2<Tuple3<Integer,Integer,Integer>,Tuple4<Integer,Double,Double,Double>>, Tuple2<Integer,Integer>, Tuple4<Integer,Double,Double,Double>>() {

                    @Override
                    public Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                            Tuple2<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t)
                            throws Exception {
                        if(t._2()._1().equals(0)) {
                            return new Tuple2<>(new Tuple2<>(t._1()._1(), t._1._3()), new Tuple4<>(0, (double)0, (double)0, (double)0));
                        }else {
                            return new Tuple2<>(new Tuple2<>(t._1()._1(), t._1._3()), t._2());
                        }
                    }
                }).reduceByKey(new Function2<Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>, Tuple4<Integer,Double,Double,Double>>() {
                    
                    @Override
                    public Tuple4<Integer, Double, Double, Double> call(Tuple4<Integer, Double, Double, Double> v1,
                            Tuple4<Integer, Double, Double, Double> v2) throws Exception {
                        return new Tuple4<Integer, Double, Double, Double>(v1._1()+v2._1(), v1._2()+v2._2(), v1._3()+v2._3(), v1._4()+v2._4());
                    }
                });
               
            Function3<
                    Tuple2<//key
                            Integer,//computation_id,
                            Integer//global wanted
                        >,
                     Optional< // fresh data
                         Tuple4<
                             Integer,//count
                             Double,// r1
                             Double,// r2
                             Double// r3
                         >
                     >, 
                     State<    // what to keep in memory
                         Tuple4<
                             Integer,//count
                             Double,// r1
                             Double,// r2
                             Double// r3
                         >
                    >, 
                    Tuple2<
                        Tuple2<Integer,Integer>,
                        Tuple4< //what to return
                            Integer,
                             Double,// r1
                             Double,// r2
                             Double// r3
                         >
                    >
            > mapFunction2 =  new Function3<Tuple2<Integer,Integer>, Optional<Tuple4<Integer,Double,Double,Double>>, State<Tuple4<Integer,Double,Double,Double>>, Tuple2<Tuple2<Integer,Integer>,Tuple4<Integer,Double,Double,Double>>>() {
                @Override
                public Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>> call(
                        Tuple2<Integer, Integer> key,
                        Optional<Tuple4<Integer, Double, Double, Double>> input,
                        State<Tuple4<Integer, Double, Double, Double>> state) throws Exception {
                     Tuple4<Integer, Double, Double, Double> new_state;
                     if(input.isPresent()){
                        /*
                         * If local sums are present, it means that, we got
                         * something for this round
                         */
                        if(state.exists()){
                            new_state = new Tuple4<Integer, Double, Double, Double>(state.get()._1()+input.get()._1(),
                                                                                             state.get()._2()+input.get()._2(),
                                                                                             state.get()._3()+input.get()._3(),
                                                                                             state.get()._4()+input.get()._4());
                            if(new_state._1().equals(key._2())){
                                state.remove();
                            }else {
                                state.update(new_state);
                            }
                        }else{
                            new_state = input.get(); 
                            if(new_state._1().equals(key._2())){
                                state.remove();
                            }else {
                                state.update(new_state);
                            }
                        }
                        return new Tuple2<>(key,new_state);
                       
                    }else{
                        state.remove();
                    }
                    return null;
                }
            };

            JavaMapWithStateDStream<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>, Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>>>
            mapped2_even = addition_even.mapWithState(StateSpec.function(mapFunction2));//.partitioner(partitioner2).timeout(new Duration(Long.MAX_VALUE)));

            JavaMapWithStateDStream<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>, Tuple4<Integer, Double, Double, Double>, Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>>>
            mapped2_odd = addition_odd.mapWithState(StateSpec.function(mapFunction2));//.partitioner(partitioner2).timeout(new Duration(Long.MAX_VALUE)));
            
            mapped2_odd.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Tuple2<Integer,Integer>,Tuple4<Integer,Double,Double,Double>>>>() {
                
                @Override
                public void call(JavaRDD<Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>>> t)
                        throws Exception {
                    t.foreach(new VoidFunction<Tuple2<Tuple2<Integer,Integer>,Tuple4<Integer,Double,Double,Double>>>() {
                        
                        @Override
                        public void call(Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t) throws Exception {
                            if (t._1._2().equals(t._2._1())) {
                                double r1 = t._2._3() / t._1._2;
                                double r2 = t._2._3() / t._1._2;
                                double r3 = t._2._3() / t._1._2;
                                System.out.println(" -> "+t._1._1()+" : r1 "+r1+" r2 "+r2+" r3 "+r3);
                            }
                        }
                    });
                    
                }
            });
            mapped2_even.foreachRDD(new VoidFunction<JavaRDD<Tuple2<Tuple2<Integer,Integer>,Tuple4<Integer,Double,Double,Double>>>>() {
                
                @Override
                public void call(JavaRDD<Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>>> t)
                        throws Exception {
                    t.foreach(new VoidFunction<Tuple2<Tuple2<Integer,Integer>,Tuple4<Integer,Double,Double,Double>>>() {
                        
                        @Override
                        public void call(Tuple2<Tuple2<Integer, Integer>, Tuple4<Integer, Double, Double, Double>> t) throws Exception {
                            System.out.println("Try computation "+t._1._1());
                            if (t._1._2().equals(t._2._1())) {
                                double r1 = t._2._3() / t._1._2;
                                double r2 = t._2._3() / t._1._2;
                                double r3 = t._2._3() / t._1._2;
                                System.out.println(" -> "+t._1._1()+" : r1 "+r1+" r2 "+r2+" r3 "+r3);
                            }
                        }
                    });
                    
                }
            });
        }
    }
}
