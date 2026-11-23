import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrdenTotal {
    private record Evento(int lamport, int id, String linea) {}
    public static void main(String[] args) throws Exception {
        List<Evento> eventos = new ArrayList<>();
        String[] archivos = {"coordinador.log", "puerta1.log", "puerta2.log", "puerta3.log"};
        for (String archivo : archivos) {
            Path ruta = Path.of("logs", archivo);
            if (!Files.exists(ruta)) {
                System.out.println("Aviso: aun no existe " + ruta);
                continue;
            }
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(";", 4);
                if (partes.length != 4) {
                    System.out.println("Se omite linea malformada: " + linea);
                    continue;
                }
                try {
                    eventos.add(new Evento(Integer.parseInt(partes[0]),
                            Integer.parseInt(partes[1]), linea));
                } catch (NumberFormatException e) {
                    System.out.println("Se omite linea con reloj o id invalido: " + linea);
                }
            }
        }
        eventos.sort(Comparator.comparingInt(Evento::lamport).thenComparingInt(Evento::id));
        Path salida = Path.of("logs", "orden_total.log");
        Files.createDirectories(salida.getParent());
        Files.write(salida, eventos.stream().map(Evento::linea).toList(), StandardCharsets.UTF_8);
        System.out.println("Eventos ordenados: " + eventos.size() + " -> " + salida);
        System.out.println("Busque un ENVIO de puerta y la RECEPCION posterior en el coordinador.");
    }
}
