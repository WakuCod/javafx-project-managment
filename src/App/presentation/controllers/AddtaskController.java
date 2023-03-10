package App.presentation.controllers;

import App.dao.*;
import com.jfoenix.controls.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class AddtaskController implements Initializable {

    public JFXTextField getTask_name() {
        return Task_name;
    }

    public JFXTextField getAssignedto() {
        return Assignedto;
    }

    public JFXDatePicker getTask_start_date() {
        return task_start_date;
    }

    public JFXDatePicker getTask_end_date() {
        return task_end_date;
    }

    public JFXColorPicker getTask_color() {
        return task_color;
    }


    public JFXTextField getTaskProjectID() {
        return TaskProjectID;
    }

    public void setProjectName(JFXTextField projectName) {
        TaskProjectName.setText(projectName.getText());
    }

    public void setProject_id_task(JFXTextField project_id_task) {
        getTaskProjectID().setText(project_id_task.getText());
    }
    final private String NO_START_DATE_ERROR = "Date de début non définie";
    final private String INVALID_END_DATE = "Date de fin doit être supérieure à la date de début";
    final private String NO_END_DATE_ERROR = "Date de fin non définie";

    @FXML
    private JFXTextField TaskProjectName;
    @FXML
    private JFXTextField TaskProjectID;
    @FXML
    private JFXTextField Task_name;
    @FXML
    private JFXTextField Assignedto;
    @FXML
    private JFXDatePicker task_start_date = new JFXDatePicker();
    @FXML
    private JFXDatePicker task_end_date = new JFXDatePicker();
    @FXML
    private JFXColorPicker task_color = new JFXColorPicker();
    @FXML
    private javafx.scene.control.Label invalid_date_label = new javafx.scene.control.Label();

    private ObservableList<String> dependencyTaskList = FXCollections.observableArrayList();
    private ObservableList<String> employeeList = FXCollections.observableArrayList();


    public void setDependency(ChoiceBox<String> dependency) {
        this.dependency = dependency;
    }

    public ChoiceBox<String> getDependency() {
        return dependency;
    }

    @FXML
    ChoiceBox<String> dependency = new ChoiceBox<String>();
    public ChoiceBox<String> employeeAssigned = new ChoiceBox<String>();

    private boolean hasValidDateRange() {
        if (task_start_date.getValue() == null || task_end_date.getValue() == null) {
            return false;
        }
        final Date startDate = Date.valueOf(task_start_date.getValue());
        final Date endDate = Date.valueOf(task_end_date.getValue());

        return endDate.compareTo(startDate) >= 0;
    }

    public void getDependencyList() {
        SingletonConnexionDB singletonConnexionDB = new SingletonConnexionDB();
        Connection connection = singletonConnexionDB.getConnection();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT task_name FROM project_task WHERE id=" + getTaskProjectID().getText());

            while (rs.next()) {
                dependencyTaskList.add(rs.getString("task_name"));
            }

            dependency.setItems(dependencyTaskList);

            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void getEmployeeList() {
        SingletonConnexionDB singletonConnexionDB = new SingletonConnexionDB();
        Connection connection= singletonConnexionDB.getConnection();
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT id, name FROM EMPLOYEE");

            while (rs.next()) {
                employeeList.add(rs.getInt("id") + " - " + rs.getString("name"));
            }

            employeeAssigned.setItems(employeeList);

            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @FXML
    private JFXButton AddTaskButton;


    private String calcDays(JFXDatePicker start_date, JFXDatePicker end_date) throws IOException {
        long intervalDays = (ChronoUnit.DAYS.between(start_date.getValue(), end_date.getValue()) + 1);
        return String.valueOf(intervalDays);
    }

    @FXML
    private void onDateChange(javafx.event.ActionEvent event) {
        if (hasValidDateRange()) {
            invalid_date_label.setText(null);
        }
        else if (task_start_date.getValue() == null) {
            invalid_date_label.setText(NO_START_DATE_ERROR);
        }
        else if (task_end_date.getValue() != null){
            invalid_date_label.setText(INVALID_END_DATE);
        }
        else if (event == null) {
            invalid_date_label.setText(NO_END_DATE_ERROR);

        }

        if (invalid_date_label.getText() != null) {
            invalid_date_label.setVisible(true);
        } else {
            invalid_date_label.setVisible(false);
        }
    }

    @FXML
    private void AddTaskDetail(javafx.event.ActionEvent event) throws Exception {
        if (hasValidDateRange() && event.getSource() == AddTaskButton) {
            invalid_date_label.setVisible(false);
            insertProjectTask();
        } else {
            onDateChange(null);
        }

    }

    private void insertProjectTask() {

        String[] EmpArrayStr = employeeAssigned.getValue().split("-");
        int assignedEmployeeId = Integer.parseInt(EmpArrayStr[0].trim());

        if(assignedEmployeeId == 0){
            invalid_date_label.setText("Un employé doit être selectionné.");
            return;
        } else {
            invalid_date_label.setText("");
        }

        SingletonConnexionDB singletonConnexionDB = new SingletonConnexionDB();
        Connection connection= singletonConnexionDB.getConnection();

        PreparedStatement ps = null;

        try {
            String sql = "insert into PROJECT_TASK(id,task_name, task_time, task_start_date, task_end_date, progress, color, dependency, assigned) values (?,?,?,?,?,?,?,?,?)";
            ps = connection.prepareStatement(sql);
            ps.setString(1, getTaskProjectID().getText());
            ps.setString(2, getTask_name().getText());
            ps.setString(3, calcDays(task_start_date,task_end_date) + " Jours");
            ps.setDate(4, Date.valueOf(task_start_date.getValue()));
            ps.setDate(5, Date.valueOf(task_end_date.getValue()));
            ps.setString(6, "0%");
            ps.setString(7, String.valueOf(getTask_color().getValue()));
            ps.setString(8, dependency.getValue() == null ? "" : dependency.getValue());
            ps.setInt(9, assignedEmployeeId);
            ps.executeUpdate();

            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


        Stage stage = (Stage) AddTaskButton.getScene().getWindow();
        stage.close();
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

}
