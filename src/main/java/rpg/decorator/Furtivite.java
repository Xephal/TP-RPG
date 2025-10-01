package rpg.decorator;

import rpg.core.Character;
import java.util.Random;

public class Furtivite extends CharacterDecorator {
    private boolean justActivated = true;
    private int evadeTurns = 3;
    private final Random rng = new Random();

    public Furtivite(Character wrapped) { super(wrapped); }

    @Override public void beginTurn() {
        super.beginTurn();
        if (!justActivated && evadeTurns > 0) evadeTurns--;
        justActivated = false;
    }

    @Override public int attackDamage() {
        return justActivated ? 0 : super.attackDamage();
    }

    @Override public int onReceiveDamage(int dmg) {
        if (evadeTurns > 0 && rng.nextBoolean()) {
            // esquive totale
            return 0;
        }
        return super.onReceiveDamage(dmg);
    }

    @Override public String getDescription() { return super.getDescription() + " + Furtivité"; }
}
