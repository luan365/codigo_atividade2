import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Pedido extends Comunicado {
    private static final long serialVersionUID = 2L;

    private byte[] numeros;

    public Pedido(byte[] numeros) {
        this.numeros = numeros;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    /**
     * Ordena o vetor usando Merge Sort com paralelismo interno:
     * - Divide em N partes (N ≤ número de processadores)
     * - Cria threads ordenadoras (cada thread ordena sua parte)
     * - Faz merge 2-a-2 com threads até resultar em 1 vetor
     */
    public void ordenar() throws InterruptedException {
        if (numeros == null || numeros.length < 2)
            return;

        int cpus = Runtime.getRuntime().availableProcessors();
        int numParts = Math.min(cpus, numeros.length);

        // Passo 1: Dividir em segmentos
        List<byte[]> segments = new ArrayList<>();
        int baseSize = numeros.length / numParts;
        int remainder = numeros.length % numParts;
        int offset = 0;

        for (int i = 0; i < numParts; i++) {
            int size = baseSize + (i < remainder ? 1 : 0);
            byte[] segment = new byte[size];
            System.arraycopy(numeros, offset, segment, 0, size);
            offset += size;
            segments.add(segment);
        }

        // Passo 2: Threads ordenadoras (cada thread ordena seu segmento)
        List<Thread> sortThreads = new ArrayList<>();
        for (byte[] segment : segments) {
            Thread t = new Thread(() -> {
                mergeSortSegment(segment, 0, segment.length - 1);
            });
            sortThreads.add(t);
            t.start();
        }

        // Aguardar término de todas as threads ordenadoras
        for (Thread t : sortThreads) {
            t.join();
        }

        // Passo 3: Merges 2-a-2 com threads até resultar em 1 array
        while (segments.size() > 1) {
            List<byte[]> nextRound = new ArrayList<>();
            List<Thread> mergeThreads = new ArrayList<>();
            List<byte[]> mergeResults = new ArrayList<>();

            for (int i = 0; i < segments.size(); i += 2) {
                if (i + 1 < segments.size()) {
                    final byte[] a = segments.get(i);
                    final byte[] b = segments.get(i + 1);
                    mergeResults.add(null); // placeholder
                    final int resultIdx = mergeResults.size() - 1;

                    Thread mergeThread = new Thread(() -> {
                        byte[] merged = mergeArrays(a, b);
                        mergeResults.set(resultIdx, merged);
                    });
                    mergeThreads.add(mergeThread);
                    mergeThread.start();
                } else {
                    // Array ímpar passa direto
                    nextRound.add(segments.get(i));
                }
            }

            // Aguardar todos os merges
            for (Thread t : mergeThreads) {
                t.join();
            }

            // Adicionar resultados mesclados
            for (byte[] merged : mergeResults) {
                nextRound.add(merged);
            }

            segments = nextRound;
        }

        // Resultado final
        this.numeros = segments.get(0);
    }

    // Merge Sort recursivo para um segmento
    private void mergeSortSegment(byte[] arr, int left, int right) {
        if (left >= right)
            return;

        int mid = (left + right) / 2;
        mergeSortSegment(arr, left, mid);
        mergeSortSegment(arr, mid + 1, right);
        mergeSegment(arr, left, mid, right);
    }

    // Intercala dois segmentos ordenados
    private void mergeSegment(byte[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        byte[] leftArr = new byte[n1];
        byte[] rightArr = new byte[n2];

        System.arraycopy(arr, left, leftArr, 0, n1);
        System.arraycopy(arr, mid + 1, rightArr, 0, n2);

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (leftArr[i] <= rightArr[j]) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }

        while (i < n1) {
            arr[k++] = leftArr[i++];
        }

        while (j < n2) {
            arr[k++] = rightArr[j++];
        }
    }

    // Merge de dois arrays distintos
    private byte[] mergeArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        int i = 0, j = 0, k = 0;

        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) {
                result[k++] = a[i++];
            } else {
                result[k++] = b[j++];
            }
        }

        while (i < a.length) {
            result[k++] = a[i++];
        }

        while (j < b.length) {
            result[k++] = b[j++];
        }

        return result;
    }
}
