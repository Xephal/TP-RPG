package rpg.command;

import rpg.core.Character;

public class UsePowerCommand implements Command {
    private final Character caster;
    private final String powerName;
    private int effectValue;

    public UsePowerCommand(Character caster, String powerName) {
        this.caster = caster;
        this.powerName = powerName;
    }

    @Override
    public void execute() {
        effectValue = caster.getIntelligence() + (int)(Math.random() * 5);
    }

    @Override
    public void undo() {
    }

    @Override
    public String getDescription() {
        return caster.getName() + " uses " + powerName + " (effect: " + effectValue + ")";
    }

    public int getEffectValue() {
        return effectValue;
    }
}