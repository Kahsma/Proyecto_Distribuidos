package com.javeriana;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// ... (existing imports)

public class Monitor {

    @Option(name = "-t", aliases = "--monitorType", required = true, usage = "Monitor type (oxygeno, ph, or temperatura)")
    private SensorType monitorType;

    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        CmdLineParser parser = new CmdLineParser(monitor);

        try {
            // Parse the command-line arguments
            parser.parseArgument(args);

            // Use the parsed monitorType
            System.out.println("Monitor Type: " + monitor.monitorType);

            try (ZContext context = new ZContext()) {
                // ZeroMQ socket to receive messages from the broker
                org.zeromq.ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
                subscriber.connect("tcp://192.168.0.4:5560"); // Update with your broker's IP
                String monitorTypeString = monitor.monitorType.toString() + "#";
                subscriber.subscribe(monitorTypeString.getBytes());
                System.out.println("Monitor is monitoring " + monitor.monitorType);

                // ZeroMQ socket to send alerts to "sistema de calidad"
                org.zeromq.ZMQ.Socket calidadSocket = context.createSocket(SocketType.PUSH);
                calidadSocket.connect("tcp://192.168.0.4:5555"); // Update with "sistema de calidad" IP and port

                // Initialize Jackson ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                // Create or load JSON file
                File jsonFile = new File(monitor.monitorType + "_data.json");
                List<MeasurementData> measurementDataList;

                if (jsonFile.exists()) {
                    // If the file exists, load existing data
                    measurementDataList = objectMapper.readValue(jsonFile,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, MeasurementData.class));
                } else {
                    // If the file doesn't exist, create a new list
                    measurementDataList = new ArrayList<>();
                }

                // Create a Poller to check for socket events
                ZMQ.Poller poller = context.createPoller(2);
                poller.register(subscriber, ZMQ.Poller.POLLIN);

                // ZeroMQ socket to handle health checks
                org.zeromq.ZMQ.Socket healthCheckSocket = context.createSocket(SocketType.REP);
                int healthCheckPort = getHealthCheckPort(monitor.monitorType);
                healthCheckSocket.bind("tcp://*:" + healthCheckPort); // Bind to a port determined by the sensor type
                poller.register(healthCheckSocket, ZMQ.Poller.POLLIN);

                while (!Thread.currentThread().isInterrupted()) {
                    // Check for health check request
                    if (poller.poll(1000) > 0) {
                        if (poller.pollin(1)) {
                            String healthCheckRequest = healthCheckSocket.recvStr(0);
                            System.out.println("Received health check request: " + healthCheckRequest);

                            // Respond with acknowledgment
                            healthCheckSocket.send("OK", 0);
                        }
                    }

                    // Receive sensor data
                    if (poller.pollin(0)) {
                        String message = subscriber.recvStr(0);
                        System.out.println("Monitor received: " + message);

                        // Parse the message
                        String[] parts = message.split("#");
                        if (parts.length == 3) {
                            String sensorType = parts[0];
                            double measurement = Double.parseDouble(parts[1]);
                            String timestamp = parts[2];

                            // Check for invalid value (-1)
                            if (measurement == -1.0) {
                                // Send alert to "sistema de calidad"
                                sendAlertToSistemaDeCalidad(calidadSocket, sensorType, timestamp);
                            }

                            // Add the data to the list
                            measurementDataList.add(new MeasurementData(sensorType, measurement, timestamp));

                            // Update the JSON file
                            objectMapper.writeValue(jsonFile, measurementDataList);
                        } else {
                            System.out.println("Invalid message format");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }

    private static void sendAlertToSistemaDeCalidad(org.zeromq.ZMQ.Socket calidadSocket, String sensorType,
            String timestamp) {
        // Send the alert to "sistema de calidad"
        String alertMessage = sensorType + "#" + timestamp;
        calidadSocket.send(alertMessage.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Sent alert to 'sistema de calidad': SensorType=" + sensorType + ", Timestamp=" + timestamp);
    }

    private static int getHealthCheckPort(SensorType sensorType) {
        // Determine the health check port based on the sensor type
        switch (sensorType) {
            case temperatura:
                return 5562;
            case ph:
                return 5563;
            case oxygeno:
                return 5564;
            default:
                throw new IllegalArgumentException("Invalid sensor type");
        }
    }
}
