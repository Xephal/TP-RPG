package rpg.decorator;

import rpg.core.Character;

/**
 * Capacit\u00e9 offensive : Surcharge
 * Augmente les d\u00e9g\u00e2ts de 50% pour une attaque apr\u00e8s un tour de pr\u00e9paration.
 * 
 * <p><strong>M\u00e9canique:</strong></p>
 * <ul>
 *   <li>Tour 1: Pr\u00e9paration (state=1, pas d'attaque)</li>
 *   <li>Tour 2: Surcharge active (state=2, +50% d\u00e9g\u00e2ts)</li>
 *   <li>Apr\u00e8s utilisation: Consomm\u00e9 (state=0)</li>
 * </ul>
 * 
 * @see AbilityPriority#OFFENSIVE
 */
public class Surcharge extends CharacterDecorator {
    private int state = 1; // 1=charge, 2=boost, 0=off

    public Surcharge(Character wrapped) { 
        super(wrapped); 
    }

    @Override
    public String getName() {
        return "Surcharge";
    }

    @Override
    public String getDescription() {
        return "Augmente les d\u00e9g\u00e2ts de 50% apr\u00e8s un tour de pr\u00e9paration";
    }

    @Override
    public AbilityPriority getPriority() {
        return AbilityPriority.OFFENSIVE;
    }

    @Override
    public void onBeginTurn(Character owner) {
        if (state == 1) {
            state = 2; // Pr\u00e9paration termin\u00e9e, prêt pour le boost
        }
    }

    @Override
    public int modifyOutgoingDamage(Character attacker, Character target, int baseDamage) {
        if (state == 1) {
            return 0; // Pas d'attaque pendant la pr\u00e9paration
        }
        if (state == 2) {
            state = 0; // Consommer le boost
            return (int) Math.round(baseDamage * 1.5);
        }
        return baseDamage;
    }

    @Override
    public boolean isActive() {
        return state > 0;
    }
}
