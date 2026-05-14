package com.sleekydz86.carebridge.backend.testsupport;

public final class OruMessageSamples {
    private OruMessageSamples() {}

    public static String oruR01(String messageControlId, String patientNo, String orderNo) {
        return "MSH|^~\\&|HL7GW|APP|EMR|HOSP|20260101120000||ORU^R01|" + messageControlId + "|P|2.5\n"
                + "PID|1||" + patientNo + "||TEST^PATIENT|||M\n"
                + "OBR|1|" + orderNo + "||ECG^Electrocardiogram\n"
                + "OBX|1|NM|8867-4^Heart rate|HR|72|bpm|60-100|N\n";
    }
}
