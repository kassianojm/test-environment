package org.yggdrasil.messages;

import java.util.ArrayList;

public class YggdrasilApplicationMessage extends AbstractYggdrasilMessage {
    /**
     * Construct a message
     * @param message    : the message to send
     * @param dest       : the destination ID on group
     * @param group      : the destination group
     * @param from       : the sender's ID on group_from
     * @param group_from : the sender's group
     */
    public YggdrasilApplicationMessage(String message, String dest, String group,
                            String from, String group_from){
        super(message, dest, group, AbstractYggdrasilMessage.TYPE_MPI, from,
                group_from);
    }

    public YggdrasilApplicationMessage(final byte[] bs){
        super(null, null, null, null, null, null);
        final ArrayList<String> fields = unpackFields(bs);
        this.type       = fields.get(0);
        this.dest       = fields.get(1);
        this.group      = fields.get(2);
        this.from       = fields.get(3);
        this.group_from = fields.get(4);
        this.message    = fields.get(5);
    }

}
