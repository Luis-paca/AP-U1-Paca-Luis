import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Puerta {
    private final Config cfg;
    private final int id;
    private final RegistroLamport log;
    private final ScheduledExecutorService latidos = Executors.newSingleThreadScheduledExecutor();

    private Puerta(Config cfg, int id) throws IOException {
        this.cfg = cfg; this.id = id;
        log = new RegistroLamport(id, "puerta" + id + ".log");
    }

    private Protocolo.Respuesta solicitar(String tipo, String token, boolean silencioso) {
        int reloj = log.evento("ENVIO", "Solicitud " + tipo + " desde P" + id);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(cfg.host, cfg.puerto), 1000);
            socket.setSoTimeout(2000);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            salida.println(Protocolo.solicitud(tipo, token, id, reloj));
            Protocolo.Respuesta r = Protocolo.leerRespuesta(entrada.readLine());
            log.recibir(r.lamport(), "RECEPCION", "Respuesta " + r.resultado() + " a " + tipo);
            if (!silencioso) {
                System.out.println("Respuesta: " + r.resultado() + " | " + r.motivo());
                System.out.println("Dentro: " + r.dentro() + " / " + r.aforo());
                System.out.println("Puertas: " + r.puertas());
            }
            return r;
        } catch (Exception e) {
            log.evento("ERROR_RED", "Coordinador no disponible: " + e.getClass().getSimpleName());
            if (!silencioso) System.out.println("SIN COORDINADOR: entrada NO autorizada (" +
                    e.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private void ejecutar(String[] args) {
        log.evento("INICIO", "Puerta P" + id);
        boolean tokenInvalido = java.util.Arrays.asList(args).contains("--token-invalido");
        String token = tokenInvalido ? "TOKEN_ERRONEO" : cfg.token;
        int unaVez = java.util.Arrays.asList(args).indexOf("--una-vez");
        if (unaVez >= 0 && unaVez + 1 < args.length) {
            solicitar(args[unaVez + 1].toUpperCase(), token, false);
            cerrar(); return;
        }
        latidos.scheduleAtFixedRate(() -> solicitar("LATIDO", cfg.token, true),
                0, cfg.latidoMs, TimeUnit.MILLISECONDS);
        System.out.println("P" + id + " conectada. Ordenes: entrar, salir, estado, cerrar");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("P" + id + "> ");
                if (!scanner.hasNextLine()) break;
                String orden = scanner.nextLine().trim().toUpperCase();
                if (orden.equals("CERRAR")) break;
                if (orden.equals("ENTRAR") || orden.equals("SALIR") || orden.equals("ESTADO")) {
                    solicitar(orden, token, false);
                } else System.out.println("Orden desconocida. Use entrar, salir, estado o cerrar.");
            }
        }
        cerrar();
    }

    private void cerrar() {
        latidos.shutdownNow();
        try { log.close(); } catch (IOException e) { System.err.println(e.getMessage()); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java -cp out Puerta <1|2|3> [--una-vez entrar|salir|estado] [--token-invalido]");
            return;
        }
        int id = Integer.parseInt(args[0]);
        if (id < 1 || id > 3) throw new IllegalArgumentException("La puerta debe ser 1, 2 o 3");
        new Puerta(Config.cargar(), id).ejecutar(args);
    }
}
