package com.sleekydz86.carebridge.backend.server.adapter.out.websocket;

import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.Hl7MessageLogView;

public record Hl7RealtimeEvent(Hl7MessageLogView view) {
}

