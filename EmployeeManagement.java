import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class EmployeeManagement {
    public void displayEmployeePage() {
        while (true) {
            String[] options = {"Add Employee", "View Employees", "Remove Employee", "Exit to Main Menu"};
            int choice = JOptionPane.showOptionDialog(null, "Select an option:", "Owner Menu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == -1 || choice == 3) { // Exit to Main Menu
                break;
            }

            switch (choice) {
                case 0:
                    addEmployee();
                    break;
                case 1:
                    viewEmployees();
                    break;
                case 2:
                    removeEmployee();
                    break;
                
                default:
                    JOptionPane.showMessageDialog(null, "Invalid choice! Please try again.");
            }
        }
    }

    private void addEmployee() {
        JTextField nameField = new JTextField(15);
        JTextField ageField = new JTextField(15);
        JTextField salaryField = new JTextField(15);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Employee Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Age:"));
        inputPanel.add(ageField);
        inputPanel.add(new JLabel("Salary:"));
        inputPanel.add(salaryField);

        int result = JOptionPane.showConfirmDialog(null, inputPanel, "Add Employee", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = TextUtils.toSentenceCase(nameField.getText().trim());
            int age;
            long salary;

            try {
                age = Integer.parseInt(ageField.getText().trim());
                salary = Long.parseLong(salaryField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid input for age or salary!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM employees WHERE name = ?");
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO employees (name, age, salary) VALUES (?, ?, ?)")) {

                checkStmt.setString(1, name);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(null, "Employee with the same name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                ps.setString(1, name);
                ps.setInt(2, age);
                ps.setLong(3, salary);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(null, "Employee added successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error adding employee: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewEmployees() {
        StringBuilder employeesList = new StringBuilder("Employees:\n");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM employees");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                employeesList.append(String.format("ID: %d, Name: %s, Age: %d, Salary: ₹%d\n",
                        rs.getInt("id"), rs.getString("name"), rs.getInt("age"), rs.getLong("salary")));
            }

            if (employeesList.length() > 0) {
                JOptionPane.showMessageDialog(null, employeesList.toString(), "Employees", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "No employees found!", "View Employees", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error fetching employees: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeEmployee() {
        String employeeId = JOptionPane.showInputDialog(null, "Enter Employee ID to remove:", "Remove Employee", JOptionPane.QUESTION_MESSAGE);
        if (employeeId == null || employeeId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Employee ID is required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM employees WHERE id = ?")) {

            ps.setInt(1, Integer.parseInt(employeeId.trim()));
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Employee removed successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Employee not found!", "Remove Employee", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Error removing employee: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
