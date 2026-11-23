import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class PruebaCarga {
    public static void main(String[] args) throws Exception {
        Config cfg = Config.cargar();
        String modo = args.length == 0 ? "seguro" : args[0];
        ExecutorService pool = Executors.newFixedThreadPool(30);
        CountDownLatch listos = new CountDownLatch(30);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger permitidas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();
        AtomicInteger[] reloj = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
        List<Future<?>> tareas = new ArrayList<>();
        for (int puerta = 1; puerta <= 3; puerta++) {
            final int id = puerta;
            for (int j = 0; j < 10; j++) {
                tareas.add(pool.submit(() -> {
                    listos.countDown();
                    try {
                        inicio.await();
                        try (Socket socket = new Socket()) {
                            socket.connect(new InetSocketAddress(cfg.host, cfg.puerto), 2000);
                            socket.setSoTimeout(5000);
                            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                            BufferedReader entrada = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream(), StandardCharsets.UTF_8));
                            salida.println(Protocolo.solicitud("ENTRAR", cfg.token, id,
                                    reloj[id - 1].incrementAndGet()));
                            Protocolo.Respuesta r = Protocolo.leerRespuesta(entrada.readLine());
                            if (r.resultado().equals("PERMITIDA")) permitidas.incrementAndGet();
                            else rechazadas.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    }
                }));
            }
        }
        listos.await();
        inicio.countDown();
        for (Future<?> t : tareas) t.get();
        pool.shutdown();

        int total = -1;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(cfg.host, cfg.puerto), 2000);
            socket.setSoTimeout(5000);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            salida.println(Protocolo.solicitud("ESTADO", cfg.token, 1, reloj[0].incrementAndGet()));
            total = Protocolo.leerRespuesta(entrada.readLine()).dentro();
        }
        String texto = "MODO=" + modo + System.lineSeparator()
                + "SOLICITUDES=30" + System.lineSeparator()
                + "PERMITIDAS=" + permitidas.get() + System.lineSeparator()
                + "RECHAZADAS=" + rechazadas.get() + System.lineSeparator()
                + "ERRORES=" + errores.get() + System.lineSeparator()
                + "DENTRO=" + total + System.lineSeparator();
        System.out.print(texto);
        Files.createDirectories(Path.of("logs"));
        Files.writeString(Path.of("logs", "carga_" +
                (modo.equals("inseguro") ? "inseguro" : "seguro") + ".txt"), texto);
        if (errores.get() > 0) System.err.println("Atencion: hubo errores de conexion; revise el servidor.");
    }
}
