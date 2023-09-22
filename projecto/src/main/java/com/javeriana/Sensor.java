package com.javeriana;

import org.zeromq.ZMQ;
import java.util.Random;

public class Sensor {
    public static void main(String[] args) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket publisher = context.socket(ZMQ.PUB);
        publisher.bind("tcp://*:5556");

        Random random = new Random();

        while (!Thread.currentThread().isInterrupted()) {
            // Simular mediciones aleatorias
            double measurement = 68 + (random.nextDouble() * 21); // Rango de temperatura

            // Publicar la medición en el canal
            publisher.send(String.valueOf(measurement), 0);
        }

        publisher.close();
        context.term();
    }
}
