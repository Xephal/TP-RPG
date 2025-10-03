package rpg.builder;

import rpg.core.Character;
import rpg.validation.ValidationContext;
import rpg.validation.ValidationResult;
import rpg.validation.ValidatorChain;

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
