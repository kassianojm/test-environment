package org.yggdrasil.receivers;

import java.util.ArrayList;
import java.util.Iterator;
import utils.ByteArrayUtils;

import scala.Tuple2;

public class IntUnpacker{

    public Iterator<Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> deserialise(ArrayList<byte[]> data) {
        Iterator<Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>> ret = new Iterator<Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>>>() {
            private ArrayList<byte[]> multi_part = data;
            private int part = 0;
            private int count = 8;
            @Override
            public Tuple2<Tuple2<Integer,Integer>, Tuple2<Integer, Double>> next() {
                byte[] rank = new byte[4];
                byte[] step = new byte[4];
                System.arraycopy(multi_part.get(part), 0, rank,   0, 4);
                System.arraycopy(multi_part.get(part), 4, step,   0, 4);
                double v = ByteArrayUtils.byteArrayToFloat(multi_part.get(part), count);
                count+=4;
                if(count == multi_part.get(part).length) {
                    count=8;
                    part++;
                }
                return new Tuple2<>(new Tuple2<>(ByteArrayUtils.byteArrayToInt(rank),ByteArrayUtils.byteArrayToInt(step)), new Tuple2<>(1, v));
            }
            @Override
            public boolean hasNext() {
                return (part < multi_part.size() || part == multi_part.size() -1 && count < multi_part.get(part).length) && multi_part.get(part).length > 0;
            }
        };
        return ret;
    }
}
