import org.apache.spark.streaming.StateSpec;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.spark.streaming.State;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.AbstractJavaRDDLike;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.Optional;

import scala.Tuple14;
import scala.Tuple2;
import spark.AbstractClusterServer;
import spark.ResultSender;
import utils.ByteArrayUtils;

import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.scheduler.RateController;
import org.yggdrasil.receivers.CoMDReceiver;
import org.yggdrasil.receivers.SparkClusterMPIByteArrayReceiver;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;


public class CoMDServer extends AbstractClusterServer implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 5091864330412367509L;

    public static void main(String[] args) throws Exception {(new CoMDServer()).run(args);}

    protected String getName() {
        return "SimpleApp";
    }

    @SuppressWarnings("serial")
    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);

        ArrayList<JavaPairDStream<Tuple2<Integer, Integer>, Tuple14<Integer, Integer, Integer, Integer,
                                Double, Double, Double, Double, Double, Double, Double, Double, Double, Double>>> to_union = new ArrayList<>();
        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(new CoMDReceiver(in_port, sender));
                in_port+=2;
                //Step, gid -> rank, nLocal, nGlobal, iSpecies, R1, r2, r3, p1, p2, p3, f1, f2, f3, u
                JavaPairDStream<
                    Tuple2<Integer, // Step
                          Integer>  // atom ID
                    ,
                    Tuple14<Integer,// issued mpi rank
                            Integer,// nLocal
                            Integer,// nGlobal
                            Integer,//iSpecies
                            Double,// r1
                            Double,// r2
                            Double,// r3
                            Double,// p1
                            Double,// p2
                            Double,// p2
                            Double,// f1
                            Double,// f2
                            Double,// f3
                            Double // U
                            >
                > atomsPerStep_map = lines.flatMapToPair(new PairFlatMapFunction<byte[], 
                    Tuple2<Integer, // Step
                          Integer>  // atom id
                        , 
                    Tuple14<Integer,// issued mpi rank
                            Integer,// nLocal
                            Integer,// nGlobal
                            Integer,//iSpecies
                            Double,// r1
                            Double,// r2
                            Double,// r3
                            Double,// p1
                            Double,// p2
                            Double,// p2
                            Double,// f1
                            Double,// f2
                            Double,// f3
                            Double // U
                            >
                        >() {
                            @Override
                            public Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple14<Integer, Integer, Integer, Integer,
                                Double, Double, Double, Double, Double, Double, Double, Double, Double, Double>>> call(
                                    byte[] t) throws Exception {
                                int position = 0;
                                int step           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int rank           = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nLocal         = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int nGlobal        = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                int size_of_result = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                ArrayList<Tuple2<Tuple2<Integer, Integer>, Tuple14<Integer, Integer, Integer, Integer, 
                                    Double, Double, Double, Double, Double, Double, Double, Double, Double, Double>>>
                                my_list = new ArrayList<>();
                                for(int i=0; i<nLocal; i++) {
                                    int gid      = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                    int iSpecies = ByteArrayUtils.byteArrayToInt(t, position); position +=4;
                                    double r1;
                                    double r2;
                                    double r3;
                                    double p1;
                                    double p2;
                                    double p3;
                                    double f1;
                                    double f2;
                                    double f3;
                                    double u;
                                    if(size_of_result == 4) {
                                        r1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        r2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        r3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        p1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        p2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        p3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        f1 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        f2 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        f3 = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                        u  = (double) ByteArrayUtils.byteArrayToFloat(t, position); position+=size_of_result;
                                    }else {
                                        r1 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        r2 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        r3 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        p1 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        p2 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        p3 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        f1 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        f2 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        f3 = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                        u  = ByteArrayUtils.byteArrayToDouble(t, position); position+=size_of_result;
                                    }
                                    // Step, atomId
                                    Tuple2<Integer, Integer> key = new Tuple2<Integer, Integer>(step, gid);
                                    Tuple14<Integer,// issued mpi rank
                                            Integer,// nLocal
                                            Integer,// nGlobal
                                            Integer,//iSpecies
                                            Double,// r1
                                            Double,// r2
                                            Double,// r3
                                            Double,// p1
                                            Double,// p2
                                            Double,// p2
                                            Double,// f1
                                            Double,// f2
                                            Double,// f3
                                            Double // U
                                    >value = new Tuple14<>(
                                            rank,
                                            nLocal,
                                            nGlobal,
                                            iSpecies,
                                            r1,
                                            r2,
                                            r3,
                                            p1,
                                            p2,
                                            p3,
                                            f1,
                                            f2,
                                            f3,
                                            u);
                                    
                                    my_list.add(new Tuple2<Tuple2<Integer,Integer>,
                                            Tuple14<Integer,Integer,Integer,Integer,Double,Double,Double,Double,
                                            Double,Double,Double,Double,Double,Double>>(key, value));
                                }
                                return my_list.iterator();
                            }
                });
                to_union.add(atomsPerStep_map);
            }
            /*
             * Assemble every JavaPaitDStream together
             */
            JavaPairDStream<Tuple2<Integer,Integer>,Tuple14<Integer,Integer,Integer,Integer,Double,Double,
                            Double,Double,Double,Double,Double,Double,Double,Double>> together = null;
            for (JavaPairDStream<Tuple2<Integer,Integer>,Tuple14<Integer,Integer,Integer,Integer,Double,Double,Double,
                                 Double,Double,Double,Double,Double,Double,Double>> joined : to_union){
                if (together == null){
                    together = joined;
                }else{
                    together = together.union(joined);
                }
            }
            together.print();
            int inipc  = TH;
            String network = this.network;
            together.foreachRDD(new VoidFunction<JavaPairRDD<Tuple2<Integer,Integer>,Tuple14<Integer,Integer,Integer,Integer,Double,Double,Double,Double,Double,Double,Double,Double,Double,Double>>>() {
                
                @Override
                public void call(
                        JavaPairRDD<Tuple2<Integer, Integer>, Tuple14<Integer, Integer, Integer, Integer, Double, Double, Double, Double, Double, Double, Double, Double, Double, Double>> t)
                            throws Exception {
                    //this code is driver side
                    t.foreachPartition(new VoidFunction<Iterator<Tuple2<Tuple2<Integer,Integer>,Tuple14<Integer,Integer,Integer,Integer,Double,Double,Double,Double,Double,Double,Double,Double,Double,Double>>>>() {
                        
                        @Override
                        public void call(
                                Iterator<Tuple2<Tuple2<Integer, Integer>, Tuple14<Integer, Integer, Integer, Integer, Double, Double, Double, Double, Double, Double, Double, Double, Double, Double>>> records) throws Exception {
                            //this code is executor side
                            //ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                            while (records.hasNext()) {
                                Tuple2<
                                    Tuple2<Integer, // Step
                                          Integer>  // atom id
                                    , //atom ID
                                    Tuple14<Integer,// issued mpi rank
                                            Integer,// nLocal
                                            Integer,// nGlobal
                                            Integer,//iSpecies
                                            Double,// r1
                                            Double,// r2
                                            Double,// r3
                                            Double,// p1
                                            Double,// p2
                                            Double,// p2
                                            Double,// f1
                                            Double,// f2
                                            Double,// f3
                                            Double // U
                                            >
                                > record = records.next();
                                int rank  = record._2._1();
                                int step  = record._1._1;
                                int count = record._2._4();
                                byte[] element = new byte[16];
                                System.arraycopy(ByteArrayUtils.intToByteArray(rank), 0, element, 0, 4);
                                System.arraycopy(ByteArrayUtils.intToByteArray(step), 0, element, 4, 4);
                                System.arraycopy(ByteArrayUtils.floatToByteArray((float)count), 0, element, 8, 4);
                              //  queue.put(element);
                            }
                        }
                    });
                }
            });
        }
    }
}
