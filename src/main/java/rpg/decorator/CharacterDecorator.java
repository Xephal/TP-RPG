package rpg.decorator;

import rpg.core.Character;

public abstract class CharacterDecorator extends Character {
    protected final Character wrapped;

    public CharacterDecorator(Character wrapped) {
        super(wrapped.getName(), wrapped.getStrength(), wrapped.getAgility(), wrapped.getIntelligence());
        this.wrapped = wrapped;
    }

    @Override
    public int getPowerLevel() {
        return wrapped.getPowerLevel();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }
}
