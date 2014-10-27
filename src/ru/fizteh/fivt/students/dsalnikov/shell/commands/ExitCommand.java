package ru.fizteh.fivt.students.dsalnikov.shell.commands;

import ru.fizteh.fivt.students.dsalnikov.shell.Shell;

public class ExitCommand implements Command {

    private Shell link;

    public ExitCommand(Shell s) {
        link = s;
    }

    public String getName() {
        return "exit";
    }

    public int getArgsCount() {
        return 0;
    }

    public void execute(String[] st) {
            System.exit(0);
    }
}
