import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Base64;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 6789;
    private IncomingReader incomingReader;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JFrame frame;
    private JPanel messagePanel;
    private JTextField textField;
    private JButton disconnectButton;
    private JButton sendButton;
    private JButton callButton;
    private JButton audioButton;
    private String clientName;
    private AudioRecorder audioRecorder;
    private boolean isRecording = false; // Estado de grabación
    private boolean isCalling = false; // Estado de la llamada
    private Socket audioSocket; // Socket de audio para la llamada
    private ServerSocket audioServerSocket; // ServerSocket para aceptar la llamada entrante

    public Client() {
        // Configuración de la interfaz gráfica
        frame = new JFrame("Chat Client");
        messagePanel = new JPanel();
        textField = new JTextField(30);
        disconnectButton = new JButton("Desconectar");
        sendButton = new JButton("Enviar Mensaje");
        callButton = new JButton("Llamar"); // El botón será dinámico: "Llamar" o "Colgar"
        audioButton = new JButton("Grabar Audio");

        audioRecorder = new AudioRecorder();

        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Configuración de colores
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textField.setBackground(new Color(230, 230, 250));
        textField.setForeground(Color.BLACK);
        textField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 2));
        textField.setFont(new Font("Arial", Font.PLAIN, 16));

        // Configuración de la ventana principal
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(400, 600);
        frame.setLocationRelativeTo(null);

        frame.add(scrollPane, BorderLayout.CENTER);

        // Panel para los botones de llamada y audio
        JPanel buttonPanelTop = new JPanel();
        buttonPanelTop.add(callButton);
        buttonPanelTop.add(audioButton);
        buttonPanelTop.add(disconnectButton);

        // Panel para el campo de texto y el botón de enviar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(textField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(buttonPanelTop, BorderLayout.NORTH);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Acciones de los botones
        sendButton.addActionListener(e -> sendMessage());
        callButton.addActionListener(e -> toggleCall());
        disconnectButton.addActionListener(e -> disconnect());
        audioButton.addActionListener(e -> toggleRecording());

        frame.setVisible(true);
    }

    public void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            System.out.println("Conectado al servidor.");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Crear e iniciar el hilo para recibir mensajes del servidor
            incomingReader = new IncomingReader();
            new Thread(incomingReader).start();

            // Solicitar nombre de usuario
            requestUserName();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "No se pudo conectar al servidor.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void requestUserName() {
        while (true) {
            clientName = JOptionPane.showInputDialog(frame, "Ingrese su nombre:");
            if (clientName != null && !clientName.trim().isEmpty()) {
                out.println(clientName);
                break;
            } else {
                JOptionPane.showMessageDialog(frame, "Por favor, ingrese un nombre válido.");
            }
        }
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            displayMessage(clientName + ": " + message, true);
            out.println(message);
            textField.setText("");
        }
    }

    // Método para iniciar y detener llamadas con el mismo botón
    private void toggleCall() {
        if (!isCalling) {
            makeCall(); // Iniciar la llamada
        } else {
            stopAudioCall(); // Colgar la llamada
        }
    }

    private void makeCall() {
        String recipientName = JOptionPane.showInputDialog(frame, "¿A quién deseas llamar?");
        if (recipientName != null && !recipientName.trim().isEmpty()) {
            out.println("CALL:" + recipientName); // Enviar la solicitud de llamada
            isCalling = true;
            callButton.setText("Colgar"); // Cambiar el texto del botón a "Colgar"
        }
    }

    private void stopAudioCall() {
        try {
            // Cerrar el socket de audio si está abierto
            if (audioSocket != null && !audioSocket.isClosed()) {
                audioSocket.close();
                System.out.println("Socket de audio cerrado.");
            }

            // Cerrar el ServerSocket de audio si está abierto
            if (audioServerSocket != null && !audioServerSocket.isClosed()) {
                audioServerSocket.close();
                System.out.println("ServerSocket de audio cerrado.");
            }

            isCalling = false;
            callButton.setText("Llamar"); // Cambiar el texto del botón de nuevo a "Llamar"

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleRecording() {
        if (!isRecording) {
            // Iniciar grabación
            try {
                audioRecorder.startRecording();
                isRecording = true;
                audioButton.setText("Detener Grabación");
                displaySystemMessage("Grabación iniciada...");
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "No se pudo acceder al micrófono.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Detener grabación y enviar audio
            byte[] audioData = audioRecorder.stopRecording();
            isRecording = false;
            audioButton.setText("Grabar Audio");
            displaySystemMessage("Grabación detenida.");

            // Convertir los bytes de audio a Base64 para enviarlos como string
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            sendAudioMessage(audioBase64);
        }
    }

    private void sendAudioMessage(String audioBase64) {
        out.println("AUDIO:" + audioBase64); // Enviar el audio como un mensaje especial
        displayMessage("Has enviado un audio.", true);
    }

    private void displayMessage(String message, boolean isOwnMessage) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BorderLayout());
        messageBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        messageBubble.setMaximumSize(new Dimension(300, 100));

        if (isOwnMessage) {
            messageBubble.setBackground(new Color(255, 182, 193)); // Mensaje propio
            JLabel messageLabel = new JLabel("<html>" + message + "</html>");
            messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            messageBubble.add(messageLabel, BorderLayout.EAST);
        } else {
            if (message.startsWith("AUDIO:")) {
                String audioBase64 = message.substring(6); // Extraer los datos de audio

                JButton playButton = new JButton("▶");
                playButton.setPreferredSize(new Dimension(50, 30));
                playButton.addActionListener(e -> playAudio(audioBase64));

                messageBubble.setBackground(new Color(240, 240, 240));
                messageBubble.add(playButton, BorderLayout.WEST);
            } else {
                messageBubble.setBackground(new Color(240, 240, 240)); // Mensaje recibido
                JLabel messageLabel = new JLabel("<html>" + message + "</html>");
                messageLabel.setHorizontalAlignment(SwingConstants.LEFT);
                messageBubble.add(messageLabel, BorderLayout.WEST);
            }
        }

        messagePanel.add(messageBubble);
        messagePanel.revalidate();
        messagePanel.repaint();

        JScrollBar vertical = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private void displaySystemMessage(String message) {
        JPanel messageBubble = new JPanel();
        JLabel messageLabel = new JLabel("<html><i>" + message + "</i></html>");
        messageBubble.setLayout(new BorderLayout());
        messageBubble.setBackground(new Color(255, 255, 224)); // Mensaje del sistema
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageBubble.add(messageLabel, BorderLayout.CENTER);

        messageBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        messageBubble.setMaximumSize(new Dimension(300, 30));

        messagePanel.add(messageBubble);
        messagePanel.revalidate();
        messagePanel.repaint();

        JScrollBar vertical = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private void playAudio(String audioBase64) {
        byte[] audioData = Base64.getDecoder().decode(audioBase64);

        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 2, true, true);
                InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    speakers.write(buffer, 0, bytesRead);
                }

                speakers.drain();
                speakers.close();
                audioInputStream.close();

                displaySystemMessage("Has reproducido un audio.");
            } catch (LineUnavailableException | IOException e) {
                e.printStackTrace();
                displaySystemMessage("Error al reproducir el audio.");
            }
        }).start();
    }

    private class IncomingReader implements Runnable {
        private volatile boolean isRunning = true;

        public void run() {
            try {
                String message;
                while (isRunning && (message = in.readLine()) != null) {
                    if (message.startsWith("AUDIO:")) {
                        displayMessage(message, false);
                    } else if (message.startsWith("CALLPORT:")) {
                        int port = Integer.parseInt(message.substring(9));
                        audioSocket = new Socket(SERVER_IP, port);
                        new AudioCallHandler(audioSocket).startAudioCall();
                    } else {
                        if (!message.startsWith(clientName + ":")) {
                            displayMessage(message, false);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }
    }

    private class AudioCallHandler {
        private Socket audioSocket;
        private AudioFormat format;

        public AudioCallHandler(Socket socket) {
            this.audioSocket = socket;
            this.format = new AudioFormat(16000, 16, 2, true, true);
        }

        public void startAudioCall() {
            new Thread(() -> {
                try {
                    TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
                    microphone.open(format);
                    microphone.start();

                    OutputStream outStream = audioSocket.getOutputStream();
                    byte[] buffer = new byte[4096];
                    while (!audioSocket.isClosed()) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            outStream.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
                    speakers.open(format);
                    speakers.start();

                    InputStream inStream = audioSocket.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        if (bytesRead > 0) {
                            speakers.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void disconnect() {
        System.out.println("Desconectando...");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket principal cerrado.");
            }

            if (audioSocket != null && !audioSocket.isClosed()) {
                audioSocket.close();
                System.out.println("Socket de audio cerrado.");
            }

            if (audioServerSocket != null && !audioServerSocket.isClosed()) {
                audioServerSocket.close();
                System.out.println("ServerSocket de audio cerrado.");
            }

            if (in != null) {
                in.close();
                System.out.println("BufferedReader cerrado.");
            }

            if (out != null) {
                out.close();
                System.out.println("PrintWriter cerrado.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Cerrando ventana del cliente...");
                frame.dispose();
            });
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.connectToServer();
    }
}
