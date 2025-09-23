package rpg.decorator;

import rpg.core.Character;

public class Telepathy extends CharacterDecorator {
    public Telepathy(Character wrapped) {
        super(wrapped);
    }

    @Override
    public int getPowerLevel() {
        return super.getPowerLevel() + 4;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + Telepathy";
    }
}
