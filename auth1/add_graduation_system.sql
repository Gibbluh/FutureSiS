-- Add graduation_status column to students table
ALTER TABLE students ADD COLUMN graduation_status INT DEFAULT 0;

-- Create graduation_requests table
CREATE TABLE graduation_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    request_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_date DATETIME,
    comments TEXT,
    FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Add indexes for better performance
CREATE INDEX idx_graduation_requests_student_id ON graduation_requests(student_id);
CREATE INDEX idx_graduation_requests_status ON graduation_requests(status);
CREATE INDEX idx_graduation_requests_request_date ON graduation_requests(request_date); 