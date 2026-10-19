import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Prueba opcional: necesita escritorio. Captura SOLO la ventana de la aplicación. */
public final class PruebaInterfaz {
    private static JFrame ventana;
    private static List<Component> componentes;
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> Aplicacion.main(new String[0]));
            SwingUtilities.invokeAndWait(() -> {
                for (Window w : Window.getWindows()) if (w instanceof JFrame f && f.isVisible()) ventana = f;
                if (ventana == null) throw new AssertionError("No se abrió la ventana.");
                componentes = descendientes(ventana);
                boton().doClick();
            });
            esperar("Calculado por");
            SwingUtilities.invokeAndWait(() -> {
                boolean primitiva = componentes.stream().anyMatch(c -> c instanceof JTextArea t && t.getText().contains("F(x) ="));
                if (!primitiva) throw new AssertionError("No se mostró la primitiva.");
                BufferedImage captura = new BufferedImage(ventana.getWidth(), ventana.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = captura.createGraphics(); ventana.paintAll(g); g.dispose();
                try { Files.createDirectories(Path.of("docs")); ImageIO.write(captura,"png",Path.of("docs/captura.png").toFile()); }
                catch (Exception e) { throw new RuntimeException(e); }
                List<JTextField> campos = componentes.stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).toList();
                campos.get(0).setText("1/x"); campos.get(1).setText("-1"); campos.get(2).setText("1");
                boton().doClick();
            });
            esperar("No se pudo calcular");
            SwingUtilities.invokeAndWait(() -> {
                boolean rechazo = componentes.stream().anyMatch(c -> c instanceof JTextArea t && t.getText().contains("continua/real"));
                if (!rechazo) throw new AssertionError("No se mostró el rechazo de dominio.");
            });
            System.out.println("Swing: cálculo real, primitiva visible, error de dominio y captura correctos.");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window w : Window.getWindows()) w.dispose(); });
        }
    }
    private static JButton boton() {
        return componentes.stream().filter(c -> c instanceof JButton b && b.getText().startsWith("Calcular integral"))
                .map(JButton.class::cast).findFirst().orElseThrow();
    }
    private static List<Component> descendientes(Container raiz) {
        List<Component> resultado = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            resultado.add(c); if (c instanceof Container contenedor) resultado.addAll(descendientes(contenedor));
        }
        return resultado;
    }
    private static void esperar(String prefijo) throws Exception {
        for (int i = 0; i < 220; i++) {
            AtomicBoolean listo = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> listo.set(componentes.stream().anyMatch(c -> c instanceof JLabel l && l.getText().startsWith(prefijo))));
            if (listo.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Tiempo agotado esperando: " + prefijo);
    }
}
