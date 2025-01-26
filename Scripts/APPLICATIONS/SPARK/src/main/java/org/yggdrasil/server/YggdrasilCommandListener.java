package org.yggdrasil.server;

import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.yggdrasil.NodeReservationDetail;//ok
import org.yggdrasil.YggdrasilConnector;//ok
import org.yggdrasil.messages.JSONMessage;//ok
import org.yggdrasil.messages.YggdrasilApplicationMessage;//ok
import org.yggdrasil.messages.YggdrasilReceivedMessage;//ok
import java.net.SocketException;
import java.util.ArrayList;

public class YggdrasilCommandListener {

    public static final int MAX_RETRY_TERMINATION = 10;
    public static final int TIME_WAIT_TERMINATION = 1;

    private YggdrasilConnector connector;
    private JavaStreamingContext ssc;
    private Thread listener;
    private String network;
    private boolean continue_read = true;
    private ArrayList<WorkerState> workers = new ArrayList<WorkerState>();
    private int workers_free = 0;
    private int nb_producer  = 0;

    public YggdrasilCommandListener(JavaStreamingContext ssc_, String network, int in_ipc, int out_ipc){
        this.ssc       = ssc_;
        this.listener  = new Thread(this::receive);
        this.network   = network;
        //Connect on Yggdrasil
        connector = new YggdrasilConnector("0", network, in_ipc, out_ipc);
        //connector = new YggdrasilConnector(in_ipc, out_ipc);
    }

    private void receive() {
        try{
            YggdrasilReceivedMessage m = null;
            System.out.println("Demarrage du receiver");
            while(continue_read){
                m = connector.receive();

                System.out.println("RECV "+m.getMessage()
                                   + " from "+m.getFrom()+"@"+m.getGroupFrom());
                if(m != null){
                    JSONMessage message = JSONMessage.from_json(m.getMessage());

                    if (message.type.equals(JSONMessage.STOP)){
                        System.out.println("notify stop");
                        nb_producer -=1;
                        if(nb_producer == 0){
                            continue_read = false;
                            ssc.stop(true,true);
                            boolean wait_stop = true;
                            for(int i=0; i<MAX_RETRY_TERMINATION && wait_stop; i++){
                                System.out.println("wait "+i);
                                wait_stop = !ssc.awaitTerminationOrTimeout(
                                        TIME_WAIT_TERMINATION);
                                System.out.println("done wait "+i);
                            }
                            System.out.println("can stop");
                        }
                        message.ack_question();
                    }

                    else if (message.type.equals(JSONMessage.START)){
                        System.out.println("notify start");
                        nb_producer+=1;
                        message.ack_question();
                    }
                    else {
                        message.nack_question("WUT?");
                    }

                    System.out.println("SEND "+ JSONMessage.to_json(message));
                    YggdrasilApplicationMessage sm = new
                        YggdrasilApplicationMessage(
                                JSONMessage.to_json(message),
                                m.getFrom(),
                                m.getGroupFrom(),
                                "0",
                                network);
                    connector.sendTo(sm);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void init() throws SocketException{
        listener.start();
    }
	//init - in_port, senders
    public void init(int incoming_port, String[] workers) throws SocketException{
        int port = incoming_port;
        for(int i=0; i<workers.length; i++){
            String worker = workers[i];
            WorkerState workerState = new WorkerState();
            workerState.name = worker;
            workerState.in_port = port;
            workerState.out_port = port +1;
            port +=2;
            this.workers.add(workerState);
            this.workers_free+=1;
        }
        listener.start();
    }
}

