package rpg.builder;

import rpg.core.Character;
import rpg.settings.GameSettings;

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
        Character c = new Character(name, strength, agility, intelligence);
        GameSettings settings = GameSettings.getInstance();
        if (!settings.isValid(c)) {
            throw new IllegalArgumentException("Character does not respect game settings");
        }
        return c;
    }
}
