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
public class Main {
    private static final String MENSAJES_FILE = "mensajes.txt";
    private static final String USERS_FILE = "users.txt";
    private static final String BANEADOS_FILE = "baneados.txt";
    public static void main(String[] args) {
        try {
            File file = new File(USERS_FILE);
            if (file.createNewFile()) {
                System.out.println("Archivo 'users.txt' creado.");
            } else {
                System.out.println("El archivo 'users.txt' ya existe.");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo de usuarios.");
            System.exit(1);
        }
        try {
            File file = new File(MENSAJES_FILE);
            if (file.createNewFile()) {
                System.out.println("Archivo 'mensajes.txt' creado.");
            } else {
                System.out.println("El archivo 'mensajes.txt' ya existe.");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo de mensajes.");
            System.exit(1);
        }
        try {
            File file = new File(BANEADOS_FILE);
            if (file.createNewFile()) {
                System.out.println("Archivo 'baneados.txt' creado.");
            } else {
                System.out.println("El archivo 'baneados.txt' ya existe.");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo de baneados.");
            System.exit(1);
        }

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

        try (
                PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
                BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()))
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

                    if (usuarioExiste(username)) {
                        escritor.println("ERROR: El nombre de usuario ya existe. Intente de nuevo.");
                    } else {
                        registrarUsuario(username, password);
                        escritor.print("EXITO: Usuario registrado.");
                    }
                } else if ("LOGIN".equalsIgnoreCase(opcion)) {
                    escritor.println("Ingrese su nombre de usuario:");
                    String username = lectorSocket.readLine().trim();
                    escritor.println("Ingrese su contraseña:");
                    String password = lectorSocket.readLine().trim();

                    if (validarCredenciales(username, password)) {
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

                while ((comandoCliente = lectorSocket.readLine()) != null){
                    if ("FIN".equalsIgnoreCase(comandoCliente)){
                        System.out.println("El cliente '" + usuarioLogueado + "' ha terminado la conexión.");
                        break;
                    }

                String[] partesComando = comandoCliente.split(":", 3);
                String accion = partesComando[0];
                System.out.println("Usted a seleccionado " + accion);

                switch (accion.toUpperCase()){
                    case "LISTA_USUARIOS":
                        String listaUsuarios = getTodosUsuarios();
                        escritor.println(listaUsuarios);
                break;

                case "ENVIAR_MENSAJE":
                    if (partesComando.length == 3) {
                        String destinatario = partesComando[1];
                        String mensaje = partesComando[2];

                        if (destinatario.equalsIgnoreCase(usuarioLogueado)){
                            escritor.println("No puedes enviarte mensajes a ti mismo, socializa!!!");
                        } else if (usuarioExiste(destinatario)) {
                            guardarMensaje(usuarioLogueado, destinatario, mensaje);
                            escritor.println("Mensaje enviado correctamente a " + destinatario);
                        } else {
                            escritor.println("ERROR: El usuario '" + destinatario + "' no existe.");
                        }
                    } else {
                        escritor.println("ERROR: Formato de mensaje incorrecto.");
                    }
                break;

                case "MIS_MENSAJES":
                        List<String> misMensajes = getMensajesEnviadosPorUsuario(usuarioLogueado);
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
                                boolean exito = eliminarMensajeEnviado(usuarioLogueado, indice);
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
                    case "BLOQUEAR":
                        break;
                case "VER_BUZON":
                        List<String> mensajes = getMensajesParaUsuario(usuarioLogueado);
                        escritor.println("TIENES " + mensajes.size() + " MENSAJES SIN LEER:");
                        for (String msg : mensajes) {
                            escritor.println(msg);
                        }
                        escritor.println("FIN_MENSAJES");
                break;

                case "ELIMINAR_CUENTA":
                        try {
                            eliminarUsuario(usuarioLogueado);
                            eliminarMensajesDeUsuario(usuarioLogueado);
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
                if (cliente != null) cliente.close();
                if (socketEspecial != null) socketEspecial.close();
                System.out.println("Conexiones cerradas.");
            } catch (IOException e) {
                System.out.println("Hubo problemas al cerrar las conexiones.");
            }
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
    private static String getTodosUsuarios() throws IOException {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length > 0) {
                    usuarios.add(parts[0].trim());
                }
            }
        }
        return String.join(", ", usuarios);
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

}