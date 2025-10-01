package rpg.decorator;

import rpg.core.Character;

public class Surcharge extends CharacterDecorator {
    private int state = 1; // 1=charge, 2=boost, 0=off

    public Surcharge(Character wrapped) { super(wrapped); }

    @Override public void beginTurn() {
        super.beginTurn();
        if (state == 1) state = 2; // on a patienté
    }

    @Override public int attackDamage() {
        if (state == 1) return 0; // passe un tour
        int base = super.attackDamage();
        if (state == 2) {
            state = 0; // consommé
            return (int)Math.round(base * 1.5);
        }
        return base;
    }

    @Override public String getDescription() { return super.getDescription() + " + Surcharge"; }
}
