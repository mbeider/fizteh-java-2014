package ru.fizteh.fivt.students.deserg.proxy;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by deserg on 20.10.14.
 */
public class DbTableProvider implements TableProvider, AutoCloseable {

    private Map<String, DbTable> tables = new HashMap<>();
    private Map<String, List<Class<?>>> signatures = new HashMap<>();
    private Set<String> removedTables = new HashSet<>();
    private Path dbPath;
    private DbTable currentTable = null;
    private ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private boolean closed = false;


    /**
     * Возвращает таблицу с указанным названием.
     *
     * Последовательные вызовы метода с одинаковыми аргументами должны возвращать один и тот же объект таблицы,
     * если он не был удален с помощью {@link #removeTable(String)}.
     *
     * @param name Название таблицы.
     * @return Объект, представляющий таблицу. Если таблицы с указанным именем не существует, возвращает null.
     *
     * @throws IllegalArgumentException Если название таблицы null или имеет недопустимое значение.
     */
    @Override
    public Table getTable(String name) {

        checkClosed();

        if (name == null) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": getTable: null table name");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": getTable: empty table name");
        }

        try {
            String testName = Paths.get("").resolve(name).getFileName().toString();

            if (!testName.equals(name)) {
                throw new IllegalArgumentException("Database \"" + name + "\": getTable: unacceptable table name");
            }

        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Database \"" + name + "\": getTable: unacceptable table name");
        }


        try {
            lock.writeLock().lock();
            if (removedTables.contains(name)) {
                throw new IllegalStateException("Database \"" + name + "\": getTable: table was removed");
            } else {
                DbTable table = tables.get(name);
                if (table != null && table.isClosed()) {
                    table = new DbTable(dbPath.resolve(name), signatures.get(name));
                }
                return table;
            }

        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * Создаёт таблицу с указанным названием.
     * Создает новую таблицу. Совершает необходимые дисковые операции.
     *
     * @param name Название таблицы.
     * @param columnTypes Типы колонок таблицы. Не может быть пустой.
     * @return Объект, представляющий таблицу. Если таблица с указанным именем существует, возвращает null.
     *
     * @throws IllegalArgumentException Если название таблицы null или имеет недопустимое значение. Если список типов
     *                                  колонок null или содержит недопустимые значения.
     * @throws java.io.IOException При ошибках ввода/вывода.
     */
    @Override
    public Table createTable(String name, List<Class<?>> columnTypes) throws IOException {

        checkClosed();

        if (name == null) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": createTable: null table name");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": createTable: empty table name");
        }

        if (!checkSignature(columnTypes)) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": createTable: invalid signature");
        }

        try {
            String testName = Paths.get("").resolve(name).getFileName().toString();

            if (!testName.equals(name)) {
                throw new IllegalArgumentException("Database \"" + name + "\": createTable: unacceptable table name");
            }

        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Database \"" + name + "\": createTable: unacceptable table name");
        }



        try {
            lock.writeLock().lock();
            if (tables.containsKey(name)) {
                return null;
            } else {

                DbTable table = new DbTable(dbPath.resolve(name), columnTypes);
                tables.put(name, table);
                signatures.put(name, columnTypes);
                removedTables.remove(name);
                return table;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * Удаляет существующую таблицу с указанным названием.
     *
     * Объект удаленной таблицы, если был кем-то взят с помощью {@link #getTable(String)},
     * с этого момента должен бросать {@link IllegalStateException}.
     *
     * @param name Название таблицы.
     *
     * @throws IllegalArgumentException Если название таблицы null или имеет недопустимое значение.
     * @throws IllegalStateException Если таблицы с указанным названием не существует.
     * @throws java.io.IOException - при ошибках ввода/вывода.
     */
    @Override
    public void removeTable(String name) {

        checkClosed();

        if (name == null) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": removeTable: null Table name");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Database \"" + dbPath + "\": removeTable: empty Table name");
        }


        try {
            String testName = Paths.get("").resolve(name).getFileName().toString();

            if (!testName.equals(name)) {
                throw new IllegalArgumentException("Database \"" + name + "\": removeTable: unacceptable table name");
            }

        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Database \"" + name + "\": removeTable: unacceptable table name");
        }


        try {

            lock.writeLock().lock();
            if (!tables.containsKey(name)) {
                throw new IllegalStateException("Database \"" + dbPath + "\": removeTable: table does not exist");
            }

            removedTables.add(name);
            if (currentTable != null && currentTable.getName().equals(name)) {
                currentTable = null;
            }

            tables.remove(name);
            signatures.remove(name);

        } finally {
            lock.writeLock().unlock();
        }

    }




    /**
     * Преобразовывает строку в объект {@link ru.fizteh.fivt.storage.structured.Storeable}, соответствующий структуре таблицы.
     *
     * @param table Таблица, которой должен принадлежать {@link ru.fizteh.fivt.storage.structured.Storeable}.
     * @param value Строка, из которой нужно прочитать {@link ru.fizteh.fivt.storage.structured.Storeable}.
     * @return Прочитанный {@link ru.fizteh.fivt.storage.structured.Storeable}.
     *
     * @throws java.text.ParseException - при каких-либо несоответстиях в прочитанных данных.
     */
    @Override
    public Storeable deserialize(Table table, String value) throws ParseException {

        checkClosed();

        return Serializer.deserialize((DbTable) table, value);

    }

    /**
     * Преобразовывает объект {@link Storeable} в строку.
     *
     * @param table Таблица, которой должен принадлежать {@link Storeable}.
     * @param value {@link Storeable}, который нужно записать.
     * @return Строка с записанным значением.
     *
     * @throws ru.fizteh.fivt.storage.structured.ColumnFormatException При несоответствии типа в {@link Storeable} и типа колонки в таблице.
     */
    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {

        checkClosed();

        return Serializer.serialize(table, value);

    }

    /**
     * Создает новый пустой {@link Storeable} для указанной таблицы.
     *
     * @param table Таблица, которой должен принадлежать {@link Storeable}.
     * @return Пустой {@link Storeable}, нацеленный на использование с этой таблицей.
     */
    @Override
    public Storeable createFor(Table table) {

        checkClosed();

        DbTable mTable = (DbTable) table;
        return new TableRow(mTable.getSignature());

    }

    /**
     * Создает новый {@link Storeable} для указанной таблицы, подставляя туда переданные значения.
     *
     * @param table Таблица, которой должен принадлежать {@link Storeable}.
     * @param values Список значений, которыми нужно проинициализировать поля Storeable.
     * @return {@link Storeable}, проинициализированный переданными значениями.
     * @throws ColumnFormatException При несоответствии типа переданного значения и колонки.
     * @throws IndexOutOfBoundsException При несоответствии числа переданных значений и числа колонок.
     */
    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {

        checkClosed();

        DbTable mTable = (DbTable) table;
        if (mTable.getColumnsCount() != values.size()) {
            throw new IndexOutOfBoundsException("Database \"" + dbPath + "\": createFor: invalid number of columns");
        }

        Storeable row = new TableRow(mTable.getSignature());
        for (int i = 0; i < values.size(); i++) {
            row.setColumnAt(i, values.get(i));
        }

        return row;
    }

    /**
     * Возвращает имена существующих таблиц, которые могут быть получены с помощью {@link #getTable(String)}.
     *
     * @return Имена существующих таблиц.
     */
    @Override
    public List<String> getTableNames() {

        checkClosed();

        try {
            lock.readLock().lock();
            List<String> list = new LinkedList<>();
            list.addAll(tables.keySet());
            return list;
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void close() {

        checkClosed();
        for (HashMap.Entry<String, DbTable> entry: tables.entrySet()) {

            DbTable table = entry.getValue();
            table.close();
        }
        closed = true;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + dbPath + "]";

    }

    /**
     * Not-interface methods begin here
     */

    //Constructor
    public DbTableProvider(Path inpPath) {

        try {

            lock.writeLock().lock();
            dbPath = inpPath;
            read();

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Database \"" + dbPath.getFileName() + "\" is closed");
        }
    }


    public DbTable getCurrentTable() {
        return currentTable;
    }

    public void setCurrentTable(String name) {


        try {

            lock.writeLock().lock();
            if (!tables.containsKey(name)) {
                System.out.println(name + " not exists");
                return;
            }

            if (currentTable != null && currentTable.getNumberOfUncommittedChanges() > 0) {
                System.out.println(currentTable.getNumberOfUncommittedChanges() + " unsaved changes");
                return;
            }

            currentTable = tables.get(name);
            System.out.println("using " + name);

        } finally {
            lock.writeLock().unlock();
        }
    }


    public void showTableSet() {


        try {

            lock.readLock().lock();
            System.out.println("table_name row_count");
            for (HashMap.Entry<String, DbTable> entry : tables.entrySet()) {
                DbTable table = entry.getValue();
                System.out.println(table.getName() + " " + table.size());
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean checkSignature(List<Class<?>> signature) {

        if (signature == null) {
            return false;
        }

        Set<Class<?>> set = new HashSet<>();
        set.add(Integer.class);
        set.add(Long.class);
        set.add(Byte.class);
        set.add(Float.class);
        set.add(Double.class);
        set.add(Boolean.class);
        set.add(String.class);

        for (Class<?> type: signature) {
            if (!set.contains(type)) {
                return false;
            }
        }

        return true;
    }



    public void read() {


        try {

            lock.writeLock().lock();
            if (dbPath == null) {
                return;
            }

            if (!Files.exists(dbPath)) {

                try {
                    Files.createDirectory(dbPath);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    System.exit(1);
                }

            }

            File curDir = new File(dbPath.toString());
            File[] content = curDir.listFiles();

            if (content != null) {
                for (File item : content) {
                    if (Files.isDirectory(item.toPath())) {

                        Path tablePath = item.toPath();
                        try {
                            List<Class<?>> signature = Shell.readSignature(tablePath);
                            DbTable table = new DbTable(tablePath, signature);
                            table.read();
                            tables.put(item.getName(), table);
                        } catch (MyIOException ex) {
                            System.out.println(ex.getMessage());
                            System.exit(1);
                        }

                    }
                }
            }

            currentTable = null;

        } finally {
            lock.writeLock().unlock();
        }

    }


    public void write() {


        try {
            lock.writeLock().lock();
            Shell.deleteContent(dbPath);

            for (HashMap.Entry<String, DbTable> entry : tables.entrySet()) {

                try {
                    entry.getValue().write();
                } catch (MyIOException ex) {
                    System.out.println(ex.getMessage());
                    System.exit(1);
                }

            }
        } finally {
            lock.writeLock().lock();
        }
    }



}
