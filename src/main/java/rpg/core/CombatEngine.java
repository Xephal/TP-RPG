package rpg.core;

import rpg.command.AttackCommand;
import rpg.command.CommandHistory;
import rpg.history.BattleAction;
import rpg.history.BattleHistory;
import rpg.observer.EventBus;

public class CombatEngine {
    private final EventBus eventBus;
    private final CommandHistory commandHistory;
    private BattleHistory currentBattle;

    public CombatEngine(EventBus eventBus) {
        this.eventBus = eventBus;
        this.commandHistory = new CommandHistory();
    }

    public Character simulate(Character a, Character b) {
        int hpA = Math.max(10, a.getStrength() * 10 + a.getIntelligence() * 2);
        int hpB = Math.max(10, b.getStrength() * 10 + b.getIntelligence() * 2);

        eventBus.notifyObservers("COMBAT_START", a.getName() + " vs " + b.getName());

        int turn = 0;
        int actionNumber = 1;
        while (hpA > 0 && hpB > 0 && turn < 1000) {
            if (turn % 2 == 0) {
                AttackCommand cmd = new AttackCommand(a, b);
                commandHistory.execute(cmd);
                hpB -= cmd.getDamageDealt();
                String actionDesc = cmd.getDescription() + " (hpB=" + Math.max(0, hpB) + ")";
                eventBus.notifyObservers("COMBAT_ACTION", actionDesc);
                
                // Record action in battle history if available
                if (currentBattle != null) {
                    BattleAction battleAction = new BattleAction(
                        actionNumber++,
                        a,
                        b,
                        "attacks",
                        actionDesc,
                        cmd.getDamageDealt()
                    );
                    currentBattle.addAction(battleAction);
                }
            } else {
                AttackCommand cmd = new AttackCommand(b, a);
                commandHistory.execute(cmd);
                hpA -= cmd.getDamageDealt();
                String actionDesc = cmd.getDescription() + " (hpA=" + Math.max(0, hpA) + ")";
                eventBus.notifyObservers("COMBAT_ACTION", actionDesc);
                
                // Record action in battle history if available
                if (currentBattle != null) {
                    BattleAction battleAction = new BattleAction(
                        actionNumber++,
                        b,
                        a,
                        "attacks",
                        actionDesc,
                        cmd.getDamageDealt()
                    );
                    currentBattle.addAction(battleAction);
                }
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
    
    public void setCurrentBattle(BattleHistory battle) {
        this.currentBattle = battle;
    }
    
    public BattleHistory getCurrentBattle() {
        return currentBattle;
    }
}