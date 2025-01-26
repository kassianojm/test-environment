package org.yggdrasil.messages;
import com.google.gson.Gson;

import org.yggdrasil.NodeReservationDetail;

public class JSONMessage{
    public final static Gson gson = new Gson();
    public final static String START            = "START";
    public final static String STOP             = "STOP";
    public final static String NODE_RESERVATION = "NODE_RESERVATION";
    public final static String NODE_FREE        = "NODE_FREE";

    public String  type;
    public boolean isQuestion;
    public boolean isACK;
    public String  data;

    public static JSONMessage from_json(String data){
        return gson.fromJson(data, JSONMessage.class);
    }

    public NodeReservationDetail getNodeReservation(){
        if(type.equals(NODE_RESERVATION) || (type.equals(NODE_FREE))){
            return gson.fromJson(data, NodeReservationDetail.class);
        }else{
            return null;
        }
    }

    public static String make_askForReceiver(int how){
        JSONMessage message = new JSONMessage();
        message.type = NODE_RESERVATION;
        message.isQuestion = true;
        message.isACK      = false;
        NodeReservationDetail details = new NodeReservationDetail();
        details.how = how;
        message.data       = gson.toJson(details);
        return gson.toJson(message);
    }

    public static String make_free_reveivers(NodeReservationDetail details){
        JSONMessage message = new JSONMessage();
        message.type = NODE_FREE;
        message.isQuestion = true;
        message.isACK      = false;
        message.data       = gson.toJson(details);
        return gson.toJson(message);
    }

    public static String make_start(){
        JSONMessage message = new JSONMessage();
        message.type = START;
        message.isQuestion = true;
        message.isACK      = false;
        message.data       = "";
        return gson.toJson(message);
    }

    public static String make_stop(){
        JSONMessage message = new JSONMessage();
        message.type = STOP;
        message.isQuestion = true;
        message.isACK      = false;
        message.data       = "";
        return gson.toJson(message);
    }

    public static String to_json(JSONMessage message){
        return gson.toJson(message);
    }

    public void ack_question(){
        isQuestion = false;
        isACK      = true;
    }

    public void nack_question(){
        isQuestion = false;
        isACK      = false;
    }

    public void nack_question(String text){
        isQuestion = false;
        isACK      = false;
        data       = text;
    }

    public void update_reservation_details(NodeReservationDetail details){
        data = gson.toJson(details);
    }
}
