package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class Monitor {
    public static void main(String[] args) throws InterruptedException {
        try (ZContext context = new ZContext()) {
            Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5559");

            String monitorType = "Sensor1";
            subscriber.subscribe(monitorType.getBytes());

            System.out.println("Monitor1 is monitoring " + monitorType);

            while (true) {
                String message = subscriber.recvStr(0);
                System.out.println("Monitor1 received: " + message);
            }
        }
    }
}
