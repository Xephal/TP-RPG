package rpg.core;

public class Character {
    private final String name;
    private final int strength;
    private final int agility;
    private final int intelligence;

    public Character(String name, int strength, int agility, int intelligence) {
        this.name = name;
        this.strength = strength;
        this.agility = agility;
        this.intelligence = intelligence;
    }

    public String getName() { return name; }
    public int getStrength() { return strength; }
    public int getAgility() { return agility; }
    public int getIntelligence() { return intelligence; }

    public int getPowerLevel() {
        return strength * 2 + agility * 2 + intelligence * 3;
    }

    public String getDescription() {
        return String.format("%s (STR=%d, AGI=%d, INT=%d)", name, strength, agility, intelligence);
    }

    @Override
    public String toString() { return getDescription() + " Power=" + getPowerLevel(); }

    // ===== Hooks de combat, par défaut comportement basique =====
    public void beginTurn() {}
    public int healThisTurn() { return 0; }                 // montant de soin à appliquer ce tour
    public int attackDamage() { return strength; }          // dégâts de base
    public int onReceiveDamage(int dmg) { return Math.max(0, dmg); } // modif côté défense
}
