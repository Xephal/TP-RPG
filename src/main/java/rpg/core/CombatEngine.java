package rpg.core;

import rpg.command.AttackCommand;
import rpg.command.CommandHistory;
import rpg.observer.EventBus;

public class CombatEngine {
    private final EventBus eventBus;
    private final CommandHistory commandHistory;

    public CombatEngine(EventBus eventBus) {
        this.eventBus = eventBus;
        this.commandHistory = new CommandHistory();
    }

    public Character simulate(Character a, Character b) {
        int hpA = Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2);
        int hpB = Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2);

        eventBus.notifyObservers("COMBAT_START", a.getName() + " vs " + b.getName());

        int turn = 0;
        while (hpA > 0 && hpB > 0 && turn < 1000) {
            if (turn % 2 == 0) {
                AttackCommand cmd = new AttackCommand(a, b);
                commandHistory.execute(cmd);
                hpB -= cmd.getDamageDealt();
                eventBus.notifyObservers("COMBAT_ACTION", cmd.getDescription() + " (hpB=" + Math.max(0, hpB) + ")");
            } else {
                AttackCommand cmd = new AttackCommand(b, a);
                commandHistory.execute(cmd);
                hpA -= cmd.getDamageDealt();
                eventBus.notifyObservers("COMBAT_ACTION", cmd.getDescription() + " (hpA=" + Math.max(0, hpA) + ")");
            }
            turn++;
        }

        Character winner = hpA > hpB ? a : b;
        eventBus.notifyObservers("COMBAT_END", "Winner: " + winner.getName());
        return winner;
    }

    public CommandHistory getCommandHistory() {
        return commandHistory;
    }
}