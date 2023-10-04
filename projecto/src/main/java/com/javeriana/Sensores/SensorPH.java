package com.javeriana.Sensores;

import org.zeromq.ZMQ;

public class SensorPH {
    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket publisher = context.socket(ZMQ.PUB);
        publisher.bind("tcp://*:5557"); // Escuchar en todas las interfaces de red

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
    }

    // Método para simular mediciones
    private static double generateMeasurement() {
        // Implementa la lógica de generación de mediciones aquí
        return 68 + Math.random() * 21; // Ejemplo: rango de temperatura
    }
}