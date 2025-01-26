package org.yggdrasil.receivers;

import org.apache.spark.api.java.StorageLevels;
import java.io.*; 
import java.util.*; 
import java.util.ArrayList;
import java.io.IOException;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import scala.Tuple2;
import spark.ResultSender;

public class SparkClusterMPIByteArrayReceiver extends AbastractReceiver<byte[]> {

    private static int ID = 1;
    private static final long serialVersionUID = 1;
    private int port;
    private String hostname;
    private long nbMessagesProcessed=0;
    public int my_id;
    private int batch=1;
	private int th;
    /**
     * @param port     to listen to
     * @param hostname where to execute
     */
     //hostname == senders[i] 
     //port == in_port - initial values is 9999
    public SparkClusterMPIByteArrayReceiver(int port_, String hostname_,int TH) throws IOException{
        super(StorageLevels.MEMORY_ONLY);
        my_id = ID;
        ID++;
        port = port_;
        hostname = hostname_;
        th=TH;
    }

    @Override
    public void onStart() {
	    // Start the thread that receives data over a connection
        System.out.println("je start Kassiano");
        ResultSender.sync_get_instance().registerExecutorID(this.my_id);
        new Thread(this::receive).start();
    }

    @Override
    public void onStop() {
        System.out.println("je dois stop");
        System.out.println("Messages processed "+nbMessagesProcessed);
    }

    //Create a socket connection and receive data until receiver is stopped
    private void receive() {
        //init zmq reply
        
        ZContext context = new ZContext();
        ZMQ.Socket pull = context.createSocket(ZMQ.PULL);
        ZMQ.Socket push = context.createSocket(ZMQ.PUSH);
        pull.connect("tcp://"+hostname+":"+port);
        push.connect("tcp://"+hostname+":"+(port+1));
        System.out.println("connected on "+hostname+" PULL "+port+" PUSH "+(port+1));
        ArrayList<byte[]>  multi_part  = new ArrayList<byte[]>();
        boolean cread = true;
        try{
            while(!isStopped() || cread){
                try{
					
                    multi_part.add(pull.recv());
                    while(pull.hasReceiveMore()) {
                        multi_part.add(pull.recv());
                    }
                    if(multi_part.get(0).length == 2){
                        cread = false;
                        push.send("ACK".getBytes(),0);
                    }else{
			//System.out.println("port: "+ port +" sender: "+hostname + "batch: "+ batch +" nbMessagesProcessed: "+nbMessagesProcessed); 			
			final Iterator<byte[]> itr = multi_part.iterator();
             while(itr.hasNext()) {
					store(itr.next());
			}

			//store(multi_part.iterator());
                        nbMessagesProcessed+=multi_part.size();
                        multi_part.clear();
                        batch++;
                        //port represents the ack threshold
                        if (batch > th){
					//System.out.println("port: "+ port +" sender: "+hostname + " ACK"); 
					push.send("ACK".getBytes(),0);
							batch=1;
						}
						
                    }
                }catch(Exception e){
                    System.out.println(e.getMessage());
                    e.printStackTrace(System.out);
                    System.out.println("ERROR !!!!!!!!!!!!");
                    push.send("NACK".getBytes(),0);
                }
            }
        }catch(Exception e){
            restart("error", e);
        }
        try{
            pull.close();
            push.close();
        }catch(Exception t){}
        try{
            context.close();
        }catch(Exception t){}
    }
}
