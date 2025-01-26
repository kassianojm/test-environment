package org.yggdrasil.receivers;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

public abstract class AbastractReceiver<T> extends Receiver<T>{

    /**
     * 
     */
    private static final long serialVersionUID = 6505080632884643460L;

    public AbastractReceiver(StorageLevel storageLevel) {
        super(storageLevel);
    }
}
