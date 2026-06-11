package com.clinic.mapper.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record LabOrderResponse(
        String id,
        String patientId,
        String doctorId,
        String appointmentId,
        List<String> tests,
        LabUrgency urgency,
        String clinicalNotes,
        LabOrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
