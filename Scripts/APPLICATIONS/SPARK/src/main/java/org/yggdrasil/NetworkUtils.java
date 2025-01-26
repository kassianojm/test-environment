package org.yggdrasil;
import java.net.*;
import java.util.*;

public class NetworkUtils {

    /**
     * Return a hasmap containing for each interface every available IP adress
     */
    public static HashMap<String, ArrayList<InetAddress>> getInteAdresses() throws SocketException {
        HashMap<String, ArrayList<InetAddress>> ret = new HashMap<String, ArrayList<InetAddress>>();

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)){
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            ArrayList<InetAddress> intes = new ArrayList<InetAddress>();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if(!inetAddress.isAnyLocalAddress() &&
                        !inetAddress.isLoopbackAddress() &&
                        !inetAddress.isLinkLocalAddress()){
                    intes.add(inetAddress);
                }
            }
            if(intes.size() > 0)
                ret.put(netint.getName(), intes);
        }
        return ret;
    }
}
