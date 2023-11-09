package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class Broker {
    public static void main(String[] args) throws Exception {
        try (ZContext context = new ZContext()) {
            // Socket facing clients
            Socket frontend = context.createSocket(SocketType.REP);
            frontend.bind("tcp://*:5559");

            // Socket for publishing messages to subscribers
            Socket backend = context.createSocket(SocketType.PUB);
            backend.bind("tcp://localhost:5560");// IP DE DONDE SE ESTA CORRIENDO EL BROKER

            System.out.println("Launch and connect broker.");

            while (!Thread.currentThread().isInterrupted()) {

                byte[] reply = frontend.recv(0);
                backend.send(reply, 0);
                String messageReceived = new String(reply, ZMQ.CHARSET);
                System.out.println("Received from client: " + messageReceived);
                // Handle incoming requests from sensors

                frontend.send("nominal", 0);
            }
        }
    }
}
