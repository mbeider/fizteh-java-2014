package ru.fizteh.fivt.students.ZatsepinMikhail.Parallel.MultiFileHashMap;

import ru.fizteh.fivt.students.ZatsepinMikhail.Parallel.FileMap.FileMap;
import ru.fizteh.fivt.students.ZatsepinMikhail.Parallel.FileMap.FmCommandGet;

public class CommandGetDistribute extends CommandMultiFileHashMap {
    public CommandGetDistribute() {
        name = "get";
        numberOfArguments = 2;
    }

    @Override
    public boolean run(MFileHashMap myMap, String[] args) {
        FileMap currentTable = myMap.getCurrentTable();
        if (myMap.getCurrentTable() == null) {
            System.out.println("no table");
            return true;
        }
        FmCommandGet commandGet = new FmCommandGet();
        return commandGet.run(currentTable, args);
    }
}
