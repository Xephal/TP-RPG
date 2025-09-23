package rpg.decorator;

import rpg.core.Character;

public class Invisibility extends CharacterDecorator {
    public Invisibility(Character wrapped) {
        super(wrapped);
    }

    @Override
    public int getPowerLevel() {
        return super.getPowerLevel() + 5;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + Invisibility";
    }
}
