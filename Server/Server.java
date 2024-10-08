import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.text.*;

public class Server {

    private static Set<PrintWriter> writers = new HashSet<>();
    private static JTextPane textArea;
    private static Map<String, PrintWriter> clientWriters = new HashMap<>();
    private static Set<String> clientNames = new HashSet<>();

    public static void main(String[] args) {
        int PORT = 6789;

        // Configuración de la interfaz gráfica
        JFrame frame = new JFrame("Chat Server");
        textArea = new JTextPane();
        textArea.setEditable(false);

        // Configuración de diseño estético
        textArea.setBackground(Color.PINK);
        textArea.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5));
        textArea.setMargin(new Insets(10, 10, 10, 10));

        // Etiqueta de título
        JLabel titleLabel = new JLabel("Servidor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBackground(Color.PINK);
        titleLabel.setOpaque(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(titleLabel, BorderLayout.NORTH);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Fondo rosado para toda la ventana
        frame.getContentPane().setBackground(Color.PINK);

        // Establecer tamaño mínimo y preferido de la ventana
        frame.setSize(800, 600);
        frame.setMinimumSize(new Dimension(600, 400));

        frame.pack();
        frame.setVisible(true);

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            appendToTextArea("Servidor iniciado. Esperando clientes...", Color.BLUE, Font.ITALIC);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                appendToTextArea("Nuevo cliente conectado: " + clientSocket, Color.GREEN, Font.PLAIN);

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
                textArea.setCaretPosition(doc.getLength());
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
                    clientName = name;
                    break;
                }

                // Guardar el escritor del cliente
                synchronized (writers) {
                    writers.add(out);
                    clientWriters.put(name, out);
                }

                appendToTextArea(name + " se ha unido.", Color.MAGENTA, Font.BOLD);
                broadcast(name + " se ha conectado.");

                // Manejo de mensajes de los clientes
                String message;
                while (!Thread.currentThread().isInterrupted()) {
                    if (socket.isClosed()) {
                        break;
                    }
                    message = in.readLine();

                    if (message == null) {
                        break;
                    }

                    if (message.startsWith("AUDIO:")) {
                        String audioBase64 = message.substring(6);
                        appendToTextArea(name + " ha enviado un audio.", Color.BLUE, Font.ITALIC);
                        broadcastExceptSender("AUDIO:" + audioBase64, out);
                    } else if (message.startsWith("@")) {
                        int spaceIndex = message.indexOf(' ');
                        if (spaceIndex > 1) {
                            String targetName = message.substring(1, spaceIndex);
                            String privateMessage = message.substring(spaceIndex + 1);
                            sendPrivateMessage(targetName, name + " (privado): " + privateMessage);
                        }
                    } else {
                        appendToTextArea(name + ": " + message, Color.BLACK, Font.PLAIN);
                        broadcast(name + ": " + message);
                    }
                }
            } catch (IOException e) {
                // Manejar la excepción de conexión
                System.out.println("Conexión cerrada: " + e.getMessage());
                Thread.currentThread().interrupt();
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
                broadcast(clientName + " ha salido.");
                appendToTextArea(clientName + " ha salido.", Color.RED, Font.BOLD);
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

        // Método para reenviar mensajes a todos excepto al remitente
        private void broadcastExceptSender(String message, PrintWriter senderOut) {
            synchronized (writers) {
                for (PrintWriter writer : writers) {
                    if (writer != senderOut) {
                        writer.println(message);
                    }
                }
            }
        }
    }
}
