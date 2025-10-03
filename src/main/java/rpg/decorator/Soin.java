package rpg.decorator;

import rpg.core.Character;

/**
 * Capacit\u00e9 de support : Soin
 * Restaure 30 PV une seule fois. Le personnage ne peut pas attaquer le tour du soin.
 * 
 * <p><strong>M\u00e9canique:</strong></p>
 * <ul>
 *   <li>Activation: Restaure 30 PV, pas d'attaque possible</li>
 *   <li>Apr\u00e8s: Capacit\u00e9 consomm\u00e9e</li>
 * </ul>
 * 
 * @see AbilityPriority#SUPPORTIVE
 */
public class Soin extends CharacterDecorator {
    private boolean active = true;

    public Soin(Character wrapped) { 
        super(wrapped); 
    }

    @Override
    public String getName() {
        return "Soin";
    }

    @Override
    public String getDescription() {
        return "Restaure 30 PV (une seule utilisation)";
    }

    @Override
    public AbilityPriority getPriority() {
        return AbilityPriority.SUPPORTIVE;
    }

    @Override
    public int getHealAmount(Character owner) {
        if (active) {
            active = false;
            return 30;
        }
        return 0;
    }

    @Override
    public int modifyOutgoingDamage(Character attacker, Character target, int baseDamage) {
        // Pas d'attaque le tour du soin
        return active ? 0 : baseDamage;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
