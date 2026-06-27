public class ConsoleUtil {
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void gotoxy(int x, int y) {
        for (int i = 0; i < y; i++) System.out.println();
        for (int i = 0; i < x; i++) System.out.print(" ");
    }
}