package org.yggdrasil.receivers;

import java.util.ArrayList;
import java.util.Iterator;
import utils.ByteArrayUtils;

import scala.Tuple2;

public class AIntUnpacker{

    public Iterator<Tuple2<Tuple2<Integer,Integer>, float[]>> deserialise(ArrayList<byte[]> data) {
        Iterator<Tuple2<Tuple2<Integer,Integer>, float[]>> ret = new Iterator<Tuple2<Tuple2<Integer,Integer>, float[]>>() {
            private ArrayList<byte[]> multi_part = data;
            private int part = 0;
            @Override
            public Tuple2<Tuple2<Integer,Integer>, float[]> next() {
                byte[] rank = new byte[4];
                byte[] step = new byte[4];
                System.arraycopy(multi_part.get(part), 0, rank,   0, 4);
                System.arraycopy(multi_part.get(part), 4, step,   0, 4);
                int values = multi_part.get(part).length -8;
                float[] v = new float[values/4];
                for(int i=8,count=0; i<multi_part.get(part).length; i+=4,count++) {
                    v[count] = ByteArrayUtils.byteArrayToFloat(multi_part.get(part), i);
                }
                part++;
                return new Tuple2<>(new Tuple2<>(ByteArrayUtils.byteArrayToInt(rank),ByteArrayUtils.byteArrayToInt(step)), v);
            }
            @Override
            public boolean hasNext() {
                return part < multi_part.size()  && multi_part.get(part).length > 0;
            }
        };
        return ret;
    }
}
