package com.sleekydz86.carebridge.backend.server.application.port.out;

import com.sleekydz86.carebridge.backend.server.domain.emr.Hl7Models.ParsedHl7Message;

public interface Hl7MessageParser {
    ParsedHl7Message parse(String rawMessage);
}

