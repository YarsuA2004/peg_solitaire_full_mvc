import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PegSolitaireView view = new PegSolitaireView();
            new PegSolitaireController(view);
            view.setVisible(true);
        });
    }
}
