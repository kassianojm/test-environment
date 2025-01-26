package spark;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.yggdrasil.YggdrasilConnector; //
import org.yggdrasil.messages.YggdrasilApplicationMessage; //ok
import org.yggdrasil.messages.YggdrasilReceivedMessage; //ok
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ResultSender {

    private static ResultSender me = null;
    public int ID;
    private ResultSender() {}
    private YggdrasilConnector yggdrasil;
    private String network;
    private String yggdrasil_id;
    private String endpoint;
    private Thread sender;
    private ArrayBlockingQueue<byte[]> messages_to_send;
    private boolean isstoped = false;

    synchronized public static ResultSender sync_get_instance() {
        if(me == null) {
            me = new ResultSender();
        }
        return me;
    }
    public static ResultSender get_instance() {
        if(me == null) {
            me = sync_get_instance();
        }
        return me;
    }

    synchronized
    public void registerExecutorID(int ID) {
        this.ID = ID;
    }

    private void _connectYggdrasil(String network, int in_ipc, int out_ipc) {
        synchronized(this) {
            if(yggdrasil == null) {
                System.out.println("connect yggdrasil, suppose only once");
                this.network      = network;
                this.yggdrasil_id = ""+ID;
                //yggdrasil = new YggdrasilConnector(in_ipc, out_ipc+ID+1);

                yggdrasil = new YggdrasilConnector(yggdrasil_id, network, in_ipc, out_ipc+ID+1);
                yggdrasil.sendTo(new YggdrasilApplicationMessage("spark_connect", "0", "validator", yggdrasil_id, network));
                YggdrasilReceivedMessage response = yggdrasil.receive();
                System.out.println(response.getMessage());
                this.endpoint = response.getMessage();
                messages_to_send = new ArrayBlockingQueue<byte[]>(1000);
                sender = new Thread(this::send_result);
                sender.start();
            }
        }
    }

    public ArrayBlockingQueue<byte[]> connectYggdrasil(String network, int in_ipc, int out_ipc) {
        if(yggdrasil == null) {
            _connectYggdrasil(network, in_ipc, out_ipc);
        }
        return messages_to_send;
    }
    
    private void send_result() {
        ZContext context = new ZContext();
        ZMQ.Socket req = context.createSocket(ZMQ.PUSH);
        req.connect(endpoint);
        isstoped = false;
        while(!isstoped) {
            byte[] messageToSend = null;
            try {
                //try to get a message
                messageToSend = messages_to_send.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(messageToSend != null) {
                req.send(messageToSend, 0);
                //byte[] res = req.recv();
                //if(res[0] == 'q') {
                //    isstoped = true;
                //}
            }
        }
        context.close();
    }
}
