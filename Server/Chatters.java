import java.util.Set;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Chatters {

    private Set<Person> clientes = new HashSet<>();

    public Chatters() {
    }

    public boolean existeUsr(String name) {
        boolean response = false;
        for (Person p : clientes) {
            if (name.equals(p.getName())) {
                response = true;
                break;
            }
        }
        return response;
    }

    public void addUsr(String name, PrintWriter out) {
        if (!name.isBlank() && !existeUsr(name)) {
            Person p = new Person(name, out);
            clientes.add(p);
        }
    }

    public void removeUsr(String name) {
        for (Person p : clientes) {
            if (name.equals(p.getName())) {
                clientes.remove(p);
                break;
            }
        }
    }

    public void broadcastMessage(String message) {

        for (Person p : clientes) {
            p.getOut().println(message);
        }

    }

    public void sendPrivateMessage(String recipientName, String message) { // Mensaje privado
        synchronized (clientes) {
            for (Person p : clientes) {
                if (recipientName.equals(p.getName())) {
                    p.getOut().println(message);
                    return;
                }
            }
            // Enviar un mensaje de error si el usuario no existe
            for (Person p : clientes) {
                if (p.getName().equals(message.split(":")[0].split(" ")[0])) {
                    p.getOut().println("Error: Usuario no encontrado.");
                }
            }
        }
    }

}
