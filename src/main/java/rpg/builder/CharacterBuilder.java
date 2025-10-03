package rpg.builder;

import rpg.core.Character;
import rpg.validation.ValidationContext;
import rpg.validation.ValidationResult;
import rpg.validation.ValidatorChain;

/**
 * Builder pour cr\u00e9er des personnages avec validation.
 * Utilise le pattern Builder avec validation via une cha\u00eene de validateurs.
 * 
 * <p><strong>R\u00e9utilisabilit\u00e9:</strong> Ce builder est <strong>r\u00e9utilisable</strong>.</p>
 * Apr\u00e8s avoir appel\u00e9 {@link #build()} ou {@link #buildOrNull()}, vous pouvez:
 * <ul>
 *   <li>Appeler {@link #reset()} pour r\u00e9initialiser tous les champs aux valeurs par d\u00e9faut</li>
 *   <li>Modifier les champs et construire un nouveau personnage</li>
 *   <li>Cr\u00e9er plusieurs personnages successivement avec le m\u00eame builder</li>
 * </ul>
 * 
 * <p><strong>Exemple d'utilisation:</strong></p>
 * <pre>
 * CharacterBuilder builder = new CharacterBuilder();
 * 
 * // Premier personnage
 * Character hero = builder
 *     .setName("Aragorn")
 *     .setStrength(10)
 *     .setAgility(8)
 *     .setIntelligence(7)
 *     .build();
 * 
 * // R\u00e9initialisation
 * builder.reset();
 * 
 * // Second personnage
 * Character villain = builder
 *     .setName("Sauron")
 *     .setStrength(15)
 *     .setAgility(5)
 *     .setIntelligence(12)
 *     .build();
 * </pre>
 * 
 * <p><strong>Exemple avec l'API fluide de ValidatorChain:</strong></p>
 * <pre>
 * ValidatorChain chain = ValidatorChain.start()
 *     .add(new NameValidator())
 *     .add(new StatsValidator());
 * </pre>
 * 
 * @see ValidatorChain
 * @see Character
 */
public class CharacterBuilder {
    private static final String DEFAULT_NAME = "Unnamed";
    private static final int DEFAULT_STAT = 0;
    
    private String name = DEFAULT_NAME;
    private int strength = DEFAULT_STAT;
    private int agility = DEFAULT_STAT;
    private int intelligence = DEFAULT_STAT;

    /**
     * D\u00e9finit le nom du personnage.
     * @param name le nom (ne doit pas \u00eatre null ou vide)
     * @return cette instance pour le cha\u00eenage fluide
     */
    public CharacterBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * D\u00e9finit la force du personnage.
     * @param strength la force (doit \u00eatre >= 0)
     * @return cette instance pour le cha\u00eenage fluide
     */
    public CharacterBuilder setStrength(int strength) {
        this.strength = strength;
        return this;
    }

    /**
     * D\u00e9finit l'agilit\u00e9 du personnage.
     * @param agility l'agilit\u00e9 (doit \u00eatre >= 0)
     * @return cette instance pour le cha\u00eenage fluide
     */
    public CharacterBuilder setAgility(int agility) {
        this.agility = agility;
        return this;
    }

    /**
     * D\u00e9finit l'intelligence du personnage.
     * @param intelligence l'intelligence (doit \u00eatre >= 0)
     * @return cette instance pour le cha\u00eenage fluide
     */
    public CharacterBuilder setIntelligence(int intelligence) {
        this.intelligence = intelligence;
        return this;
    }

    /**
     * R\u00e9initialise tous les champs du builder aux valeurs par d\u00e9faut.
     * Permet de r\u00e9utiliser le m\u00eame builder pour cr\u00e9er plusieurs personnages.
     * 
     * <p>Valeurs par d\u00e9faut:</p>
     * <ul>
     *   <li>name: "Unnamed"</li>
     *   <li>strength: 0</li>
     *   <li>agility: 0</li>
     *   <li>intelligence: 0</li>
     * </ul>
     * 
     * @return cette instance pour le cha\u00eenage fluide
     */
    public CharacterBuilder reset() {
        this.name = DEFAULT_NAME;
        this.strength = DEFAULT_STAT;
        this.agility = DEFAULT_STAT;
        this.intelligence = DEFAULT_STAT;
        return this;
    }

    /**
     * Construit un personnage avec les valeurs actuelles du builder.
     * Ex\u00e9cute la validation compl\u00e8te et accumule toutes les erreurs.
     * 
     * <p><strong>Important:</strong> Le builder reste utilisable apr\u00e8s cet appel.
     * Les valeurs ne sont pas r\u00e9initialis\u00e9es automatiquement. Appelez {@link #reset()}
     * si vous voulez repartir de z\u00e9ro.</p>
     * 
     * @return un nouveau personnage valid\u00e9
     * @throws InvalidCharacterException si la validation \u00e9choue (contient toutes les erreurs)
     */
    public Character build() {
        ValidationContext context = new ValidationContext(name, strength, agility, intelligence);
        ValidatorChain chain = new ValidatorChain();
        ValidationResult result = chain.validate(context);
        
        if (!result.isValid()) {
            throw new InvalidCharacterException(result.getErrors());
        }
        
        return new Character(name, strength, agility, intelligence);
    }
}
