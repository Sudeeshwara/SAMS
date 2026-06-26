# Student Attendance Management System (SAMS)

## Project Overview
The Student Attendance Management System (SAMS) is a desktop-based application built to automate and manage academic attendance records, class schedules, and university entities. It provides role-based access control with distinct panels for:
- **Administrators**: Manage courses, students, lecturers, class sessions, mark attendance, and generate reports.
- **Lecturers**: View assigned classes, mark student attendance for active sessions, and review attendance logs.

## Technologies Used
- **Language**: Java 17
- **UI Framework**: JavaFX 21
- **Database**: MySQL 8
- **Build Tool**: Apache Maven

## Setup Instructions
1. **Database Setup**:
   - Create a MySQL database named `sams_db`.
   - Import the schema and seed data using the SQL script located at [sql/sams_database.sql](file:///d:/IJSE/CW_2/SAMS/sql/sams_database.sql).
2. **Configuration**:
   - Open the [DBConnection.java](file:///d:/IJSE/CW_2/SAMS/src/main/java/com/sams/util/DBConnection.java) configuration file.
   - Update the database connection credentials (`USER` and `PASSWORD`) to match your local MySQL configuration.
3. **Execution**:
   - Open a terminal in the root directory of the project.
   - Execute the following command to build and run the application:
     ```bash
     mvn clean javafx:run
     ```

## Login Credentials
Use the default accounts below to sign in:

| Username | Password | Role |
| :--- | :--- | :--- |
| **`admin`** | `admin123` | Administrator |
| **`lec_gugsi`** | `gugsi123` | Lecturer |
| **`lec_torin`** | `torin123` | Lecturer |
| **`lec_hiruni`** | `hiruni123` | Lecturer |
