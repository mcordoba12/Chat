import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.text.*; // Para StyledDocument

public class Server {

    private static Set<PrintWriter> writers = new HashSet<>();
    private static JTextPane textArea; // Usar JTextPane para formateo de texto
    private static Map<String, PrintWriter> clientWriters = new HashMap<>(); // Almacena nombres de clientes y sus escritores
    private static Set<String> clientNames = new HashSet<>(); // Para almacenar nombres de clientes

    public static void main(String[] args) {
        int PORT = 6789;

        // Configuración de la interfaz gráfica
        JFrame frame = new JFrame("Chat Server");
        textArea = new JTextPane();
        textArea.setEditable(false);

        // Configuración de diseño estético
        textArea.setBackground(Color.PINK);  // Fondo rosado
        textArea.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5));  // Borde blanco
        textArea.setMargin(new Insets(10, 10, 10, 10)); // Márgenes en el área de texto

        // Etiqueta de título
        JLabel titleLabel = new JLabel("Servidor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36)); // Fuente grande y en negrita
        titleLabel.setForeground(Color.WHITE); // Color del texto
        titleLabel.setBackground(Color.PINK); // Fondo rosado
        titleLabel.setOpaque(true); // Permitir que el fondo sea visible

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(titleLabel, BorderLayout.NORTH); // Añadir el título en la parte superior
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Fondo rosado para toda la ventana
        frame.getContentPane().setBackground(Color.PINK);

        // Establecer tamaño mínimo y preferido de la ventana
        frame.setSize(800, 600); // Tamaño inicial de la ventana
        frame.setMinimumSize(new Dimension(600, 400)); // Tamaño mínimo de la ventana

        frame.pack();
        frame.setVisible(true);

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            appendToTextArea("Servidor iniciado. Esperando clientes...", Color.BLUE, Font.ITALIC); // Mensaje de inicio

            while (true) {
                Socket clientSocket = serverSocket.accept();
                appendToTextArea("Nuevo cliente conectado: " + clientSocket, Color.GREEN, Font.PLAIN); // Mensaje de nuevo cliente

                // Crea el objeto Runnable
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                // Inicia el hilo con el objeto Runnable
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para agregar mensajes al área de texto
    private static void appendToTextArea(String message, Color color, int fontStyle) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = textArea.getStyledDocument();
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                StyleConstants.setFontSize(attrs, 14);
                StyleConstants.setFontFamily(attrs, "Arial");
                StyleConstants.setItalic(attrs, fontStyle == Font.ITALIC);
                StyleConstants.setBold(attrs, fontStyle == Font.BOLD);

                doc.insertString(doc.getLength(), message + "\n", attrs);
                textArea.setCaretPosition(doc.getLength()); // Desplazar hacia abajo
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    // Clase interna para manejar clientes
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Lógica para manejar el nombre del cliente
                String name;
                while (true) {
                    name = in.readLine();
                    if (name == null || clientNames.contains(name)) {
                        continue;
                    }
                    clientNames.add(name);
                    break;
                }

                // Guardar el escritor del cliente
                synchronized (writers) {
                    writers.add(out);
                    clientWriters.put(name, out);
                }

                appendToTextArea(name + " se ha unido.", Color.MAGENTA, Font.BOLD); // Mensaje de conexión
                broadcast(name + " se ha conectado."); // Notificar a todos los clientes

                // Manejo de mensajes de los clientes
                String message;
                while (!Thread.currentThread().isInterrupted()) {
                    // Verificar si el socket está cerrado antes de leer
                    if (socket.isClosed()) {
                        break; // Salir del bucle si el socket está cerrado
                    }
                    message = in.readLine(); // Intentar leer el mensaje

                    if (message == null) {
                        // Si el mensaje es nulo, el servidor cerró la conexión
                        break;
                    }
                    if (message.startsWith("@")) {
                        // Mensaje privado
                        int spaceIndex = message.indexOf(' ');
                        if (spaceIndex > 1) {
                            String targetName = message.substring(1, spaceIndex);
                            String privateMessage = message.substring(spaceIndex + 1);
                            sendPrivateMessage(targetName, name + " (privado): " + privateMessage);
                        }
                    } else {
                        appendToTextArea(name + ": " + message, Color.BLACK, Font.PLAIN); // Mensaje normal
                        broadcast(name + ": " + message); // Reenviar mensaje a todos los clientes
                    }
                }
            } catch (IOException e) {
                // Manejar la excepción de conexión
                System.out.println("Conexión cerrada: " + e.getMessage());
                Thread.currentThread().interrupt(); // Interrumpir el hilo
            } finally {
                // Cerrar el socket y limpiar recursos
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (writers) {
                    writers.remove(out);
                }
                clientNames.remove(clientName);
            }
        }


        // Método para enviar un mensaje a un cliente específico
        private void sendPrivateMessage(String targetName, String message) {
            PrintWriter targetWriter = clientWriters.get(targetName);
            if (targetWriter != null) {
                targetWriter.println(message);
            } else {
                out.println("El usuario " + targetName + " no está conectado.");
            }
        }

        // Método para enviar un mensaje a todos los clientes
        private void broadcast(String message) {
            synchronized (writers) {
                for (PrintWriter writer : writers) {
                    writer.println(message);
                }
            }
        }
    }
}
