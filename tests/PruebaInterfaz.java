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
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Prueba opcional en escritorio. Captura únicamente la ventana de la calculadora. */
public final class PruebaInterfaz {
    private static JFrame ventana;
    private static List<Component> componentes;
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> Aplicacion.main(new String[0]));
            SwingUtilities.invokeAndWait(() -> {
                for (Window w : Window.getWindows()) if (w instanceof JFrame f && f.isVisible()) ventana=f;
                comprobar(ventana!=null,"Ventana visible");
                componentes=descendientes(ventana);
                comprobar(componentes.stream().noneMatch(c -> c instanceof JComboBox<?>),"Sin selector antiguo");
                comprobar(componentes.stream().noneMatch(c -> c instanceof JLabel l && l.getText().contains("Prolog")),"Sin mensajes técnicos");
                campo("expresion").requestFocusInWindow();
                campo("expresion").setText("");
                boton("tecla-2").doClick(); boton("tecla-x").doClick();
                comprobar(campo("expresion").getText().equals("2x"),"Teclado inserta 2x");
                boton("calcular").doClick();
            });
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeAndWait(() -> {
                comprobar(componentes.stream().anyMatch(c -> c instanceof JLabel l && l.getText().equals("4")),"Resultado 2x = 4");
                captura("docs/captura.png");
                boton("ejemplos").doClick();
                JPopupMenu popup=null;
                for (MenuElement e:MenuSelectionManager.defaultManager().getSelectedPath()) if(e instanceof JPopupMenu p) popup=p;
                comprobar(popup!=null && popup.isVisible(),"Menú Ejemplos visible");
                comprobar(popup.getComponentCount()==8,"Ocho ejemplos");
                JMenuItem seno=(JMenuItem)popup.getComponent(3);
                seno.doClick();
                comprobar(campo("expresion").getText().equals("sin(x)"),"Ejemplo carga función");
                comprobar(campo("superior").getText().equals("pi"),"Ejemplo carga límite");
            });
            // La edición invalida los resultados anteriores.
            SwingUtilities.invokeAndWait(() -> comprobar(primitiva().getText().equals("—"),"Sin resultados obsoletos"));
            SwingUtilities.invokeAndWait(() -> boton("calcular").doClick());
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeAndWait(() -> {
                captura("docs/captura-seno.png");
                // Confirmar inserción en el límite enfocado, no en la función.
                campo("superior").requestFocusInWindow();
            });
            SwingUtilities.invokeAndWait(() -> {
                campo("superior").setText(""); boton("tecla-π").doClick();
                comprobar(campo("superior").getText().equals("π"),"Teclado escribe en límite activo");
                campo("expresion").setText("1/x"); campo("inferior").setText("-1"); campo("superior").setText("1");
                boton("calcular").doClick();
            });
            esperar(() -> primitiva().getText().contains("continua/real"));
            SwingUtilities.invokeAndWait(() -> {
                comprobar(componentes.stream().anyMatch(c -> c instanceof JLabel l && l.getText().equals("Revisa tu integral")),"Error visible");
                ventana.setSize(980,790); ventana.validate();
                comprobar(boton("calcular").getHeight()>=40,"Controles utilizables en tamaño mínimo");
            });
            System.out.println("Swing: teclado, 2x, ejemplos, límites, resultados, error y tamaño mínimo correctos.");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); });
        }
    }
    private static JTextArea primitiva() {
        return componentes.stream().filter(c -> c instanceof JTextArea t && t.getFont().getSize()==22)
                .map(JTextArea.class::cast).findFirst().orElseThrow();
    }
    private static JButton boton(String nombre) {
        return componentes.stream().filter(c -> c instanceof JButton && nombre.equals(c.getName()))
                .map(JButton.class::cast).findFirst().orElseThrow();
    }
    private static JTextField campo(String nombre) {
        return componentes.stream().filter(c -> c instanceof JTextField && nombre.equals(c.getName()))
                .map(JTextField.class::cast).findFirst().orElseThrow();
    }
    private static List<Component> descendientes(Container raiz) {
        List<Component> r=new ArrayList<>();
        for(Component c:raiz.getComponents()) { r.add(c); if(c instanceof Container cont) r.addAll(descendientes(cont)); }
        return r;
    }
    private static void comprobar(boolean valor,String nombre) { if(!valor) throw new AssertionError(nombre); }
    private static void esperar(BooleanSupplier condicion) throws Exception {
        for(int i=0;i<220;i++) {
            AtomicBoolean listo=new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> listo.set(condicion.getAsBoolean()));
            if(listo.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("No llegó el resultado esperado.");
    }
    private static void captura(String nombre) {
        BufferedImage img=new BufferedImage(ventana.getWidth(),ventana.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics(); ventana.paintAll(g); g.dispose();
        try { Files.createDirectories(Path.of("docs")); ImageIO.write(img,"png",Path.of(nombre).toFile()); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
}
