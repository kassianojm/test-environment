package org.yggdrasil.receivers;

import java.util.ArrayList;
import java.util.Iterator;

import scala.Tuple2;

public class ByteArrayUnpacker{

    public Iterator<Tuple2<byte[], Tuple2<byte[], byte[]>>> deserialise(ArrayList<byte[]> data) {
        Iterator<Tuple2<byte[], Tuple2<byte[], byte[]>>> ret = new Iterator<Tuple2<byte[], Tuple2<byte[], byte[]>>>() {
            private ArrayList<byte[]> multi_part = data;
            private int part = 0;
            @Override
            public Tuple2<byte[], Tuple2<byte[], byte[]>> next() {
                byte[] rank = new byte[4];
                byte[] step = new byte[4];
                byte[] values = new byte[multi_part.get(part).length - 8];
                System.arraycopy(multi_part.get(part), 0, rank,   0, 4);
                System.arraycopy(multi_part.get(part), 4, step,   0, 4);
                System.arraycopy(multi_part.get(part), 8, values, 0, values.length);
                part++;
                return new Tuple2<byte[], Tuple2<byte[], byte[]>>(rank, new Tuple2<byte[], byte[]>(step, values));
            }
            @Override
            public boolean hasNext() {
                return part < multi_part.size() && multi_part.get(part).length > 0;
            }
        };
        return ret;
    }
}
