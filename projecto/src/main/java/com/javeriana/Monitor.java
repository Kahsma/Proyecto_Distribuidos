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

// Definición de la clase Monitor
public class Monitor {

    // Definición de opciones de línea de comandos usando args4j
    @Option(name = "-t", aliases = "--monitorType", required = true, usage = "Monitor type (oxygeno, ph, or temperatura)")
    private SensorType monitorType;

    // Método principal
    public static void main(String[] args) {
        Monitor monitor = new Monitor();// Crear una instancia de la clase Monitor
        CmdLineParser parser = new CmdLineParser(monitor);// Crear un parser para analizar argumentos de línea de
                                                          // comandos

        try {
            // Analizar los argumentos de línea de comandos
            parser.parseArgument(args);

            // Imprimir el tipo de monitor obtenido de los argumentos
            System.out.println("Monitor Type: " + monitor.monitorType);

            // Crear un contexto ZeroMQ
            try (ZContext context = new ZContext()) {

                // Crear un socket ZeroMQ de tipo SUB para recibir mensajes del broker
                org.zeromq.ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
                subscriber.connect("tcp://192.168.0.4:5560"); // IP y puerto del broker
                String monitorTypeString = monitor.monitorType.toString() + "#";
                subscriber.subscribe(monitorTypeString.getBytes());
                System.out.println("Monitor is monitoring " + monitor.monitorType);

                // Crear un socket ZeroMQ de tipo PUSH para enviar alertas al "sistema de
                // calidad"
                org.zeromq.ZMQ.Socket calidadSocket = context.createSocket(SocketType.PUSH);
                calidadSocket.connect("tcp://192.168.0.4:5555"); // IP y puerto del "sistema de calidad"

                // Inicializar ObjectMapper de Jackson
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

                // Crear o cargar un archivo JSON
                File jsonFile = new File(monitor.monitorType + "_data.json");
                List<MeasurementData> measurementDataList;

                if (jsonFile.exists()) {
                    // Si el archivo existe, cargar datos existentes
                    measurementDataList = objectMapper.readValue(jsonFile,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, MeasurementData.class));
                } else {
                    // Si el archivo no existe, crear una nueva lista
                    measurementDataList = new ArrayList<>();
                }

                // Crear un Poller para verificar eventos de socket
                ZMQ.Poller poller = context.createPoller(2);
                poller.register(subscriber, ZMQ.Poller.POLLIN);

                // Crear un socket ZeroMQ de tipo REP para manejar chequeos de salu
                org.zeromq.ZMQ.Socket healthCheckSocket = context.createSocket(SocketType.REP);
                int healthCheckPort = getHealthCheckPort(monitor.monitorType);
                healthCheckSocket.bind("tcp://*:" + healthCheckPort); // Bind to a port determined by the sensor type
                poller.register(healthCheckSocket, ZMQ.Poller.POLLIN);

                while (!Thread.currentThread().isInterrupted()) {
                    // Verificar solicitudes de chequeo de salud
                    if (poller.poll(1000) > 0) {
                        if (poller.pollin(1)) {
                            String healthCheckRequest = healthCheckSocket.recvStr(0);// Enlazar a un puerto determinado
                                                                                     // por el tipo de sensor
                            System.out.println("Received health check request: " + healthCheckRequest);

                            // Responder con un acuse de recibo (OK) se utilizo PING PONG para el chequeo de
                            // salud
                            healthCheckSocket.send("OK", 0);
                        }
                    }

                    // Recibir datos del sensor desde el broker
                    if (poller.pollin(0)) {
                        String message = subscriber.recvStr(0);
                        System.out.println("Monitor received: " + message);

                        // Analizar el mensaje
                        String[] parts = message.split("#");
                        if (parts.length == 3) {
                            String sensorType = parts[0];
                            double measurement = Double.parseDouble(parts[1]);
                            String timestamp = parts[2];

                            // Verificar valor no válido (-1)
                            if (measurement == -1.0) {
                                // Enviar alerta al "sistema de calidad"
                                sendAlertToSistemaDeCalidad(calidadSocket, sensorType, timestamp);
                            }

                            // Agregar los datos a la lista
                            measurementDataList.add(new MeasurementData(sensorType, measurement, timestamp));

                            // Actualizar el archivo JSON
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

    // Método para enviar una alerta al "sistema de calidad"
    private static void sendAlertToSistemaDeCalidad(org.zeromq.ZMQ.Socket calidadSocket, String sensorType,
            String timestamp) {
        // Enviar la alerta al "sistema de calidad
        String alertMessage = sensorType + "#" + timestamp;
        calidadSocket.send(alertMessage.getBytes(ZMQ.CHARSET), 0);
        System.out.println("Sent alert to 'sistema de calidad': SensorType=" + sensorType + ", Timestamp=" + timestamp);
    }

    // Método para obtener el puerto de chequeo de salud según el tipo de sensor
    private static int getHealthCheckPort(SensorType sensorType) {
        // Determinar el puerto de chequeo de salud según el tipo de sensor
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
