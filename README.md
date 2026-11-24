# AP-U1-Paca-Luis
## AforoSeguro - Control de Aforo Distribuido

**Estudiante:** Luis Alberto Paca Pilataza  
**Universidad:** Universidad Técnica Estatal de Quevedo (UTEQ)  
**Carrera:** Ingeniería de Software  
**Asignatura:** Aplicaciones Distribuidas  
**Actividad:** AP-U1 - Aporte Práctico de la Unidad I

---

## 1. Descripción del proyecto

AforoSeguro es un sistema desarrollado en Java que tiene como objetivo controlar el ingreso y salida de personas mediante tres puertas conectadas a un coordinador central.

El coordinador se encarga de recibir las solicitudes de las puertas y administrar el contador de personas, tomando en cuenta el aforo máximo permitido.

Durante la actividad se avanzó en los componentes principales del sistema utilizando sockets TCP, concurrencia, autenticación por token, latidos y relojes lógicos de Lamport.

El proyecto quedó como un avance, debido a que no se completaron todas las pruebas y evidencias solicitadas.

## 2. Estructura del proyecto

Los archivos principales desarrollados son:

```text
AP-U1-Paca-Luis/
|
|-- src/
|   |-- Coordinador.java
|   |-- Puerta.java
|   |-- PruebaCarga.java
|   |-- OrdenTotal.java
|
|-- config.properties
|-- README.md
```

La documentación del proyecto se organiza en la carpeta `docs/`, mientras que los registros de ejecución están previstos para la carpeta `logs/`.

## 3. Descripción de los archivos

### Coordinador.java

Contiene la lógica del servidor central, encargado de recibir las solicitudes de las puertas y administrar el contador de personas.

Incluye código relacionado con la validación del token, el control de concurrencia y la recepción de latidos.

### Puerta.java

Contiene las funciones para comunicarse con el coordinador y realizar solicitudes de entrada, salida y consulta del estado.

Se plantea utilizar una misma clase para representar las puertas P1, P2 y P3.

### PruebaCarga.java

Contiene la lógica para generar solicitudes concurrentes y comprobar el comportamiento del contador cuando varias puertas solicitan entradas al mismo tiempo.

La prueba prevista utiliza 30 solicitudes, distribuidas entre las tres puertas.

### OrdenTotal.java

Contiene el procedimiento para procesar los registros de eventos y ordenarlos utilizando los valores del reloj lógico de Lamport.

### config.properties

Es el archivo utilizado para almacenar los parámetros de configuración, como el aforo, el token, el host y el puerto de comunicación.

## 4. Tecnologías utilizadas

- Java 21.
- Sockets TCP.
- Hilos para concurrencia.
- Autenticación mediante token.
- Relojes lógicos de Lamport.
- Git y GitHub para el control de versiones.
- LaTeX para la documentación.

## 5. Configuración del sistema

El sistema contempla los siguientes parámetros:

- `aforo`: capacidad máxima del establecimiento.
- `token`: clave utilizada para validar solicitudes.
- `host`: dirección del coordinador.
- `puerto`: puerto de comunicación TCP.

Para las pruebas se estableció un aforo máximo de 10 personas.

## 6. Compilación y ejecución prevista

El proyecto está desarrollado para Java 21.

Para compilar los archivos desde la carpeta principal:

```bash
javac -encoding UTF-8 -d out src/*.java
```

Para ejecutar el coordinador:

```bash
java -cp out Coordinador
```

Para ejecutar las tres puertas en terminales independientes:

```bash
java -cp out Puerta 1
java -cp out Puerta 2
java -cp out Puerta 3
```

Para ejecutar la prueba de carga:

```bash
java -cp out PruebaCarga seguro
```

Para iniciar el coordinador en modo inseguro:

```bash
java -cp out Coordinador --inseguro
```

Para procesar los registros:

```bash
java -cp out OrdenTotal
```

**Nota:** Estos comandos representan la ejecución prevista. No se verificó la compilación completa del repositorio publicado porque faltan las clases auxiliares `Config`, `Protocolo` y `RegistroLamport`, utilizadas por el código principal.

## 7. Comunicación entre procesos

La comunicación entre las puertas y el coordinador está planteada mediante sockets TCP.

Las solicitudes contempladas son:

- `ENTRAR`: solicitar permiso para ingresar.
- `SALIR`: registrar la salida de una persona.
- `ESTADO`: consultar el aforo y estado del sistema.
- `LATIDO`: informar que una puerta continúa activa.

El intercambio utiliza mensajes de texto delimitados por líneas. La estructura interna de los mensajes depende de la clase auxiliar `Protocolo`, pendiente de incluir en el repositorio.

## 8. Relojes lógicos y registros

El sistema contempla el uso de relojes de Lamport para organizar los eventos generados por los procesos.

Los registros utilizan el formato previsto:

```text
lamport;id_proceso;evento;detalle
```

El programa `OrdenTotal` tiene como finalidad reunir los registros y organizarlos por el valor del reloj lógico y el identificador del proceso.

## 9. Estado de las pruebas

Se desarrolló código relacionado con los requerimientos principales de la actividad.

Sin embargo, no se dispone de evidencias completas que permitan confirmar la ejecución satisfactoria de los escenarios E0 a E6.

Por este motivo, no se presentan cifras de ejecución o resultados no comprobados.

## 10. Documentación en LaTeX

El informe del proyecto se encuentra previsto en la carpeta `docs/`.

Para compilar el informe:

```bash
cd docs
pdflatex informe.tex
pdflatex informe.tex
```

Para generar el documento de entrega del SGA:

```bash
pdflatex entrega_SGA.tex
```

El informe técnico debe tener entre 2 y 4 páginas. El documento destinado al SGA debe ocupar una sola página e incluir los datos identificativos y la URL completa del repositorio.

Para ejecutar estos comandos se necesita una distribución de LaTeX con las dependencias correspondientes.

## 11. Limitaciones del proyecto

Durante el desarrollo de la actividad se trabajó en los cuatro archivos Java principales.

No se logró verificar el funcionamiento completo del sistema ni finalizar todas las evidencias de los escenarios solicitados.

También quedaron pendientes las clases auxiliares necesarias para compilar el proyecto completo desde el repositorio publicado.

## 12. Reutilización de código

No se reutilizó código.

## 13. Repositorio del proyecto

**GitHub:**  
https://github.com/Luis-paca/AP-U1-Paca-Luis

**Universidad Técnica Estatal de Quevedo**  
**Carrera de Ingeniería de Software**
