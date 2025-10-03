package rpg.decorator;

import rpg.core.Character;

/**
 * Interface repr\u00e9sentant une capacit\u00e9/comp\u00e9tence qu'un personnage peut poss\u00e9der.
 * Utilise des hooks pour modifier le comportement de combat sans couplage fort.
 * 
 * <p>Cette interface remplace le besoin d'utiliser {@code instanceof} dans les commandes
 * en offrant des points d'extension clairs et d\u00e9coupl\u00e9s.</p>
 * 
 * <p><strong>Cycle de vie des hooks pendant un combat:</strong></p>
 * <ol>
 *   <li>{@link #onBeginTurn(Character)} - D\u00e9but du tour du personnage</li>
 *   <li>{@link #onBeforeAttack(Character, Character)} - Avant de porter une attaque</li>
 *   <li>{@link #modifyOutgoingDamage(Character, Character, int)} - Modification des d\u00e9g\u00e2ts sortants</li>
 *   <li>{@link #onAfterAttack(Character, Character, int)} - Apr\u00e8s avoir port\u00e9 une attaque</li>
 *   <li>{@link #onBeforeReceiveDamage(Character, Character, int)} - Avant de recevoir des d\u00e9g\u00e2ts</li>
 *   <li>{@link #modifyIncomingDamage(Character, Character, int)} - Modification des d\u00e9g\u00e2ts entrants</li>
 *   <li>{@link #onAfterReceiveDamage(Character, Character, int)} - Apr\u00e8s avoir re\u00e7u des d\u00e9g\u00e2ts</li>
 *   <li>{@link #getHealAmount(Character)} - Calcul du montant de soin pour ce tour</li>
 *   <li>{@link #onEndTurn(Character)} - Fin du tour du personnage</li>
 * </ol>
 * 
 * @see CharacterDecorator
 * @see AbilityPriority
 */
public interface Ability {
    
    /**
     * Retourne le nom de la capacit\u00e9 (ex: "Surcharge", "Boule de Feu").
     * @return le nom lisible de la capacit\u00e9
     */
    String getName();
    
    /**
     * Retourne une description courte de la capacit\u00e9.
     * @return la description de l'effet de la capacit\u00e9
     */
    String getDescription();
    
    /**
     * Retourne la priorit\u00e9 d'application de cette capacit\u00e9.
     * Les capacit\u00e9s avec une priorit\u00e9 plus haute s'ex\u00e9cutent en premier.
     * 
     * @return la priorit\u00e9 de la capacit\u00e9
     * @see AbilityPriority
     */
    AbilityPriority getPriority();
    
    /**
     * Hook appel\u00e9 au d\u00e9but du tour du personnage.
     * Utile pour g\u00e9rer les cooldowns, buffs temporaires, etc.
     * 
     * @param owner le personnage poss\u00e9dant cette capacit\u00e9
     */
    default void onBeginTurn(Character owner) {}
    
    /**
     * Hook appel\u00e9 avant que le personnage n'effectue une attaque.
     * Permet de pr\u00e9parer des buffs, afficher des messages, etc.
     * 
     * @param attacker le personnage qui attaque
     * @param target la cible de l'attaque
     */
    default void onBeforeAttack(Character attacker, Character target) {}
    
    /**
     * Modifie les d\u00e9g\u00e2ts sortants d'une attaque.
     * Permet d'augmenter ou r\u00e9duire les d\u00e9g\u00e2ts inflig\u00e9s.
     * 
     * @param attacker le personnage qui attaque
     * @param target la cible de l'attaque
     * @param baseDamage les d\u00e9g\u00e2ts de base avant modification
     * @return les d\u00e9g\u00e2ts apr\u00e8s modification (retourner baseDamage si pas de changement)
     */
    default int modifyOutgoingDamage(Character attacker, Character target, int baseDamage) {
        return baseDamage;
    }
    
    /**
     * Hook appel\u00e9 apr\u00e8s qu'une attaque a \u00e9t\u00e9 port\u00e9e.
     * Utile pour appliquer des effets post-attaque, consommer des buffs, etc.
     * 
     * @param attacker le personnage qui a attaqu\u00e9
     * @param target la cible qui a \u00e9t\u00e9 attaqu\u00e9e
     * @param damageDealt les d\u00e9g\u00e2ts r\u00e9ellement inflig\u00e9s
     */
    default void onAfterAttack(Character attacker, Character target, int damageDealt) {}
    
    /**
     * Hook appel\u00e9 avant que le personnage ne re\u00e7oive des d\u00e9g\u00e2ts.
     * Permet de pr\u00e9parer des d\u00e9fenses, tenter une esquive, etc.
     * 
     * @param defender le personnage qui va recevoir les d\u00e9g\u00e2ts
     * @param attacker le personnage qui attaque
     * @param incomingDamage les d\u00e9g\u00e2ts entrants
     * @return true pour esquiver compl\u00e8tement l'attaque, false sinon
     */
    default boolean onBeforeReceiveDamage(Character defender, Character attacker, int incomingDamage) {
        return false; // pas d'esquive par d\u00e9faut
    }
    
    /**
     * Modifie les d\u00e9g\u00e2ts re\u00e7us par le personnage.
     * Permet de r\u00e9duire (d\u00e9fense) ou augmenter (vuln\u00e9rabilit\u00e9) les d\u00e9g\u00e2ts.
     * 
     * @param defender le personnage qui re\u00e7oit les d\u00e9g\u00e2ts
     * @param attacker le personnage qui attaque
     * @param incomingDamage les d\u00e9g\u00e2ts entrants avant modification
     * @return les d\u00e9g\u00e2ts apr\u00e8s modification (retourner incomingDamage si pas de changement)
     */
    default int modifyIncomingDamage(Character defender, Character attacker, int incomingDamage) {
        return incomingDamage;
    }
    
    /**
     * Hook appel\u00e9 apr\u00e8s que le personnage a re\u00e7u des d\u00e9g\u00e2ts.
     * Utile pour des effets de riposte, d\u00e9clenchement de capacit\u00e9s d\u00e9fensives, etc.
     * 
     * @param defender le personnage qui a re\u00e7u les d\u00e9g\u00e2ts
     * @param attacker le personnage qui a attaqu\u00e9
     * @param damageReceived les d\u00e9g\u00e2ts r\u00e9ellement re\u00e7us
     */
    default void onAfterReceiveDamage(Character defender, Character attacker, int damageReceived) {}
    
    /**
     * Retourne le montant de soin que cette capacit\u00e9 apporte ce tour.
     * Appel\u00e9 automatiquement pendant la phase de soin.
     * 
     * @param owner le personnage poss\u00e9dant cette capacit\u00e9
     * @return le montant de PV \u00e0 restaurer (0 si pas de soin)
     */
    default int getHealAmount(Character owner) {
        return 0;
    }
    
    /**
     * Hook appel\u00e9 \u00e0 la fin du tour du personnage.
     * Utile pour nettoyer des \u00e9tats temporaires, d\u00e9cr\u00e9menter des compteurs, etc.
     * 
     * @param owner le personnage poss\u00e9dant cette capacit\u00e9
     */
    default void onEndTurn(Character owner) {}
    
    /**
     * Indique si cette capacit\u00e9 est actuellement active.
     * Permet de d\u00e9sactiver temporairement une capacit\u00e9 sans la retirer.
     * 
     * @return true si la capacit\u00e9 est active, false sinon
     */
    default boolean isActive() {
        return true;
    }
}
