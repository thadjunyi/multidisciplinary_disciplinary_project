package Network;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class ServerSim {

    private static final Logger LOGGER = Logger.getLogger(ServerSim.class.getName());

    private int port;
    private static ServerSocket server = null;

    public ServerSim(int port) {
        this.port = port;
        initConn();
    }

    public boolean initConn() {
        try {
            server = new ServerSocket(port);
            System.out.println("Listening...");
            return true;
        } catch (IOException e) {
            LOGGER.warning("Connection Failed: IOException\n" + e.toString());
            return false;
        }

    }

    public void runServer() {
        try {
            while (true) {
                Socket socket = server.accept();

                System.out.println("Client connected");

                // Pass the socket to the RequestHandler thread for processing
                RequestHandler requestHandler = new RequestHandler(socket);
                requestHandler.start();
            }
        } catch (IOException e) {
            LOGGER.warning("Connection Failed: IOException\n" + e.toString());
        }
    }

    public static void main(String[] args) {
        ServerSim serverSim = new ServerSim(NetworkConstants.PORT);

        serverSim.runServer();
    }

}

