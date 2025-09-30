import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;
public class Main {
    private static final String MENSAJES_FILE = "mensajes.txt";
    private static final String USERS_FILE = "users.txt";
    private static final String BANEADOS_FILE = "baneados.txt";
    private static final String FILES_DIRECTORY = "user_files";
    public static void main(String[] args) {
        new File(FILES_DIRECTORY).mkdir();
        crearArchivoSiNoExiste(USERS_FILE, "usuarios");
        crearArchivoSiNoExiste(MENSAJES_FILE, "mensajes");
        crearArchivoSiNoExiste(BANEADOS_FILE, "baneados");

        ServerSocket socketEspecial = null;
        try {
            socketEspecial = new ServerSocket(8080);
            System.out.println("Servidor iniciado en el puerto 8080, esperando cliente...");
        } catch (IOException e) {
            System.out.println("Hubo problemas en la conexion de red");
            System.exit(1);
        }

        Socket cliente = null;
        try {
            cliente = socketEspecial.accept();
            System.out.println("Cliente conectado: " + cliente.getInetAddress().getHostName());
        } catch (IOException e) {
            System.out.println("Hubo problemas en la conexion de red");
            System.exit(1);
        }


    }
    private static void crearArchivoSiNoExiste(String nombreArchivo, String tipo) {
        try {
            File file = new File(nombreArchivo);
            if (file.createNewFile()) {
                System.out.println("Archivo '" + nombreArchivo + "' creado.");
            } else {
                System.out.println("El archivo '" + nombreArchivo + "' ya existe.");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo de " + tipo + ".");
            System.exit(1);
        }
    }

    private static boolean usuarioExiste(String username) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length > 0 && parts[0].trim().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void registrarUsuario(String username, String password) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + ":" + password);
            writer.newLine();
        }
    }

    private static boolean validarCredenciales(String username, String password) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            String credentialsToFind = username + ":" + password;
            while ((line = reader.readLine()) != null) {
                if (line.equals(credentialsToFind)) {
                    return true;
                }
            }
        }
        return false;
    }
    private static void guardarMensaje(String emisor, String receptor, String mensaje) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MENSAJES_FILE, true))) {
            writer.write(emisor + ":" + receptor + ":" + mensaje);
            writer.newLine();
        }
    }
    private static List<String> getMensajesParaUsuario(String usuarioDestinatario) throws IOException {
        List<String> mensajesDelUsuario = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(MENSAJES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3 && parts[1].trim().equalsIgnoreCase(usuarioDestinatario)) {
                    mensajesDelUsuario.add("De: " + parts[0].trim() + " - Mensaje: " + parts[2].trim());
                }
            }
        }
        return mensajesDelUsuario;
    }
    private static void eliminarUsuario(String usuarioAEliminar) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(USERS_FILE));
        List<String> lineasActualizadas = lineas.stream()
                .filter(linea -> !linea.trim().startsWith(usuarioAEliminar + ":"))
                .collect(Collectors.toList());
        Files.write(Paths.get(USERS_FILE), lineasActualizadas);
    }

    private static void eliminarMensajesDeUsuario(String usuarioAEliminar) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(MENSAJES_FILE));
        List<String> lineasActualizadas = lineas.stream()
                .filter(linea -> {
                    String[] partes = linea.split(":", 3);
                    return partes.length == 3 && !partes[0].trim().equals(usuarioAEliminar) && !partes[1].trim().equals(usuarioAEliminar);
                })
                .collect(Collectors.toList());

        Files.write(Paths.get(MENSAJES_FILE), lineasActualizadas);
    }

    private static List<String> getMensajesEnviadosPorUsuario(String usuarioEmisor) throws IOException {
        List<String> mensajesEnviados = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(MENSAJES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3 && parts[0].trim().equalsIgnoreCase(usuarioEmisor)) {
                    mensajesEnviados.add("Para: " + parts[1].trim() + " - Mensaje: " + parts[2].trim());
                }
            }
        }
        return mensajesEnviados;
    }

    private static boolean eliminarMensajeEnviado(String usuarioEmisor, int indiceAEliminar) throws IOException {
        List<String> todasLasLineas = Files.readAllLines(Paths.get(MENSAJES_FILE));
        List<String> lineasActualizadas = new ArrayList<>();
        int contadorMensajesPropios = 0;
        boolean eliminado = false;

        for (String linea : todasLasLineas) {
            String[] partes = linea.split(":", 3);
            if (partes.length == 3 && partes[0].trim().equalsIgnoreCase(usuarioEmisor)) {
                contadorMensajesPropios++;
                if (contadorMensajesPropios == indiceAEliminar) {
                    eliminado = true;
                    continue;
                }
            }
            lineasActualizadas.add(linea);
        }

        if (eliminado) {
            Files.write(Paths.get(MENSAJES_FILE), lineasActualizadas);
        }

        return eliminado;
    }
    private static String getUsuariosDisponibles(String usuarioLogueado) throws IOException {
        List<String> todos = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                todos.add(line.split(":")[0]);
            }
        }
        List<String> bloqueados = getUsuariosBloqueados(usuarioLogueado);
        todos.remove(usuarioLogueado);
        todos.removeAll(bloqueados);
        return String.join(", ", todos);
    }
    private static List<String> getUsuariosBloqueados(String usuario) throws IOException {
        List<String> bloqueados = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(BANEADOS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && parts[0].trim().equals(usuario)) {
                    bloqueados.add(parts[1].trim());
                }
            }
        }
        return bloqueados;
    }
    private static void bloquearUsuario(String bloqueador, String bloqueado) throws IOException {
        if (!getUsuariosBloqueados(bloqueador).contains(bloqueado)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BANEADOS_FILE, true))) {
                writer.write(bloqueador + ":" + bloqueado);
                writer.newLine();
            }
        }
    }
    private static void crearArchivoUsuario(String usuario, String nombreArchivo, String contenido, PrintWriter escritor) {
        if (nombreArchivo == null || !nombreArchivo.matches("[a-zA-Z0-9_\\-]+")) {
            escritor.println("ERROR: El nombre del archivo contiene caracteres no válidos.");
            return;
        }

        String nombreCompleto = usuario + "_" + nombreArchivo + ".txt";
        File archivo = new File(FILES_DIRECTORY, nombreCompleto);

        if (archivo.exists()) {
            escritor.println("ERROR: Ya existe un archivo con ese nombre.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            writer.write(contenido.replace("\\n", System.lineSeparator()));
            escritor.println("EXITO: Archivo '" + nombreArchivo + ".txt' creado correctamente.");
        } catch (IOException e) {
            escritor.println("ERROR: No se pudo crear el archivo en el servidor.");
        }
    }
    private static void listarArchivosDeUsuario(String usuario, PrintWriter escritor) {
        File dir = new File(FILES_DIRECTORY);
        File[] archivos = dir.listFiles((d, name) -> name.startsWith(usuario + "_") && name.endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            escritor.println("El usuario '" + usuario + "' no tiene archivos.");
        } else {
            for (File archivo : archivos) {
                escritor.println(archivo.getName());
            }
        }
        escritor.println("FIN_LISTA_ARCHIVOS");
    }
    private static void compartirArchivo(String usuarioReceptor, String duenoArchivo, String nombreArchivo, PrintWriter escritor) throws IOException {
        Path rutaOrigen = Paths.get(FILES_DIRECTORY, nombreArchivo);

        if (!Files.exists(rutaOrigen) || !nombreArchivo.startsWith(duenoArchivo + "_")) {
            escritor.println("ERROR: El archivo no existe o no pertenece al usuario indicado.");
            return;
        }

        String nombreSimple = nombreArchivo.substring(duenoArchivo.length() + 1); // Quita el "usuario_"
        String nombreArchivoCopia = "copia_de_" + nombreSimple;
        Path rutaDestino = Paths.get(FILES_DIRECTORY, usuarioReceptor + "_" + nombreArchivoCopia);

        try {
            Files.copy(rutaOrigen, rutaDestino);
            escritor.println("EXITO: Archivo copiado a tu directorio como '" + usuarioReceptor + "_" + nombreArchivoCopia + "'");
        } catch (IOException e) {
            escritor.println("ERROR: No se pudo copiar el archivo.");
        }
    }
    private static boolean desbloquearUsuario(String desbloqueador, String desbloqueado) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(BANEADOS_FILE));
        String lineaAEliminar = desbloqueador + ":" + desbloqueado;
        List<String> lineasActualizadas = lineas.stream()
                .filter(linea -> !linea.trim().equals(lineaAEliminar))
                .collect(Collectors.toList());

        if (lineas.size() > lineasActualizadas.size()) {
            Files.write(Paths.get(BANEADOS_FILE), lineasActualizadas);
            return true;
        }
        return false;
    }
}
class ClientHandler implements Runnable {
    private Socket clienteSocket;
    public ClientHandler(Socket socket) {
        this.clienteSocket = socket;
    }
    @Override
    public void run() {

    }
}
