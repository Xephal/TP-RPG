package rpg.decorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import rpg.core.Character;

/**
 * Classe abstraite de base pour tous les d\u00e9corateurs de personnage.
 * Impl\u00e9mente le pattern Decorator et l'interface Ability pour un syst\u00e8me extensible.
 * 
 * <p>Chaque d\u00e9corateur concret doit:</p>
 * <ul>
 *   <li>Impl\u00e9menter les hooks Ability qu'il souhaite utiliser</li>
 *   <li>D\u00e9finir sa priorit\u00e9 via {@link #getPriority()}</li>
 *   <li>Fournir un nom et une description via {@link #getName()} et {@link #getDescription()}</li>
 * </ul>
 * 
 * @see Ability
 * @see AbilityPriority
 */
public abstract class CharacterDecorator extends Character implements Ability {
    protected final Character wrapped;

    public CharacterDecorator(Character wrapped) {
        super(wrapped.getName(), wrapped.getStrength(), wrapped.getAgility(), wrapped.getIntelligence());
        this.wrapped = wrapped;
    }

    @Override 
    public int getPowerLevel() { 
        return wrapped.getPowerLevel(); 
    }
    
    /**
     * Retourne une description compl\u00e8te du personnage avec toutes ses capacit\u00e9s.
     * Compose automatiquement les descriptions de toutes les abilities.
     */
    @Override 
    public String getDescription() { 
        StringBuilder sb = new StringBuilder(wrapped.getDescription());
        List<Ability> abilities = getAbilities();
        if (!abilities.isEmpty()) {
            sb.append(" [");
            sb.append(abilities.stream()
                .map(Ability::getName)
                .collect(Collectors.joining(", ")));
            sb.append("]");
        }
        return sb.toString();
    }
    
    public Character getWrappedCharacter() { 
        return wrapped; 
    }

    /**
     * Retourne la liste de toutes les capacit\u00e9s du personnage en parcourant la cha\u00eene.
     * Les capacit\u00e9s sont tri\u00e9es par priorit\u00e9 d\u00e9croissante (DEFENSIVE -> SUPPORTIVE -> OFFENSIVE).
     * 
     * @return liste immuable des capacit\u00e9s, tri\u00e9e par priorit\u00e9
     */
    public List<Ability> getAbilities() {
        List<Ability> abilities = new ArrayList<>();
        Character current = this;
        
        // Parcourir la cha\u00eene de d\u00e9corateurs
        while (current instanceof CharacterDecorator) {
            abilities.add((Ability) current);
            current = ((CharacterDecorator) current).wrapped;
        }
        
        // Trier par priorit\u00e9 d\u00e9croissante (DEFENSIVE en premier, OFFENSIVE en dernier)
        abilities.sort((a1, a2) -> Integer.compare(
            a2.getPriority().getValue(), 
            a1.getPriority().getValue()
        ));
        
        return Collections.unmodifiableList(abilities);
    }

    // ===== Hooks de combat d\u00e9l\u00e9gu\u00e9s + appel des Ability hooks =====
    
    @Override 
    public void beginTurn() { 
        wrapped.beginTurn();
        // Appeler le hook Ability
        onBeginTurn(this);
    }
    
    @Override 
    public int healThisTurn() { 
        int baseHeal = wrapped.healThisTurn();
        int abilityHeal = getHealAmount(this);
        return baseHeal + abilityHeal;
    }
    
    @Override 
    public int attackDamage() { 
        int baseDamage = wrapped.attackDamage();
        // Les modifications offensives sont g\u00e9r\u00e9es via modifyOutgoingDamage
        return baseDamage;
    }
    
    @Override 
    public int onReceiveDamage(int dmg) { 
        // Les modifications d\u00e9fensives sont g\u00e9r\u00e9es via modifyIncomingDamage
        return wrapped.onReceiveDamage(dmg);
    }
    
    // ===== M\u00e9thodes Ability par d\u00e9faut (peuvent \u00eatre surcharg\u00e9es) =====
    
    /**
     * Par d\u00e9faut, retourne le nom de la classe sans le package.
     * Les sous-classes peuvent surcharger pour un nom plus lisible.
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Indique si cette capacit\u00e9 est active.
     * Par d\u00e9faut, toujours active.
     */
    @Override
    public boolean isActive() {
        return true;
    }
}
