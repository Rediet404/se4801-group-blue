-- V13: Add database indexes for laboratory tables to optimize query performance and match JPA entity declarations

CREATE INDEX idx_patient_id_lo ON lab_orders(patient_id);
CREATE INDEX idx_doctor_id_lo ON lab_orders(doctor_id);
CREATE INDEX idx_status_lo ON lab_orders(status);

CREATE INDEX idx_lab_order_id_lr ON lab_results(lab_order_id);
CREATE INDEX idx_lab_technician_id_lr ON lab_results(lab_technician_id);
CREATE INDEX idx_status_lr ON lab_results(status);

CREATE INDEX idx_user_id_ln ON lab_notifications(user_id);
CREATE INDEX idx_is_read_ln ON lab_notifications(is_read);
