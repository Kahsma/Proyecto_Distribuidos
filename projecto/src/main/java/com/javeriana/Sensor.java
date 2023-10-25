package com.javeriana;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Sensor {

    @Option(name = "-t", aliases = "--sensorType", required = true, usage = "Sensor type (oxygeno, ph, or temperatura)")
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

            try (ZContext context = new ZContext()) {
                ZMQ.Socket socket = context.createSocket(SocketType.REQ);
                socket.connect("tcp://localhost:5559");

                while (!Thread.currentThread().isInterrupted()) {
                    String message = sensor.sensorType + "#" + "Hello from " + sensor.sensorType;
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
}
