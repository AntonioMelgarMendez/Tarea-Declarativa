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
                comprobar(!boton("procedimiento").isEnabled(),"Procedimiento desactivado sin cálculo");
                comprobarCentrado();
                captura("docs/captura-vacia.png");
                campo("expresion").requestFocusInWindow();
                campo("expresion").setText("");
                boton("tecla-2").doClick(); boton("tecla-x").doClick();
                comprobar(campo("expresion").getText().equals("2x"),"Teclado inserta 2x");
                boton("calcular").doClick();
            });
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeAndWait(() -> {
                comprobar(componentes.stream().anyMatch(c -> c instanceof JLabel l && l.getText().equals("4")),"Resultado 2x = 4");
                comprobar(!vacio().isVisible(),"Mensaje oculto al mostrar curva");
                comprobar(!vacio().getParent().isVisible(),"Tarjeta oculta al mostrar curva");
                comprobar(boton("procedimiento").isEnabled(),"Procedimiento disponible tras calcular");
                captura("docs/captura.png");
            });
            SwingUtilities.invokeLater(() -> boton("procedimiento").doClick());
            esperar(() -> dialogoProcedimiento()!=null);
            SwingUtilities.invokeAndWait(() -> {
                JDialog dialogo=dialogoProcedimiento();
                comprobar(dialogo!=null,"Diálogo visible");
                var contenido=descendientes(dialogo);
                comprobar(contenido.stream().noneMatch(c -> c instanceof JOptionPane),"Sin cuadro genérico antiguo");
                comprobar(contenido.stream().filter(c -> c instanceof JScrollPane && "pasos-procedimiento".equals(c.getName()))
                        .map(JScrollPane.class::cast).noneMatch(s -> s.getVerticalScrollBar().isVisible()),
                        "Procedimiento corto completo sin desplazamiento");
                comprobar(contenido.stream().anyMatch(c -> c instanceof JTextArea t
                        && "resultado-procedimiento".equals(c.getName()) && t.getText().endsWith("= 4")),"Resultado en procedimiento");
                captura(dialogo,"docs/captura-procedimiento.png");
                contenido.stream().filter(c -> c instanceof JButton && "cerrar-procedimiento".equals(c.getName()))
                        .map(JButton.class::cast).findFirst().orElseThrow().doClick();
            });
            esperar(() -> dialogoProcedimiento()==null);
            SwingUtilities.invokeAndWait(() -> {
                campo("expresion").setText("3x"); boton("calcular").doClick();
            });
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeAndWait(() -> {
                comprobar(primitiva().getText().equals("F(x) = 3/2·x² + C"),"Fracción visible en antiderivada");
                captura("docs/captura-fraccion.png");
            });
            SwingUtilities.invokeAndWait(() -> {
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
            SwingUtilities.invokeAndWait(() -> {
                comprobar(primitiva().getText().equals("—"),"Sin resultados obsoletos");
                comprobar(!boton("procedimiento").isEnabled(),"No mostrar procedimiento obsoleto");
            });
            SwingUtilities.invokeAndWait(() -> boton("calcular").doClick());
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeAndWait(() -> {
                captura("docs/captura-seno.png");
                campo("expresion").setText("x+x+x+x+x+x+x+x+x+x+x+x+x+x+x+x");
                campo("inferior").setText("0"); campo("superior").setText("1");
                boton("calcular").doClick();
            });
            esperar(() -> primitiva().getText().startsWith("F(x) ="));
            SwingUtilities.invokeLater(() -> boton("procedimiento").doClick());
            esperar(() -> dialogoProcedimiento()!=null);
            SwingUtilities.invokeAndWait(() -> {
                JDialog dialogo=dialogoProcedimiento();
                var scroll=descendientes(dialogo).stream()
                        .filter(c -> c instanceof JScrollPane && "pasos-procedimiento".equals(c.getName()))
                        .map(JScrollPane.class::cast).findFirst().orElseThrow();
                comprobar(scroll.getVerticalScrollBar().getMaximum()>scroll.getVerticalScrollBar().getVisibleAmount(),
                        "Procedimiento largo desplazable");
                Object enlace=dialogo.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke("ESCAPE"));
                comprobar(enlace!=null,"Escape configurado");
                dialogo.getRootPane().getActionMap().get(enlace).actionPerformed(
                        new java.awt.event.ActionEvent(dialogo,java.awt.event.ActionEvent.ACTION_PERFORMED,"escape"));
            });
            esperar(() -> dialogoProcedimiento()==null);
            SwingUtilities.invokeAndWait(() -> {
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
                comprobar(!boton("procedimiento").isEnabled(),"Sin procedimiento tras error");
                ventana.setSize(980,790); ventana.validate();
                comprobar(boton("calcular").getHeight()>=40,"Controles utilizables en tamaño mínimo");
                comprobarCentrado();
            });
            System.out.println("Swing: estado vacío centrado, procedimiento, teclado, resultados y errores correctos.");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for(Window w:Window.getWindows()) w.dispose(); });
        }
    }
    private static JTextArea primitiva() {
        return componentes.stream().filter(c -> c instanceof JTextArea t && t.getFont().getSize()==22)
                .map(JTextArea.class::cast).findFirst().orElseThrow();
    }
    private static JLabel vacio() {
        return componentes.stream().filter(c -> c instanceof JLabel && "grafica-vacia".equals(c.getName()))
                .map(JLabel.class::cast).findFirst().orElseThrow();
    }
    private static void comprobarCentrado() {
        JLabel mensaje=vacio();
        comprobar(mensaje.isVisible(),"Estado vacío visible");
        comprobar(Math.abs(mensaje.getX()+mensaje.getWidth()/2.0-mensaje.getParent().getWidth()/2.0)<=1,
                "Mensaje centrado horizontalmente");
        comprobar(Math.abs(mensaje.getY()+mensaje.getHeight()/2.0-mensaje.getParent().getHeight()/2.0)<=1,
                "Mensaje centrado verticalmente");
        Container tarjeta=mensaje.getParent();
        comprobar("tarjeta-grafica-vacia".equals(tarjeta.getName()),"Mensaje dentro de tarjeta");
        comprobar(tarjeta.getBackground().getGreen()>tarjeta.getBackground().getRed(),"Fondo verde suave");
        comprobar(Math.abs(tarjeta.getX()+tarjeta.getWidth()/2.0-tarjeta.getParent().getWidth()/2.0)<=1,
                "Tarjeta centrada horizontalmente");
        comprobar(Math.abs(tarjeta.getY()+tarjeta.getHeight()/2.0-tarjeta.getParent().getHeight()/2.0)<=1,
                "Tarjeta centrada verticalmente");
    }
    private static JDialog dialogoProcedimiento() {
        for(Window w:Window.getWindows()) if(w instanceof JDialog d && d.isVisible()
                && "dialogo-procedimiento".equals(d.getName())) return d;
        return null;
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
        captura(ventana,nombre);
    }
    private static void captura(Window objetivo,String nombre) {
        Component contenido=objetivo instanceof RootPaneContainer raiz?raiz.getContentPane():objetivo;
        BufferedImage img=new BufferedImage(contenido.getWidth(),contenido.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics(); contenido.paintAll(g); g.dispose();
        try { Files.createDirectories(Path.of("docs")); ImageIO.write(img,"png",Path.of(nombre).toFile()); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
}
