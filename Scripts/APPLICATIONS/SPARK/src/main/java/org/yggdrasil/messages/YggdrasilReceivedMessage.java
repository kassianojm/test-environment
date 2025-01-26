package org.yggdrasil.messages;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class YggdrasilReceivedMessage extends AbstractYggdrasilMessage{

    public YggdrasilReceivedMessage(final byte[] bs){
        super(null, null, null, null, null, null);
        final ArrayList<String> fields = unpackFields(bs);
        this.from       = fields.get(0);
        this.group_from = fields.get(1);
        this.message    = fields.get(2);
    }

    /**
     * Transform an YggdrasilMessage into a networkable message
     * @override
     */
    public byte[] PackYggdrasilMessage(){
        return ByteBuffer.allocate(0).array();
    }

}
