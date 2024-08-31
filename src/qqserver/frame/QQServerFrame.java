package qqserver.frame;

import qqserver.service.QQServer;

/**
 * @author 田磊
 * @version 1.0
 * @date 2024/8/30
 * @time 19:18
 * 该类创建一个QQServer对象，启动后台服务
 */
public class QQServerFrame {
    public static void main(String[] args) {
        new QQServer();
    }
}
