package com.javeriana;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Sensor {

    @Option(name = "-t", aliases = "--sensorType", required = true, usage = "Sensor type (oxygen, ph, or temperature)")
    private SensorType sensorType;

    @Option(name = "-i", aliases = "--interval", required = true, usage = "Time interval in milliseconds")
    private int interval;

    @Option(name = "-c", aliases = "--configFile", required = true, usage = "Configuration file name")
    private String configFile;

    public static void main(String[] args) {
        Sensor sensor = new Sensor();
        CmdLineParser parser = new CmdLineParser(sensor);

        try {
            // Parse the command-line arguments
            parser.parseArgument(args);

            // Use the parsed values
            System.out.println("Sensor Type: " + sensor.sensorType);
            System.out.println("Interval: " + sensor.interval + " milliseconds");
            System.out.println("Config File: " + sensor.configFile);

            // Read configuration file
            double[] probabilities = readConfigFile(sensor.configFile);

            try (ZContext context = new ZContext()) {
                ZMQ.Socket socket = context.createSocket(SocketType.REQ);
                socket.connect("tcp://localhost:5559");

                while (!Thread.currentThread().isInterrupted()) {
                    // Generate sensor measurement
                    double measurement = generateMeasurement(sensor.sensorType, probabilities);
                    // Get the current date and time in a specific format
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String timestamp = now.format(formatter);

                    // Send sensor measurement to broker
                    String message = sensor.sensorType + "#" + measurement + "#" + timestamp;
                    System.out.println(sensor.sensorType + " sending: " + message);
                    socket.send(message.getBytes(ZMQ.CHARSET));
                    byte[] reply = socket.recv(0);
                    System.out.println(sensor.sensorType + " received: " + new String(reply, ZMQ.CHARSET));

                    // Wait for the specified interval before sending the next message
                    Thread.sleep(sensor.interval);
                }
            } catch (InterruptedException e) {
                // Handle the exception
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    private static double[] readConfigFile(String configFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            double[] probabilities = new double[3];
            for (int i = 0; i < 3; i++) {
                probabilities[i] = Double.parseDouble(reader.readLine().split(" ")[0]);
            }
            return probabilities;
        }
    }

    private static double generateMeasurement(SensorType sensorType, double[] probabilities) {
        Random random = new Random();
        double value;
        double randomValue = random.nextDouble();

        // Check which probability range the random value falls into
        if (randomValue < probabilities[0]) {
            // Values within the range
            value = generateValueWithinRange(sensorType);
        } else if (randomValue < probabilities[0] + probabilities[1]) {
            // Values outside the range
            value = generateValueOutsideRange(sensorType);
        } else {
            // Invalid values
            value = generateInvalidValue();
        }

        return value;
    }

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
        // For simplicity, generating a fixed invalid value for all sensor types
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
            // Generate a value below the minimum
            value = min - range * randomMultiplier;
        } else {
            // Generate a value above the maximum
            value = max + range * randomMultiplier;
        }
        return value;
    }
}
