package com.sleekydz86.carebridge.backend.server.application.device;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import com.sleekydz86.carebridge.backend.global.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeviceSimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulationScheduler.class);

    private final AppProperties appProperties;
    private final DeviceSimulationPayloadFactory payloadFactory;

    public DeviceSimulationScheduler(AppProperties appProperties, DeviceSimulationPayloadFactory payloadFactory) {
        this.appProperties = appProperties;
        this.payloadFactory = payloadFactory;
    }

    @Scheduled(fixedDelayString = "${app.simulator.interval-millis:7000}", initialDelayString = "${app.simulator.initial-delay-millis:5000}")
    public void pushSimulatedDeviceMessage() {
        if (!appProperties.simulator().enabled()) {
            return;
        }

        String payload = payloadFactory.nextPayload();
        int connectTimeout = (int) Math.min(Math.max(appProperties.simulator().connectTimeoutMillis(), 500), 120_000);
        int readTimeout = (int) Math.min(Math.max(appProperties.simulator().readTimeoutMillis(), 1_000), 300_000);

        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(appProperties.simulator().host(), appProperties.simulator().port()),
                    connectTimeout);
            socket.setSoTimeout(readTimeout);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            writer.write(payload);
            writer.newLine();
            writer.flush();
            socket.shutdownOutput();

            String ack = reader.readLine();
            log.info("가상 장비 페이로드 전송 성공. ack={}, target={}:{}", ack, appProperties.simulator().host(),
                    appProperties.simulator().port());
        } catch (SocketTimeoutException exception) {
            log.warn(
                    "가상 장비 페이로드 응답 시간 초과({}ms). target={}:{} — DB·HL7 처리 지연이 있으면 read-timeout-millis를 늘리세요.",
                    readTimeout,
                    appProperties.simulator().host(),
                    appProperties.simulator().port());
        } catch (Exception exception) {
            log.warn("가상 장비 페이로드 전송 실패. target={}:{}", appProperties.simulator().host(),
                    appProperties.simulator().port(), exception);
        }
    }
}
