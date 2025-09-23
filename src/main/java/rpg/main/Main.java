package rpg.main;

import rpg.builder.CharacterBuilder;
import rpg.builder.InvalidCharacterException;
import rpg.core.Character;
import rpg.core.Combat;
import rpg.core.Party;
import rpg.dao.CharacterDAO;
import rpg.decorator.FireResistance;
import rpg.decorator.Invisibility;
import rpg.decorator.Telepathy;
import rpg.settings.GameSettings;

public class Main {
    public static void main(String[] args) {
        GameSettings.getInstance().setMaxStatPoints(30);

        CharacterBuilder b = new CharacterBuilder();
        Character alice = b.setName("Alice").setStrength(8).setAgility(6).setIntelligence(8).build();
        Character bob = b.setName("Bob").setStrength(10).setAgility(5).setIntelligence(5).build();
        // demonstrate invalid character handling
        try {
            Character invalid = b.setName("TooStrong").setStrength(50).setAgility(10).setIntelligence(10).build();
            System.out.println("Created invalid: " + invalid);
        } catch (InvalidCharacterException ex) {
            System.out.println("Validation error: " + ex.getMessage());
        }

        alice = new Invisibility(alice);
        bob = new FireResistance(bob);
        bob = new Telepathy(bob);

        CharacterDAO dao = new CharacterDAO();
        dao.save(alice);
        dao.save(bob);

        for (Character c : dao.findAll()) {
            System.out.println(c.getDescription() + " -> Power=" + c.getPowerLevel());
        }

        System.out.println("\nSorted by power:");
        for (Character c : dao.findAllSortedByPower()) {
            System.out.println(c.getDescription() + " -> Power=" + c.getPowerLevel());
        }

        System.out.println("\nSorted by name:");
        for (Character c : dao.findAllSortedByName()) {
            System.out.println(c.getDescription() + " -> Power=" + c.getPowerLevel());
        }

        Party party = new Party();
        party.add(alice);
        party.add(bob);
        System.out.println("Party total power: " + party.totalPower());

        System.out.println("Find Bob: " + dao.findByName("Bob"));

    // Simulate a combat between Alice and Bob
    System.out.println("\nSimulating combat Alice vs Bob:");
    Character winner = Combat.simulate(alice, bob, true);
    System.out.println("Winner: " + winner.getName());
    }
}
