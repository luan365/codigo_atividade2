import java.io.*;
import java.net.*;

public class Receptor {
    private final int port;

    public Receptor(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("[R] Escutando porta " + port);

        while (true) {
            final Socket clientSocket = serverSocket.accept();
            System.out.println("[R] Conexão recebida de " + clientSocket.getInetAddress());

            // Cada cliente em uma thread separada
            Thread clientHandler = new Thread(() -> handleClient(clientSocket));
            clientHandler.start();
        }
    }

    private void handleClient(Socket socket) {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Object obj;
            while ((obj = ois.readObject()) != null) {
                if (obj instanceof ComunicadoEncerramento) {
                    System.out.println("[R] Recebido ComunicadoEncerramento de " + socket.getInetAddress());
                    break;
                }

                if (obj instanceof ComunicadoResultado) {
                    ComunicadoResultado cr = (ComunicadoResultado) obj;
                    String fname = cr.getFilename();
                    System.out.println("[R] Recebido ComunicadoResultado de " + socket.getInetAddress() + " -> gravando em " + fname);
                    byte[] data = cr.getData();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fname))) {
                        for (int i = 0; i < data.length; i++) {
                            bw.write(Byte.toString(data[i]));
                            if (i < data.length - 1) bw.write(' ');
                        }
                    }
                    System.out.println("[R] Arquivo gravado: " + fname);
                    continue;
                }

                if (obj instanceof Pedido) {
                    Pedido pedido = (Pedido) obj;
                    System.out.println("[R] Pedido recebido com " + pedido.getNumeros().length + " bytes. Ordenando...");

                    long inicio = System.nanoTime();
                    try {
                        pedido.ordenar();
                    } catch (InterruptedException ie) {
                        System.err.println("[R] Ordenação interrompida!");
                        Thread.currentThread().interrupt();
                    }
                    long fim = System.nanoTime();

                    byte[] ordenado = pedido.getNumeros();
                    Resposta resposta = new Resposta(ordenado);
                    oos.writeObject(resposta);
                    oos.flush();

                    double tempoS = (fim - inicio) / 1e9;
                    System.out.println("[R] Resposta enviada (" + ordenado.length + " bytes) em " + String.format("%.3f", tempoS) + "s");
                } else {
                    System.err.println("[R] Objeto desconhecido recebido: " + obj.getClass().getName());
                }
            }
        } catch (EOFException eof) {
            // Cliente fechou conexão
        } catch (Exception e) {
            System.err.println("[R] Erro ao processar cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        Receptor receptor = new Receptor(port);
        receptor.start();
    }
}
