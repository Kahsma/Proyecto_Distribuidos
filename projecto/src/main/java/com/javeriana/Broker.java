package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class Broker {
    public static void main(String[] args) throws Exception {
        try (ZContext context = new ZContext()) {
            Socket frontend = context.createSocket(SocketType.PUB);
            Socket backend = context.createSocket(SocketType.SUB);
            frontend.bind("tcp://*:5559");
            backend.connect("tcp://localhost:5560");

            frontend.setHWM(0); // Set the high-water mark to prevent message loss

            System.out.println("Launch and connect broker.");

            // Subscribe to specific topics for each monitor
            backend.subscribe("Sensor1".getBytes());
            backend.subscribe("Monitor2".getBytes());
            backend.subscribe("Monitor3".getBytes());

            while (!Thread.currentThread().isInterrupted()) {
                // Receive a message from subscribers and publish it
                byte[] message = backend.recv(0);
                frontend.send(message);
            }
        }
    }
}
