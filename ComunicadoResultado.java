public class ComunicadoResultado extends Comunicado {
    private static final long serialVersionUID = 5L;

    private final String filename;
    private final byte[] data;

    public ComunicadoResultado(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }
}
