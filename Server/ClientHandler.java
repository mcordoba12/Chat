import java.io.*;
import java.net.*;
import java.util.*;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    Chatters clientes;

    public ClientHandler(Socket socket, Chatters clientes) {
        this.clientes = clientes;
        this.clientSocket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // Se inicializa el flujo de salida aquí
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {

            while (true) {
                // out.println("Ingrese su nombre:");
                out.println("SUBMITNAME");
                clientName = in.readLine();
                if (clientName == null) {
                    return;
                }
                synchronized (clientName) {
                    if (!clientName.isBlank() && !clientes.existeUsr(clientName)) {

                        clientes.broadcastMessage(clientName + " se ha unido al chat.");
                        out.println("NAMEACCEPTED " + clientName);
                        clientes.addUsr(clientName, out);
                        break;
                    }

                }
            }

            String message;
            // esperar mensajes de cada cliente y enviarlo a todos los clientes
            // si el mensaje es dirijido a un cliente en especial, se debe
            while ((message = in.readLine()) != null) {

                if (message.startsWith("@")) { // Mensaje privado
                    // Mensaje privado
                    int spaceIndex = message.indexOf(' '); // Buscar el primer espacio
                    if (spaceIndex > 0) { // Si hay un espacio
                        String recipientName = message.substring(1, spaceIndex); // Extraer el nombre del destinatario
                        String privateMessage = message.substring(spaceIndex + 1); // Extraer el mensaje
                        clientes.sendPrivateMessage(recipientName, clientName + ": " + privateMessage); // Enviar el
                                                                                                        // mensaje
                    }
                } else {
                    // Mensaje público
                    clientes.broadcastMessage(clientName + ": " + message);
                }
            }

        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println(clientName + " ha abandonado el chat.");
                clientes.broadcastMessage(clientName + " ha abandonado el chat.");
                clientes.removeUsr(clientName);
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
    }
}
