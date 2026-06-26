package com.sams.controller;

import com.sams.dao.*;
import com.sams.model.*;
import com.sams.service.*;
import com.sams.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * PRESENTATION LAYER — Admin dashboard shell + all admin sub-panels.
 */
public class AdminDashboardController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label     userLabel;

    private final AuthService         authService    = AuthService.getInstance();
    private final CourseService       courseService  = new CourseService();
    private final StudentService      studentService = new StudentService();
    private final LecturerService     lecturerService= new LecturerService();
    private final ClassSessionService sessionService = new ClassSessionService();
    private final AttendanceService   attService     = new AttendanceService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userLabel.setText("Hello, " + authService.getLoggedInUser().getFullName());
        showDashboard();
    }

    //  SIDEBAR NAVIGATION
    @FXML public void showDashboard()  { contentArea.getChildren().setAll(buildDashboardPanel()); }
    @FXML public void showCourses()    { contentArea.getChildren().setAll(buildCoursesPanel()); }
    @FXML public void showStudents()   { contentArea.getChildren().setAll(buildStudentsPanel()); }
    @FXML public void showLecturers()  { contentArea.getChildren().setAll(buildLecturersPanel()); }
    @FXML public void showSchedule()   { contentArea.getChildren().setAll(buildSchedulePanel()); }
    @FXML public void showAttendance() { contentArea.getChildren().setAll(buildAttendancePanel()); }
    @FXML public void showReports()    { contentArea.getChildren().setAll(buildReportsPanel()); }

    @FXML
    public void handleLogout() {
        authService.logout();
        try { SceneManager.switchScene("/fxml/Login.fxml", "Login"); }
        catch (Exception e) { e.printStackTrace(); }
    }


    //  DASHBOARD PANEL
    private VBox buildDashboardPanel() {
        VBox root = new VBox(20);

        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Overview of the system");
        sub.getStyleClass().add("page-subtitle");

        HBox stats = new HBox(16);
        try {
            stats.getChildren().addAll(
                statCard("Courses",   String.valueOf(courseService.getAllCourses().size()),   "#2E86AB"),
                statCard("Students",  String.valueOf(studentService.getAllStudents().size()),  "#27AE60"),
                statCard("Lecturers", String.valueOf(lecturerService.getAllLecturers().size()),"#8E44AD"),
                statCard("Sessions",  String.valueOf(sessionService.getAllSessions().size()),  "#F39C12")
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

        root.getChildren().addAll(title, sub, stats);
        return root;
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label num = new Label(value);
        num.getStyleClass().add("stat-number");
        num.setStyle("-fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        card.getChildren().addAll(num, lbl);
        return card;
    }

    //  COURSES PANEL
    private VBox buildCoursesPanel() {
        VBox root = new VBox(16);

        Label title = new Label("Course Management");
        title.getStyleClass().add("page-title");

        // Toolbar
        HBox toolbar = new HBox(10);
        Button addBtn = new Button("+ Add Course");
        addBtn.getStyleClass().add("btn-primary");
        toolbar.getChildren().add(addBtn);

        // Table
        TableView<Course> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Course, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<Course, String> colName = new TableColumn<>("Course Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("courseName"));

        TableColumn<Course, String> colDur = new TableColumn<>("Duration (yrs)");
        colDur.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDuration())));

        TableColumn<Course, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Course, Void> colActions = actionColumn(table,
            c -> showCourseDialog(c, table),
            c -> { try { if (confirm("Delete course?")) { courseService.deleteCourse(c.getCourseId()); refreshTable(table, courseService.getAllCourses()); } } catch (Exception e) { alert(e.getMessage()); } }
        );

        table.getColumns().addAll(colCode, colName, colDur, colDesc, colActions);

        try { table.setItems(FXCollections.observableArrayList(courseService.getAllCourses())); }
        catch (SQLException e) { alert(e.getMessage()); }

        addBtn.setOnAction(e -> showCourseDialog(null, table));

        root.getChildren().addAll(title, toolbar, table);
        return root;
    }

    private void showCourseDialog(Course existing, TableView<Course> table) {
        boolean isEdit = existing != null;
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Course" : "Add Course");

        TextField codeF = styledField(isEdit ? existing.getCourseCode() : "");
        TextField nameF = styledField(isEdit ? existing.getCourseName() : "");
        TextField durF  = styledField(isEdit ? String.valueOf(existing.getDuration()) : "3");
        TextArea  descF = new TextArea(isEdit ? existing.getDescription() : "");
        descF.setPrefRowCount(3); descF.setWrapText(true);

        GridPane grid = formGrid(
            "Course Code:", codeF,
            "Course Name:", nameF,
            "Duration (years):", durF,
            "Description:", descF
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(420);
        styleDialog(dialog);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Course c = isEdit ? existing : new Course();
                c.setCourseCode(codeF.getText().trim());
                c.setCourseName(nameF.getText().trim());
                try { c.setDuration(Integer.parseInt(durF.getText().trim())); } catch (NumberFormatException ex) { c.setDuration(3); }
                c.setDescription(descF.getText().trim());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                if (isEdit) courseService.updateCourse(c);
                else        courseService.addCourse(c);
                refreshTable(table, courseService.getAllCourses());
            } catch (Exception e) { alert(e.getMessage()); }
        });
    }

    //  STUDENTS PANEL
    private VBox buildStudentsPanel() {
        VBox root = new VBox(16);
        Label title = new Label("Student Management");
        title.getStyleClass().add("page-title");

        HBox toolbar = new HBox(10);
        Button addBtn = new Button("+ Add Student");
        addBtn.getStyleClass().add("btn-primary");
        toolbar.getChildren().add(addBtn);

        TableView<Student> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Student, String> c1 = col("Reg No.", "regNumber");
        TableColumn<Student, String> c2 = new TableColumn<>("Full Name");
        c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        TableColumn<Student, String> c3 = col("Email", "email");
        TableColumn<Student, String> c4 = col("Phone", "phone");
        TableColumn<Student, String> c5 = col("Course", "courseName");
        TableColumn<Student, String> c6 = new TableColumn<>("Enrolled");
        c6.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEnrolledAt().toString()));

        TableColumn<Student, Void> colA = actionColumn(table,
            s -> showStudentDialog(s, table),
            s -> { try { if (confirm("Delete student?")) { studentService.deleteStudent(s.getStudentId()); refreshTable(table, studentService.getAllStudents()); } } catch (Exception e) { alert(e.getMessage()); } }
        );

        table.getColumns().addAll(c1, c2, c3, c4, c5, c6, colA);
        try { table.setItems(FXCollections.observableArrayList(studentService.getAllStudents())); }
        catch (SQLException e) { alert(e.getMessage()); }

        addBtn.setOnAction(e -> showStudentDialog(null, table));
        root.getChildren().addAll(title, toolbar, table);
        return root;
    }

    private void showStudentDialog(Student existing, TableView<Student> table) {
        boolean isEdit = existing != null;
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Student" : "Add Student");

        TextField regF   = styledField(isEdit ? existing.getRegNumber() : "");
        TextField fnF    = styledField(isEdit ? existing.getFirstName() : "");
        TextField lnF    = styledField(isEdit ? existing.getLastName() : "");
        TextField emF    = styledField(isEdit ? existing.getEmail() : "");
        TextField phF    = styledField(isEdit ? existing.getPhone() : "");
        DatePicker dpF   = new DatePicker(isEdit ? existing.getEnrolledAt() : LocalDate.now());

        ComboBox<Course> courseCombo = new ComboBox<>();
        try { courseCombo.setItems(FXCollections.observableArrayList(courseService.getAllCourses())); }
        catch (SQLException e) { alert(e.getMessage()); }
        if (isEdit) courseCombo.getItems().stream()
            .filter(c -> c.getCourseId() == existing.getCourseId()).findFirst()
            .ifPresent(courseCombo::setValue);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(10));
        String[] labels = {"Reg Number:", "First Name:", "Last Name:", "Email:", "Phone:", "Course:", "Enrolled Date:"};
        javafx.scene.Node[] fields = {regF, fnF, lnF, emF, phF, courseCombo, dpF};
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(440);
        styleDialog(dialog);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Student s = isEdit ? existing : new Student();
                s.setRegNumber(regF.getText().trim());
                s.setFirstName(fnF.getText().trim());
                s.setLastName(lnF.getText().trim());
                s.setEmail(emF.getText().trim());
                s.setPhone(phF.getText().trim());
                if (courseCombo.getValue() != null) s.setCourseId(courseCombo.getValue().getCourseId());
                s.setEnrolledAt(dpF.getValue());
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            try {
                if (isEdit) studentService.updateStudent(s);
                else        studentService.addStudent(s);
                refreshTable(table, studentService.getAllStudents());
            } catch (Exception e) { alert(e.getMessage()); }
        });
    }

    //  LECTURERS PANEL
    private VBox buildLecturersPanel() {
        VBox root = new VBox(16);
        Label title = new Label("Lecturer Management");
        title.getStyleClass().add("page-title");

        HBox toolbar = new HBox(10);
        Button addBtn = new Button("+ Add Lecturer");
        addBtn.getStyleClass().add("btn-primary");
        toolbar.getChildren().add(addBtn);

        TableView<Lecturer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().addAll(
            col("Employee ID", "employeeId"),
            col("Full Name",   "fullName"),
            col("Username",    "username"),
            col("Department",  "department"),
            actionColumn(table,
                l -> showLecturerDialog(l, table),
                l -> { try { if (confirm("Delete lecturer?")) { lecturerService.deleteLecturer(l.getUserId()); refreshTable(table, lecturerService.getAllLecturers()); } } catch (Exception e) { alert(e.getMessage()); } }
            )
        );

        try { table.setItems(FXCollections.observableArrayList(lecturerService.getAllLecturers())); }
        catch (SQLException e) { alert(e.getMessage()); }

        addBtn.setOnAction(e -> showLecturerDialog(null, table));
        root.getChildren().addAll(title, toolbar, table);
        return root;
    }

    private void showLecturerDialog(Lecturer existing, TableView<Lecturer> table) {
        boolean isEdit = existing != null;
        Dialog<Lecturer> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Lecturer" : "Add Lecturer");

        TextField empF  = styledField(isEdit ? existing.getEmployeeId() : "");
        TextField nameF = styledField(isEdit ? existing.getFullName() : "");
        TextField userF = styledField(isEdit ? existing.getUsername() : "");
        TextField passF = styledField(isEdit ? existing.getPassword() != null ? existing.getPassword() : "" : "");
        TextField deptF = styledField(isEdit ? existing.getDepartment() : "");

        GridPane grid = formGrid(
            "Employee ID:", empF,
            "Full Name:",   nameF,
            "Username:",    userF,
            "Password:",    passF,
            "Department:",  deptF
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(420);
        styleDialog(dialog);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Lecturer l = isEdit ? existing : new Lecturer();
                l.setEmployeeId(empF.getText().trim());
                l.setFullName(nameF.getText().trim());
                l.setUsername(userF.getText().trim());
                l.setPassword(passF.getText().trim());
                l.setDepartment(deptF.getText().trim());
                return l;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(l -> {
            try {
                if (isEdit) lecturerService.updateLecturer(l);
                else        lecturerService.addLecturer(l);
                refreshTable(table, lecturerService.getAllLecturers());
            } catch (Exception e) { alert(e.getMessage()); }
        });
    }

    //  CLASS SCHEDULE PANEL

    private VBox buildSchedulePanel() {
        VBox root = new VBox(16);
        Label title = new Label("Class Schedule");
        title.getStyleClass().add("page-title");

        HBox toolbar = new HBox(10);
        Button addBtn = new Button("+ Schedule Class");
        addBtn.getStyleClass().add("btn-primary");
        toolbar.getChildren().add(addBtn);

        TableView<ClassSession> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ClassSession, String> c1 = col("Subject", "subjectName");
        TableColumn<ClassSession, String> c2 = col("Lecturer", "lecturerName");
        TableColumn<ClassSession, String> c3 = new TableColumn<>("Date");
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSessionDate().toString()));
        TableColumn<ClassSession, String> c4 = new TableColumn<>("Start");
        c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStartTime().toString()));
        TableColumn<ClassSession, String> c5 = new TableColumn<>("End");
        c5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEndTime().toString()));
        TableColumn<ClassSession, String> c6 = col("Venue", "venue");

        TableColumn<ClassSession, Void> colA = actionColumn(table,
            s -> showSessionDialog(s, table),
            s -> { try { if (confirm("Delete this session?")) { sessionService.deleteSession(s.getSessionId()); refreshTable(table, sessionService.getAllSessions()); } } catch (Exception e) { alert(e.getMessage()); } }
        );

        table.getColumns().addAll(c1, c2, c3, c4, c5, c6, colA);

        try { table.setItems(FXCollections.observableArrayList(sessionService.getAllSessions())); }
        catch (SQLException e) { alert(e.getMessage()); }

        addBtn.setOnAction(e -> showSessionDialog(null, table));
        root.getChildren().addAll(title, toolbar, table);
        return root;
    }

    private void showSessionDialog(ClassSession existing, TableView<ClassSession> table) {
        boolean isEdit = existing != null;
        Dialog<ClassSession> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Session" : "Schedule Class");

        ComboBox<Subject>  subjectCombo  = new ComboBox<>();
        ComboBox<Lecturer> lecturerCombo = new ComboBox<>();
        DatePicker datePicker = new DatePicker(isEdit ? existing.getSessionDate() : LocalDate.now());
        TextField startF = styledField(isEdit ? existing.getStartTime().toString() : "09:00");
        TextField endF   = styledField(isEdit ? existing.getEndTime().toString()   : "11:00");
        TextField venueF = styledField(isEdit ? existing.getVenue() : "");

        try {
            subjectCombo.setItems(FXCollections.observableArrayList(courseService.getAllSubjects()));
            lecturerCombo.setItems(FXCollections.observableArrayList(lecturerService.getAllLecturers()));
            if (isEdit) {
                subjectCombo.getItems().stream().filter(s -> s.getSubjectId() == existing.getSubjectId()).findFirst().ifPresent(subjectCombo::setValue);
                lecturerCombo.getItems().stream().filter(l -> l.getLecturerId() == existing.getLecturerId()).findFirst().ifPresent(lecturerCombo::setValue);
            }
        } catch (SQLException e) { alert(e.getMessage()); }

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(10));
        String[] labels = {"Subject:", "Lecturer:", "Date:", "Start Time (HH:mm):", "End Time (HH:mm):", "Venue:"};
        javafx.scene.Node[] fields = {subjectCombo, lecturerCombo, datePicker, startF, endF, venueF};
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(440);
        styleDialog(dialog);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                ClassSession cs = isEdit ? existing : new ClassSession();
                if (subjectCombo.getValue()  != null) cs.setSubjectId(subjectCombo.getValue().getSubjectId());
                if (lecturerCombo.getValue() != null) cs.setLecturerId(lecturerCombo.getValue().getLecturerId());
                cs.setSessionDate(datePicker.getValue());
                try {
                    cs.setStartTime(java.time.LocalTime.parse(startF.getText().trim()));
                    cs.setEndTime(java.time.LocalTime.parse(endF.getText().trim()));
                } catch (Exception ex) { alert("Invalid time format. Use HH:mm"); return null; }
                cs.setVenue(venueF.getText().trim());
                return cs;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(cs -> {
            try {
                if (isEdit) sessionService.updateSession(cs);
                else        sessionService.addSession(cs);
                refreshTable(table, sessionService.getAllSessions());
            } catch (Exception e) { alert(e.getMessage()); }
        });
    }

    //  ATTENDANCE MARKING PANEL (Admin view)
    private VBox buildAttendancePanel() {
        VBox root = new VBox(16);
        Label title = new Label("Attendance Marking");
        title.getStyleClass().add("page-title");

        // Session selector
        ComboBox<ClassSession> sessionCombo = new ComboBox<>();
        try { sessionCombo.setItems(FXCollections.observableArrayList(sessionService.getAllSessions())); }
        catch (SQLException e) { alert(e.getMessage()); }

        Button loadBtn = new Button("Load Students");
        loadBtn.getStyleClass().add("btn-secondary");

        HBox selector = new HBox(12, new Label("Select Session:"), sessionCombo, loadBtn);
        selector.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TableView<Attendance> attTable = new TableView<>();
        attTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(attTable, Priority.ALWAYS);

        TableColumn<Attendance, String> c1 = col("Reg No.", "regNumber");
        TableColumn<Attendance, String> c2 = col("Student", "studentName");
        TableColumn<Attendance, String> c3 = new TableColumn<>("Status");
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        c3.setCellFactory(col -> new StatusComboCell());

        attTable.getColumns().addAll(c1, c2, c3);

        Button saveBtn = new Button("Save Attendance");
        saveBtn.getStyleClass().add("btn-success");

        loadBtn.setOnAction(e -> {
            if (sessionCombo.getValue() == null) return;
            try {
                List<Attendance> sheet = attService.getAttendanceSheet(sessionCombo.getValue().getSessionId());
                attTable.setItems(FXCollections.observableArrayList(sheet));
            } catch (SQLException ex) { alert(ex.getMessage()); }
        });

        saveBtn.setOnAction(e -> {
            try {
                attService.saveAll(attTable.getItems());
                showInfo("Attendance saved successfully!");
            } catch (SQLException ex) { alert(ex.getMessage()); }
        });

        root.getChildren().addAll(title, selector, attTable, saveBtn);
        return root;
    }

    //  REPORTS PANEL

    private VBox buildReportsPanel() {
        VBox root = new VBox(16);
        Label title = new Label("Attendance Reports");
        title.getStyleClass().add("page-title");

        // Filter row
        ComboBox<Student> studentCombo = new ComboBox<>();
        ComboBox<Subject> subjectCombo = new ComboBox<>();
        DatePicker fromDP = new DatePicker();
        DatePicker toDP   = new DatePicker();
        Button searchBtn  = new Button("🔍 Search");
        searchBtn.getStyleClass().add("btn-primary");

        try {
            studentCombo.getItems().add(null);
            studentCombo.getItems().addAll(studentService.getAllStudents());
            subjectCombo.getItems().add(null);
            subjectCombo.getItems().addAll(courseService.getAllSubjects());
        } catch (SQLException e) { alert(e.getMessage()); }

        studentCombo.setPromptText("All Students");
        subjectCombo.setPromptText("All Subjects");
        fromDP.setPromptText("From Date");
        toDP.setPromptText("To Date");

        HBox filters = new HBox(10, new Label("Student:"), studentCombo,
            new Label("Subject:"), subjectCombo,
            new Label("From:"), fromDP,
            new Label("To:"), toDP,
            searchBtn);
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filters.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8;");

        TableView<Attendance> reportTable = new TableView<>();
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        TableColumn<Attendance, String> c1 = col("Reg No.", "regNumber");
        TableColumn<Attendance, String> c2 = col("Student", "studentName");
        TableColumn<Attendance, String> c3 = new TableColumn<>("Status");
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        TableColumn<Attendance, String> c4 = new TableColumn<>("Session ID");
        c4.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getSessionId())));

        reportTable.getColumns().addAll(c1, c2, c3, c4);

        searchBtn.setOnAction(e -> {
            int stuId  = studentCombo.getValue() != null ? studentCombo.getValue().getStudentId() : -1;
            int subId  = subjectCombo.getValue() != null ? subjectCombo.getValue().getSubjectId() : -1;
            try {
                List<Attendance> result = attService.getReport(stuId, subId, fromDP.getValue(), toDP.getValue());
                reportTable.setItems(FXCollections.observableArrayList(result));
            } catch (SQLException ex) { alert(ex.getMessage()); }
        });

        root.getChildren().addAll(title, filters, reportTable);
        return root;
    }

    //  HELPERS
    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> col(String header, String property) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    private <T> TableColumn<T, Void> actionColumn(TableView<T> table,
                                                   java.util.function.Consumer<T> editAction,
                                                   java.util.function.Consumer<T> deleteAction) {
        TableColumn<T, Void> col = new TableColumn<>("Actions");
        col.setMinWidth(140);
        col.setCellFactory(c -> new TableCell<>() {
            final Button editBtn   = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final HBox   box       = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-secondary");
                deleteBtn.getStyleClass().add("btn-danger");
                editBtn.setStyle("-fx-font-size:11px; -fx-padding: 4 10;");
                deleteBtn.setStyle("-fx-font-size:11px; -fx-padding: 4 10;");
                editBtn.setOnAction(e -> editAction.accept(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteAction.accept(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
        return col;
    }

    private <T> void refreshTable(TableView<T> table, List<T> data) {
        table.setItems(FXCollections.observableArrayList(data));
    }

    private TextField styledField(String value) {
        TextField tf = new TextField(value);
        tf.getStyleClass().add("text-field-custom");
        return tf;
    }

    private GridPane formGrid(Object... labelAndFields) {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        for (int i = 0; i < labelAndFields.length; i += 2) {
            Label l = new Label((String) labelAndFields[i]);
            l.setStyle("-fx-font-weight: bold;");
            javafx.scene.Node f = (javafx.scene.Node) labelAndFields[i + 1];
            grid.add(l, 0, i / 2);
            grid.add(f, 1, i / 2);
            GridPane.setHgrow(f, Priority.ALWAYS);
        }
        return grid;
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    //  INLINE CELL — Status ComboBox
    private static class StatusComboCell extends TableCell<Attendance, String> {
        private final ComboBox<String> combo = new ComboBox<>(
            FXCollections.observableArrayList("PRESENT", "ABSENT", "LATE")
        );
        {
            combo.setOnAction(e -> {
                Attendance a = getTableRow().getItem();
                if (a != null) a.setStatus(Attendance.Status.valueOf(combo.getValue()));
            });
        }
        @Override protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); return; }
            combo.setValue(item);
            setGraphic(combo);
        }
    }
}
