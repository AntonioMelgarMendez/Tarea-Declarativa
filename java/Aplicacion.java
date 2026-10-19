import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Calculadora de escritorio. El conocimiento y el cálculo siguen en Prolog. */
public final class Aplicacion {
    private static final Color FONDO = new Color(248, 249, 251);
    private static final Color TINTA = new Color(31, 41, 47);
    private static final Color SUAVE = new Color(117, 128, 136);
    private static final Color LINEA = new Color(231, 235, 238);
    private static final Color ACENTO = new Color(0, 121, 112);
    private static final Color VERDE_CLARO = new Color(235, 247, 243);
    private static final DecimalFormat NUMERO = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.US));
    private final JTextField expresion = campo("2x", "expresion");
    private final JTextField inferior = campo("0", "inferior");
    private final JTextField superior = campo("2", "superior");
    private JTextField activo = expresion;
    private final Boton calcular = new Boton("Calcular integral  =", ACENTO, Color.WHITE);
    private final JLabel estado = etiqueta(" ", 12, SUAVE);
    private final JLabel definida = valor("—");
    private final JLabel area = valor("—");
    private final JTextArea primitiva = texto(22);
    private final JTextArea evaluaciones = texto(13);
    private final JTextArea pasos = texto(14);
    private final JLabel funcionGrafica = etiqueta("f(x) = 2x", 13, SUAVE);
    private final JLabel tituloResultado = etiqueta("Resultado", 18, TINTA);
    private final Grafica grafica = new Grafica();
    private final VistaIntegral vista = new VistaIntegral();
    private final PuenteProlog puente = new PuenteProlog(Path.of("."));
    private boolean ocupado;
    private boolean pendiente;
    private long revision;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font", fuente(13, Font.PLAIN));
            UIManager.put("MenuItem.font", fuente(14, Font.PLAIN));
            UIManager.put("PopupMenu.background", Color.WHITE);
            new Aplicacion().mostrar();
        });
    }

    private void mostrar() {
        JFrame ventana = new JFrame("Integral · Calculadora");
        ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel raiz = panel(new BorderLayout(22, 22));
        raiz.setOpaque(true);
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel cabecera = panel(new BorderLayout());
        JPanel marca = panel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        JLabel icono = etiqueta("∫", 36, ACENTO);
        JPanel nombres = panel(new GridLayout(2, 1, 0, 2));
        nombres.add(negrita("Integral", 25));
        nombres.add(etiqueta("Calculadora de integrales", 13, SUAVE));
        marca.add(icono); marca.add(nombres);
        cabecera.add(marca, BorderLayout.WEST);
        Boton ejemplos = new Boton("Ejemplos  ↗", Color.WHITE, TINTA);
        ejemplos.setName("ejemplos");
        ejemplos.setPreferredSize(new Dimension(136, 42));
        ejemplos.addActionListener(e -> ejemplos(ejemplos));
        JPanel acciones = panel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
        acciones.add(ejemplos); cabecera.add(acciones, BorderLayout.EAST);
        raiz.add(cabecera, BorderLayout.NORTH);

        JPanel contenido = panel(new BorderLayout(22, 0));
        JPanel calculadora = calculadora();
        calculadora.setPreferredSize(new Dimension(358, 650));
        contenido.add(calculadora, BorderLayout.WEST);
        contenido.add(resultados(), BorderLayout.CENTER);
        raiz.add(contenido, BorderLayout.CENTER);
        ventana.setContentPane(raiz);
        ventana.setSize(1120, 850);
        ventana.setMinimumSize(new Dimension(980, 790));
        ventana.setLocationRelativeTo(null);
        ventana.getRootPane().setDefaultButton(calcular);
        for (JTextField campo : List.of(expresion, inferior, superior)) {
            campo.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { activo = campo; }
            });
            campo.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { editar(); }
                @Override public void removeUpdate(DocumentEvent e) { editar(); }
                @Override public void changedUpdate(DocumentEvent e) { editar(); }
            });
        }
        calcular.setName("calcular");
        calcular.addActionListener(e -> calcular());
        limpiarResultado();
        ventana.setVisible(true);
        expresion.requestFocusInWindow();
    }

    private JPanel calculadora() {
        Tarjeta tarjeta = new Tarjeta(Color.WHITE);
        tarjeta.setLayout(new BorderLayout(0, 18));
        JPanel entradas = panel(new BorderLayout(0, 15));
        JPanel rotulo = panel(new BorderLayout());
        rotulo.add(negrita("Tu integral", 15), BorderLayout.WEST);
        rotulo.add(etiqueta("dx", 14, SUAVE), BorderLayout.EAST);
        entradas.add(rotulo, BorderLayout.NORTH);
        JPanel edicion = panel(new BorderLayout(0, 10));
        vista.setPreferredSize(new Dimension(260, 90));
        edicion.add(vista, BorderLayout.NORTH);
        JPanel campos = panel(new BorderLayout(0, 7));
        campos.add(etiqueta("Función", 12, SUAVE), BorderLayout.NORTH);
        expresion.setFont(fuente(22, Font.PLAIN));
        expresion.setPreferredSize(new Dimension(250, 48));
        campos.add(expresion, BorderLayout.CENTER);
        JPanel limites = panel(new GridLayout(1, 2, 12, 0));
        limites.add(conCampo("Desde · a", inferior)); limites.add(conCampo("Hasta · b", superior));
        campos.add(limites, BorderLayout.SOUTH);
        edicion.add(campos, BorderLayout.CENTER);
        entradas.add(edicion, BorderLayout.CENTER);
        tarjeta.add(entradas, BorderLayout.NORTH);

        JPanel teclas = panel(new GridLayout(6, 5, 7, 7));
        String[] nombres = {"sin", "cos", "tan", "ln", "π", "x", "x²", "^", "(", ")",
                "7", "8", "9", "÷", "⌫", "4", "5", "6", "×", "C",
                "1", "2", "3", "−", "e", "0", ".", "asin", "acos", "+"};
        for (String nombre : nombres) {
            boolean numero = nombre.matches("[0-9.]");
            Boton tecla = new Boton(nombre, numero ? Color.WHITE : FONDO, numero ? TINTA : ACENTO);
            tecla.setName("tecla-" + nombre);
            tecla.setFont(fuente(numero ? 21 : 15, numero ? Font.PLAIN : Font.BOLD));
            tecla.setFocusable(false);
            tecla.addActionListener(e -> insertar(nombre));
            teclas.add(tecla);
        }
        tarjeta.add(teclas, BorderLayout.CENTER);
        JPanel pie = panel(new BorderLayout(0, 7));
        calcular.setPreferredSize(new Dimension(280, 49));
        pie.add(calcular, BorderLayout.CENTER);
        estado.setName("estado");
        estado.setPreferredSize(new Dimension(280, 18));
        pie.add(estado, BorderLayout.SOUTH);
        tarjeta.add(pie, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel resultados() {
        JPanel columna = panel(new BorderLayout(0, 18));
        Tarjeta resultado = new Tarjeta(Color.WHITE);
        resultado.setLayout(new BorderLayout(0, 15));
        resultado.add(tituloResultado, BorderLayout.NORTH);
        JPanel cifras = panel(new GridLayout(1, 2, 14, 0));
        cifras.add(cifra("Integral definida", definida));
        cifras.add(cifra("Área bajo la curva ≈", area));
        JPanel resumen = panel(new BorderLayout(0, 15));
        resumen.add(cifras, BorderLayout.NORTH);
        JPanel formula = panel(new BorderLayout(0, 6));
        formula.add(etiqueta("Antiderivada", 12, SUAVE), BorderLayout.NORTH);
        JScrollPane formulaScroll = new JScrollPane(primitiva);
        formulaScroll.setBorder(null); formulaScroll.setPreferredSize(new Dimension(250, 65));
        formula.add(formulaScroll, BorderLayout.CENTER);
        formula.add(evaluaciones, BorderLayout.SOUTH);
        resumen.add(formula, BorderLayout.CENTER);
        resultado.add(resumen, BorderLayout.CENTER);
        Boton verPasos = new Boton("Ver procedimiento  ↗", Color.WHITE, SUAVE);
        verPasos.setName("procedimiento");
        verPasos.setPreferredSize(new Dimension(190, 32));
        verPasos.addActionListener(e -> {
            JScrollPane scroll = new JScrollPane(pasos); scroll.setBorder(null);
            scroll.setPreferredSize(new Dimension(490, 320));
            JOptionPane.showMessageDialog(verPasos, scroll, "Procedimiento", JOptionPane.PLAIN_MESSAGE);
        });
        JPanel mas = panel(new FlowLayout(FlowLayout.LEFT, 0, 0)); mas.add(verPasos);
        resultado.add(mas, BorderLayout.SOUTH);
        columna.add(resultado, BorderLayout.NORTH);

        Tarjeta curva = new Tarjeta(Color.WHITE);
        curva.setLayout(new BorderLayout(0, 8));
        JPanel encabezado = panel(new BorderLayout());
        encabezado.add(negrita("Gráfica", 16), BorderLayout.WEST);
        encabezado.add(funcionGrafica, BorderLayout.EAST);
        curva.add(encabezado, BorderLayout.NORTH);
        curva.add(grafica, BorderLayout.CENTER);
        JPanel leyenda = panel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        leyenda.add(etiqueta("●  Positiva", 11, ACENTO));
        leyenda.add(etiqueta("●  Negativa", 11, new Color(195, 114, 98)));
        curva.add(leyenda, BorderLayout.SOUTH);
        columna.add(curva, BorderLayout.CENTER);
        return columna;
    }

    private void ejemplos(JButton origen) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(new BordeRedondo(), BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        menu.setBackground(Color.WHITE);
        String[][] datos = {{"2x", "0", "2"}, {"x^2", "0", "3"}, {"3x^2 + 2x", "0", "2"},
                {"sin(x)", "0", "pi"}, {"cos(2x)", "0", "pi/4"}, {"2^x", "0", "3"},
                {"1/x", "1", "e"}, {"x", "-1", "1"}};
        for (String[] dato : datos) {
            JMenuItem item = new JMenuItem(visual(dato[0]) + "     ·     " + visual(dato[1]) + " → " + visual(dato[2]));
            item.setBackground(Color.WHITE); item.setForeground(TINTA);
            item.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
            item.addActionListener(e -> {
                menu.setVisible(false);
                expresion.setText(dato[0]); inferior.setText(dato[1]); superior.setText(dato[2]);
                activo = expresion; expresion.requestFocusInWindow();
            });
            menu.add(item);
        }
        menu.show(origen, origen.getWidth() - menu.getPreferredSize().width, origen.getHeight() + 7);
    }

    private void insertar(String tecla) {
        if (tecla.equals("C")) { activo.setText(""); activo.requestFocusInWindow(); return; }
        if (tecla.equals("⌫")) {
            int inicio = activo.getSelectionStart(), fin = activo.getSelectionEnd();
            if (inicio == fin && inicio > 0) activo.select(inicio - 1, fin);
            activo.replaceSelection(""); activo.requestFocusInWindow(); return;
        }
        String entrada = switch (tecla) {
            case "x²" -> "x^2";
            case "sin", "cos", "tan", "ln", "asin", "acos" -> tecla + "()";
            default -> tecla;
        };
        activo.replaceSelection(entrada);
        if (entrada.endsWith("()")) activo.setCaretPosition(activo.getCaretPosition() - 1);
        activo.requestFocusInWindow();
    }

    private void editar() {
        revision++;
        vista.repaint();
        if (!pendiente) {
            pendiente = true;
            SwingUtilities.invokeLater(() -> { pendiente = false; limpiarResultado(); });
        }
    }
    private void limpiarResultado() {
        definida.setText("—"); area.setText("—");
        primitiva.setText("—"); evaluaciones.setText(""); pasos.setText("");
        tituloResultado.setText("Resultado"); tituloResultado.setForeground(TINTA);
        funcionGrafica.setText("f(x) = " + visual(expresion.getText()));
        grafica.actualizar(List.of());
        estado.setText(ocupado ? "Calculando…" : " ");
    }

    private void calcular() {
        if (ocupado) return;
        String funcion = expresion.getText().strip(), a = inferior.getText().strip(), b = superior.getText().strip();
        long consulta = revision;
        ocupado = true; calcular.setEnabled(false); calcular.setText("Calculando…");
        limpiarResultado();
        new SwingWorker<PuenteProlog.Resultado, Void>() {
            @Override protected PuenteProlog.Resultado doInBackground() throws Exception { return puente.resolver(funcion,a,b); }
            @Override protected void done() {
                ocupado = false; calcular.setEnabled(true); calcular.setText("Calcular integral  =");
                if (consulta != revision) { limpiarResultado(); return; }
                try {
                    PuenteProlog.Resultado r = get();
                    definida.setText(formato(r.integral())); area.setText(formato(r.area()));
                    primitiva.setText("F(x) = " + visual(r.primitiva()) + " + C");
                    evaluaciones.setText("F(a) = " + formato(r.fa()) + "     ·     F(b) = " + formato(r.fb()));
                    StringBuilder detalle = new StringBuilder();
                    for (int i = 0; i < r.pasos().size(); i++) detalle.append(i+1).append(". ").append(r.pasos().get(i)).append("\n\n");
                    detalle.append("F(b) − F(a) = ").append(formato(r.integral()));
                    pasos.setText(detalle.toString()); pasos.setCaretPosition(0);
                    grafica.actualizar(r.puntos());
                    estado.setText(" ");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); error("Cálculo interrumpido.");
                } catch (ExecutionException e) { error(e.getCause().getMessage()); }
            }
        }.execute();
    }
    private void error(String mensaje) {
        tituloResultado.setText("Revisa tu integral"); tituloResultado.setForeground(new Color(175,75,61));
        primitiva.setText(mensaje == null ? "No se pudo completar el cálculo." : mensaje);
        estado.setText(" ");
    }

    private static Font fuente(int tamano, int estilo) { return new Font(Font.SANS_SERIF, estilo, tamano); }
    private static JLabel etiqueta(String texto,int tamano,Color color) {
        JLabel l = new JLabel(texto); l.setFont(fuente(tamano,Font.PLAIN)); l.setForeground(color); return l;
    }
    private static JLabel negrita(String texto,int tamano) { JLabel l=etiqueta(texto,tamano,TINTA); l.setFont(fuente(tamano,Font.BOLD)); return l; }
    private static JLabel valor(String texto) { JLabel l=etiqueta(texto,30,ACENTO); l.setFont(fuente(30,Font.BOLD)); return l; }
    private static JPanel panel(LayoutManager layout) { JPanel p=new JPanel(layout); p.setOpaque(false); return p; }
    private static JTextField campo(String valor,String nombre) {
        JTextField t=new JTextField(valor); t.setName(nombre); t.setFont(fuente(17,Font.PLAIN));
        t.setForeground(TINTA); t.setCaretColor(ACENTO); t.setBackground(FONDO);
        t.setBorder(BorderFactory.createCompoundBorder(new BordeRedondo(),BorderFactory.createEmptyBorder(9,12,9,12)));
        t.setPreferredSize(new Dimension(100,43)); return t;
    }
    private static JTextArea texto(int tamano) {
        JTextArea t=new JTextArea(); t.setEditable(false); t.setLineWrap(true); t.setWrapStyleWord(true);
        t.setForeground(TINTA); t.setBackground(Color.WHITE); t.setFont(fuente(tamano,Font.PLAIN)); return t;
    }
    private static JPanel conCampo(String titulo,JTextField campo) {
        JPanel p=panel(new BorderLayout(0,5)); p.add(etiqueta(titulo,12,SUAVE),BorderLayout.NORTH); p.add(campo,BorderLayout.CENTER); return p;
    }
    private static JPanel cifra(String titulo,JLabel valor) {
        Tarjeta p=new Tarjeta(VERDE_CLARO); p.setBorder(BorderFactory.createEmptyBorder(13,15,13,15));
        p.setLayout(new BorderLayout(0,8)); p.add(etiqueta(titulo,12,SUAVE),BorderLayout.NORTH);
        valor.setToolTipText(titulo); p.add(valor,BorderLayout.CENTER); return p;
    }
    private static String formato(double n) {
        if(n!=0 && (Math.abs(n)<1e-8 || Math.abs(n)>=1e10)) return String.format(Locale.US,"%.8g",n);
        return NUMERO.format(n);
    }
    private static String visual(String s) {
        return s.replace("pi","π").replace("*","·").replace("+-","−").replace("+ -","− ")
                .replaceAll("\\^2(?![0-9.])","²").replaceAll("\\^3(?![0-9.])","³");
    }
    private static void calidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    private static final class BordeRedondo extends AbstractBorder {
        private static final long serialVersionUID=1L;
        @Override public void paintBorder(Component c,Graphics original,int x,int y,int w,int h) {
            Graphics2D g=(Graphics2D)original.create(); calidad(g);
            g.setColor(c.hasFocus()?ACENTO:LINEA); g.drawRoundRect(x,y,w-1,h-1,12,12); g.dispose();
        }
    }
    private static final class Tarjeta extends JPanel {
        private static final long serialVersionUID=1L;
        Tarjeta(Color color) { setOpaque(false); setBackground(color); setBorder(BorderFactory.createEmptyBorder(20,20,20,20)); }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g=(Graphics2D)original.create(); calidad(g); g.setColor(getBackground());
            g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
            g.setColor(LINEA); g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,20,20); g.dispose();
        }
    }
    private static final class Boton extends JButton {
        private static final long serialVersionUID=1L;
        Boton(String texto,Color fondo,Color tinta) {
            super(texto); setFont(fuente(14,Font.BOLD)); setBackground(fondo); setForeground(tinta);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(8,5,8,5));
        }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g=(Graphics2D)original.create(); calidad(g); Color fondo=getBackground();
            if(!isEnabled()) fondo=new Color(216,228,226);
            else if(getModel().isPressed()) fondo=fondo.darker();
            else if(getModel().isRollover()) fondo=fondo.equals(ACENTO)?ACENTO.darker():new Color(232,243,240);
            g.setColor(fondo); g.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
            if(!getBackground().equals(ACENTO)) { g.setColor(LINEA); g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12); }
            g.dispose(); super.paintComponent(original);
        }
    }
    private final class VistaIntegral extends JPanel {
        private static final long serialVersionUID=1L;
        VistaIntegral() { setOpaque(false); }
        @Override protected void paintComponent(Graphics original) {
            Graphics2D g=(Graphics2D)original.create(); calidad(g);
            g.setColor(VERDE_CLARO); g.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
            g.setColor(ACENTO); g.setFont(fuente(65,Font.PLAIN)); g.drawString("∫",17,70);
            g.setFont(fuente(13,Font.PLAIN)); g.drawString(visual(superior.getText()),48,21); g.drawString(visual(inferior.getText()),43,78);
            g.setClip(70,15,Math.max(0,getWidth()-80),60); g.setColor(TINTA); g.setFont(fuente(23,Font.PLAIN));
            g.drawString(visual(expresion.getText())+" dx",76,55); g.dispose();
        }
    }
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
                // Una sola region evita las costuras entre 240 trapecios antialias.
                Path2D relleno = new Path2D.Double(); relleno.moveTo(50,cero);
                for (var p : puntos) relleno.lineTo(50+(p.x()-minX)*sx,cero-p.y()*sy);
                relleno.lineTo(50+(puntos.get(puntos.size()-1).x()-minX)*sx,cero); relleno.closePath();
                Shape recorte = g.getClip();
                g.clip(new java.awt.geom.Rectangle2D.Double(0,0,w,Math.max(0,cero)));
                g.setColor(new Color(222,241,234)); g.fill(relleno); g.setClip(recorte);
                g.clip(new java.awt.geom.Rectangle2D.Double(0,cero,w,Math.max(0,h-cero)));
                g.setColor(new Color(248,225,218)); g.fill(relleno); g.setClip(recorte);
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
    }
}
