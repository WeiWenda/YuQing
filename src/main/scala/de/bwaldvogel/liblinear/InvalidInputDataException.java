//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.bwaldvogel.liblinear;

import java.io.File;

public class InvalidInputDataException extends Exception {
    private static final long serialVersionUID = 2945131732407207308L;
    private final int _line;
    private File _file;

    public InvalidInputDataException(String message, File file, int line) {
        super(message);
        this._file = file;
        this._line = line;
    }

    public InvalidInputDataException(String message, String filename, int line) {
        this(message, new File(filename), line);
    }

    public InvalidInputDataException(String message, File file, int lineNr, Exception cause) {
        super(message, cause);
        this._file = file;
        this._line = lineNr;
    }

    public InvalidInputDataException(String message, String filename, int lineNr, Exception cause) {
        this(message, new File(filename), lineNr, cause);
    }

    public File getFile() {
        return this._file;
    }

    /** @deprecated */
    public String getFilename() {
        return this._file.getPath();
    }

    public int getLine() {
        return this._line;
    }

    public String toString() {
        return super.toString() + " (" + this._file + ":" + this._line + ")";
    }
}
