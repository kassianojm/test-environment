package org.yggdrasil;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.yggdrasil.messages.AbstractYggdrasilMessage;//ok
import org.yggdrasil.messages.YggdrasilReceivedMessage;//ok

import org.zeromq.ZMQ;
import org.zeromq.ZContext;



public class YggdrasilConnector {

    private String rank    = null;
    private String in_ipc  = null;
    private String out_ipc = null;
    private ZContext    ctx    = null;
    private ZMQ.Socket     puller = null;
    private ZMQ.Socket     pusher = null;

	public YggdrasilConnector(String rank, String network, int in_ipc, int out_ipc) {
        this.rank    = rank;
        this.in_ipc  = in_ipc+"";
        this.out_ipc = out_ipc+"";
        System.out.println(in_ipc);
        System.out.println(out_ipc);
        initConnection();
    }

    /**
     * Initialise the connection to the erebor daemon.
     * TODO handle errors with custom exception
     */
    private void initConnection(){
		System.out.println("in_ipc " +in_ipc);
        System.out.println("out_ipc " +out_ipc);
        
        
        
        ctx    = new ZContext();
        puller = ctx.createSocket(ZMQ.PULL);
        puller.bind("tcp://*:"+out_ipc);
        pusher = ctx.createSocket(ZMQ.PUSH);
        pusher.connect("tcp://127.0.0.1:"+ in_ipc);
       
        //Connect puller and pusher
        //Create the connection message
        ArrayList<byte[]> connectionMessage = new ArrayList<byte[]>();
        connectionMessage.add(ByteBuffer.allocate(4).putInt("register".length()) .array());
        connectionMessage.add("register".getBytes());
        
        connectionMessage.add(ByteBuffer.allocate(4).putInt(rank.length()) .array());
        connectionMessage.add(rank.getBytes());
        
        connectionMessage.add(ByteBuffer.allocate(4).putInt(out_ipc.length()) .array());
        connectionMessage.add(out_ipc.getBytes());
        //Pack the connection message as a byte array
        byte[] bcm = AbstractYggdrasilMessage.packFields(connectionMessage);
        //send the message to the Yggdrasil instance
        pusher.send(bcm,0);
    }

    public boolean sendTo(AbstractYggdrasilMessage m){
        return pusher.send(m.pack(),0);
    }

    public boolean nonBlockSendTo(AbstractYggdrasilMessage m){
        return pusher.send(m.pack(), 0, ZMQ.DONTWAIT);
    }

    public YggdrasilReceivedMessage receive(){
        byte[] msg = puller.recv();
        if (msg == null)
            return null;
        else{
            return new YggdrasilReceivedMessage(msg);
        }
    }

    public YggdrasilReceivedMessage nonBlockReceive(){
        byte[] msg = puller.recv(ZMQ.DONTWAIT);
        if (msg == null)
            return null;
        else{
            return new YggdrasilReceivedMessage(msg);
        }
    }

    public void close() throws java.io.IOException{
        puller.close();
        pusher.close();
        ctx.close();
    }
}
