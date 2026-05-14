package com.sleekydz86.carebridge.backend.server.adapter.out.hl7;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7AckGeneratorTest {

    private final Hl7AckGenerator generator = new Hl7AckGenerator();

    @Test
    void successAckContainsAaAndMsa() {
        String ack = generator.success("MSG-OK-1");
        assertThat(ack).contains("MSA|AA|MSG-OK-1");
        assertThat(ack).contains("ACK|MSG-OK-1");
        assertThat(ack).doesNotContain("\nERR|");
    }

    @Test
    void failureAckContainsAeAndErr() {
        String ack = generator.failure("MSG-ERR-1", "PATIENT_NOT_FOUND");
        assertThat(ack).contains("MSA|AE|MSG-ERR-1");
        assertThat(ack).contains("ERR|||PATIENT_NOT_FOUND");
    }

    @Test
    void failureUsesUnknownWhenControlIdBlank() {
        String ack = generator.failure("  ", "INVALID_HL7_FORMAT");
        assertThat(ack).contains("MSA|AE|UNKNOWN");
    }
}
