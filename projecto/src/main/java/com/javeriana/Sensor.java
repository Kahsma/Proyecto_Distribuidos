package com.javeriana;

import org.zeromq.ZMQ;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class Sensor {
    public static void main(String[] args) {
        String localIpAddress = getLocalIpAddress();
        if (localIpAddress != null) {
            ZMQ.Context context = ZMQ.context(1);
            ZMQ.Socket publisher = context.socket(ZMQ.PUB);
            publisher.bind("tcp://" + localIpAddress + ":5556");

            // Simular mediciones y publicarlas
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    double measurement = generateMeasurement();
                    publisher.send(String.valueOf(measurement), 0);
                    Thread.sleep(1000); // Simular un intervalo de tiempo
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                publisher.close();
                context.term();
            }
        } else {
            System.out.println("No se pudo obtener la dirección IP de la red local.");
        }
    }

    // Método para simular mediciones
    private static double generateMeasurement() {
        // Implementa la lógica de generación de mediciones aquí
        return 68 + Math.random() * 21; // Ejemplo: rango de temperatura
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
