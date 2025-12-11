import java.io.*;
import java.net.*;
import java.util.*;

public class Distribuidor {

    private static final String[] IPS = {
        "10.59.153.107",
        "10.59.153.161"
    };

    private final String[] hosts;
    private final int port = 12345;

    public Distribuidor(String[] hosts) {
        this.hosts = hosts;
    }

    public void execute(String outputFile, int totalBytes) throws Exception {
        System.out.println("Distribuidor: gerando vetor com " + totalBytes + " bytes...");
        byte[] big = new byte[totalBytes];
        new Random().nextBytes(big);

        // dividir entre hosts configurados (usa hosts param; se null, usa IPS hardcoded)
        String[] targetHosts = (hosts != null && hosts.length > 0) ? hosts : IPS;
        int parts = targetHosts.length;

        List<byte[]> chunks = new ArrayList<byte[]>();
        int base = big.length / parts;
        int rem = big.length % parts;
        int off = 0;
        for (int i = 0; i < parts; i++) {
            int size = base + (i < rem ? 1 : 0);
            byte[] seg = new byte[size];
            System.arraycopy(big, off, seg, 0, size);
            off += size;
            chunks.add(seg);
        }

        // threads para enviar e receber (manual com start/join)
        final byte[][] results = new byte[parts][];
        Thread[] threads = new Thread[parts];

        for (int i = 0; i < parts; i++) {
            final int idx = i;
            final String host = targetHosts[i];
            final byte[] chunk = chunks.get(i);
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try (Socket s = new Socket(host, port);
                         ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                         ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {

                        System.out.println("Distribuidor: conectado a " + host + " enviando " + chunk.length + " bytes");
                        Pedido p = new Pedido(chunk);
                        oos.writeObject(p);
                        oos.flush();

                        Object o = ois.readObject();
                        if (o instanceof Resposta) {
                            Resposta resp = (Resposta) o;
                            results[idx] = resp.getVetor();
                            System.out.println("Distribuidor: recebido de " + host + " (" + results[idx].length + " bytes)");
                        } else {
                            System.err.println("Distribuidor: resposta inesperada de " + host);
                            results[idx] = new byte[0];
                        }

                        // enviar encerramento
                        oos.writeObject(new ComunicadoEncerramento());
                        oos.flush();

                    } catch (Exception e) {
                        System.err.println("Distribuidor: erro com host " + host + " -> " + e.getMessage());
                        results[idx] = new byte[0];
                    }
                }
            });
            threads[i].start();
        }

        // join em todas as threads de rede
        for (int i = 0; i < parts; i++) {
            threads[i].join();
        }

        System.out.println("Distribuidor: todas respostas recebidas. Iniciando merges locais...");

        // agora fazemos rodadas de merges 2-a-2 com threads e join (mesma lógica do Pedido)
        List<byte[]> segments = new ArrayList<byte[]>();
        for (int i = 0; i < parts; i++) segments.add(results[i]);

        while (segments.size() > 1) {
            List<byte[]> next = new ArrayList<byte[]>();
            List<Thread> mergeThreads = new ArrayList<Thread>();
            final List<byte[]> mergedResults = new ArrayList<byte[]>();

            for (int i = 0; i < segments.size(); i += 2) {
                if (i + 1 < segments.size()) {
                    final byte[] A = segments.get(i);
                    final byte[] B = segments.get(i + 1);
                    mergedResults.add(null);
                    final int pos = mergedResults.size() - 1;

                    Thread mt = new Thread(new Runnable() {
                        public void run() {
                            byte[] r = MergeSort.mergeArrays(A, B);
                            mergedResults.set(pos, r);
                        }
                    });
                    mergeThreads.add(mt);
                    mt.start();
                } else {
                    next.add(segments.get(i));
                }
            }

            // join merges
            for (Thread mt : mergeThreads) mt.join();

            // adicionar resultados mesclados
            for (byte[] m : mergedResults) next.add(m);

            segments = next;
        }

        // resultado final
        byte[] finalResult = segments.get(0);

        // gravar resultado.txt localmente no distribuidor
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (int i = 0; i < finalResult.length; i++) {
                bw.write(Byte.toString(finalResult[i]));
                if (i < finalResult.length - 1) bw.write(' ');
            }
        }

        System.out.println("Distribuidor: arquivo gravado em " + outputFile);

        // enviar resultado final para todos os receptores com sua respectiva porção
        Thread[] sendThreads = new Thread[targetHosts.length];

        for (int i = 0; i < targetHosts.length; i++) {
            final String host = targetHosts[i];
            final byte[] resultPortion = results[i];
            sendThreads[i] = new Thread(() -> {
                try (Socket s = new Socket(host, port);
                     ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {

                    System.out.println("Distribuidor: enviando resultado final para receptor " + host + " (" + resultPortion.length + " bytes)");
                    ComunicadoResultado cr = new ComunicadoResultado(outputFile, resultPortion);
                    oos.writeObject(cr);
                    oos.flush();
                    System.out.println("Distribuidor: resultado enviado ao receptor " + host);

                } catch (Exception e) {
                    System.err.println("Distribuidor: falha ao enviar resultado ao receptor " + host + ": " + e.getMessage());
                }
            });
            sendThreads[i].start();
        }

        // aguardar envios
        for (Thread t : sendThreads) {
            try { t.join(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }

    public static void main(String[] args) throws Exception {
        String outFile = "resultado.txt";
        int tamanho = -1;
        String[] hosts = null;

        if (args.length >= 1) outFile = args[0];
        if (args.length >= 2) {
            try {
                tamanho = Integer.parseInt(args[1]);
                // se houver mais args, são hosts
                if (args.length > 2) {
                    hosts = new String[args.length - 2];
                    System.arraycopy(args, 2, hosts, 0, args.length - 2);
                }
            } catch (NumberFormatException nfe) {
                // args[1] não é número -> é host; usa tamanho por prompt
                hosts = new String[args.length - 1];
                System.arraycopy(args, 1, hosts, 0, args.length - 1);
            }
        }

        if (tamanho <= 0) {
            System.out.print("Informe o tamanho do vetor (numero de bytes): ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            tamanho = Integer.parseInt(br.readLine().trim());
        }

        // se hosts for null, usa IPS hardcoded
        String[] usedHosts = (hosts != null && hosts.length > 0) ? hosts : IPS;

        Distribuidor d = new Distribuidor(usedHosts);
        d.execute(outFile, tamanho);
    }
}