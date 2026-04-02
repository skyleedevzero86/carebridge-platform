package com.sleekydz86.carebridge.backend.server.application.device;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    @Scheduled(
            fixedDelayString = "${app.simulator.interval-millis:7000}",
            initialDelayString = "${app.simulator.initial-delay-millis:5000}"
    )
    public void pushSimulatedDeviceMessage() {
        if (!appProperties.simulator().enabled()) {
            return;
        }

        String payload = payloadFactory.nextPayload();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(appProperties.simulator().host(), appProperties.simulator().port()), 2_000);
            socket.setSoTimeout(2_000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            writer.write(payload);
            writer.newLine();
            writer.flush();

            String ack = reader.readLine();
            log.info("가상 장비 페이로드 전송 성공. ack={}, target={}:{}", ack, appProperties.simulator().host(), appProperties.simulator().port());
        } catch (Exception exception) {
            log.warn("가상 장비 페이로드 전송 실패. target={}:{}", appProperties.simulator().host(), appProperties.simulator().port(), exception);
        }
    }
}