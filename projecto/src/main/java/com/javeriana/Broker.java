package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

// Definición de la clase Broker
public class Broker {
    public static void main(String[] args) throws Exception {
        try (ZContext context = new ZContext()) {
            // Socket para enfrentar a los clientes (sensores)
            Socket frontend = context.createSocket(SocketType.REP);
            frontend.bind("tcp://localhost:5559");

            // Socket para publicar mensajes a los suscriptores (monitores)
            Socket backend = context.createSocket(SocketType.PUB);
            backend.bind("tcp://192.168.0.4:5560");// IP DE DONDE SE ESTA CORRIENDO EL BROKER

            System.out.println("Launch and connect broker.");

            while (!Thread.currentThread().isInterrupted()) {

                // Recibir mensaje del cliente (sensor)
                byte[] reply = frontend.recv(0);

                // Enviar el mensaje al backend (suscriptores)
                backend.send(reply, 0);

                // Imprimir el mensaje recibido del cliente (sensor)
                String messageReceived = new String(reply, ZMQ.CHARSET);
                System.out.println("Received from client: " + messageReceived);

                // Manejar las solicitudes entrantes de los sensores

                // Enviar una respuesta al cliente (sensor)
                frontend.send("nominal", 0);
            }
        }
    }
}
