import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Store {

    public void displayEmployeeMenu() {
        while (true) {
            String[] options = {"Order Food", "Add Food", "Delete Food", "Search Food", "Generate Bill", "Exit to Main Menu"};
            int choice = JOptionPane.showOptionDialog(null, "Select an option:", "Employee Menu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == -1 || choice == 5) {  // Exit to Main Menu
                JOptionPane.showMessageDialog(null, "Returning to Main Menu.");
                break;
            }

            switch (choice) {
                case 0:
                    orderFood();  // Order food
                    break;
                case 1:
                    addFoodItem(null);  // Add food item
                    break;
                case 2:
                    deleteFoodItem();  // Delete food item
                    break;
                case 3:
                    searchFoodItem();  // Search food item
                    break;
                case 4:
                    generateBill();  // Generate bill
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Invalid choice! Please try again.");
            }
        }
    }

    private void orderFood() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String tableNumberInput = JOptionPane.showInputDialog(null, "Enter Table Number (1-10):", "Table Number", JOptionPane.QUESTION_MESSAGE);
            
            if (tableNumberInput == null || tableNumberInput.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Table number is required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int tableNumber;
            try {
                tableNumber = Integer.parseInt(tableNumberInput.trim());
                if (tableNumber < 1 || tableNumber > 10) {
                    JOptionPane.showMessageDialog(null, "Table number must be between 1 and 10!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid table number! Please enter a number between 1 and 10.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String checkTableQuery = "SELECT order_id FROM orders WHERE table_number = ? AND order_closed = 0";
            int orderId;

            try (PreparedStatement checkTableStmt = conn.prepareStatement(checkTableQuery)) {
                checkTableStmt.setInt(1, tableNumber);
                ResultSet rs = checkTableStmt.executeQuery();

                if (rs.next()) {
                    orderId = rs.getInt("order_id");
                } else {
                    String createOrderQuery = "INSERT INTO orders (table_number, order_closed) VALUES (?, 0)";
                    try (PreparedStatement createOrderStmt = conn.prepareStatement(createOrderQuery, Statement.RETURN_GENERATED_KEYS)) {
                        createOrderStmt.setInt(1, tableNumber);
                        createOrderStmt.executeUpdate();

                        ResultSet generatedKeys = createOrderStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            orderId = generatedKeys.getInt(1);
                        } else {
                            JOptionPane.showMessageDialog(null, "Error creating order. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }

            while (true) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM food_menu", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet rs = ps.executeQuery();

                StringBuilder menu = new StringBuilder("Available Menu:\n");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    menu.append(String.format("%d. %s, Price: ₹%.2f%n", id, name, price));
                }

                String itemInput = JOptionPane.showInputDialog(null, menu.toString() + "\nEnter the item number to order:");
                if (itemInput == null || itemInput.trim().isEmpty()) {
                    break;
                }

                try {
                    int itemNumber = Integer.parseInt(itemInput.trim());

                    rs.beforeFirst();
                    while (rs.next()) {
                        if (rs.getInt("id") == itemNumber) {
                            String itemName = rs.getString("name");
                            double price = rs.getDouble("price");

                            String quantityInput = JOptionPane.showInputDialog(null, "Enter quantity for " + itemName + ":");
                            if (quantityInput != null && !quantityInput.trim().isEmpty()) {
                                int quantity = Integer.parseInt(quantityInput.trim());
                                double totalCost = price * quantity;

                                String orderItemQuery = "INSERT INTO order_items (order_id, food_item_id, quantity, total_price) VALUES (?, ?, ?, ?)";
                                try (PreparedStatement orderItemStmt = conn.prepareStatement(orderItemQuery)) {
                                    orderItemStmt.setInt(1, orderId);
                                    orderItemStmt.setInt(2, itemNumber);
                                    orderItemStmt.setInt(3, quantity);
                                    orderItemStmt.setDouble(4, totalCost);
                                    orderItemStmt.executeUpdate();
                                }
                                JOptionPane.showMessageDialog(null, String.format("%d x %s added to your order. Total: ₹%.2f", quantity, itemName, totalCost));
                            }
                            break;
                        }
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid input! Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                }

                int choice = JOptionPane.showConfirmDialog(null, "Do you want to add more items?", "Add More Items", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) break;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error placing order. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addFoodItem(JFrame parentFrame) {
        JTextField nameField = new JTextField(15);
        JTextField priceField = new JTextField(15);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Food Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(priceField);

        int result = JOptionPane.showConfirmDialog(parentFrame, inputPanel, "Add Food Item", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String name = toSentenceCase(nameField.getText().trim());
            double price;

            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Invalid price input!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String checkFoodQuery = "SELECT * FROM food_menu WHERE LOWER(name) = LOWER(?)";

                try (PreparedStatement checkFoodStmt = conn.prepareStatement(checkFoodQuery)) {
                    checkFoodStmt.setString(1, name);
                    ResultSet rs = checkFoodStmt.executeQuery();

                    if (rs.next()) {
                        JOptionPane.showMessageDialog(parentFrame, "Food item already exists!", "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                String sql = "INSERT INTO food_menu (name, price) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setDouble(2, price);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(parentFrame, "Food item added successfully!");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Error adding item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String toSentenceCase(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder sentenceCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                sentenceCase.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1))
                            .append(" ");
            }
        }
        return sentenceCase.toString().trim();
    }



    private void searchFoodItem() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM food_menu");
             ResultSet rs = ps.executeQuery()) {

            StringBuilder menu = new StringBuilder("Available Food Items:\n");
            while (rs.next()) {
                menu.append(String.format("%d. %s - ₹%.2f%n", rs.getInt("id"), rs.getString("name"), rs.getDouble("price")));
            }

            if (menu.length() == 0) {
                JOptionPane.showMessageDialog(null, "No food items available!", "View Menu", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, menu.toString(), "Menu", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error fetching menu: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void deleteFoodItem() {
        String foodId = JOptionPane.showInputDialog(null, "Enter Food ID to delete:", "Delete Food", JOptionPane.QUESTION_MESSAGE);
        if (foodId == null || foodId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Food ID cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM food_menu WHERE id = ?")) {
    
            ps.setInt(1, Integer.parseInt(foodId.trim()));
            int rowsAffected = ps.executeUpdate();
    
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Food item deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Food item not found!");
            }
    
        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Error deleting food item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void generateBill() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Prompt the user for a table number
            String tableNumberInput = JOptionPane.showInputDialog(null, "Enter Table Number (1-10):", "Generate Bill", JOptionPane.QUESTION_MESSAGE);
    
            if (tableNumberInput == null || tableNumberInput.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Table number is required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            int tableNumber;
            try {
                tableNumber = Integer.parseInt(tableNumberInput.trim());
                if (tableNumber < 1 || tableNumber > 10) {
                    JOptionPane.showMessageDialog(null, "Table number must be between 1 and 10!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid table number! Please enter a number between 1 and 10.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            String sql = "SELECT f.name AS item_name, oi.quantity, oi.total_price " +
                    "FROM order_items oi " +
                    "JOIN food_menu f ON oi.food_item_id = f.id " +
                    "JOIN orders o ON oi.order_id = o.order_id " +
                    "WHERE o.table_number = ? AND o.order_closed = 0";
    
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tableNumber);
                ResultSet rs = ps.executeQuery();
    
                double totalBill = 0.0;
                StringBuilder bill = new StringBuilder("----- Bill Details for Table ").append(tableNumber).append(" -----\n\n");
    
                boolean itemsFound = false;
                while (rs.next()) {
                    itemsFound = true;
                    String itemName = rs.getString("item_name");
                    int quantity = rs.getInt("quantity");
                    double totalPrice = rs.getDouble("total_price");
    
                    totalBill += totalPrice;
                    bill.append(String.format("%d x %s, Total: ₹%.2f%n", quantity, itemName, totalPrice));
                }
    
                if (!itemsFound) {
                    JOptionPane.showMessageDialog(null, "No items found for Table " + tableNumber, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
    
                bill.append("\nTotal Amount: ₹").append(totalBill);
    
                // Mark the order for this table as closed
                String closeOrderQuery = "UPDATE orders SET order_closed = 1 WHERE table_number = ?";
                try (PreparedStatement closeOrderStmt = conn.prepareStatement(closeOrderQuery)) {
                    closeOrderStmt.setInt(1, tableNumber);
                    closeOrderStmt.executeUpdate();
                }
    
                // Create a panel for displaying the bill and QR code
                JPanel panel = new JPanel(new BorderLayout());
    
                JTextArea billArea = new JTextArea(bill.toString());
                billArea.setEditable(false);
                billArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    
                // Load and resize QR code image
                ImageIcon originalIcon = new ImageIcon("C:\\Users\\HP\\Desktop\\mini project01\\mini project01\\mini project\\qr_code.jpg"); // Replace with your QR code image path
                if (originalIcon.getIconWidth() == -1) {
                    JOptionPane.showMessageDialog(null, "QR code image not found or invalid.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
    
                Image scaledImage = originalIcon.getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                JLabel qrLabel = new JLabel(scaledIcon, JLabel.CENTER);
    
                panel.add(new JScrollPane(billArea), BorderLayout.CENTER);
                panel.add(qrLabel, BorderLayout.SOUTH);
    
                JOptionPane.showMessageDialog(null, panel, "Generate Bill", JOptionPane.INFORMATION_MESSAGE);
    
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error generating bill: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database connection error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    
    
}