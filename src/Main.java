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
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Servidor iniciado en el puerto 8080, esperando clientes...");
            while (true) {
                try {
                    Socket clienteSocket = serverSocket.accept();
                    System.out.println("Cliente conectado: " + clienteSocket.getInetAddress().getHostName());
                    ClientHandler clientHandler = new ClientHandler(clienteSocket);
                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    System.out.println("No se pudo aceptar la conexión del cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error grave en el servidor: " + e.getMessage());
        }
    }

    public static void crearArchivoSiNoExiste(String nombreArchivo, String tipo) {
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

    public static boolean usuarioExiste(String username) throws IOException {
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

    public static void registrarUsuario(String username, String password) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + ":" + password);
            writer.newLine();
        }
    }

    public static boolean validarCredenciales(String username, String password) throws IOException {
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
    public static void guardarMensaje(String emisor, String receptor, String mensaje) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MENSAJES_FILE, true))) {
            writer.write(emisor + ":" + receptor + ":" + mensaje);
            writer.newLine();
        }
    }
    public static List<String> getMensajesParaUsuario(String usuarioDestinatario) throws IOException {
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
    public static void eliminarUsuario(String usuarioAEliminar) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(USERS_FILE));
        List<String> lineasActualizadas = lineas.stream()
                .filter(linea -> !linea.trim().startsWith(usuarioAEliminar + ":"))
                .collect(Collectors.toList());
        Files.write(Paths.get(USERS_FILE), lineasActualizadas);
    }

    public static void eliminarMensajesDeUsuario(String usuarioAEliminar) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(MENSAJES_FILE));
        List<String> lineasActualizadas = lineas.stream()
                .filter(linea -> {
                    String[] partes = linea.split(":", 3);
                    return partes.length == 3 && !partes[0].trim().equals(usuarioAEliminar) && !partes[1].trim().equals(usuarioAEliminar);
                })
                .collect(Collectors.toList());

        Files.write(Paths.get(MENSAJES_FILE), lineasActualizadas);
    }

    public static List<String> getMensajesEnviadosPorUsuario(String usuarioEmisor) throws IOException {
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

    public static boolean eliminarMensajeEnviado(String usuarioEmisor, int indiceAEliminar) throws IOException {
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
    public static String getUsuariosDisponibles(String usuarioLogueado) throws IOException {
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
    public static List<String> getUsuariosBloqueados(String usuario) throws IOException {
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
    public static void bloquearUsuario(String bloqueador, String bloqueado) throws IOException {
        if (!getUsuariosBloqueados(bloqueador).contains(bloqueado)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BANEADOS_FILE, true))) {
                writer.write(bloqueador + ":" + bloqueado);
                writer.newLine();
            }
        }
    }
    public static void crearArchivoUsuario(String usuario, String nombreArchivo, String contenido, PrintWriter escritor) {
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
    public static void listarArchivosDeUsuario(String usuario, PrintWriter escritor) {
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
    public static void compartirArchivo(String usuarioReceptor, String duenoArchivo, String nombreArchivo, PrintWriter escritor) throws IOException {
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
    public static boolean desbloquearUsuario(String desbloqueador, String desbloqueado) throws IOException {
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
        try (
                PrintWriter escritor = new PrintWriter(clienteSocket.getOutputStream(), true);
                BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()))
        ) {
            boolean autenticado = false;
            String usuarioLogueado = "";
            while (!autenticado) {
                escritor.println("BIENVENIDO. Escriba 'REGISTRO' o 'LOGIN'");
                String opcion = lectorSocket.readLine();
                opcion.toLowerCase();
                if (opcion == null) break;

                if ("REGISTRO".equalsIgnoreCase(opcion)) {
                    escritor.println("Ingrese un nuevo nombre de usuario:");
                    String username = lectorSocket.readLine().trim();
                    escritor.println("Ingrese una nueva contraseña:");
                    String password = lectorSocket.readLine().trim();

                    if (Main.usuarioExiste(username)) {
                        escritor.println("ERROR: El nombre de usuario ya existe. Intente de nuevo.");
                    } else {
                        Main.registrarUsuario(username, password);
                        escritor.print("EXITO: Usuario registrado.");
                    }
                } else if ("LOGIN".equalsIgnoreCase(opcion)) {
                    escritor.println("Ingrese su nombre de usuario:");
                    String username = lectorSocket.readLine().trim();
                    escritor.println("Ingrese su contraseña:");
                    String password = lectorSocket.readLine().trim();

                    if (Main.validarCredenciales(username, password)) {
                        escritor.println("EXITO: Login correcto. Bienvenido " + username + "!");
                        usuarioLogueado = username;
                        autenticado = true;
                    } else {
                        escritor.println("ERROR: Usuario o contraseña incorrectos. Intente de nuevo.");
                    }
                } else {
                    escritor.println("ERROR: Opcion no valida.");
                }
            }
            if (autenticado) {
                System.out.println("El cliente" + usuarioLogueado + "ha iniciado sesión. Mostrando menu de opciones");
                String comandoCliente;

                while ((comandoCliente = lectorSocket.readLine()) != null) {
                    if ("FIN".equalsIgnoreCase(comandoCliente)) {
                        System.out.println("El cliente '" + usuarioLogueado + "' ha terminado la conexión.");
                        break;
                    }

                    String[] partesComando = comandoCliente.split(":", 3);
                    String accion = partesComando[0];
                    System.out.println("Usted a seleccionado " + accion);

                    switch (accion.toUpperCase()) {
                        case "LISTA_USUARIOS":
                            String listaUsuarios = Main.getUsuariosDisponibles(usuarioLogueado);
                            escritor.println(listaUsuarios);
                            break;

                        case "ENVIAR_MENSAJE":
                            if (partesComando.length == 3) {
                                String destinatario = partesComando[1];
                                String mensaje = partesComando[2];
                                List<String> usuariosBloqueados = Main.getUsuariosBloqueados(usuarioLogueado);
                                if (destinatario.equalsIgnoreCase(usuarioLogueado)) {
                                    escritor.println("No puedes enviarte mensajes a ti mismo, socializa!!!");
                                } else if (usuariosBloqueados.contains(destinatario)) {
                                    escritor.println("ERROR: No puedes enviar mensajes a " + destinatario + " porque estas enojado con el.");
                                } else if (Main.usuarioExiste(destinatario)) {
                                    Main.guardarMensaje(usuarioLogueado, destinatario, mensaje);
                                    escritor.println("Mensaje enviado correctamente a " + destinatario);
                                } else {
                                    escritor.println("ERROR: El usuario '" + destinatario + "' no existe.");
                                }
                            } else {
                                escritor.println("ERROR: Formato de mensaje incorrecto.");
                            }
                            break;

                        case "MIS_MENSAJES":
                            List<String> misMensajes = Main.getMensajesEnviadosPorUsuario(usuarioLogueado);
                            int i = 1;
                            for (String msg : misMensajes) {
                                escritor.println(i + ". " + msg);
                                i++;
                            }
                            escritor.println("FIN_LISTA_MENSAJES");
                            break;
                        case "ELIMINAR_MENSAJE":
                            if (partesComando.length == 2) {
                                try {
                                    int indice = Integer.parseInt(partesComando[1]);
                                    boolean exito = Main.eliminarMensajeEnviado(usuarioLogueado, indice);
                                    if (exito) {
                                        escritor.println("EXITO: Mensaje eliminado correctamente.");
                                    } else {
                                        escritor.println("ERROR: El número de mensaje no es válido.");
                                    }
                                } catch (NumberFormatException e) {
                                    escritor.println("ERROR: Debes ingresar un número válido.");
                                } catch (IOException e) {
                                    escritor.println("ERROR: No se pudo eliminar el mensaje del archivo.");
                                }
                            } else {
                                escritor.println("ERROR: Comando de eliminación no válido.");
                            }
                            break;
                        case "BLOQUEAR_USUARIO":
                            if (partesComando.length == 2) {
                                String usuarioABloquear = partesComando[1];
                                if (usuarioABloquear.equalsIgnoreCase(usuarioLogueado)) {
                                    escritor.println("ERROR: No te puedes bloquear a ti mismo.");
                                } else if (!Main.usuarioExiste(usuarioABloquear)) {
                                    escritor.println("ERROR: El usuario '" + usuarioABloquear + "' no existe.");
                                } else {
                                    Main.bloquearUsuario(usuarioLogueado, usuarioABloquear);
                                    escritor.println("EXITO: Has bloqueado a " + usuarioABloquear);
                                }
                            } else {
                                escritor.println("ERROR: Comando no válido.");
                            }
                            break;
                        case "LISTA_BLOQUEADOS":
                            List<String> bloqueados = Main.getUsuariosBloqueados(usuarioLogueado);
                            if (bloqueados.isEmpty()) {
                                escritor.println("No tienes a nadie bloqueado.");
                            } else {
                                escritor.println(String.join(", ", bloqueados));
                            }
                            escritor.println("FIN_LISTA_BLOQUEADOS");
                            break;
                        case "DESBLOQUEAR_USUARIO":
                            if (partesComando.length == 2) {
                                String usuarioADesbloquear = partesComando[1];
                                if (Main.desbloquearUsuario(usuarioLogueado, usuarioADesbloquear)) {
                                    escritor.println("EXITO: Has desbloqueado a " + usuarioADesbloquear);
                                } else {
                                    escritor.println("ERROR: No tenías bloqueado a ese usuario.");
                                }
                            } else {
                                escritor.println("ERROR: Comando no válido.");
                            }
                            break;
                        case "VER_BUZON":
                            List<String> mensajes = Main.getMensajesParaUsuario(usuarioLogueado);
                            escritor.println("TIENES " + mensajes.size() + " MENSAJES SIN LEER:");
                            for (String msg : mensajes) {
                                escritor.println(msg);
                            }
                            escritor.println("FIN_MENSAJES");
                            break;
                        case "CREAR_ARCHIVO":
                            if (partesComando.length == 3) {
                                String nombreArchivo = partesComando[1];
                                String contenido = partesComando[2];
                                Main.crearArchivoUsuario(usuarioLogueado, nombreArchivo, contenido, escritor);
                            }
                            break;
                        case "MIS_ARCHIVOS":
                            Main.listarArchivosDeUsuario(usuarioLogueado, escritor);
                            break;
                        case "LISTAR_ARCHIVOS_DE":
                            if (partesComando.length == 2) {
                                String otroUsuario = partesComando[1];
                                if (!Main.usuarioExiste(otroUsuario)) {
                                    escritor.println("ERROR: El usuario '" + otroUsuario + "' no existe.");
                                    escritor.println("FIN_LISTA_ARCHIVOS");
                                } else if (Main.getUsuariosBloqueados(usuarioLogueado).contains(otroUsuario) || Main.getUsuariosBloqueados(otroUsuario).contains(usuarioLogueado)) {
                                    escritor.println("ERROR: No puedes acceder a los archivos de " + otroUsuario + " porque estas enojado con el.");
                                    escritor.println("FIN_LISTA_ARCHIVOS");
                                } else {
                                    Main.listarArchivosDeUsuario(otroUsuario, escritor);
                                }
                            }
                            break;
                        case "COMPARTIR_ARCHIVO":
                            if (partesComando.length == 3) {
                                String duenoArchivo = partesComando[1];
                                String nombreArchivo = partesComando[2];
                                if (Main.getUsuariosBloqueados(usuarioLogueado).contains(duenoArchivo) || Main.getUsuariosBloqueados(duenoArchivo).contains(usuarioLogueado)) {
                                    escritor.println("ERROR: No puedes compartir archivos de " + duenoArchivo + " porque estas enojado con el.");
                                } else {
                                    Main.compartirArchivo(usuarioLogueado, duenoArchivo, nombreArchivo, escritor);
                                }
                            } else {
                                escritor.println("ERROR: Formato de comando incorrecto.");
                            }
                            break;

                        case "ELIMINAR_CUENTA":
                            try {
                                Main.eliminarUsuario(usuarioLogueado);
                                Main.eliminarMensajesDeUsuario(usuarioLogueado);
                                escritor.println("EXITO: Tu usuario y mensajes han sido eliminados. Desconectando.");
                                System.out.println("El usuario '" + usuarioLogueado + "' ha eliminado su cuenta.");
                                comandoCliente = "FIN";
                            } catch (IOException e) {
                                escritor.println("ERROR: No se pudo eliminar el usuario.");
                                System.err.println("Error al eliminar usuario de " + usuarioLogueado + ": " + e.getMessage());
                            }
                            break;
                        default:
                            escritor.println("ERROR: comando desconocido.");
                            break;


                    }

                }
            }

        } catch (IOException e) {
            System.out.println("Error de comunicacion con el cliente.");
        } finally {
            try {
                if (clienteSocket != null) clienteSocket.close();
                System.out.println("Conexiones cerradas.");
            } catch (IOException e) {
                System.out.println("Hubo problemas al cerrar las conexiones.");
            }
        }
    }
}
