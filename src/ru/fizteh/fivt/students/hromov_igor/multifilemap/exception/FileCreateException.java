package ru.fizteh.fivt.students.hromov_igor.multifilemap.exception;

public class FileCreateException extends IllegalArgumentException {
    public FileCreateException() {
        super("Can't create file");
    }
}
