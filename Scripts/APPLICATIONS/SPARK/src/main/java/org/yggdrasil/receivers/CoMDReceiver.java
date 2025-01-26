package org.yggdrasil.receivers;

import org.apache.spark.api.java.StorageLevels;

import java.util.ArrayList;
import java.io.IOException;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import scala.Tuple2;
import spark.ResultSender;

public class CoMDReceiver extends AbastractReceiver<byte[]> {

    private static int ID = 1;
    private static final long serialVersionUID = 1;
    private int port;
    private String hostname;
    private long nbMessagesProcessed=0;
    public int my_id;

    /**
     * @param port     to listen to
     * @param hostname where to execute
     */
    public CoMDReceiver(int port_, String hostname_) throws IOException{
        super(StorageLevels.MEMORY_ONLY);
        my_id = ID;
        ID++;
        port = port_;
        hostname = hostname_;
    }

    @Override
    public void onStart() {
        System.out.println("je start");
        new Thread(this::receive).start();
    }

    @Override
    public void onStop() {
        System.out.println("je dois stop");
        System.out.println("Messages processed "+nbMessagesProcessed);
    }

    /** Create a socket connection and receive data until receiver is stopped */
    private void receive() {
        ResultSender.sync_get_instance().registerExecutorID(this.my_id);
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
                        store(multi_part.iterator());
                        push.send("ACK".getBytes(),0);
                        nbMessagesProcessed+=multi_part.size();
                        multi_part.clear();
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
