CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(30),
  avatar_url VARCHAR(500),
  role VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_users_email (email)
);
CREATE TABLE IF NOT EXISTS specialties (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS dental_services (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(80),
  price DECIMAL(12,2) NOT NULL,
  duration_minutes INT NOT NULL,
  description VARCHAR(2000),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  KEY idx_services_active_name (active,name)
);
CREATE TABLE IF NOT EXISTS doctor_profiles (
  user_id BIGINT NOT NULL PRIMARY KEY,
  specialty_id BIGINT,
  experience_years INT NOT NULL,
  qualification VARCHAR(200),
  description VARCHAR(2000),
  achievements VARCHAR(3000),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_profile_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);
CREATE TABLE IF NOT EXISTS doctor_profile_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL UNIQUE,
  specialty_id BIGINT NOT NULL,
  experience_years INT NOT NULL,
  qualification VARCHAR(200),
  description VARCHAR(2000),
  achievements VARCHAR(3000),
  status VARCHAR(20) NOT NULL,
  review_note VARCHAR(1000),
  submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_profile_submission_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  CONSTRAINT fk_profile_submission_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
);
CREATE TABLE IF NOT EXISTS doctor_schedules (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  day_of_week VARCHAR(12) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  break_start TIME,
  break_end TIME,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uk_schedule_doctor_day (doctor_id,day_of_week),
  CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS doctor_unavailabilities (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  reason VARCHAR(300),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  KEY idx_unavailability_doctor_start (doctor_id,start_at),
  CONSTRAINT fk_unavailability_doctor FOREIGN KEY (doctor_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS appointments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  price_at_booking DECIMAL(12,2) NOT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  symptoms VARCHAR(2000),
  internal_note VARCHAR(2000),
  follow_up_requested BOOLEAN NOT NULL DEFAULT FALSE,
  reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_appointment_doctor_start (doctor_id,start_at),
  KEY idx_appointment_patient_start (patient_id,start_at),
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES dental_services(id)
);
CREATE TABLE IF NOT EXISTS medical_records (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  appointment_id BIGINT NOT NULL UNIQUE,
  examination_date DATE NOT NULL,
  symptoms VARCHAR(2000),
  diagnosis VARCHAR(2000),
  treatment VARCHAR(4000),
  prescription VARCHAR(3000),
  notes VARCHAR(3000),
  follow_up_date DATE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_record_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_record_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  CONSTRAINT fk_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(80) NOT NULL UNIQUE,
  expires_at TIMESTAMP(6) NOT NULL,
  KEY idx_refresh_token_value (token),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(80) NOT NULL UNIQUE,
  expires_at TIMESTAMP(6) NOT NULL,
  KEY idx_password_reset_token (token),
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS follow_ups (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  previous_appointment_id BIGINT NOT NULL,
  scheduled_at TIMESTAMP(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(300),
  notes VARCHAR(1000),
  reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_followup_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_followup_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  CONSTRAINT fk_followup_appointment FOREIGN KEY (previous_appointment_id) REFERENCES appointments(id)
);
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  action_path VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at TIMESTAMP(6),
  KEY idx_notification_user_read (user_id,read_at),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  content VARCHAR(4000) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at TIMESTAMP(6),
  KEY idx_chat_sender_recipient_created (sender_id,recipient_id,created_at),
  KEY idx_chat_recipient_read (recipient_id,read_at),
  CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users(id),
  CONSTRAINT fk_chat_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);
