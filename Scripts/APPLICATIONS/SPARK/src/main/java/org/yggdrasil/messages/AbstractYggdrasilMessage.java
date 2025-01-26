package org.yggdrasil.messages;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public abstract class AbstractYggdrasilMessage {
    public final static String TYPE_MPI     = "mpi send";
    public final static String TYPE_MESSAGE = "message";
    protected String message;
    protected String dest;
    protected String group;
    protected String type;
    protected String from;
    protected String group_from;

    /**
     * Construct a message
     * @param message    : the message to send
     * @param dest       : the destination ID on group
     * @param group      : the destination group
     * @param type       : the type of message TYPE_MPI or TYPE_MESSAGE
     * @param from       : the sender's ID on group_from
     * @param group_from : the sender's group
     */
    protected AbstractYggdrasilMessage(String message, String dest, String group,
                            String type, String from, String group_from){
        this.message    = message;
        this.dest       = dest;
        this.group      = group;
        this.type       = type;
        this.from       = from;
        this.group_from = group_from;
    }

    public String getMessage(){
        return message;
    }

    public String getDest(){
        return dest;
    }

    public String getGroup(){
        return group;
    }

    public String getGroupFrom(){
        return group_from;
    }

    public String getFrom(){
        return from;
    }

    public AbstractYggdrasilMessage(final byte[] bs){
        final ArrayList<String> fields = unpackFields(bs);
        this.type       = fields.get(0);
        this.dest       = fields.get(1);
        this.group      = fields.get(2);
        this.from       = fields.get(3);
        this.group_from = fields.get(4);
        this.message    = fields.get(5);
    }


    /**
     * Transform an YggdrasilMessage into a networkable message
     */
    public byte[] pack(){
        ArrayList<byte[]> fields = new ArrayList<byte[]>();
        //type
        fields.add(ByteBuffer.allocate(4).putInt(type.length()) .array());
        fields.add(type.getBytes());
        //Dest
        fields.add(ByteBuffer.allocate(4).putInt(dest.length()) .array());
        fields.add(dest.getBytes());
        //group
        fields.add(ByteBuffer.allocate(4).putInt(group.length()).array());
        fields.add(group.getBytes());
        //from
        fields.add(ByteBuffer.allocate(4).putInt(from.length()).array());
        fields.add(from.getBytes());
        //group_from
        fields.add(ByteBuffer.allocate(4).putInt(group_from.length()).array());
        fields.add(group_from.getBytes());
        //message
        fields.add(ByteBuffer.allocate(4).putInt(message.length()).array());
        fields.add(message.getBytes());
        return packFields(fields);
    }

    /**
     * Transform an array of bytes into a packed message ready to transit over
     * the network
     */
    public static byte[] packFields(ArrayList<byte[]> fields){
        int arraySize = 0;
        for(byte[] field : fields){
            arraySize += field.length;
        }
        byte[] pack = ByteBuffer.allocate(arraySize).array();
        int offset = 0;
        for(byte[]field :  fields){
            for(int i=0; i<field.length; i++)
                pack[i+offset] = field[i];
            offset+=field.length;
        }
        return pack;
    }

    /**
     * Transform a network message into a valid ArrayList of strings
     */
    public static ArrayList<String> unpackFields(final byte[] bs){
        ArrayList<String> fields = new ArrayList<String>();
        int readIndex = 0;
        while (readIndex < bs.length) {
            //recompose the size of the following bloc
            int size = parseSize(bs[readIndex], bs[readIndex + 1],
                                 bs[readIndex+2], bs[readIndex+3]);
            readIndex += 4;
            //user a stringBuilder as a buffer to rebuild the name value
            StringBuilder builder = new StringBuilder();
            for (int i = readIndex; (i - readIndex) < size; i++) {
                builder.append((char) bs[i]);
            }
            readIndex += size;
            fields.add(builder.toString());
        }
        return fields;
    }

    /**
     * Transform 4 bytes into an Integer
     */
    public static int parseSize(final byte... bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public String toString(){
        return message+";"+dest+";"+group+";"+type+";"+from+";"+group_from;
    }

}
