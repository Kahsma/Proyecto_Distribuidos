package com.javeriana;

import org.kohsuke.args4j.CmdLineParser;

import org.kohsuke.args4j.Option;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

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

                while (true) {
                    String message = subscriber.recvStr(0);
                    System.out.println("Monitor received: " + message);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
    }
}
