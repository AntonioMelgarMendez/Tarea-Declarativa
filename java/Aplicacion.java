import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Interfaz de escritorio; las muestras de la gráfica también proceden de Prolog. */
public final class Aplicacion {
    private static final Color FONDO = new Color(244, 247, 247);
    private static final Color TINTA = new Color(26, 49, 54);
    private static final Color ACENTO = new Color(0, 115, 111);
    private static final DecimalFormat NUMERO = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.US));
    private final JTextField expresion = new JTextField("3*x^2 + sin(x)");
    private final JTextField inferior = new JTextField("0");
    private final JTextField superior = new JTextField("pi");
    private final JButton calcular = new JButton("Calcular integral  →");
    private final JLabel estado = new JLabel("Listo. Introduzca una función de x.");
    private final JLabel definida = valor("—");
    private final JLabel area = valor("—");
    private final JTextArea primitiva = texto(false);
    private final JTextArea pasos = texto(false);
    private final Grafica grafica = new Grafica();
    private final PuenteProlog puente = new PuenteProlog(Path.of("."));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.BOLD, 14));
            UIManager.put("TextField.font", new Font(Font.MONOSPACED, Font.PLAIN, 16));
            new Aplicacion().mostrar();
        });
    }

    private void mostrar() {
        JFrame ventana = new JFrame("Integral · Sistema experto | Programación Declarativa");
        ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel raiz = new JPanel(new BorderLayout(20, 20));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JPanel cabecera = new JPanel(new BorderLayout(8, 8));
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel("∫  Integral");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        titulo.setForeground(TINTA);
        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(new JLabel("Sistema experto de integración simbólica · Java + SWI-Prolog"), BorderLayout.SOUTH);
        raiz.add(cabecera, BorderLayout.NORTH);

        JPanel izquierda = new JPanel(new BorderLayout(12, 18));
        izquierda.setOpaque(false);
        izquierda.add(formulario(), BorderLayout.NORTH);
        JTabbedPane detalles = new JTabbedPane();
        JPanel resultados = new JPanel(new BorderLayout(12, 12));
        resultados.setBackground(Color.WHITE);
        resultados.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel tarjetas = new JPanel(new GridLayout(1, 2, 12, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(tarjeta("Integral definida · F(b) − F(a)", definida));
        tarjetas.add(tarjeta("Área geométrica · aproximada", area));
        resultados.add(tarjetas, BorderLayout.NORTH);
        primitiva.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 17));
        primitiva.setText("La antiderivada aparecerá aquí.");
        resultados.add(new JScrollPane(primitiva), BorderLayout.CENTER);
        detalles.addTab("Resultado", resultados);
        detalles.addTab("Reglas aplicadas", new JScrollPane(pasos));
        JTextArea ayuda = texto(false);
        ayuda.setText(AYUDA);
        detalles.addTab("Sintaxis y cobertura", new JScrollPane(ayuda));
        izquierda.add(detalles, BorderLayout.CENTER);

        JPanel panelGrafica = new JPanel(new BorderLayout(8, 8));
        panelGrafica.setBackground(Color.WHITE);
        panelGrafica.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panelGrafica.add(new JLabel("CURVA Y REGIÓN DE INTEGRACIÓN"), BorderLayout.NORTH);
        panelGrafica.add(grafica, BorderLayout.CENTER);
        JLabel leyenda = new JLabel("Verde: f(x) ≥ 0   ·   Coral: f(x) < 0");
        leyenda.setForeground(TINTA);
        panelGrafica.add(leyenda, BorderLayout.SOUTH);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, panelGrafica);
        division.setBorder(null);
        division.setResizeWeight(0.51);
        izquierda.setMinimumSize(new Dimension(380, 400));
        panelGrafica.setMinimumSize(new Dimension(280, 400));
        raiz.add(division, BorderLayout.CENTER);
        estado.setForeground(ACENTO);
        raiz.add(estado, BorderLayout.SOUTH);
        ventana.setContentPane(raiz);
        ventana.setSize(1160, 760);
        ventana.setMinimumSize(new Dimension(850, 620));
        ventana.setLocationRelativeTo(null);
        ventana.getRootPane().setDefaultButton(calcular);
        ventana.setVisible(true);
    }

    private JPanel formulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 4, 5, 4);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        panel.add(new JLabel("Función f(x)"), c);
        c.gridy++; panel.add(expresion, c);
        c.gridy++; c.gridwidth = 1;
        panel.add(new JLabel("Límite a"), c);
        c.gridx = 1; panel.add(new JLabel("Límite b"), c);
        c.gridy++; c.gridx = 0; panel.add(inferior, c);
        c.gridx = 1; panel.add(superior, c);
        c.gridy++; c.gridx = 0; c.gridwidth = 2;
        JComboBox<String> ejemplos = new JComboBox<>(new String[]{"Cargar un ejemplo…", "Polinomio + seno", "Cambio de signo", "Exponencial compuesta", "Recíproco", "Inversa hiperbólica"});
        ejemplos.addActionListener(e -> {
            String[][] datos = {{"3*x^2 + sin(x)", "0", "pi"}, {"x", "-1", "1"},
                    {"2^(3*x+1)", "0", "1"}, {"x^(-1)", "1", "e"}, {"arcsinh(x)", "0", "2"}};
            int i = ejemplos.getSelectedIndex() - 1;
            if (i >= 0) {
                expresion.setText(datos[i][0]); inferior.setText(datos[i][1]); superior.setText(datos[i][2]);
            }
        });
        panel.add(ejemplos, c);
        c.gridy++; calcular.setBackground(ACENTO); calcular.setForeground(Color.WHITE);
        calcular.setOpaque(true); calcular.addActionListener(e -> calcular());
        panel.add(calcular, c);
        return panel;
    }

    private void calcular() {
        String funcion = expresion.getText().strip();
        String a = inferior.getText().strip();
        String b = superior.getText().strip();
        calcular.setEnabled(false);
        definida.setText("—"); area.setText("—"); primitiva.setText(""); pasos.setText("");
        grafica.actualizar(List.of());
        estado.setForeground(ACENTO);
        estado.setText("Consultando la base de conocimiento…");
        new SwingWorker<PuenteProlog.Resultado, Void>() {
            @Override protected PuenteProlog.Resultado doInBackground() throws Exception {
                return puente.resolver(funcion, a, b);
            }
            @Override protected void done() {
                calcular.setEnabled(true);
                try {
                    PuenteProlog.Resultado r = get();
                    definida.setText(formato(r.integral())); area.setText(formato(r.area()));
                    primitiva.setText("f(x) = " + funcion + "\n\nF(x) = " + r.primitiva() + " + C\n\n"
                            + "F(a) = " + formato(r.fa()) + "\nF(b) = " + formato(r.fb()));
                    StringBuilder detalle = new StringBuilder();
                    for (int i = 0; i < r.pasos().size(); i++) detalle.append(i + 1).append(". ").append(r.pasos().get(i)).append("\n\n");
                    detalle.append("Evaluación: F(b) − F(a) = ").append(formato(r.integral()))
                            .append("\n\nÁrea: integración numérica de |f(x)| en [min(a,b), max(a,b)].\n")
                            .append("Simpson adaptativo; tolerancia absoluta objetivo 1e-7 (no garantía formal de error).");
                    pasos.setText(detalle.toString()); pasos.setCaretPosition(0);
                    grafica.actualizar(r.puntos());
                    estado.setText("Calculado por SWI-Prolog · gráfico de 241 muestras, ilustrativo.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); error("Consulta interrumpida.");
                } catch (ExecutionException e) {
                    error(e.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void error(String mensaje) {
        estado.setForeground(new Color(170, 57, 47));
        estado.setText("No se pudo calcular. Consulte el detalle.");
        primitiva.setText(mensaje == null ? "Error inesperado al consultar Prolog." : mensaje);
    }
    private static String formato(double n) {
        if (n != 0 && (Math.abs(n) < 1e-8 || Math.abs(n) >= 1e10)) return String.format(Locale.US, "%.8g", n);
        return NUMERO.format(n);
    }
    private static JLabel valor(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23)); label.setForeground(ACENTO);
        return label;
    }
    private static JPanel tarjeta(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(4, 10)); p.setBackground(FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        JLabel etiqueta = new JLabel(titulo); etiqueta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        p.add(etiqueta, BorderLayout.NORTH); p.add(valor, BorderLayout.CENTER); return p;
    }
    private static JTextArea texto(boolean editable) {
        JTextArea t = new JTextArea(); t.setEditable(editable); t.setLineWrap(true); t.setWrapStyleWord(true);
        t.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); t.setForeground(TINTA);
        t.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14)); return t;
    }

    private static final String AYUDA = """
            Escriba x en minúscula, multiplicación explícita (*) y potencias con ^.
            Ejemplos: 5, x, 3*x, x^4, x^(-1), 2^(3*x+1), 4*sin(x), x^2-cos(x).
            Límites: números o expresiones constantes, por ejemplo -2, pi/2 o e.

            Funciones:
            sin, cos, tan; asin, acos, atan;
            sinh (también sh), cosh, tanh;
            asinh/arcsinh, acosh/arccosh, atanh/arctanh;
            exp y log (también ln).

            Sumas, restas y factores constantes se resuelven recursivamente.
            Las funciones, potencias enteras y exponenciales admiten argumentos afines: a*x+b, a ≠ 0.
            No se aceptan funciones arbitrarias ni productos generales como x*sin(x), o sin(x^2).
            asin y acos son funciones inversas, NO 1/sin y 1/cos.
            Ángulos en radianes. Solo aritmética real e intervalos continuos.
            No se resuelven integrales impropias ni valores principales de Cauchy.

            Integral definida: F(b) − F(a), con signo y orientación.
            Área geométrica: integral de |f|; no negativa, independiente de la orientación.
            El área es aproximada; el gráfico no se usa para calcularla.
            """;

    private static final class Grafica extends JPanel {
        private static final long serialVersionUID = 1L;
        private List<PuenteProlog.Punto> puntos = List.of();
        Grafica() { setBackground(Color.WHITE); }
        void actualizar(List<PuenteProlog.Punto> nuevos) { puntos = nuevos; repaint(); }
        @Override protected void paintComponent(Graphics original) {
            super.paintComponent(original);
            Graphics2D g = (Graphics2D) original.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (puntos.isEmpty()) {
                    g.setColor(TINTA); g.drawString("La curva aparecerá al calcular.", 35, h / 2); return;
                }
                double minX = puntos.get(0).x(), maxX = puntos.get(puntos.size() - 1).x();
                double minY = 0, maxY = 0;
                for (var p : puntos) { minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y()); }
                if (maxX == minX) { minX -= 1; maxX += 1; }
                if (maxY == minY) { minY -= 1; maxY += 1; }
                double margen = (maxY - minY) * 0.12;
                minY -= margen; maxY += margen;
                double sx = (w - 80.0) / (maxX - minX), sy = (h - 80.0) / (maxY - minY);
                double cero = h - 40 + minY * sy;
                g.setColor(new Color(230, 237, 238));
                for (int i = 0; i <= 4; i++) {
                    int y = 40 + (h - 80) * i / 4;
                    g.drawLine(50, y, w - 30, y);
                    g.setColor(TINTA); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                    g.drawString(String.format(Locale.US, "%.3g", maxY - (maxY - minY) * i / 4), 1, y + 4);
                    g.setColor(new Color(230, 237, 238));
                }
                for (int i = 1; i < puntos.size(); i++) {
                    var a = puntos.get(i - 1); var b = puntos.get(i);
                    double x1 = 50 + (a.x() - minX) * sx, x2 = 50 + (b.x() - minX) * sx;
                    double y1 = cero - a.y() * sy, y2 = cero - b.y() * sy;
                    if ((a.y() < 0) != (b.y() < 0)) {
                        double cruce = x1 + (x2-x1) * Math.abs(a.y()) / (Math.abs(a.y()) + Math.abs(b.y()));
                        region(g,x1,y1,cruce,cero,cero,a.y() >= 0);
                        region(g,cruce,cero,x2,y2,cero,b.y() >= 0);
                    } else region(g,x1,y1,x2,y2,cero,a.y() >= 0);
                }
                g.setColor(new Color(140, 158, 162)); g.setStroke(new BasicStroke(1));
                g.drawLine(50, (int)cero, w - 30, (int)cero);
                if (minX <= 0 && maxX >= 0) {
                    int eje = (int)(50 - minX*sx); g.drawLine(eje, 40, eje, h - 40);
                }
                Path2D curva = new Path2D.Double();
                for (int i = 0; i < puntos.size(); i++) {
                    var p = puntos.get(i); double x = 50 + (p.x()-minX)*sx, y = cero-p.y()*sy;
                    if (i == 0) curva.moveTo(x,y); else curva.lineTo(x,y);
                }
                g.setColor(ACENTO); g.setStroke(new BasicStroke(2.5f)); g.draw(curva);
                g.setColor(TINTA);
                g.drawString(formato(puntos.get(0).x()), 50, h - 17);
                g.drawString(formato(puntos.get(puntos.size()-1).x()), w - 70, h - 17);
                g.drawString("x", w - 20, (int)cero - 6);
            } finally { g.dispose(); }
        }
        private static void region(Graphics2D g, double x1, double y1, double x2, double y2, double cero, boolean positiva) {
            Path2D relleno = new Path2D.Double(); relleno.moveTo(x1,cero); relleno.lineTo(x1,y1);
            relleno.lineTo(x2,y2); relleno.lineTo(x2,cero); relleno.closePath();
            g.setColor(positiva ? new Color(207,236,226) : new Color(248,218,209)); g.fill(relleno);
        }
    }
}
