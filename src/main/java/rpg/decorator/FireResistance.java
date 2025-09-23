package rpg.decorator;

import rpg.core.Character;

public class FireResistance extends CharacterDecorator {
    public FireResistance(Character wrapped) {
        super(wrapped);
    }

    @Override
    public int getPowerLevel() {
        return super.getPowerLevel() + 3;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + FireResistance";
    }
}
