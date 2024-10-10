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
    private Socket audioSocket; // Socket de audio para la llamada
    private ServerSocket audioServerSocket; // ServerSocket para aceptar la llamada entrante

    public Client() {
        // Configuración de la interfaz gráfica
        frame = new JFrame("Chat Client");
        messagePanel = new JPanel();
        textField = new JTextField(30);
        disconnectButton = new JButton("Desconectar");
        sendButton = new JButton("Enviar Mensaje");
        callButton = new JButton("Llamar");
        audioButton = new JButton("Grabar Audio"); // Estado inicial

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
        callButton.addActionListener(e -> makeCall());
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

    private void makeCall() {
        String recipientName = JOptionPane.showInputDialog(frame, "¿A quién deseas llamar?");
        if (recipientName != null && !recipientName.trim().isEmpty()) {
            // Envía una solicitud de llamada al servidor
            out.println("CALL:" + recipientName);
        }
    }

    private void startAudioCall() {
        new Thread(() -> {
            try {
                audioServerSocket = new ServerSocket(0); // Crea un nuevo ServerSocket en un puerto aleatorio
                out.println("CALLPORT:" + audioServerSocket.getLocalPort()); // Enviar el puerto al servidor
                audioSocket = audioServerSocket.accept(); // Acepta la conexión de audio

                // Iniciar transmisión de audio
                new AudioCallHandler(audioSocket).startAudioCall();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
        // Enviar el mensaje con un prefijo especial para identificar que es un audio
        out.println("AUDIO:" + audioBase64);
        // Mostrar una indicación en la interfaz
        displayMessage("Has enviado un audio.", true);
    }

    private void displayMessage(String message, boolean isOwnMessage) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BorderLayout());
        messageBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        messageBubble.setMaximumSize(new Dimension(300, 100));

        if (isOwnMessage) {
            // Mensaje propio de texto
            messageBubble.setBackground(new Color(255, 182, 193)); // Rosado claro
            JLabel messageLabel = new JLabel("<html>" + message + "</html>");
            messageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            messageBubble.add(messageLabel, BorderLayout.EAST);
        } else {
            if (message.startsWith("AUDIO:")) {
                // Mensaje de audio
                String audioBase64 = message.substring(6); // Extraer los datos de audio

                // Crear un botón de reproducción con un icono de triángulo
                JButton playButton = new JButton("▶");
                playButton.setFocusPainted(false);
                playButton.setMargin(new Insets(5, 5, 5, 5));
                playButton.setPreferredSize(new Dimension(50, 30));

                // Agregar ActionListener para reproducir el audio cuando se haga clic
                playButton.addActionListener(e -> playAudio(audioBase64));

                // Añadir el botón al panel del mensaje
                messageBubble.setBackground(new Color(240, 240, 240)); // Gris claro
                messageBubble.add(playButton, BorderLayout.WEST);
            } else {
                // Mensaje de texto recibido
                messageBubble.setBackground(new Color(240, 240, 240)); // Gris claro
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

        messageBubble.setBackground(new Color(255, 255, 224)); // Amarillo claro
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
                // Configurar el formato de audio (debe coincidir con el formato de grabación)
                AudioFormat format = new AudioFormat(16000, 16, 2, true, true);
                InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());

                // Obtener una línea de reproducción
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();

                // Buffer para reproducir el audio
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    speakers.write(buffer, 0, bytesRead);
                }

                // Finalizar la reproducción
                speakers.drain();
                speakers.close();
                audioInputStream.close();

                // Opcional: Mostrar una indicación de que se ha reproducido el audio
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
                        // Mensaje de audio
                        displayMessage(message, false);
                    } else if (message.startsWith("CALLPORT:")) {
                        int port = Integer.parseInt(message.substring(9));
                        audioSocket = new Socket(SERVER_IP, port); // Conectar al puerto de la llamada
                        new AudioCallHandler(audioSocket).startAudioCall();
                    } else {
                        // Mensajes de texto
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
            // Cambiamos el formato de audio a 44.1 kHz, 16 bits, mono, little-endian
            this.format = new AudioFormat(44100, 16, 1, true, false); // 44.1 kHz, 16 bits, mono, little-endian
        }

        public void startAudioCall() {
            // Transmisión de audio desde el micrófono al otro cliente
            new Thread(() -> {
                try {
                    TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
                    microphone.open(format);
                    microphone.start();

                    OutputStream outStream = audioSocket.getOutputStream();
                    byte[] buffer = new byte[4096]; // Buffer para los datos de audio
                    while (!audioSocket.isClosed()) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            outStream.write(buffer, 0, bytesRead); // Enviar el audio capturado
                            outStream.flush();
                        }
                    }
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }).start();

            // Recepción de audio desde el otro cliente y reproducción
            new Thread(() -> {
                try {
                    SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
                    speakers.open(format);
                    speakers.start();

                    InputStream inStream = audioSocket.getInputStream();
                    byte[] buffer = new byte[4096]; // Buffer para los datos de audio recibidos
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        speakers.write(buffer, 0, bytesRead); // Reproducir el audio recibido
                    }
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }



    // Método para cerrar la conexión y la ventana
    private void disconnect() {
        System.out.println("Desconectando...");
        try {
            // Cerrar el socket principal si está abierto
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket principal cerrado.");
            }

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

            // Cerrar el BufferedReader (entrada)
            if (in != null) {
                in.close();
                System.out.println("BufferedReader cerrado.");
            }

            // Cerrar el PrintWriter (salida)
            if (out != null) {
                out.close();
                System.out.println("PrintWriter cerrado.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Cerrar la ventana del cliente
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
