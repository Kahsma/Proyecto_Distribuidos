package com.javeriana;

// Importaciones de las bibliotecas necesarias
import org.kohsuke.args4j.CmdLineParser; // Para analizar argumentos de línea de comandos
import org.kohsuke.args4j.Option; // Para definir opciones de línea de comandos
import org.zeromq.SocketType; // Para el tipo de socket ZeroMQ
import org.zeromq.ZContext; // Para el contexto ZeroMQ
import org.zeromq.ZMQ; // Para la comunicación ZeroMQ

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Definición de la clase principal Sensor
public class Sensor {

    // Definición de opciones de línea de comandos usando args4j
    @Option(name = "-t", aliases = "--sensorType", required = true, usage = "Sensor type (oxygen, ph, or temperature)")
    private SensorType sensorType;

    @Option(name = "-i", aliases = "--interval", required = true, usage = "Time interval in milliseconds")
    private int interval;

    @Option(name = "-c", aliases = "--configFile", required = true, usage = "Configuration file name")
    private String configFile;

    // Método principal
    public static void main(String[] args) {
        Sensor sensor = new Sensor();// Crear una instancia de la clase Sensor
        CmdLineParser parser = new CmdLineParser(sensor);// Crear un parser para analizar argumentos de línea de
                                                         // comandos

        try {

            parser.parseArgument(args);// Analizar los argumentos de línea de comandos

            // Analizar los argumentos de línea de comandos
            System.out.println("Sensor Type: " + sensor.sensorType);
            System.out.println("Interval: " + sensor.interval + " milliseconds");
            System.out.println("Config File: " + sensor.configFile);

            // Leer el archivo de configuración
            double[] probabilities = readConfigFile(sensor.configFile);

            // Crear un contexto ZeroMQ y un socket de tipo REQ
            try (ZContext context = new ZContext()) {
                ZMQ.Socket socket = context.createSocket(SocketType.REQ);
                socket.connect("tcp://localhost:5559");

                // Bucle principal del sensor
                while (!Thread.currentThread().isInterrupted()) {

                    // Generar una medida del sensor
                    double measurement = generateMeasurement(sensor.sensorType, probabilities);

                    // Obtener la fecha y hora actual en un formato específico
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String timestamp = now.format(formatter);

                    // Enviar la medida del sensor al servidor
                    String message = sensor.sensorType + "#" + measurement + "#" + timestamp;
                    System.out.println(sensor.sensorType + " sending: " + message);
                    socket.send(message.getBytes(ZMQ.CHARSET));
                    byte[] reply = socket.recv(0);
                    System.out.println(sensor.sensorType + " received: " + new String(reply, ZMQ.CHARSET));

                    // Esperar el intervalo especificado antes de enviar el próximo mensaje
                    Thread.sleep(sensor.interval);
                }
            } catch (InterruptedException e) {
                // Manejar la excepción de interrupción
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    // Método para leer el archivo de configuración
    private static double[] readConfigFile(String configFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            double[] probabilities = new double[3];
            for (int i = 0; i < 3; i++) {
                probabilities[i] = Double.parseDouble(reader.readLine().split(" ")[0]);
            }
            return probabilities;
        }
    }

    // Método para generar una medida del sensor
    private static double generateMeasurement(SensorType sensorType, double[] probabilities) {
        Random random = new Random();
        double value;
        double randomValue = random.nextDouble();

        // Verificar en qué rango de probabilidad cae el valor aleatorio
        if (randomValue < probabilities[0]) {
            // Valor dentro del rango
            value = generateValueWithinRange(sensorType);
        } else if (randomValue < probabilities[0] + probabilities[1]) {
            // Valor fuera del rango
            value = generateValueOutsideRange(sensorType);
        } else {
            // Valor inválido
            value = generateInvalidValue();
        }

        return value;
    }

    // Métodos para generar valores de medida según el tipo de sensor
    private static double generateValueWithinRange(SensorType sensorType) {
        switch (sensorType) {
            case temperatura:
                return generateRandomInRange(68, 89);
            case ph:
                return generateRandomInRange(6.0, 8.0);
            case oxygeno:
                return generateRandomInRange(2, 11);
            default:
                throw new IllegalArgumentException("Invalid sensor type");
        }
    }

    private static double generateValueOutsideRange(SensorType sensorType) {
        switch (sensorType) {
            case temperatura:
                return generateRandomOutsideRange(68, 89);
            case ph:
                return generateRandomOutsideRange(6.0, 8.0);
            case oxygeno:
                return generateRandomOutsideRange(2, 11);
            default:
                throw new IllegalArgumentException("Invalid sensor type");
        }
    }

    private static double generateInvalidValue() {
        // Se usa -1.0 para indicar que el valor es inválido
        return -1.0;
    }

    private static double generateRandomInRange(double min, double max) {
        Random random = new Random();
        return min + (max - min) * random.nextDouble();
    }

    private static double generateRandomOutsideRange(double min, double max) {
        Random random = new Random();
        double value;
        double range = max - min;
        double randomMultiplier = random.nextDouble();
        if (random.nextBoolean()) {
            // Generar un valor por debajo del mínimo
            value = min - range * randomMultiplier;
        } else {
            // Generar un valor por encima del máximo
            value = max + range * randomMultiplier;
        }
        return value;
    }
}
