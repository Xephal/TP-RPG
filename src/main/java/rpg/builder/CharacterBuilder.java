package rpg.builder;

import rpg.core.Character;
import rpg.validation.ValidationContext;
import rpg.validation.ValidationResult;
import rpg.validation.ValidatorChain;

/**
 * Builder pour cr\u00e9er des personnages avec validation.
 * Utilise le pattern Builder avec validation via une cha\u00eene de validateurs.
 * 
 * Exemple d'utilisation avec l'API fluide de ValidatorChain:
 * <pre>
 * ValidatorChain chain = ValidatorChain.start()
 *     .add(new NameValidator())
 *     .add(new StatsValidator());
 * </pre>
 */
public class CharacterBuilder {
    private String name = "Unnamed";
    private int strength = 0;
    private int agility = 0;
    private int intelligence = 0;

    public CharacterBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public CharacterBuilder setStrength(int strength) {
        this.strength = strength;
        return this;
    }

    public CharacterBuilder setAgility(int agility) {
        this.agility = agility;
        return this;
    }

    public CharacterBuilder setIntelligence(int intelligence) {
        this.intelligence = intelligence;
        return this;
    }

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
