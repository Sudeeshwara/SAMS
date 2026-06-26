-- ============================================================
--  STUDENT ATTENDANCE MANAGEMENT SYSTEM (SAMS)
--  Database Schema + Sample Data
-- ============================================================

CREATE DATABASE IF NOT EXISTS sams_db;
USE sams_db;

-- ─────────────────────────────────────────
-- TABLE: users  (login + role)
-- ─────────────────────────────────────────
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        ENUM('ADMIN','LECTURER') NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────
-- TABLE: courses
-- ─────────────────────────────────────────
CREATE TABLE courses (
    course_id   INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20)  NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    description TEXT,
    duration    INT          NOT NULL COMMENT 'Duration in years'
);

-- ─────────────────────────────────────────
-- TABLE: subjects  (belong to a course)
-- ─────────────────────────────────────────
CREATE TABLE subjects (
    subject_id   INT AUTO_INCREMENT PRIMARY KEY,
    subject_code VARCHAR(20)  NOT NULL UNIQUE,
    subject_name VARCHAR(100) NOT NULL,
    course_id    INT          NOT NULL,
    credits      INT          NOT NULL DEFAULT 3,
    CONSTRAINT fk_subject_course FOREIGN KEY (course_id)
        REFERENCES courses(course_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- TABLE: students
-- ─────────────────────────────────────────
CREATE TABLE students (
    student_id  INT AUTO_INCREMENT PRIMARY KEY,
    reg_number  VARCHAR(20)  NOT NULL UNIQUE,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    course_id   INT          NOT NULL,
    enrolled_at DATE         NOT NULL,
    CONSTRAINT fk_student_course FOREIGN KEY (course_id)
        REFERENCES courses(course_id) ON DELETE RESTRICT
);

-- ─────────────────────────────────────────
-- TABLE: lecturers
-- ─────────────────────────────────────────
CREATE TABLE lecturers (
    lecturer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL UNIQUE,
    employee_id VARCHAR(20)  NOT NULL UNIQUE,
    department  VARCHAR(100),
    CONSTRAINT fk_lecturer_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- TABLE: lecturer_subjects  (many-to-many)
-- ─────────────────────────────────────────
CREATE TABLE lecturer_subjects (
    lecturer_id INT NOT NULL,
    subject_id  INT NOT NULL,
    PRIMARY KEY (lecturer_id, subject_id),
    CONSTRAINT fk_ls_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id) ON DELETE CASCADE,
    CONSTRAINT fk_ls_subject  FOREIGN KEY (subject_id)
        REFERENCES subjects(subject_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- TABLE: class_sessions
-- ─────────────────────────────────────────
CREATE TABLE class_sessions (
    session_id   INT AUTO_INCREMENT PRIMARY KEY,
    subject_id   INT         NOT NULL,
    lecturer_id  INT         NOT NULL,
    session_date DATE        NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    venue        VARCHAR(100),
    CONSTRAINT fk_session_subject  FOREIGN KEY (subject_id)
        REFERENCES subjects(subject_id) ON DELETE RESTRICT,
    CONSTRAINT fk_session_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id) ON DELETE RESTRICT
);

-- ─────────────────────────────────────────
-- TABLE: attendance
-- ─────────────────────────────────────────
CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    session_id    INT  NOT NULL,
    student_id    INT  NOT NULL,
    status        ENUM('PRESENT','ABSENT','LATE') NOT NULL DEFAULT 'ABSENT',
    remarks       VARCHAR(255),
    marked_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_session_student (session_id, student_id),
    CONSTRAINT fk_att_session FOREIGN KEY (session_id)
        REFERENCES class_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
);

-- ============================================================
--  SAMPLE DATA
-- ============================================================

-- Users  (passwords stored as plain text for demo — SHA2 in real app)
INSERT INTO users (username, password, full_name, role) VALUES
('admin',    'admin123',    'System Administrator', 'ADMIN'),
('lec_gugsi', 'gugsi123',     'Mr. Guganathan poravi',        'LECTURER'),
('lec_torin', 'torin123',     'Ms. Torin weerasingha',          'LECTURER'),
('lec_hiruni', 'hiruni123',     'Ms. Hiruni Thalangama',       'LECTURER');

-- Courses
INSERT INTO courses (course_code, course_name, description, duration) VALUES
('BSC-CS',  'BSc Computer Science',        'Undergraduate degree in Computer Science', 3),
('BSC-IT',  'BSc Information Technology',  'Undergraduate degree in IT',               3),
('DIP-SE',  'Diploma in Software Eng.',    'Diploma level software engineering',       2);

-- Subjects
INSERT INTO subjects (subject_code, subject_name, course_id, credits) VALUES
('CS101', 'Introduction to Programming',  1, 3),
('CS102', 'Object-Oriented Programming',  1, 3),
('CS201', 'Data Structures & Algorithms', 1, 4),
('IT101', 'Web Development Fundamentals', 2, 3),
('IT102', 'Database Management Systems',  2, 3),
('SE101', 'Software Design Principles',   3, 3),
('SE102', 'Agile Methodologies',          3, 3);

-- Students
INSERT INTO students (reg_number, first_name, last_name, email, phone, course_id, enrolled_at) VALUES
('2024CS001', 'Sunil',   'Fernando',  'sunil@gmail.com',  '0771234567', 1, '2024-01-15'),
('2024CS002', 'Tharindu',     'Silva',     'tharindu@gmail.com',    '0779876543', 1, '2024-01-15'),
('2024CS003', 'Mahela',   'Jayawardena','mahela@gmail.com', '0712345678', 1, '2024-01-15'),
('2024IT001', 'Kusal',   'Perera',    'kusal@gmail.com',  '0751112222', 2, '2024-01-15'),
('2024IT002', 'kaushi',    'Wijesinghe','kaushi@gmail.com',   '0763334444', 2, '2024-01-15'),
('2024SE001', 'Nimal',   'Bandara',   'nimal@gmail.com',  '0755556666', 3, '2024-01-15'),
('2024SE002', 'Kumara',   'Thirimadura',    'kumara@gmail.com',  '0777778888', 3, '2024-01-15');

-- Lecturers
INSERT INTO lecturers (user_id, employee_id, department) VALUES
(2, 'EMP001', 'Computer Science'),
(3, 'EMP002', 'Information Technology'),
(4, 'EMP003', 'Software Engineering');

-- Lecturer ↔ Subject assignments
INSERT INTO lecturer_subjects (lecturer_id, subject_id) VALUES
(1, 1),(1, 2),(1, 3),
(2, 4),(2, 5),
(3, 6),(3, 7);

-- Class Sessions
INSERT INTO class_sessions (subject_id, lecturer_id, session_date, start_time, end_time, venue) VALUES
(1, 1, '2025-06-01', '09:00:00', '11:00:00', 'Hall A'),
(2, 1, '2025-06-02', '13:00:00', '15:00:00', 'Lab 1'),
(4, 2, '2025-06-01', '10:00:00', '12:00:00', 'Hall B'),
(5, 2, '2025-06-03', '14:00:00', '16:00:00', 'Lab 2'),
(6, 3, '2025-06-02', '09:00:00', '11:00:00', 'Room 101'),
(1, 1, '2025-06-08', '09:00:00', '11:00:00', 'Hall A'),
(2, 1, '2025-06-09', '13:00:00', '15:00:00', 'Lab 1');

-- Attendance records
INSERT INTO attendance (session_id, student_id, status) VALUES
(1,1,'PRESENT'),(1,2,'ABSENT'),(1,3,'LATE'),
(2,1,'PRESENT'),(2,2,'PRESENT'),(2,3,'ABSENT'),
(3,4,'PRESENT'),(3,5,'PRESENT'),
(4,4,'LATE'),(4,5,'ABSENT'),
(5,6,'PRESENT'),(5,7,'PRESENT'),
(6,1,'PRESENT'),(6,2,'PRESENT'),(6,3,'PRESENT'),
(7,1,'ABSENT'),(7,2,'PRESENT'),(7,3,'PRESENT');
