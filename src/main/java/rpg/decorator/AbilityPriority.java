package rpg.decorator;

/**
 * D\u00e9finit l'ordre de priorit\u00e9 d'application des capacit\u00e9s.
 * 
 * <p><strong>Ordre d'empilement recommand\u00e9:</strong></p>
 * Les capacit\u00e9s sont appliqu\u00e9es dans l'ordre suivant (de la plus haute \u00e0 la plus basse priorit\u00e9):
 * 
 * <ol>
 *   <li><strong>DEFENSIVE (100):</strong> Capacit\u00e9s d\u00e9fensives (Furtivit\u00e9, boucliers, armures)
 *       <br>Appliqu\u00e9es en premier pour permettre l'esquive/r\u00e9duction avant les calculs offensifs</li>
 *   
 *   <li><strong>SUPPORTIVE (50):</strong> Capacit\u00e9s de support (Soin, buffs, debuffs)
 *       <br>Appliqu\u00e9es apr\u00e8s les d\u00e9fensives mais avant les offensives</li>
 *   
 *   <li><strong>OFFENSIVE (10):</strong> Capacit\u00e9s offensives (Surcharge, Boule de Feu, buffs d'attaque)
 *       <br>Appliqu\u00e9es en dernier pour prendre en compte tous les modificateurs pr\u00e9c\u00e9dents</li>
 * </ol>
 * 
 * <p><strong>Exemple d'empilement correct:</strong></p>
 * <pre>
 * Character base = new Character("Hero", 10, 8, 7);
 * 
 * // Ordre correct: D\u00e9fensif -> Support -> Offensif
 * Character decorated = new Furtivite(      // DEFENSIVE (100)
 *     new Soin(                             // SUPPORTIVE (50)
 *         new Surcharge(                    // OFFENSIVE (10)
 *             base
 *         )
 *     )
 * );
 * </pre>
 * 
 * <p><strong>Pourquoi cet ordre?</strong></p>
 * <ul>
 *   <li><strong>D\u00e9fensives en premier:</strong> Permet d'esquiver/bloquer compl\u00e8tement une attaque
 *       avant m\u00eame que les calculs de d\u00e9g\u00e2ts ne soient effectu\u00e9s</li>
 *   <li><strong>Support au milieu:</strong> Les soins et buffs sont appliqu\u00e9s ind\u00e9pendamment
 *       de l'attaque ou de la d\u00e9fense</li>
 *   <li><strong>Offensives en dernier:</strong> Les bonus d'attaque sont calcul\u00e9s apr\u00e8s
 *       que toutes les autres capacit\u00e9s ont eu leur effet</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> La priorit\u00e9 est utilis\u00e9e pour trier les capacit\u00e9s lors de
 * l'it\u00e9ration via {@code character.getAbilities()}. Cela garantit que les hooks sont
 * appel\u00e9s dans le bon ordre.</p>
 * 
 * @see Ability#getPriority()
 * @see CharacterDecorator
 */
public enum AbilityPriority {
    /**
     * Priorit\u00e9 maximale pour les capacit\u00e9s d\u00e9fensives.
     * Ex: Furtivit\u00e9 (esquive), Bouclier, Armure magique
     */
    DEFENSIVE(100, "D\u00e9fensive"),
    
    /**
     * Priorit\u00e9 moyenne pour les capacit\u00e9s de support.
     * Ex: Soin, Buffs de stats, R\u00e9g\u00e9n\u00e9ration
     */
    SUPPORTIVE(50, "Support"),
    
    /**
     * Priorit\u00e9 basse pour les capacit\u00e9s offensives.
     * Ex: Surcharge, Boule de Feu, Empoisonnement
     */
    OFFENSIVE(10, "Offensive");
    
    private final int value;
    private final String displayName;
    
    /**
     * Constructeur de la priorit\u00e9.
     * @param value la valeur num\u00e9rique (plus \u00e9lev\u00e9e = plus prioritaire)
     * @param displayName le nom lisible de la cat\u00e9gorie
     */
    AbilityPriority(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    /**
     * @return la valeur num\u00e9rique de la priorit\u00e9 (plus haute = plus prioritaire)
     */
    public int getValue() {
        return value;
    }
    
    /**
     * @return le nom lisible de la cat\u00e9gorie de priorit\u00e9
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Compare cette priorit\u00e9 avec une autre pour le tri.
     * @param other l'autre priorit\u00e9 \u00e0 comparer
     * @return un nombre n\u00e9gatif si cette priorit\u00e9 est plus basse,
     *         z\u00e9ro si \u00e9gale, positif si plus haute
     */
    public int compareByValue(AbilityPriority other) {
        return Integer.compare(this.value, other.value);
    }
    
    @Override
    public String toString() {
        return displayName + " (" + value + ")";
    }
}
