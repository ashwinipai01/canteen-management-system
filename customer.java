import javax.swing.*;
import java.sql.*;
import java.awt.*;



public class customer {
    public void displayCustomerMenu() {
        while (true) {
            String[] options = {"View Menu", "Order Food", "Generate Bill", "Exit"};
            int choice = JOptionPane.showOptionDialog(null, "Select an option:", "Customer Menu",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == -1 || choice == 3) { // Exit
                JOptionPane.showMessageDialog(null, "Thank you for visiting!", "Goodbye", JOptionPane.INFORMATION_MESSAGE);
                break;
            }

            switch (choice) {
                case 0:
                    viewMenu(); // View menu
                    break;
                case 1:
                    orderFood(); // Order food
                    break;
                case 2:
                    generateBill(); // Generate bill
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Invalid choice! Please try again.");
            }
        }
    }

    private void viewMenu() {
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
    private void orderFood() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Prompt for table number
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
    
            // Check if the table is already occupied
            String checkTableQuery = "SELECT order_id FROM orders WHERE table_number = ? AND order_closed = 0";
            int orderId;
            try (PreparedStatement checkTableStmt = conn.prepareStatement(checkTableQuery)) {
                checkTableStmt.setInt(1, tableNumber);
                ResultSet rs = checkTableStmt.executeQuery();
                if (rs.next()) {
                    // Active order exists for the table
                    orderId = rs.getInt("order_id");
                } else {
                    // Create a new order for the table
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
    
            // Fetch food menu and allow ordering
            while (true) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM food_menu", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet rs = ps.executeQuery();
    
                StringBuilder menu = new StringBuilder("Available Menu:\n");
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    menu.append(String.format("%d. %s, Price: ₹%.2f\n", id, name, price));
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
    
                                // Add item to the order
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
                if (choice == JOptionPane.NO_OPTION) {
                    break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error updating order. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
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