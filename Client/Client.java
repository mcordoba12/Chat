import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 6789;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JFrame frame;
    private JPanel messagePanel;
    private JTextField textField;
    private JButton disconnectButton; // Botón para desconectar
    private JButton sendButton; // Botón para enviar mensaje
    private JButton callButton; // Botón para llamar
    private JButton audioButton; // Botón para audio
    private String clientName;

    public Client() {
        // Configuración de la interfaz gráfica
        frame = new JFrame("Chat Client");
        messagePanel = new JPanel();
        textField = new JTextField(30); // Ajustar el tamaño del campo de texto
        disconnectButton = new JButton("Desconectar"); // Inicializar el botón de desconexión
        sendButton = new JButton("Enviar Mensaje"); // Inicializar el botón de enviar
        callButton = new JButton("Llamar"); // Inicializar el botón de llamada
        audioButton = new JButton("Audio"); // Inicializar el botón de audio

        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS)); // Usar BoxLayout para apilar mensajes verticalmente
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Configuración de colores
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Espaciado del borde
        textField.setBackground(new Color(230, 230, 250)); // Color de fondo del campo de texto
        textField.setForeground(Color.BLACK);
        textField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 2)); // Borde del campo de texto
        textField.setFont(new Font("Arial", Font.PLAIN, 16)); // Fuente del campo de texto

        // Configuración de la ventana principal
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(400, 600); // Tamaño de la ventana
        frame.setLocationRelativeTo(null); // Centrar ventana en la pantalla

        frame.add(scrollPane, BorderLayout.CENTER);

        // Panel para los botones de llamada y audio
        JPanel buttonPanelTop = new JPanel();
        buttonPanelTop.add(callButton); // Añadir botón de llamar
        buttonPanelTop.add(audioButton); // Añadir botón de audio
        buttonPanelTop.add(disconnectButton); // Añadir botón de desconexión

        // Panel para el campo de texto y el botón de enviar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(textField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST); // Añadir botón de enviar

        frame.add(buttonPanelTop, BorderLayout.NORTH); // Añadir el panel superior
        frame.add(bottomPanel, BorderLayout.SOUTH); // Añadir el panel inferior

        // Acciones de los botones
        sendButton.addActionListener(e -> sendMessage()); // Acción para el botón de enviar
        callButton.addActionListener(e -> makeCall()); // Acción para el botón de llamar
        audioButton.addActionListener(e -> sendAudio()); // Acción para el botón de audio
        disconnectButton.addActionListener(e -> disconnect()); // Acción para el botón de desconexión

        frame.setVisible(true);
    }

    public void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            System.out.println("Conectado al servidor.");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Hilo para recibir mensajes del servidor
            new Thread(new IncomingReader()).start();

            // Solicitar nombre de usuario
            requestUserName();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestUserName() {
        // Método para solicitar el nombre de usuario
        while (true) {
            clientName = JOptionPane.showInputDialog(frame, "Ingrese su nombre:");
            if (clientName != null && !clientName.trim().isEmpty()) {
                out.println(clientName); // Enviar el nombre al servidor
                break;
            } else {
                JOptionPane.showMessageDialog(frame, "Por favor, ingrese un nombre válido.");
            }
        }
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) { // Verificar si el mensaje no está vacío
            // Mostrar el mensaje propio en el lado derecho
            displayMessage(clientName + ": " + message, true);
            out.println(message);  // Enviar el mensaje al servidor
            textField.setText(""); // Limpiar el campo de texto
        }
    }

    private void makeCall() {
        // Implementa la lógica para hacer una llamada
        JOptionPane.showMessageDialog(frame, "Llamando a otro usuario...");
        // Aquí puedes agregar la lógica específica de la llamada
    }

    private void sendAudio() {
        // Implementa la lógica para enviar audio
        JOptionPane.showMessageDialog(frame, "Enviando audio...");
        // Aquí puedes agregar la lógica específica para el envío de audio
    }

    // Método para mostrar los mensajes en burbujas
    private void displayMessage(String message, boolean isOwnMessage) {
        JPanel messageBubble = new JPanel();
        JLabel messageLabel = new JLabel(message);
        messageBubble.setLayout(new BorderLayout());

        // Configurar colores y alineación según sea propio o de otro usuario
        if (isOwnMessage) {
            messageBubble.setBackground(new Color(255, 182, 193)); // Rosado claro para los mensajes propios
            messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            messageBubble.add(messageLabel, BorderLayout.EAST);
        } else {
            messageBubble.setBackground(new Color(240, 240, 240)); // Gris claro para mensajes recibidos
            messageLabel.setHorizontalAlignment(SwingConstants.LEFT);
            messageBubble.add(messageLabel, BorderLayout.WEST);
        }

        messageBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Espaciado
        messageBubble.setMaximumSize(new Dimension(300, 30)); // Limitar el tamaño de las burbujas

        // Añadir la burbuja al panel de mensajes
        messagePanel.add(messageBubble);
        messagePanel.revalidate();
        messagePanel.repaint();

        // Desplazar el scroll hacia abajo cuando se recibe un mensaje
        JScrollBar vertical = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private class IncomingReader implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // Mostrar mensajes de otros usuarios
                    if (!message.startsWith(clientName + ":")) {
                        displayMessage(message, false); // Mostrar mensajes de otros usuarios en el lado izquierdo
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Cierra la ventana al desconectarse
                closeConnection();
            }
        }
    }

    // Método para cerrar la conexión y la ventana
    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.dispose(); // Cierra la ventana
    }

    private void disconnect() {
        System.out.println("Desconectando...");
        try {
            // Interrumpir el hilo de entrada antes de cerrar la conexión
            Thread.currentThread().interrupt(); // Interrumpe el hilo que ejecuta IncomingReader

            if (socket != null) {
                out.println(clientName + " ha salido."); // Notificar al servidor que el usuario ha salido
                in.close();
                out.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Cerrando ventana...");

                frame.dispose(); // Cerrar la ventana inmediatamente
            });
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
