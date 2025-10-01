package rpg.decorator;

import rpg.core.Character;

public abstract class CharacterDecorator extends Character {
    protected final Character wrapped;

    public CharacterDecorator(Character wrapped) {
        super(wrapped.getName(), wrapped.getStrength(), wrapped.getAgility(), wrapped.getIntelligence());
        this.wrapped = wrapped;
    }

    @Override public int getPowerLevel() { return wrapped.getPowerLevel(); }
    @Override public String getDescription() { return wrapped.getDescription(); }
    public Character getWrappedCharacter() { return wrapped; }

    @Override public void beginTurn() { wrapped.beginTurn(); }
    @Override public int healThisTurn() { return wrapped.healThisTurn(); }
    @Override public int attackDamage() { return wrapped.attackDamage(); }
    @Override public int onReceiveDamage(int dmg) { return wrapped.onReceiveDamage(dmg); }
}
