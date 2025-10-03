package rpg.decorator;

import java.util.Random;

import rpg.core.Character;

/**
 * Capacit\u00e9 d\u00e9fensive : Furtivit\u00e9
 * Permet d'esquiver compl\u00e8tement les attaques ennemies (50% de chance) pendant 3 tours.
 * Le personnage ne peut pas attaquer le premier tour (activation).
 * 
 * <p><strong>M\u00e9canique:</strong></p>
 * <ul>
 *   <li>Activation: Le personnage ne peut pas attaquer</li>
 *   <li>3 tours suivants: 50% de chance d'esquiver une attaque</li>
 *   <li>Apr\u00e8s: Capacit\u00e9 \u00e9puis\u00e9e</li>
 * </ul>
 * 
 * @see AbilityPriority#DEFENSIVE
 */
public class Furtivite extends CharacterDecorator {
    private boolean justActivated = true;
    private int evadeTurns = 3;
    private final Random rng = new Random();

    public Furtivite(Character wrapped) { 
        super(wrapped); 
    }

    @Override
    public String getName() {
        return "Furtivit\u00e9";
    }

    @Override
    public String getDescription() {
        return "50% de chance d'esquiver les attaques pendant 3 tours";
    }

    @Override
    public AbilityPriority getPriority() {
        return AbilityPriority.DEFENSIVE;
    }

    @Override
    public void onBeginTurn(Character owner) {
        if (!justActivated && evadeTurns > 0) {
            evadeTurns--;
        }
        justActivated = false;
    }

    @Override
    public int modifyOutgoingDamage(Character attacker, Character target, int baseDamage) {
        // Pas d'attaque le tour d'activation
        return justActivated ? 0 : baseDamage;
    }

    @Override
    public boolean onBeforeReceiveDamage(Character defender, Character attacker, int incomingDamage) {
        // Tenter une esquive si la capacit\u00e9 est active
        if (evadeTurns > 0 && rng.nextBoolean()) {
            return true; // Esquive r\u00e9ussie
        }
        return false; // Pas d'esquive
    }

    @Override
    public boolean isActive() {
        return justActivated || evadeTurns > 0;
    }
}
