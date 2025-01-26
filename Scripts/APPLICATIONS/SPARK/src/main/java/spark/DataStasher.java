package spark;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.yggdrasil.YggdrasilConnector;
import org.yggdrasil.messages.YggdrasilApplicationMessage;
import org.yggdrasil.messages.YggdrasilReceivedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scala.Tuple3;

public class DataStasher {

    private static DataStasher me = null;
    private HashMap<Integer, double[]> stash_house = new HashMap<>();

    synchronized public static DataStasher sync_get_instance() {
        if(me == null) {
            me = new DataStasher();
        }
        return me;
    }
    public static DataStasher get_instance() {
        if(me == null) {
            return sync_get_instance();
        }
        return me;
    }
    
    public synchronized void add_element(int gid, double r1, double r2, double r3) {
        stash_house.put(gid, new double[]{r1, r2, r3});
    }

    public void update(Integer gid, double r1, double r2, double r3) {
        double[] v = stash_house.get(gid);
        v[0] = r1;
        v[1] = r2;
        v[2] = r3;
    }

    public double[] get(Integer gid) {
        return stash_house.get(gid);
    }
}
