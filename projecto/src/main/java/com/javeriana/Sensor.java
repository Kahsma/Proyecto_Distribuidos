package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class Sensor {
    public static void main(String[] args) throws Exception {
        try (ZContext context = new ZContext()) {
            Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5560");

            String sensorType = "Sensor1";

            System.out.println(sensorType + " is publishing.");

            while (!Thread.currentThread().isInterrupted()) {
                publisher.send(sensorType + ": Hello from Sensor1!", 0);
                Thread.sleep(1000);
            }
        }
    }
}
