package rpg.decorator;

import rpg.core.Character;

public class Soin extends CharacterDecorator {
    private boolean active = true;

    public Soin(Character wrapped) { super(wrapped); }

    @Override public void beginTurn() {
        super.beginTurn();
        // rien ici, le soin sera lu via healThisTurn()
    }

    @Override public int healThisTurn() {
        if (active) {
            active = false;
            return 30;
        }
        return super.healThisTurn();
    }

    @Override public int attackDamage() {
        return active ? 0 : super.attackDamage();
    }

    @Override public String getDescription() { return super.getDescription() + " + Soin"; }
}
