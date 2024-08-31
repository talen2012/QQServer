package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;
import qqserver.utils.Utility;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Date;

/**
 * @author 田磊
 * @version 1.0
 * @date 2024/8/31
 * @time 12:19
 * 这个类负责服务器推送线程
 */
public class SendToAllOnlineUserService implements Runnable {
    @Override
    public void run() {

        while (true) {
            System.out.println("请输入服务器要推送的新闻/消息[输入exit表示退出推送服务线程]：");
            String news = Utility.readString(100);
            if ("exit".equals(news)) {
                break;
            }

            // 打包Message对象
            Message msg = new Message();
            msg.setMsgType(MessageType.MESSAGE_SERVER_MSG);
            msg.setSender("服务器");
            msg.setContent(news);
            msg.setSndTime(new Date().toString());
            System.out.println("服务器推送消息给所有人 说: " + news);

            // 遍历服务器与在线用户连接的线程
            Collection<ServerConncetClientThread> ccstAll = ServerConnectClientThreadManager.getHm().values();
            for (ServerConncetClientThread ccst : ccstAll) {
                ObjectOutputStream oos = ccst.getObjectOutputStream();
                try {
                    oos.writeObject(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
