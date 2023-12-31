package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

// Definición de la clase SistemaDeCalidad
public class SistemaDeCalidad {

    // Método principal
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            // Socket ZeroMQ para recibir alertas de los monitores
            ZMQ.Socket calidadSocket = context.createSocket(SocketType.PULL);
            calidadSocket.bind("tcp://192.168.0.4:5555"); // IP y puerto del "sistema de calidad"

            System.out.println("Sistema de Calidad is ready to receive alerts.");

            while (true) {
                // Recibir y mostrar el mensaje de alerta
                byte[] alertMessageBytes = calidadSocket.recv(0);
                String alertMessage = new String(alertMessageBytes, StandardCharsets.UTF_8);
                System.out.println("Received alert: " + alertMessage);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
