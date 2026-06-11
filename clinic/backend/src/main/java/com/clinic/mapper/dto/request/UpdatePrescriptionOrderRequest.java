package com.clinic.mapper.dto.request;

public record UpdatePrescriptionOrderRequest(
        String patientName,
        String drugName,
        String dosage,
        String instructions
) {
}
