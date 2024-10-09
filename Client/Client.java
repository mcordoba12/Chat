import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 6789;
    private IncomingReader incomingReader; // Declaración de la variable


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

    private ArrayList<String> availableUsers = new ArrayList<>();

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

            // Crear e iniciar el hilo para recibir mensajes del servidor
            incomingReader = new IncomingReader(); // Instanciar IncomingReader
            new Thread(incomingReader).start(); // Iniciar el hilo

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

       // openParticipantsPopup();

        out.println("GET_AVAILABLE_USERS");

        // Wait a bit for the server to respond
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (availableUsers.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No users available for calling.");
            return;
        }

        String[] options = availableUsers.toArray(new String[0]);

        JPanel panel = new JPanel(new GridLayout(0, 1)); // Grid layout with one column
        List<JCheckBox> checkBoxes = new ArrayList<>();

        for (String user : options) {
            JCheckBox checkBox = new JCheckBox(user);
            checkBoxes.add(checkBox);
            panel.add(checkBox);
        }

        int result = JOptionPane.showConfirmDialog(frame, panel, "Select users to call", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            // Collect selected users
            List<String> selectedUsers = new ArrayList<>();
            for (JCheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    selectedUsers.add(checkBox.getText());
                }
            }

            if (!selectedUsers.isEmpty()) {
                // Send a call request for each selected user
                for (String user : selectedUsers) {
                    out.println("CALL_REQUEST:" + user);
                    JOptionPane.showMessageDialog(frame, "Calling " + user + "...");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "No users selected.");
            }
        }

    }

    public static void openParticipantsPopup(){ // para elegir a cuales personas quieres llamar
        JFrame popupFrame = new JFrame("Select participants to call:");
        popupFrame.setSize(300,500);
        popupFrame.setLayout(new FlowLayout());

        JLabel popupLabel = new JLabel("Select participants to call:");
        popupFrame.add(popupLabel);

       // this.out.println("getClientNames");
       // Set<String> participants = Server.getClientNames();





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
        private volatile boolean isRunning = true; // Variable para controlar el estado del hilo

        public void run() {
            try {
                String message;
                while (isRunning && (message = in.readLine()) != null) {
                    // Mostrar mensajes de otros usuarios
                    System.out.printf("received message "+message);
                    if (message.startsWith("AVAILABLE_USERS:")) {
                        updateAvailableUSers(message.substring("AVAILABLE_USERS:".length()));
                    }
                    else if (!message.startsWith(clientName + ":")) {
                        displayMessage(message, false); // Mostrar mensajes de otros usuarios en el lado izquierdo
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection(); // Cierra la ventana al desconectarse
            }
        }

        // Método para detener el hilo de lectura
        public void stop() {
            isRunning = false; // Cambia el estado para detener el bucle
            try {
                if (in != null) {
                    in.close(); // Cierra el BufferedReader
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateAvailableUSers(String userListString){
        availableUsers.clear();
        String[] users = userListString.split(",");
        for(String user: users){
            if(!user.isEmpty() && !user.equals(clientName)){
                availableUsers.add(user);
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
            if (socket != null) {
                out.println(clientName + " ha salido."); // Notificar al servidor que el usuario ha salido
                incomingReader.stop(); // Detener el hilo de lectura
                socket.close(); // Cerrar el socket
                System.out.println("se cerro");
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
