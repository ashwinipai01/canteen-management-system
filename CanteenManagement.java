import javax.swing.SwingUtilities;

public class CanteenManagement {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Login().displayLoginPage());
    }
}
 