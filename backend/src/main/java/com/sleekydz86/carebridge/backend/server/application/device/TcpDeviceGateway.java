package com.sleekydz86.carebridge.backend.server.application.device;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sleekydz86.carebridge.backend.global.config.AppProperties;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase.RegisterObservationResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class TcpDeviceGateway implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TcpDeviceGateway.class);

    private final DeviceInterfaceService deviceInterfaceService;
    private final RegisterObservationResultUseCase registerObservationResultUseCase;
    private final AppProperties appProperties;
    private final ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean running;
    private ServerSocket serverSocket;

    public TcpDeviceGateway(DeviceInterfaceService deviceInterfaceService, RegisterObservationResultUseCase registerObservationResultUseCase, AppProperties appProperties) {
        this.deviceInterfaceService = deviceInterfaceService;
        this.registerObservationResultUseCase = registerObservationResultUseCase;
        this.appProperties = appProperties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        try {
            serverSocket = new ServerSocket(appProperties.tcp().port());
            running = true;
            acceptExecutor.submit(this::acceptLoop);
            log.info("TCP device gateway started on port {}", appProperties.tcp().port());
        } catch (IOException exception) {
            throw new IllegalStateException("TCP device gateway could not start.", exception);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(socket));
            } catch (SocketException exception) {
                if (running) {
                    log.warn("TCP accept loop stopped unexpectedly.", exception);
                }
            } catch (IOException exception) {
                if (running) {
                    log.warn("TCP client accept failed.", exception);
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (
                socket;
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            String sourceIp = socket.getInetAddress().getHostAddress();
            StringBuilder messageBuffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    flushMessage(messageBuffer, sourceIp, writer);
                    continue;
                }

                messageBuffer.append(line).append('\n');
                if (!messageBuffer.toString().startsWith("MSH|")) {
                    flushMessage(messageBuffer, sourceIp, writer);
                }
            }
            flushMessage(messageBuffer, sourceIp, writer);
        } catch (IOException exception) {
            log.debug("TCP client disconnected.", exception);
        } catch (Exception exception) {
            log.warn("TCP client request failed.", exception);
        }
    }

    private void flushMessage(StringBuilder messageBuffer, String sourceIp, BufferedWriter writer) throws IOException {
        String payload = messageBuffer.toString().trim();
        messageBuffer.setLength(0);
        if (payload.isBlank()) {
            return;
        }

        if (payload.startsWith("MSH|")) {
            RegisterObservationResultResponse response = registerObservationResultUseCase.register(payload);
            writer.write(response.ackMessage());
        } else {
            DeviceInterfaceService.DeviceEventView saved = deviceInterfaceService.ingest(payload, sourceIp);
            writer.write(saved.ackCode());
        }
        writer.newLine();
        writer.flush();
    }

    @Override
    public synchronized void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            log.warn("TCP server socket close failed.", exception);
        }

        acceptExecutor.shutdownNow();
        clientExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
