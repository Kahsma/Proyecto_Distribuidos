package com.javeriana;

import org.zeromq.ZMQ;

public class Monitor {
    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket subscriber = context.socket(ZMQ.SUB);

        // Conéctate al canal del sensor
        subscriber.connect("tcp://localhost:5556");
        subscriber.subscribe("".getBytes());

        while (!Thread.currentThread().isInterrupted()) {
            // Recibe y procesa la medición
            String measurement = subscriber.recvStr();
            System.out.println("Medición recibida: " + measurement);

            // Realiza validaciones y genera alarmas si es necesario
            // Implementa la lógica de validación aquí
        }

        subscriber.close();
        context.term();
    }
}