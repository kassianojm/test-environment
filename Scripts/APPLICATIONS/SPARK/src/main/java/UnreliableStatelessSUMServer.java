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
import org.yggdrasil.receivers.SparkClusterMPIByteArrayUnreliableReceiver;


public class UnreliableStatelessSUMServer extends AbstractClusterServer implements Serializable{

    public static void main(String[] args) throws Exception {(new UnreliableStatelessSUMServer()).run(args);}

    protected String getName() {
        return "SimpleApp";
    }

    protected void register_spark_application() throws IOException {
        ssc.checkpoint(hdfs_root);
        for (int i=0; i<senders.length; i++){
            for(int j = 0;j < receivers_by_sender; j++) {
                String sender = senders[i];
                int inipc  = TH;
                String network = this.network;
                JavaReceiverInputDStream<byte[]>  lines = ssc.receiverStream(new SparkClusterMPIByteArrayUnreliableReceiver(in_port, sender));
                in_port+=2;
                JavaPairDStream<Tuple2<Integer, Integer>, Tuple2<Integer,Double>> sums = lines.mapToPair(new PairFunction<byte[], Tuple2<Integer,Integer>, Tuple2<Integer,Double>>() {

                    @Override
                    public Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> call(byte[] t) throws Exception {
                        int values = t.length -8;
                        double sum = 0;
                        for(int i=8; i<t.length; i+=4) {
                            sum += (double) ByteArrayUtils.byteArrayToFloat(t, i);
                        }
                        if(t.length > 8) {
                            return new Tuple2<>(new Tuple2<>(ByteArrayUtils.byteArrayToInt(t, 0), ByteArrayUtils.byteArrayToInt(t, 4)), new Tuple2<>(values/4, sum));
                        }else {
                            return null;
                        }
                    }
                });
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
                                //ArrayBlockingQueue<byte[]> queue = ResultSender.get_instance().connectYggdrasil(network, inipc, outipc);
                                while (records.hasNext()) {
                                    Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Double>> record = records.next();
                                    if(record != null) {
                                        int rank  = record._1._1;
                                        int step  = record._1._2;
                                        int count = record._2._1;
                                        double sum= record._2._2;
                                        byte[] element = new byte[16];
                                        double mean = sum / count;
                                        System.arraycopy(ByteArrayUtils.intToByteArray(rank), 0, element, 0, 4);
                                        System.arraycopy(ByteArrayUtils.intToByteArray(step), 0, element, 4, 4);
                                        System.arraycopy(ByteArrayUtils.floatToByteArray((float)mean), 0, element, 8, 4);
                                      //  queue.put(element);
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
