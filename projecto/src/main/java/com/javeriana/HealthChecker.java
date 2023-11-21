package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Definición de la clase HealthChecker
public class HealthChecker {

    private static final int CHECK_INTERVAL = 5000; // Intervalo en milisegundos

    private final ZContext context;// Contexto ZeroMQ
    private final Map<SensorType, Socket> healthCheckSockets;// Mapa de sockets de salud

    private static final Map<SensorType, String> MONITOR_BASE_ADDRESSES = new HashMap<>();

    // Direcciones base de los monitores asociadas a cada tipo de sensor
    static {
        MONITOR_BASE_ADDRESSES.put(SensorType.temperatura, "tcp://10.43.101.112");
        MONITOR_BASE_ADDRESSES.put(SensorType.ph, "tcp://10.43.101.124");
        MONITOR_BASE_ADDRESSES.put(SensorType.oxygeno, "tcp://10.43.101.124");
    }

    // Constructor del HealthChecker
    public HealthChecker(ZContext context) {
        this.context = context;
        this.healthCheckSockets = createHealthCheckSockets(context);
    }

    // Método principal para ejecutar el HealthChecker
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            HealthChecker healthChecker = new HealthChecker(context);

            // Iniciar el proceso de comprobación de salud
            healthChecker.startHealthCheck();

