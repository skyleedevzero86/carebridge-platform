package com.sleekydz86.carebridge.backend.server.adapter.out.hl7;

import com.sleekydz86.carebridge.backend.testsupport.OruMessageSamples;
import com.sleekydz86.carebridge.backend.server.domain.emr.Hl7Models.ParsedHl7Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultHl7MessageParserTest {

    private final DefaultHl7MessageParser parser = new DefaultHl7MessageParser();

    @Test
    void parsesOruR01WithObr2OrderNumber() {
        ParsedHl7Message parsed = parser.parse(OruMessageSamples.oruR01("MC-100", "P0001", "ORD-001"));
        assertThat(parsed.messageControlId()).isEqualTo("MC-100");
        assertThat(parsed.messageType()).isEqualTo("ORU^R01");
        assertThat(parsed.patientNo()).isEqualTo("P0001");
        assertThat(parsed.orderNo()).isEqualTo("ORD-001");
        assertThat(parsed.observations()).hasSize(1);
        assertThat(parsed.observations().getFirst().observationCode()).isEqualTo("8867-4");
        assertThat(parsed.observations().getFirst().resultValue()).isEqualTo("72");
    }

    @Test
    void fallsBackToObr3WhenObr2Blank() {
        String raw = "MSH|^~\\&|HL7GW|APP|EMR|HOSP|20260101120000||ORU^R01|MC-2|P|2.5\n"
                + "PID|1||P0001||TEST^PATIENT|||M\n"
                + "OBR|1||ORD-FALLBACK|ECG^Electrocardiogram\n"
                + "OBX|1|NM|8867-4^Heart rate|HR|70|bpm|60-100|N\n";
        ParsedHl7Message parsed = parser.parse(raw);
        assertThat(parsed.orderNo()).isEqualTo("ORD-FALLBACK");
    }

    @Test
    void rejectsMissingSegments() {
        String raw = "MSH|^~\\&|HL7GW|APP|EMR|HOSP|20260101120000||ORU^R01|MC|P|2.5\n"
                + "PID|1||P0001||TEST^PATIENT|||M\n";
        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_HL7_FORMAT");
    }

    @Test
    void rejectsUnsupportedMessageType() {
        String raw = "MSH|^~\\&|HL7GW|APP|EMR|HOSP|20260101120000||ADT^A01|MC|P|2.5\n"
                + "PID|1||P0001||TEST^PATIENT|||M\n"
                + "OBR|1|ORD-001||ECG^Electrocardiogram\n"
                + "OBX|1|NM|8867-4^Heart rate|HR|72|bpm|60-100|N\n";
        assertThatThrownBy(() -> parser.parse(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UNSUPPORTED_MESSAGE_TYPE");
    }
}
