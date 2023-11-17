package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

public class SistemaDeCalidad {

    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            // ZeroMQ socket to receive alerts from monitors
            ZMQ.Socket calidadSocket = context.createSocket(SocketType.PULL);
            calidadSocket.bind("tcp://192.168.0.4:5555"); // Update with the correct IP and port

            System.out.println("Sistema de Calidad is ready to receive alerts.");

            while (true) {
                // Receive and display the alert message
                byte[] alertMessageBytes = calidadSocket.recv(0);
                String alertMessage = new String(alertMessageBytes, StandardCharsets.UTF_8);
                System.out.println("Received alert: " + alertMessage);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
