package com.javeriana;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.zeromq.ZMQ;

public class Monitor {
    public static void main(String[] args) {
        String localIpAddress = getLocalIpAddress();
        if (localIpAddress != null) {
            ZMQ.Context context = ZMQ.context(1);
            ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
            subscriber.connect("tcp://" + localIpAddress + ":5556");
            subscriber.subscribe("".getBytes());

            // Inicia la recepción y procesamiento de mediciones
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String measurement = subscriber.recvStr();
                    processMeasurement(measurement);
                }
            } finally {
                subscriber.close();
                context.term();
            }
        } else {
            System.out.println("No se pudo obtener la dirección IP de la red local.");
        }
    }

    // Método para procesar mediciones
    private static void processMeasurement(String measurement) {
        // Implementa la lógica de procesamiento de mediciones aquí
        System.out.println("Medición recibida: " + measurement);
    }

    // Método para obtener la dirección IP de la red local
    private static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
}
