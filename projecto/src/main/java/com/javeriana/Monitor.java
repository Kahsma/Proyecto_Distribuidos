package com.javeriana;

import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class Monitor {
    @Option(name = "-t", aliases = "--monitorType", required = true, usage = "Monitor type (oxygeno, ph, or temperatura)")
    private SensorType monitorType;

    public static void main(String[] args) throws InterruptedException {
        try (ZContext context = new ZContext()) {
            Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5560");

            String monitorType = "oxygen#";
            subscriber.subscribe(monitorType.getBytes());
            System.out.println("Monitor1 is monitoring " + monitorType);

            while (true) {
                String message = subscriber.recvStr(0);
                System.out.println("Monitor1 received: " + message);
            }
        }
    }
}
