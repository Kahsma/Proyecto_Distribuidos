package com.javeriana;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HealthChecker {

    private static final int CHECK_INTERVAL = 5000; // Interval in milliseconds

    // Map monitor types to their respective IP addresses and ports
    private static final Map<SensorType, String> MONITOR_ADDRESSES = new HashMap<>();

    static {
        MONITOR_ADDRESSES.put(SensorType.temperatura, "tcp://192.168.0.4:5562");
        MONITOR_ADDRESSES.put(SensorType.ph, "tcp://192.168.0.4:5563");
        MONITOR_ADDRESSES.put(SensorType.oxygeno, "tcp://192.168.0.4:5564");
    }

    public static void main(String[] args) {
        HealthChecker healthChecker = new HealthChecker();

        // Start the health checking process
        healthChecker.startHealthCheck();
    }

    public void startHealthCheck() {
        try (ZContext context = new ZContext()) {
            // Create sockets for each monitor type
            Map<SensorType, Socket> healthCheckSockets = createHealthCheckSockets(context);

            // Create a Poller to check for socket events
            ZMQ.Poller poller = context.createPoller(healthCheckSockets.size());

            // Register sockets with the Poller
            for (Socket socket : healthCheckSockets.values()) {
                poller.register(socket, ZMQ.Poller.POLLIN);
            }

            while (!Thread.currentThread().isInterrupted()) {
                // Send health check requests for each monitor type
                for (Map.Entry<SensorType, Socket> entry : healthCheckSockets.entrySet()) {
                    checkMonitor(entry.getValue(), entry.getKey());
                }

                // Use the Poller to wait for a response or timeout
                if (poller.poll(1000) > 0) {
                    // If there's a response, receive and process it
                    for (int i = 0; i < poller.getSize(); i++) {
                        if (poller.pollin(i)) {
                            Socket socket = poller.getSocket(i);
                            SensorType sensorType = getSensorTypeBySocket(healthCheckSockets, socket);

                            // Check if the socket is ready for receiving
                            if (socket.getEvents() == ZMQ.Poller.POLLIN) {
                                processMonitorResponse(socket, sensorType);
                            } else {
                                System.out.println("Socket not ready for receiving");
                            }
                        }
                    }
                } else {
                    // Handle the case when there's no response within the specified timeout
                    System.out.println("Health check timed out");
                }

                // Wait for the next interval before the next check
                try {
                    Thread.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    // Print a message when the thread is interrupted
                    System.out.println("Health check thread interrupted");
                    Thread.currentThread().interrupt(); // Re-interrupt the thread
                }
            }
        }
    }

    private Map<SensorType, Socket> createHealthCheckSockets(ZContext context) {
        Map<SensorType, Socket> healthCheckSockets = new HashMap<>();

        for (Map.Entry<SensorType, String> entry : MONITOR_ADDRESSES.entrySet()) {
            SensorType sensorType = entry.getKey();
            String address = entry.getValue();

            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect(address);
            healthCheckSockets.put(sensorType, socket);
        }

        return healthCheckSockets;
    }

    private void checkMonitor(Socket healthCheckSocket, SensorType sensorType) {
        // Before sending a health check request
        System.out.println("Health Check Socket State before send: " + healthCheckSocket.getEvents());

        try {
            // Send a health check request for a specific monitor type
            healthCheckSocket.send(sensorType.toString().getBytes(ZMQ.CHARSET), 0);
        } catch (ZMQException e) {
            // Handle the exception when the monitor is not reachable
            System.out.println("Error sending health check request to " + sensorType + " monitor: " + e.getMessage());

            // Start a new process for the specific sensor type
            startMonitorProcess(sensorType);

        }

        // After sending a health check request
        System.out.println("Health Check Socket State after send: " + healthCheckSocket.getEvents());

        // Wait for a short period before sending the next health check request
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startMonitorProcess(SensorType sensorType) {
        System.out.println("Starting a new process for " + sensorType);
        try {
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                // For Windows, you may need to adjust this command based on your terminal
                // emulator
                processBuilder = new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "mvn", "exec:java",
                        "-Dexec.mainClass=com.javeriana.Monitor", "-Dexec.args=-t " + sensorType.toString());
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                // For Unix-like systems (Linux, macOS), opens a new terminal window
                processBuilder = new ProcessBuilder("sh", "-c",
                        "mvn exec:java -Dexec.mainClass=com.javeriana.Monitor -Dexec.args=-t " + sensorType.toString());
            } else {
                throw new UnsupportedOperationException("Unsupported operating system: " + os);
            }

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Optionally, you can wait for the process to finish or do other handling
            // process.waitFor();
        } catch (IOException e) {
            System.err.println("Error starting a new process: " + e.getMessage());
        }
    }

    private void processMonitorResponse(Socket healthCheckSocket, SensorType sensorType) {
        // If there's a response, receive and process it
        String response = healthCheckSocket.recvStr(0);
        if (response != null) {
            System.out.println(sensorType + " Monitor received: " + response);
        } else {
            // Handle the case when there's no response within the specified timeout
            System.out.println(sensorType + " Monitor did not respond within the specified timeout");
        }
    }

    private SensorType getSensorTypeBySocket(Map<SensorType, Socket> sockets, Socket socket) {
        for (Map.Entry<SensorType, Socket> entry : sockets.entrySet()) {
            if (entry.getValue() == socket) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Socket not found for the given SensorType");
    }
}
