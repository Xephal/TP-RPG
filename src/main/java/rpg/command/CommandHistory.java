package rpg.command;

import java.util.ArrayList;
import java.util.List;

public class CommandHistory {
    private final List<Command> executedCommands = new ArrayList<>();

    public void execute(Command command) {
        command.execute();
        executedCommands.add(command);
    }

    public void replay() {
        for (Command command : executedCommands) {
            command.execute();
        }
    }

    public List<String> getHistory() {
        List<String> history = new ArrayList<>();
        for (Command command : executedCommands) {
            history.add(command.getDescription());
        }
        return history;
    }

    public void clear() {
        executedCommands.clear();
    }
}