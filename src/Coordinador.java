import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Coordinador {
    private final Config cfg;
    private final boolean inseguro;
    private final RegistroLamport log;
    private final AtomicInteger dentro = new AtomicInteger(0);
    private final Object candadoAforo = new Object();
    private final Map<Integer, Long> ultimoLatido = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> activas = new ConcurrentHashMap<>();

    private Coordinador(Config cfg, boolean inseguro) throws IOException {
        this.cfg = cfg; this.inseguro = inseguro;
        log = new RegistroLamport(0, "coordinador.log");
        for (int i = 1; i <= 3; i++) activas.put(i, false);
    }

    private void latido(int puerta) {
        ultimoLatido.put(puerta, System.nanoTime());
        if (!activas.getOrDefault(puerta, false)) {
            activas.put(puerta, true);
            log.evento("PUERTA_ACTIVA", "P" + puerta + " volvio a enviar latidos");
            System.out.println("P" + puerta + " -> ACTIVA");
        }
    }

    private void vigilar() {
        long maxSinLatido = TimeUnit.MILLISECONDS.toNanos(cfg.caidaMs);
        long ahora = System.nanoTime();
        for (int i = 1; i <= 3; i++) {
            Long ultimo = ultimoLatido.get(i);
            if (ultimo != null && ahora - ultimo >= maxSinLatido &&
                    activas.replace(i, true, false)) {
                log.evento("PUERTA_CAIDA", "P" + i + " sin latidos por " + cfg.caidaMs + " ms");
                System.out.println("P" + i + " -> CAIDA");
            }
        }
    }

    private String estados() {
        return "P1=" + textoEstado(1) + ",P2=" + textoEstado(2) + ",P3=" + textoEstado(3);
    }
    private String textoEstado(int p) {
        return !ultimoLatido.containsKey(p) ? "SIN_REGISTRO" :
                activas.getOrDefault(p, false) ? "ACTIVA" : "CAIDA";
    }

    private record Decision(String resultado, String motivo) {}

    private Decision modificarAforo(String tipo) throws InterruptedException {
        if (!inseguro) {
            synchronized (candadoAforo) { return cambioInterno(tipo); }
        }
        int visto = dentro.get();
        boolean posible = tipo.equals("ENTRAR") ? visto < cfg.aforo : visto > 0;
        if (!posible) return new Decision("RECHAZADA", tipo.equals("ENTRAR") ?
                "aforo completo" : "nadie dentro");
        Thread.sleep(50);
        dentro.addAndGet(tipo.equals("ENTRAR") ? 1 : -1);
        return new Decision("PERMITIDA", "operacion aceptada (modo inseguro)");
    }

    private Decision cambioInterno(String tipo) {
        int actual = dentro.get();
        if (tipo.equals("ENTRAR")) {
            if (actual >= cfg.aforo) return new Decision("RECHAZADA", "aforo completo");
            dentro.incrementAndGet();
        } else {
            if (actual <= 0) return new Decision("RECHAZADA", "nadie dentro");
            dentro.decrementAndGet();
        }
        return new Decision("PERMITIDA", "operacion aceptada");
    }

    private void atender(Socket socket) {
        try (socket;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            socket.setSoTimeout(8000);
            String linea;
            while ((linea = entrada.readLine()) != null) {
                String resultado = "RECHAZADA", motivo = "solicitud invalida";
                try {
                    Protocolo.Solicitud s = Protocolo.leerSolicitud(linea);
                    log.recibir(s.lamport(), "RECEPCION", "P" + s.puerta() + " solicita " + s.tipo());
                    if (!cfg.token.equals(s.token())) {
                        motivo = "token invalido";
                        log.evento("TOKEN_INVALIDO", "P" + s.puerta() + " tipo=" + s.tipo());
                    } else {
                        switch (s.tipo()) {
                            case "ENTRAR", "SALIR" -> {
                                Decision d = modificarAforo(s.tipo());
                                resultado = d.resultado(); motivo = d.motivo();
                                log.evento("AFORO", "P" + s.puerta() + " " + s.tipo()
                                        + " " + resultado + " dentro=" + dentro.get());
                            }
                            case "ESTADO" -> { resultado = "PERMITIDA"; motivo = "estado actual"; }
                            case "LATIDO" -> {
                                latido(s.puerta()); resultado = "PERMITIDA"; motivo = "latido recibido";
                            }
                            default -> motivo = "tipo desconocido";
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.evento("SOLICITUD_MALFORMADA", e.getMessage());
                    motivo = "formato incorrecto";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    motivo = "operacion interrumpida";
                }
                int envio = log.evento("ENVIO", resultado + " motivo=" + motivo);
                salida.println(Protocolo.respuesta(resultado, dentro.get(), cfg.aforo,
                        motivo, envio, estados()));
            }
        } catch (IOException e) {
            log.evento("ERROR_CONEXION", e.getClass().getSimpleName());
        }
    }

    private void iniciar() throws Exception {
        ExecutorService trabajadores = Executors.newCachedThreadPool();
        ScheduledExecutorService vigilancia = Executors.newSingleThreadScheduledExecutor();
        try (ServerSocket servidor = new ServerSocket()) {
            servidor.setReuseAddress(true);
            servidor.bind(new InetSocketAddress(cfg.host, cfg.puerto));
            vigilancia.scheduleAtFixedRate(this::vigilar, 500, 500, TimeUnit.MILLISECONDS);
            System.out.println("Coordinador PID logico=0 | " + cfg.host + ":" + cfg.puerto
                    + " | aforo=" + cfg.aforo + " | modo=" + (inseguro ? "INSEGURO" : "SEGURO"));
            log.evento("INICIO", "Aforo=" + cfg.aforo + " inseguro=" + inseguro);
            while (!Thread.currentThread().isInterrupted()) {
                Socket s = servidor.accept();
                trabajadores.submit(() -> atender(s));
            }
        } finally {
            vigilancia.shutdownNow(); trabajadores.shutdownNow(); log.close();
        }
    }

    public static void main(String[] args) throws Exception {
        boolean inseguro = args.length > 0 && args[0].equals("--inseguro");
        new Coordinador(Config.cargar(), inseguro).iniciar();
    }
}
