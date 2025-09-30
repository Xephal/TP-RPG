package rpg.validation;

import rpg.core.Character;

public class ValidationContext {
    private final Character character;
    private final String name;
    private final int strength;
    private final int agility;
    private final int intelligence;

    public ValidationContext(Character character) {
        this.character = character;
        this.name = character.getName();
        this.strength = character.getStrength();
        this.agility = character.getAgility();
        this.intelligence = character.getIntelligence();
    }

    public ValidationContext(String name, int strength, int agility, int intelligence) {
        this.character = null;
        this.name = name;
        this.strength = strength;
        this.agility = agility;
        this.intelligence = intelligence;
    }

    public Character getCharacter() {
        return character;
    }

    public String getName() {
        return name;
    }

    public int getStrength() {
        return strength;
    }

    public int getAgility() {
        return agility;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public int getTotalStats() {
        return strength + agility + intelligence;
    }
}