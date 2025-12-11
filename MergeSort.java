public class MergeSort {
    /**
     * Merge two sorted byte arrays into a single sorted array.
     */
    public static byte[] mergeArrays(byte[] a, byte[] b) {
        if (a == null) a = new byte[0];
        if (b == null) b = new byte[0];

        byte[] result = new byte[a.length + b.length];
        int i = 0, j = 0, k = 0;

        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) {
                result[k++] = a[i++];
            } else {
                result[k++] = b[j++];
            }
        }

        while (i < a.length) result[k++] = a[i++];
        while (j < b.length) result[k++] = b[j++];

        return result;
    }
}
