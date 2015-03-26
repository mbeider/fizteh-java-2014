package ru.fizteh.fivt.students.hromov_igor.multifilemap.base;

import ru.fizteh.fivt.storage.strings.Table;
import ru.fizteh.fivt.students.hromov_igor.multifilemap.exception.DirCreateException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class DBaseTable implements Table {

    static final int SIZE = 16;

    static private String dirExpansion = ".dir";
    static private String fileExpansion = ".dat";
    public String tableName;
    public Path path;
    public DataBase[][] tableDateBase;
    public Map<String, String> keys;
    public Map<String, String> puted;
    public Set<String> removed;

    public DBaseTable() {
        keys = new HashMap<>();
        puted = new HashMap<>();
        removed = new HashSet<>();
        tableDateBase = new DataBase[SIZE][SIZE];
    }

    public DBaseTable(String name, Path pathTable) {
        keys = new HashMap<>();
        puted = new HashMap<>();
        removed = new HashSet<>();
        tableName = name;
        path = pathTable.resolve(Paths.get(name));
        tableDateBase = new DataBase[SIZE][SIZE];
    }

    public DBaseTable(DBaseTable DBt) {
        keys = DBt.keys;
        puted = DBt.puted;
        removed = DBt.removed;
        tableName = DBt.tableName;
        path = DBt.path;
        tableDateBase = DBt.tableDateBase;
    }

    @Override
    public String get(String key) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("No key, null");
        }

        if (keys.containsKey(key)) {
            return (keys.get(key));
        }
        return null;
     }

    @Override
    public int size() {
        return keys.size();
     }

    @Override
    public String put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("No key, null");
        }
        puted.put(key, value);
        return keys.get(key);
    }

    @Override
    public String getName() {
        return tableName;
     }

    @Override
    public String remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        if (!keys.containsKey(key)) {
            return null;
        }
        removed.add(key);
        return keys.get(key);
    }

    @Override
    public int commit() {
        if (puted.size() == 0 && removed.size() == 0) {
            return 0;
         }
        byte b;
        int nDirectory;
        int nFile;
        for (Entry<String, String> pair : puted.entrySet()) {
            b = pair.getKey().getBytes()[0];
            nDirectory = b % SIZE;
            nFile = b / SIZE % SIZE;

            if (tableDateBase[nDirectory][nFile] == null) {
                String s;
                s = String.valueOf(nDirectory);
                s = s.concat(dirExpansion);
                Path pathDir = path;
                pathDir = pathDir.resolve(s);
                try {
                    if (!pathDir.toFile().exists()) {
                        try {
                            pathDir.toFile().mkdir();
                        } catch (Exception e) {
                            throw new DirCreateException(e);
                        }
                    }
                } catch (Exception e) {
                    throw new DirCreateException(e);
                }

                s = String.valueOf(nFile);
                s = s.concat(fileExpansion);
                Path pathFile = pathDir.resolve(s);
                try {
                    if (!pathFile.toFile().exists()) {
                        try {
                            pathFile.toFile().createNewFile();
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                    }
                    tableDateBase[nDirectory][nFile] =
                            new DataBase(pathFile.toString());
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
            try {
                tableDateBase[nDirectory][nFile].
                        put(pair.getKey(), pair.getValue());
            } catch (Exception e) {
                System.err.println("Table error");
            }
            if (keys.containsKey(pair.getKey())) {
                keys.remove(pair.getKey());
            }
            keys.put(pair.getKey(), pair.getValue());
        }
        int size = puted.size();
        puted.clear();
        for (String key : removed) {
            b = key.getBytes()[0];
            nDirectory = b % SIZE;
            nFile = b / SIZE % SIZE;
            try {
                tableDateBase[nDirectory][nFile].remove(key);
            } catch (Exception e) {
                System.err.println("Table error");
            }
            keys.remove(key);
        }
        removed.clear();
        try {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (tableDateBase[i][j] != null) {
                    tableDateBase[i][j].close();
                }
            }
        }
        } catch (Exception e) {
            System.err.println("Table error");
        }
        return size;
    }

    @Override
    public int rollback() {
        int size = puted.size();
        removed.clear();
        puted.clear();
        return size;
     }

    @Override
    public List<String> list() {
        List<String> list = new ArrayList<>();
        for (String key : keys.keySet()) {
            list.add(key);
        }
        return list;
     }

}
