import java.io.*;
import java.util.*;

public class OrdenacaoSequencial {

    public static void main(String[] args) throws Exception {
        int n = 1_000_000;
        if (args.length >= 1) {
            n = Integer.parseInt(args[0]);
        }

        System.out.println("[Sequencial] Gerando vetor com " + n + " bytes...");
        byte[] arr = new byte[n];
        new Random().nextBytes(arr);

        long tempoInicio = System.nanoTime();
        mergeSortSequencial(arr, 0, arr.length - 1);
        long tempoFim = System.nanoTime();

        double tempoS = (tempoFim - tempoInicio) / 1e9;

        // Gravar arquivo
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("resultado_sequencial.txt"))) {
            for (int i = 0; i < arr.length; i++) {
                bw.write(Byte.toString(arr[i]));
                if (i < arr.length - 1) {
                    bw.write(' ');
                }
            }
        }

        System.out.println("[Sequencial] Tempo: " + String.format("%.3f", tempoS) + "s");
        System.out.println("[Sequencial] Arquivo gravado em resultado_sequencial.txt");
    }

    private static void mergeSortSequencial(byte[] arr, int left, int right) {
        if (left >= right)
            return;

        int mid = (left + right) / 2;
        mergeSortSequencial(arr, left, mid);
        mergeSortSequencial(arr, mid + 1, right);
        mergeSequencial(arr, left, mid, right);
    }

    private static void mergeSequencial(byte[] arr, int left, int mid, int right) {
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
}