            // La comprobación de salud se ejecuta indefinidamente, por lo que esta línea
            // no se alcanza a menos que se interrumpa
            System.out.println("Health checking process interrupted. Closing the application.");
        }
    }

    public void startHealthCheck() {
        try {
            // Crear un Poller para verificar eventos de socket
            ZMQ.Poller poller = createPoller();

            while (!Thread.currentThread().isInterrupted()) {
                // Enviar solicitudes de comprobación de salud para cada tipo de monitor
                for (Map.Entry<SensorType, Socket> entry : healthCheckSockets.entrySet()) {
                    checkMonitor(entry.getValue(), entry.getKey());
                }

                // Utilizar el Poller para esperar una respuesta o un tiempo de espera (TIMEOUT)
                if (poller.poll(5000) > 0) {
                    // Si hay una respuesta, procesarla
                    for (int i = 0; i < poller.getSize(); i++) {
                        if (poller.pollin(i)) {
                            Socket socket = poller.getSocket(i);
                            SensorType sensorType = getSensorTypeBySocket(healthCheckSockets, socket);

                            // Verificar si el socket está listo para recibir
                            if (socket.getEvents() == ZMQ.Poller.POLLIN) {
                                processMonitorResponse(socket, sensorType);
                            } else {
                                System.out.println("Socket not ready for receiving");
                            }
                        }
                    }
                } else {
                    // Manejar el caso cuando no hay respuesta dentro del tiempo especificado
                    System.out.println("Health check timed out");
                }

                // Esperar al siguiente intervalo antes de la próxima comprobación
                try {
                    Thread.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    // Imprimir un mensaje cuando se interrumpe el hilo
                    System.out.println("Health check thread interrupted");
                    Thread.currentThread().interrupt(); // Re-interrumpir el hilo
                }

                // Actualizar las direcciones de los monitores
                updateMonitorAddresses();
                // Cerrar el poller y crear uno nuevo con las direcciones actualizadas
                poller.close();
                poller = createPoller();
            }
        } finally {
            // Cerrar los sockets de comprobación de salud
            healthCheckSockets.values().forEach(Socket::close);
        }
    }

    // Método para crear sockets de comprobación de salud para cada tipo de sensor
    private Map<SensorType, Socket> createHealthCheckSockets(ZContext context) {
        Map<SensorType, Socket> healthCheckSockets = new HashMap<>();

        for (Map.Entry<SensorType, String> entry : MONITOR_BASE_ADDRESSES.entrySet()) {
            SensorType sensorType = entry.getKey();
            String baseAddress = entry.getValue();

            int healthCheckPort = getHealthCheckPort(sensorType);
            String address = baseAddress + ":" + healthCheckPort;

            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect(address);
            healthCheckSockets.put(sensorType, socket);
        }

        return healthCheckSockets;
    }

    // Método para realizar la comprobación de salud para un monitor específico
    private void checkMonitor(Socket healthCheckSocket, SensorType sensorType) {
        // Antes de enviar una solicitud de comprobación de salud
        System.out.println("Health Check Socket State before send: " + healthCheckSocket.getEvents());

        boolean isUpdateNeeded = false;

        try {
            // Enviar una solicitud de comprobación de salud para un tipo específico de
            // monitor
            healthCheckSocket.send(sensorType.toString().getBytes(ZMQ.CHARSET), 0);
            System.out.println("Health Check Socket State after send: " + healthCheckSocket.getEvents());

            // Esperar un breve período antes de enviar la próxima solicitud de comprobación
            // de salud
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            /// Verificar si no hay respuesta dentro del tiempo especificado
            if (healthCheckSocket.getEvents() == ZMQ.Poller.POLLIN) {
                // Procesar la respuesta del monitor
                processMonitorResponse(healthCheckSocket, sensorType);
            } else {
                // Manejar el caso cuando no hay respuesta dentro del tiempo especificado
                System.out.println(
                        sensorType + " Monitor did not respond within the specified timeout. Starting a new process.");
                startMonitorProcess(sensorType);
                isUpdateNeeded = true;
            }
        } catch (ZMQException e) {
            // Manejar la excepción cuando el monitor no está disponible
            System.out.println("Error sending health check request to " + sensorType + " monitor: " + e.getMessage());

            // Iniciar un nuevo proceso para el monitor que no está disponible
            startMonitorProcess(sensorType);
            System.out.println(MONITOR_BASE_ADDRESSES.get(sensorType));
            isUpdateNeeded = true;
        }

        if (isUpdateNeeded) {
            // Actualizar las direcciones de los monitores después de iniciar un nuevo
            // proceso para el monitor que no está disponible
            updateMonitorAddresses(sensorType);
        }

        // Después de enviar una solicitud de comprobación de salud
        System.out.println("Health Check Socket State after send: " + healthCheckSocket.getEvents());
    }

    // Mapa que almacena las direcciones anteriores para cada tipo de sensor
    private Map<SensorType, String> previousAddresses = new HashMap<>();

    // Método para actualizar las direcciones de los monitores
    private void updateMonitorAddresses() {
        String ipAddress = "tcp://10.43.101.124"; // Ip de la maquina donde se esta corriendo el health checker

        for (SensorType sensorType : MONITOR_BASE_ADDRESSES.keySet()) {
            String previousAddress = previousAddresses.getOrDefault(sensorType, "");
            if (!previousAddress.equals(MONITOR_BASE_ADDRESSES.get(sensorType))) {
                System.out.println(
                        "Changing address for " + sensorType + " to: " + MONITOR_BASE_ADDRESSES.get(sensorType));
            }
            previousAddresses.put(sensorType, MONITOR_BASE_ADDRESSES.get(sensorType));
        }

        // Imprimir un mensaje al actualizar las direcciones
        System.out.println("Updated monitor addresses");

        // Recrear sockets de comprobación de salud con las direcciones actualizadas
        this.healthCheckSockets.values().forEach(Socket::close);
        this.healthCheckSockets.clear();
        this.healthCheckSockets.putAll(createHealthCheckSockets(context));

        // Imprimir los valores actuales de las direcciones después de la actualización
        for (SensorType sensorType : MONITOR_BASE_ADDRESSES.keySet()) {
            System.out.println("Current address for " + sensorType + ": " + MONITOR_BASE_ADDRESSES.get(sensorType));
        }
    }

    // Método para actualizar la dirección de un tipo de sensor específico
    private void updateMonitorAddresses(SensorType sensorType) {
        String ipAddress = "tcp://10.43.101.124"; // Ip de la maquina donde se esta corriendo el health checker

        MONITOR_BASE_ADDRESSES.put(sensorType, ipAddress);
    }

    // Método para iniciar un nuevo proceso de monitor para un tipo de sensor
    // específico
    private void startMonitorProcess(SensorType sensorType) {
        System.out.println("Starting a new process for " + sensorType);
        try {
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "mvn", "exec:java",
                        "-Dexec.mainClass=com.javeriana.Monitor", "-Dexec.args=-t " + sensorType.toString());
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                // Unix/Linux/MacOS
                processBuilder = new ProcessBuilder("sh", "-c",
                        "mvn exec:java -Dexec.mainClass=com.javeriana.Monitor -Dexec.args=-t " + sensorType.toString());
            } else {
                throw new UnsupportedOperationException("Unsupported operating system: " + os);
            }

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Opcionalmente, puedes esperar a que el proceso termine u hacer otro manejo
            // process.waitFor();
        } catch (IOException e) {
            System.err.println("Error starting a new process: " + e.getMessage());
        }
    }

    // Método para procesar la respuesta del monitor
    private void processMonitorResponse(Socket healthCheckSocket, SensorType sensorType) {
        // Si hay una respuesta, recibir y procesarla
        String response = healthCheckSocket.recvStr(0);
        if (response != null) {
            System.out.println(sensorType + " Monitor received: " + response);

        } else {
            // Manejar el caso cuando no hay respuesta dentro del tiempo especificado
            System.out.println(sensorType + " Monitor did not respond within the specified timeout");
        }
    }

    // Método para obtener el tipo de sensor asociado a un socket
    private SensorType getSensorTypeBySocket(Map<SensorType, Socket> sockets, Socket socket) {
        for (Map.Entry<SensorType, Socket> entry : sockets.entrySet()) {
            if (entry.getValue() == socket) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Socket not found for the given SensorType");
    }

    // Método para obtener el puerto de comprobación de salud asociado a un tipo de
    // monitor
    private int getHealthCheckPort(SensorType sensorType) {
        // Determinar el puerto de comprobación de salud según el tipo de sensor que
        // está asociado al monitor
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

    // Método para crear un Poller con los sockets de comprobación de salud
    private ZMQ.Poller createPoller() {
        ZMQ.Poller poller = context.createPoller(healthCheckSockets.size());
        for (Socket socket : healthCheckSockets.values()) {
            poller.register(socket, ZMQ.Poller.POLLIN);
        }
        return poller;
    }

}
