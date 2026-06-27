import javax.swing.*;
import java.awt.*;

public class Login {
    public void displayLoginPage() {
        JFrame frame = new JFrame("Canteen Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(3, 1));

        JLabel welcomeLabel = new JLabel("Welcome to JNNCE Canteen", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(welcomeLabel);

        JButton ownerButton = new JButton("Owner Login");
        JButton employeeButton = new JButton("Employee Login");
        JButton customerButton = new JButton("Customer Login");

        ownerButton.addActionListener(_ -> performLogin(frame, "OWNER"));
        employeeButton.addActionListener(_ -> performLogin(frame, "EMPLOYEE"));
        customerButton.addActionListener(_ -> performLogin(frame, "CUSTOMER"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(ownerButton);
        buttonPanel.add(employeeButton);
        buttonPanel.add(customerButton);

        frame.add(buttonPanel);
        frame.setVisible(true);
    }
    
    private void performLogin(JFrame frame, String role) {
        if ("CUSTOMER".equals(role)) {
            // No password required for customer login
            JOptionPane.showMessageDialog(frame, "Welcome, Customer!", "Customer Login", JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            new customer().displayCustomerMenu(); // Navigate to customer menu
            return;
        }
    
        // For OWNER and EMPLOYEE roles, require password
        JPasswordField passwordField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(new JLabel("Enter Password for " + role + ":"));
        panel.add(passwordField);
    
        int result = JOptionPane.showConfirmDialog(frame, panel, "Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
        if (result == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword()); // Get password as a String
            if (password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            if (validateUser(role, password)) {
                JOptionPane.showMessageDialog(frame, "Access Granted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
    
                if ("OWNER".equals(role)) {
                    new EmployeeManagement().displayEmployeePage();
                } else if ("EMPLOYEE".equals(role)) {
                    new Store().displayEmployeeMenu();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid Password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    
    private boolean validateUser(String role, String password) {
        if ("OWNER".equals(role) && "OWN".equals(password)) {
            return true; // Owner login
        } else if ("EMPLOYEE".equals(role) && "EMP".equals(password)) {
            return true; // Employee login
        }
        return false; // Invalid credentials
    }
}