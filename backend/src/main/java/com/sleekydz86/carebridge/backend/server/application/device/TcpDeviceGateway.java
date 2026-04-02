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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;


@Component
public class TcpDeviceGateway implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TcpDeviceGateway.class);

    private final DeviceInterfaceService deviceInterfaceService;
    private final AppProperties appProperties;
    private final ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean running;
    private ServerSocket serverSocket;

    public TcpDeviceGateway(DeviceInterfaceService deviceInterfaceService, AppProperties appProperties) {
        this.deviceInterfaceService = deviceInterfaceService;
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
            log.info("TCP 장비 연동 게이트웨이 시작됨. 포트: {}", appProperties.tcp().port());
        } catch (IOException exception) {
            throw new IllegalStateException("TCP 장비 게이트웨이 서버를 시작할 수 없습니다.", exception);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(socket));
            } catch (SocketException exception) {
                if (running) {
                    log.warn("TCP 접속 대기 루프가 중단되었습니다.", exception);
                }
            } catch (IOException exception) {
                if (running) {
                    log.warn("TCP 클라이언트 접속 요청 처리에 실패했습니다.", exception);
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
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                DeviceInterfaceService.DeviceEventView saved = deviceInterfaceService.ingest(line, sourceIp);
                writer.write(saved.ackCode());
                writer.newLine();
                writer.flush();
            }
        } catch (IOException exception) {
            log.debug("TCP 클라이언트 연결이 해제되었습니다.", exception);
        } catch (Exception exception) {
            log.warn("TCP 클라이언트 요청 처리 중 오류가 발생했습니다.", exception);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            log.warn("TCP 서버 소켓 종료에 실패했습니다.", exception);
        }

        acceptExecutor.shutdownNow();
        clientExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}