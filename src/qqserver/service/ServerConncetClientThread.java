package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author 田磊
 * @version 1.0
 * @date 2024/8/30
 * @time 18:57
 */
public class ServerConncetClientThread extends Thread {
    private String userId;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public ServerConncetClientThread(String userId, Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.userId = userId;
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return oos;
    }

    /**
     * 检查和处理接收方无效和离线的情况
     * @param msg
     * @return 接收方无效，或离线， 返回true, 其它返回false
     * @throws IOException
     */
    private boolean checkGetterInvalidAndOffline(Message msg) throws IOException {
        // 验证接收方是不是有效用户
        if (! QQServer.checkGetterValid(msg.getGetter())) {
            // 用户不存在，不是返回一个MESSAGE_INVILID_GETTER类型的消息
            Message invalidGetterMsg = new Message();
            invalidGetterMsg.setMsgType(MessageType.MESSAGE_INVALID_GETTER);
            invalidGetterMsg.setSender("服务器");
            oos.writeObject(invalidGetterMsg);
            // 服务器通知后就跳过后续处理，进行下次循环了
            return true;
        }

        if (! ServerConnectClientThreadManager.checkGetterOnline(msg.getGetter())) {
            // 用户有效，但是用户不在线
            // 将消息保存起来
            QQServer.addOfflineMsgToDb(msg.getGetter(), msg);
            // 服务器保存离线消息后就跳过后续处理，进行下次循环了
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        // 通信要保持，所以采用while循环
        while (true) {
            try {
                System.out.println("服务端和客户端" + userId + " 保持通信，读取数据...");
                Message msg = (Message) ois.readObject(); // 没有消息就阻塞

                // 根据收到的消息类型，选择不同的处理方式
                if (MessageType.MESSAGE_GET_ONLINE_FRIENDS.equals(msg.getMsgType())) {
                    System.out.println("用户" + msg.getSender() + "请求在线好友列表...");
                    // 获取在线好友列表
                    String onlineFriendsList = ServerConnectClientThreadManager.getOnlineUserList();
                    // 创建一个MESSAGE_RET_ONLINE_FRIENDS类型的消息返回
                    Message msg2 = new Message();
                    msg2.setMsgType(MessageType.MESSAGE_RET_ONLINE_FRIENDS);
                    msg2.setGetter(msg.getSender());
                    msg2.setContent(onlineFriendsList);
                    oos.writeObject(msg2);
                } else if (MessageType.MESSAGE_CLIENT_EXIT.equals(msg.getMsgType())) {
                    System.out.println("用户" + userId + "退出");
                    // 从线程集合中删除
                    ServerConnectClientThreadManager.removeServerConnectClientThread(userId);
                    // 关闭连接
                    socket.close();
                    break; // 退出线程
                } else if (MessageType.MESSAGE_COMMON_MSG.equals(msg.getMsgType())) {
                    // 检查接收方是否无效和离线，函数内部也进行了相应的处理
                    if (checkGetterInvalidAndOffline(msg)) {
                        // 接收方无效和离线，跳过本次循环
                        continue;
                    }
                    // 当前线程是服务器与消息发送方通信的socket
                    // 要根据消息中的getter字段，获取服务器与消息接收方通信的socket关联的输出流
                    ObjectOutputStream toGetterOos = ServerConnectClientThreadManager
                            .getServerConnectClientThread(msg.getGetter()).getObjectOutputStream();
                    // 通过该Socket转发给消息接收方
                    // 提示如果客户不在线，可以保存到数据库，这样就可以实现离线留言, 这里不考虑不在线
                    toGetterOos.writeObject(msg);
                } else if (MessageType.MESSAGE_TO_ALL_MSG.equals(msg.getMsgType())) {
                    System.out.println(msg.getSender() + "对大家说：" + msg.getContent());
                    // 遍历所有与在线用户连接的线程，排除消息发送方
                    Collection<ServerConncetClientThread> scctALL = ServerConnectClientThreadManager.getHm().values();
                    Iterator<ServerConncetClientThread> iterator = scctALL.iterator();
                    for (ServerConncetClientThread scct : scctALL) {
                        if (scct.userId != msg.getSender()) {
                            scct.getObjectOutputStream().writeObject(msg); // 使用线程中创建的唯一oos实例转发msg
                        }
                    }
                } else if (MessageType.MESSAGE_FILE_MSG.equals(msg.getMsgType())) {
                    // 检查接收方是否无效和离线，函数内部也进行了相应的处理
                    if (checkGetterInvalidAndOffline(msg)) {
                        // 接收方无效和离线，跳过本次循环
                        continue;
                    }
                    // 服务器转发
                    ObjectOutputStream toGetterOos = ServerConnectClientThreadManager
                            .getServerConnectClientThread(msg.getGetter()).getObjectOutputStream();
                    toGetterOos.writeObject(msg);
                } else {
                    System.out.println("暂时无该消息类型的处理方式");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
