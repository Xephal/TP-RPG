package rpg.decorator;

import rpg.core.Character;

public class BouleDeFeu extends CharacterDecorator {
    private boolean firstCast = true;
    private int bonusTurns = 2;

    public BouleDeFeu(Character wrapped) { super(wrapped); }

    @Override public void beginTurn() {
        super.beginTurn();
        if (!firstCast && bonusTurns > 0) bonusTurns--;
        firstCast = false;
    }

    @Override public int attackDamage() {
        int dmg = super.attackDamage();
        if (firstCast) return dmg + 15;
        if (bonusTurns > 0) return dmg + 5;
        return dmg;
    }

    @Override public String getDescription() { return super.getDescription() + " + Boule de Feu"; }
}
