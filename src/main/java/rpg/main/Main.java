package rpg.main;

import rpg.builder.CharacterBuilder;
import rpg.core.Character;
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

        alice = new Invisibility(alice);
        bob = new FireResistance(bob);
        bob = new Telepathy(bob);

        CharacterDAO dao = new CharacterDAO();
        dao.save(alice);
        dao.save(bob);

        for (Character c : dao.findAll()) {
            System.out.println(c.getDescription() + " -> Power=" + c.getPowerLevel());
        }

        Party party = new Party();
        party.add(alice);
        party.add(bob);
        System.out.println("Party total power: " + party.totalPower());

        System.out.println("Find Bob: " + dao.findByName("Bob"));
    }
}
