package rpg.command;

import rpg.core.Character;

public class AttackCommand implements Command {
    private final Character attacker;
    private final Character target;
    private int damageDealt;

    public AttackCommand(Character attacker, Character target) {
        this.attacker = attacker;
        this.target = target;
    }

    @Override
    public void execute() {
        int attackPower = attacker.getStrength() + (int)(Math.random() * 10);
        damageDealt = Math.max(1, attackPower - target.getAgility() / 2);
    }

    @Override
    public void undo() {
    }

    @Override
    public String getDescription() {
        return attacker.getName() + " attacks " + target.getName() + " for " + damageDealt + " damage";
    }

    public int getDamageDealt() {
        return damageDealt;
    }
}