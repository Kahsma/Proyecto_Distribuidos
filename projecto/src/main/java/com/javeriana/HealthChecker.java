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

    private final ZContext context;
    private final Map<SensorType, Socket> healthCheckSockets;

    private static final Map<SensorType, String> MONITOR_BASE_ADDRESSES = new HashMap<>();

    static {
        MONITOR_BASE_ADDRESSES.put(SensorType.temperatura, "tcp://10.43.101.112");
        MONITOR_BASE_ADDRESSES.put(SensorType.ph, "tcp://10.43.101.124");
        MONITOR_BASE_ADDRESSES.put(SensorType.oxygeno, "tcp://10.43.101.124");
    }

    public HealthChecker(ZContext context) {
        this.context = context;
        this.healthCheckSockets = createHealthCheckSockets(context);
    }

    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            HealthChecker healthChecker = new HealthChecker(context);

            // Start the health checking process
            healthChecker.startHealthCheck();

            // The health checking process runs indefinitely, so this line is not reached
            // unless interrupted
            System.out.println("Health checking process interrupted. Closing the application.");
        }
    }

    public void startHealthCheck() {
        try {
            // Create a Poller to check for socket events
            ZMQ.Poller poller = createPoller();

            while (!Thread.currentThread().isInterrupted()) {
                // Send health check requests for each monitor type
                for (Map.Entry<SensorType, Socket> entry : healthCheckSockets.entrySet()) {
                    checkMonitor(entry.getValue(), entry.getKey());
                }

                // Use the Poller to wait for a response or timeout
                if (poller.poll(5000) > 0) {
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

                // Update the addresses after processing all the responses
                updateMonitorAddresses();
                // Recreate the poller with the updated addresses
                poller.close();
                poller = createPoller();
            }
        } finally {
            // Ensure that the sockets are closed when the health checking process is
            // finished
            healthCheckSockets.values().forEach(Socket::close);
        }
    }

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

    private void checkMonitor(Socket healthCheckSocket, SensorType sensorType) {
        // Before sending a health check request
        System.out.println("Health Check Socket State before send: " + healthCheckSocket.getEvents());

        boolean isUpdateNeeded = false;

        try {
            // Send a health check request for a specific monitor type
            healthCheckSocket.send(sensorType.toString().getBytes(ZMQ.CHARSET), 0);
            System.out.println("Health Check Socket State after send: " + healthCheckSocket.getEvents());

            // Wait for a short period before sending the next health check request
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Check if there's no response within the specified timeout
            if (healthCheckSocket.getEvents() == ZMQ.Poller.POLLIN) {
                // Process the response
                processMonitorResponse(healthCheckSocket, sensorType);
            } else {
                // No response received, start a new process
                System.out.println(
                        sensorType + " Monitor did not respond within the specified timeout. Starting a new process.");
                startMonitorProcess(sensorType);
                isUpdateNeeded = true;
            }
        } catch (ZMQException e) {
            // Handle the exception when the monitor is not reachable
            System.out.println("Error sending health check request to " + sensorType + " monitor: " + e.getMessage());

            // Start a new process for the specific sensor type
            startMonitorProcess(sensorType);
            System.out.println(MONITOR_BASE_ADDRESSES.get(sensorType));
            isUpdateNeeded = true;
        }

        if (isUpdateNeeded) {
            // Update the address after processing the response or catching an exception
            updateMonitorAddresses(sensorType);
        }

        // After sending a health check request
        System.out.println("Health Check Socket State after send: " + healthCheckSocket.getEvents());
    }

    private Map<SensorType, String> previousAddresses = new HashMap<>();

    private void updateMonitorAddresses() {
        String ipAddress = "tcp://10.43.101.124"; /* the new IP address you want to set */

        for (SensorType sensorType : MONITOR_BASE_ADDRESSES.keySet()) {
            String previousAddress = previousAddresses.getOrDefault(sensorType, "");
            if (!previousAddress.equals(MONITOR_BASE_ADDRESSES.get(sensorType))) {
                System.out.println(
                        "Changing address for " + sensorType + " to: " + MONITOR_BASE_ADDRESSES.get(sensorType));
            }
            previousAddresses.put(sensorType, MONITOR_BASE_ADDRESSES.get(sensorType));
        }

        // Print a message when updating addresses
        System.out.println("Updated monitor addresses");

        // Recreate health check sockets with the updated addresses
        this.healthCheckSockets.values().forEach(Socket::close);
        this.healthCheckSockets.clear();
        this.healthCheckSockets.putAll(createHealthCheckSockets(context));

        // Print the current values of the addresses after updating
        for (SensorType sensorType : MONITOR_BASE_ADDRESSES.keySet()) {
            System.out.println("Current address for " + sensorType + ": " + MONITOR_BASE_ADDRESSES.get(sensorType));
        }
    }

    private void updateMonitorAddresses(SensorType sensorType) {
        String ipAddress = "tcp://10.43.101.124"; /* the new IP address you want to set */

        MONITOR_BASE_ADDRESSES.put(sensorType, ipAddress);
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
            // Add logic to handle the response as needed
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

    private int getHealthCheckPort(SensorType sensorType) {
        // Return different port numbers for each sensor type
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

    private ZMQ.Poller createPoller() {
        ZMQ.Poller poller = context.createPoller(healthCheckSockets.size());
        for (Socket socket : healthCheckSockets.values()) {
            poller.register(socket, ZMQ.Poller.POLLIN);
        }
        return poller;
    }

}
