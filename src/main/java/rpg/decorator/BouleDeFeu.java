package rpg.decorator;

import rpg.core.Character;

/**
 * Capacit\u00e9 offensive : Boule de Feu
 * Inflige des d\u00e9g\u00e2ts suppl\u00e9mentaires au premier coup (+15), puis bonus r\u00e9duit pendant 2 tours (+5).
 * 
 * <p><strong>M\u00e9canique:</strong></p>
 * <ul>
 *   <li>Premier coup: +15 d\u00e9g\u00e2ts</li>
 *   <li>2 tours suivants: +5 d\u00e9g\u00e2ts par attaque</li>
 *   <li>Apr\u00e8s: Bonus \u00e9puis\u00e9</li>
 * </ul>
 * 
 * @see AbilityPriority#OFFENSIVE
 */
public class BouleDeFeu extends CharacterDecorator {
    private boolean firstCast = true;
    private int bonusTurns = 2;

    public BouleDeFeu(Character wrapped) { 
        super(wrapped); 
    }

    @Override
    public String getName() {
        return "Boule de Feu";
    }

    @Override
    public String getDescription() {
        return "Inflige +15 d\u00e9g\u00e2ts au premier coup, puis +5 pendant 2 tours";
    }

    @Override
    public AbilityPriority getPriority() {
        return AbilityPriority.OFFENSIVE;
    }

    @Override
    public void onBeginTurn(Character owner) {
        if (!firstCast && bonusTurns > 0) {
            bonusTurns--;
        }
        firstCast = false;
    }

    @Override
    public int modifyOutgoingDamage(Character attacker, Character target, int baseDamage) {
        if (firstCast) {
            return baseDamage + 15;
        }
        if (bonusTurns > 0) {
            return baseDamage + 5;
        }
        return baseDamage;
    }

    @Override
    public boolean isActive() {
        return firstCast || bonusTurns > 0;
    }
}
