package qqserver.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author 田磊
 * @version 1.0
 * @date 2024/8/30
 * @time 19:10
 */
public class ServerConnectClientThreadManager {
    private static HashMap<String, ServerConncetClientThread> hm = new HashMap<> ();

    public static HashMap<String, ServerConncetClientThread> getHm() {
        return hm;
    }

    public static boolean checkGetterOnline(String getterId) {
        return hm.containsKey(getterId);
    }

    public static void addServerConnectClientThread(String userId, ServerConncetClientThread scct) {
        hm.put(userId, scct);
    }

    public static ServerConncetClientThread getServerConnectClientThread(String userId) {
        return hm.get(userId);
    }
    
    public static String getOnlineUserList() {
        Iterator<String> iterator = hm.keySet().iterator();
        String onlineUserList = "";
        while (iterator.hasNext()) {
            onlineUserList += (iterator.next() + " ");
        }
        return onlineUserList;
    }

    public static void removeServerConnectClientThread(String userId) {
        hm.remove(userId);
    }
}
