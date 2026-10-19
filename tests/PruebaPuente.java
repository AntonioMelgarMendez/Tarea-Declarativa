import java.io.IOException;
import java.nio.file.Path;

public final class PruebaPuente {
    public static void main(String[] args) throws Exception {
        PuenteProlog puente = new PuenteProlog(Path.of("."));
        var r = puente.resolver("x", "-1", "1");
        comprobar(Math.abs(r.integral()) < 1e-9, "Integral orientada");
        comprobar(Math.abs(r.area()-1) < 1e-7, "Área geométrica");
        comprobar(r.puntos().size() == 241, "Muestras para Java");
        comprobar(r.primitiva().contains("x"), "Primitiva simbólica");
        var clase = puente.resolver("x^2", "3", "5");
        comprobar(Math.abs(clase.integral()-98.0/3) < 1e-9, "Ejemplo clase 13");
        var pi = puente.resolver("sin(x)", "0", "pi");
        comprobar(Math.abs(pi.integral()-2) < 1e-9, "Constantes en límites");
        var inversa = puente.resolver("arcsinh(x)", "0", "2");
        comprobar(inversa.area() > 0, "Alias de funciones");
        var implicita = puente.resolver("2x", "0", "2");
        comprobar(Math.abs(implicita.integral()-4) < 1e-9, "Entrada 2x");
        var parentesis = puente.resolver("2(x+1)", "0", "2");
        comprobar(Math.abs(parentesis.integral()-8) < 1e-9, "Paréntesis implícitos");
        var visual = puente.resolver("2×x − 1", "0", "1");
        comprobar(Math.abs(visual.area()-0.5) < 1e-7, "Teclado visual");
        var limite = puente.resolver("sin(x)", "0", "π");
        comprobar(Math.abs(limite.integral()-2) < 1e-9, "Tecla π en límites");
        rechaza(() -> puente.resolver("1/x", "-1", "1"), "Polo interior");
        rechaza(() -> puente.resolver("halt(0)", "0", "1"), "Inyección");
        rechaza(() -> puente.resolver("x\nhalt", "0", "1"), "Salto de línea");
        rechaza(() -> puente.resolver("x", "x", "1"), "Límite no constante");
        try {
            PuenteProlog.interpretar("OK\nAREA\tNaN\n");
            throw new AssertionError("Se aceptó respuesta inválida");
        } catch (IOException esperado) { /* correcto */ }
        System.out.println("Java ↔ SWI-Prolog: 16 comprobaciones correctas.");
    }
    private static void comprobar(boolean condicion, String nombre) {
        if (!condicion) throw new AssertionError(nombre);
    }
    private static void rechaza(Accion accion, String nombre) throws Exception {
        try { accion.ejecutar(); throw new AssertionError("No rechazado: " + nombre); }
        catch (IllegalArgumentException esperado) { /* correcto */ }
    }
    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
}
