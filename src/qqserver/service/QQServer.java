package qqserver.service;

import qqcommon.Message;
import qqcommon.MessageType;
import qqcommon.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 田磊
 * @version 1.0
 * @date 2024/8/30
 * @time 18:08
 * 该类用于处理用户连接、验证连接和保持连接
 */
public class QQServer {
    private ServerSocket serverSocket = null;
    //创建一个集合，存放多个用户，如果是这些用户登录，就认为是合法的
    //这里我们也可以使用 ConcurrentHashMap, 可以处理并发的集合，没有线程安全
    //HashMap 没有处理线程安全，因此在多线程情况下是不安全
    //ConcurrentHashMap 处理的线程安全,即线程同步处理, 在多线程情况下是安全的
    private static ConcurrentHashMap<String, User> validUsers = new ConcurrentHashMap<>();

    static {//在静态代码块，初始化 validUsers

        validUsers.put("100", new User("100", "123456"));
        validUsers.put("200", new User("200", "123456"));
        validUsers.put("300", new User("300", "123456"));
        validUsers.put("至尊宝", new User("至尊宝", "123456"));
        validUsers.put("紫霞仙子", new User("紫霞仙子", "123456"));
        validUsers.put("菩提老祖", new User("菩提老祖", "123456"));

    }

    public static boolean checkGetterValid(String getterId) {
        return validUsers.containsKey(getterId);
    }

    private boolean validateUser(String userId, String pwd) {
        User user = validUsers.get(userId);
        if (user == null) { // 没有该用户
            return false;
        }
        if (!(user.getPwd().equals(pwd))) { // 用户存在，密码错误
            return false;
        }
        return true;
    }

    // 创建一个map保存离线用户的消息
    private static ConcurrentHashMap<String, ArrayList<Message>> offlineDb = new ConcurrentHashMap<>();
    // 添加离线消息
    public static void addOfflineMsgToDb(String getterId, Message msg) {
        if (!offlineDb.containsKey(getterId)) {
            // 初始化
            ArrayList<Message> msgList = new ArrayList<>();
            msgList.add(msg);
            offlineDb.put(getterId, msgList);
        } else {
            offlineDb.get(getterId).add(msg);
        }
    }

    public QQServer() {
        Socket socket = null;
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;

        // 创建连接套接字, 端口也可以写在配置文件
        try {
            serverSocket = new ServerSocket(9999);
            System.out.println("服务器监听9999端口，等待连接...");

            // 创建服务器推送线程，并启动
            new Thread(new SendToAllOnlineUserService()).start();

            // 由于允许多个客户端连接，这里用while
            while (true) {
                socket = serverSocket.accept();
                // 获取socket对象关联的输入流、输出流
                ois = new ObjectInputStream(socket.getInputStream());
                oos = new ObjectOutputStream(socket.getOutputStream());

                // 首先获取客户端发来的用户id、密码
                User user = (User) ois.readObject();
                // 创建一个Message对象，用于回复客户端
                Message msg = new Message();
                if (validateUser(user.getUserId(), user.getPwd())) { // 身份验证通过
                    msg.setMsgType(MessageType.MESSAGE_LOGIN_SUCCEED);
                    oos.writeObject(msg);

                    // 离线数据库有离线消息，逐个发送, 发送完之后从数据库删除
                    if (offlineDb.containsKey(user.getUserId())) {
                        for (Message offlineMsg : offlineDb.get(user.getUserId())) {
                            oos.writeObject(offlineMsg);
                        }
                        offlineDb.remove(user.getUserId());
                    }

                    // 创建一个类ServerConnectClientThread，用于保持和客户端该用户的连接
                    ServerConncetClientThread serverConncetClientThread =
                            new ServerConncetClientThread(user.getUserId(), socket, oos, ois);
                    // 启动该线程
                    serverConncetClientThread.start();
                    // 将该线程放入一个集合里，方便管理
                    ServerConnectClientThreadManager.addServerConnectClientThread(user.getUserId(), serverConncetClientThread);
                } else { // 身份验证失败
                    System.out.println("用户 id=" + user.getUserId() + " pwd=" + user.getPwd() + " 验证失败");
                    msg.setMsgType(MessageType.MESSAGE_LOGIN_FAIL);
                    oos.writeObject(msg);
                    socket.close();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
