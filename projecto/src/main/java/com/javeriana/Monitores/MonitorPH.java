package com.javeriana.Monitores;

import org.zeromq.ZMQ;

public class MonitorPH {
    public static void main(String[] args) {
        String sensorIpAddress = "192.168.0.3"; // Dirección IP del sensor
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket subscriber = context.socket(ZMQ.SUB);

        // Conéctate al sensor (asegúrate de utilizar la dirección y puerto correctos)
        subscriber.connect("tcp://" + sensorIpAddress + ":5557");

        // Suscríbete a todos los mensajes (en blanco)
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
    }

    // Método para procesar mediciones
    private static void processMeasurement(String measurement) {
        // Implementa la lógica de procesamiento de mediciones aquí
        System.out.println("Medición recibida en el monitor PH: " + measurement);
    }
}
