package rpg.command;

import rpg.core.Character;

public class DefendCommand implements Command {
    private final Character defender;
    private int defenseBonus;

    public DefendCommand(Character defender) {
        this.defender = defender;
    }

    @Override
    public void execute() {
        defenseBonus = defender.getAgility() / 3 + 2;
    }

    @Override
    public void undo() {
    }

    @Override
    public String getDescription() {
        return defender.getName() + " defends (+" + defenseBonus + " defense)";
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }
}