package ru.fizteh.fivt.students.SurkovaEkaterina.Proxy;

import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.FileMap.FileMapCommands.*;
import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.DatabaseCommands.*;
import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.Shell.Command;
import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.Shell.Shell;
import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.TableSystem.DatabaseTableOperations;
import ru.fizteh.fivt.students.SurkovaEkaterina.Proxy.TableSystem.DatabaseTableProviderFactory;

import java.io.IOException;
import java.util.ArrayList;

public class ProxyMain {
    public static void main(final String[] args) {
        Shell<DatabaseTableOperations> shell =
                new Shell<DatabaseTableOperations>();

        ArrayList<Command<?>> commands = new ArrayList<Command<?>>();

        commands.add(new CommandPut());
        commands.add(new CommandGet());
        commands.add(new CommandRemove());
        commands.add(new CommandExit());
        commands.add(new CommandCreate());
        commands.add(new CommandDrop());
        commands.add(new CommandUse());
        commands.add(new CommandShowTables());
        commands.add(new CommandSize());
        commands.add(new CommandCommit());
        commands.add(new CommandRollback());
        commands.add(new CommandList());

        shell.setShellCommands(commands);
        shell.setArguments(args);

        try {
            String databaseDirectory =
                    System.getProperty("fizteh.db.dir");

            DatabaseTableProviderFactory factory =
                    new DatabaseTableProviderFactory();

            DatabaseTableOperations operations =
                    new DatabaseTableOperations(factory.create(databaseDirectory));

            shell.setShellOperations(operations);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        shell.beginExecuting();
    }
}
