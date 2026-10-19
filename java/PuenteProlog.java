import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Transporte solamente: Java no integra, evalúa funciones ni calcula áreas. */
public final class PuenteProlog {
    public record Punto(double x, double y) { }
    public record Resultado(String primitiva, double fa, double fb, double integral,
                            double area, List<String> pasos, List<Punto> puntos) { }

    private final Path raiz;

    public PuenteProlog(Path raiz) { this.raiz = raiz.toAbsolutePath().normalize(); }

    public Resultado resolver(String expresion, String a, String b) throws Exception {
        for (String entrada : List.of(expresion, a, b)) {
            if (entrada.isBlank() || entrada.length() > 2000 || entrada.contains("\n")
                    || entrada.contains("\r")) {
                throw new IllegalArgumentException("Complete expresión y límites en una sola línea (máximo 2000 caracteres).");
            }
        }
        List<String> comando = comandoProlog();
        comando.addAll(List.of("-q", "-s", raiz.resolve("prolog/puente.pl").toString()));
        Path errores = Files.createTempFile("integrales-prolog-", ".log");
        Process proceso = null;
        var lector = Executors.newSingleThreadExecutor(r -> {
            Thread hilo = new Thread(r, "lector-prolog");
            hilo.setDaemon(true);
            return hilo;
        });
        try {
            proceso = new ProcessBuilder(comando).directory(raiz.toFile())
                    .redirectError(errores.toFile()).start();
            Process activo = proceso;
            var salida = lector.submit(() -> new String(activo.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            try (var entrada = proceso.outputWriter(StandardCharsets.UTF_8)) {
                entrada.write(expresion + "\n" + a + "\n" + b + "\n");
            } catch (IOException e) {
                // Si el ejecutable falla antes de leer (p. ej. Flatpak sin permisos),
                // mostrar su diagnostico, no solamente "Stream closed".
                if (proceso.waitFor(1, TimeUnit.SECONDS)) {
                    String detalle = Files.readString(errores, StandardCharsets.UTF_8).strip();
                    if (!detalle.isBlank()) throw new IOException("No se pudo iniciar SWI-Prolog. " + detalle, e);
                }
                throw e;
            }
            if (!proceso.waitFor(18, TimeUnit.SECONDS)) {
                throw new IOException("Prolog excedió el tiempo de espera. Reduzca la expresión o el intervalo.");
            }
            String texto = salida.get(2, TimeUnit.SECONDS);
            if (proceso.exitValue() != 0 || texto.isBlank()) {
                String detalle = Files.readString(errores, StandardCharsets.UTF_8);
                throw new IOException("No se pudo ejecutar SWI-Prolog. " + detalle.strip());
            }
            return interpretar(texto);
        } catch (ExecutionException e) {
            throw new IOException("No se pudo leer la respuesta de Prolog.", e.getCause());
        } finally {
            if (proceso != null && proceso.isAlive()) {
                proceso.descendants().forEach(ProcessHandle::destroyForcibly);
                proceso.destroyForcibly();
            }
            lector.shutdownNow();
            Files.deleteIfExists(errores);
        }
    }

    private static List<String> comandoProlog() throws IOException {
        String configurado = System.getenv("SWIPL_EXECUTABLE");
        if (configurado != null && !configurado.isBlank()) {
            return new ArrayList<>(List.of(configurado));
        }
        if (enPath("swipl")) return new ArrayList<>(List.of("swipl"));
        if (enPath("flatpak")) {
            return new ArrayList<>(List.of("flatpak", "run", "--command=swipl", "org.swi_prolog.swipl"));
        }
        throw new IOException("Instale SWI-Prolog y agregue swipl al PATH, o configure SWIPL_EXECUTABLE.");
    }

    private static boolean enPath(String nombre) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, nombre))) return true;
        }
        return false;
    }

    static Resultado interpretar(String texto) throws IOException {
        String[] lineas = texto.split("\\R");
        if (lineas[0].startsWith("ERROR\t")) throw new IllegalArgumentException(lineas[0].substring(6));
        if (!lineas[0].equals("OK")) throw new IOException("Respuesta de Prolog no reconocida.");
        String primitiva = null;
        Double fa = null, fb = null, integral = null, area = null;
        List<String> pasos = new ArrayList<>();
        List<Punto> puntos = new ArrayList<>();
        try {
            for (int i = 1; i < lineas.length; i++) {
                String[] campos = lineas[i].split("\t", 3);
                switch (campos[0]) {
                    case "PRIMITIVA" -> primitiva = campos[1];
                    case "FA" -> fa = numero(campos[1]);
                    case "FB" -> fb = numero(campos[1]);
                    case "INTEGRAL" -> integral = numero(campos[1]);
                    case "AREA" -> area = numero(campos[1]);
                    case "PASO" -> pasos.add(campos[1]);
                    case "PUNTO" -> puntos.add(new Punto(numero(campos[1]), numero(campos[2])));
                    default -> throw new IOException("Campo desconocido en respuesta de Prolog.");
                }
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IOException("Respuesta de Prolog incompleta o inválida.", e);
        }
        if (primitiva == null || fa == null || fb == null || integral == null || area == null
                || pasos.isEmpty() || puntos.size() != 241 || area < 0) {
            throw new IOException("Respuesta de Prolog incompleta.");
        }
        return new Resultado(primitiva, fa, fb, integral, area, List.copyOf(pasos), List.copyOf(puntos));
    }

    private static double numero(String texto) {
        double valor = Double.parseDouble(texto);
        if (!Double.isFinite(valor)) throw new NumberFormatException("Valor no finito.");
        return valor;
    }
}
