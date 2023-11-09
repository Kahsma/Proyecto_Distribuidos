package com.javeriana;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                Socket subscriber = context.createSocket(SocketType.SUB);
                subscriber.connect("tcp://localhost:5560");// IP DE DONDE SE ESTA CORRIENDO EL BROKER
                String monitorTypeString = monitor.monitorType.toString() + "#";
                subscriber.subscribe(monitorTypeString.getBytes());
                System.out.println("Monitor is monitoring " + monitor.monitorType);

                // Initialize Jackson ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                // Create or load JSON file
                File jsonFile = new File(monitor.monitorType + "_data.json");
                List<MeasurementData> measurementDataList;

                if (jsonFile.exists()) {
                    // If file exists, load existing data
                    measurementDataList = objectMapper.readValue(jsonFile,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, MeasurementData.class));
                } else {
                    // If file doesn't exist, create a new list
                    measurementDataList = new ArrayList<>();
                }

                while (true) {
                    String message = subscriber.recvStr(0);
                    System.out.println("Monitor received: " + message);

                    // Parse the message
                    String[] parts = message.split("#");
                    if (parts.length == 3) {
                        String sensorType = parts[0];
                        double measurement = Double.parseDouble(parts[1]);
                        String timestamp = parts[2];

                        // Add the data to the list
                        measurementDataList.add(new MeasurementData(sensorType, measurement, timestamp));

                        // Update the JSON file
                        objectMapper.writeValue(jsonFile, measurementDataList);
                    } else {
                        System.out.println("Invalid message format");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}
