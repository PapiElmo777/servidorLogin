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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
public class Main {
    private static final String MENSAJES_FILE = "mensajes.txt";
    private static final String USERS_FILE = "users.txt";
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

                        if (usuarioExiste(destinatario)) {
                            guardarMensaje(usuarioLogueado, destinatario, mensaje);
                            escritor.println("Mensaje enviado correctamente a " + destinatario);
                        } else {
                            escritor.println("ERROR: El usuario '" + destinatario + "' no existe.");
                        }
                    } else {
                        escritor.println("ERROR: Formato de mensaje incorrecto.");
                    }
                    break;

                case "VER_BUZON":
                        List<String> mensajes = getMensajesParaUsuario(usuarioLogueado);
                        escritor.println("TIENES " + mensajes.size() + " MENSAJES SIN LEER:");
                        for (String msg : mensajes) {
                            escritor.println(msg);
                        }
                        escritor.println("FIN_MENSAJES");
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
                    // Formato: "De: [emisor] - Mensaje: [texto]"
                    mensajesDelUsuario.add("De: " + parts[0].trim() + " - Mensaje: " + parts[2].trim());
                }
            }
        }
        return mensajesDelUsuario;
    }

}