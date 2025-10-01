package rpg.main;

import java.util.Scanner;
import rpg.builder.CharacterBuilder;
import rpg.builder.InvalidCharacterException;
import rpg.core.Character;
import rpg.core.Combat;
import rpg.core.Party;
import rpg.dao.CharacterDAO;
import rpg.decorator.Surcharge;
import rpg.decorator.Furtivite;
import rpg.decorator.Soin;
import rpg.settings.GameSettings;
import rpg.ui.RpgGui;

public class Main {
    public static void main(String[] args) {
        GameSettings.getInstance().setMaxStatPoints(30);

        CharacterBuilder b = new CharacterBuilder();
        Character alice = b.setName("Alice").setStrength(8).setAgility(6).setIntelligence(8).build();
        Character bob = b.setName("Bob").setStrength(10).setAgility(5).setIntelligence(5).build();
        try {
            Character invalid = b.setName("TooStrong").setStrength(50).setAgility(10).setIntelligence(10).build();
            System.out.println("Created invalid: " + invalid);
        } catch (InvalidCharacterException ex) {
            System.out.println("Validation error: " + ex.getMessage());
        }

        alice = new Furtivite(alice);
        bob = new Surcharge(bob);
        bob = new Soin(bob);

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

        System.out.println("\nSimulating combat Alice vs Bob:");
        Character winner = Combat.simulate(alice, bob, true);
        System.out.println("Winner: " + winner.getName());

        // If started with 'gui' argument, launch GUI immediately and exit CLI
        for (String a : args) {
            if (a.equalsIgnoreCase("gui")) {
                new RpgGui(alice, bob, dao);
                return;
            }
        }

        // Use a single Scanner for all user input (pre-GUI prompt + interactive)
        Scanner scanner = new Scanner(System.in);

        // Offer to launch GUI before entering interactive CLI (WIP)
        System.out.print("Lancer l'interface graphique (WIP) ? (y/n): ");
        String preGui = "n";
        try {
            String ln = scanner.nextLine();
            if (ln != null) preGui = ln.trim();
        } catch (Exception ignored) {
        }
        if (preGui.equalsIgnoreCase("y") || preGui.equalsIgnoreCase("yes")) {
            System.out.println("Lancement de l'interface graphique (WIP)...");
            new RpgGui(alice, bob, dao);
            scanner.close();
            return;
        }

        // No CLI custom-creation anymore: custom character creation is GUI-only (WIP)
        System.out.println("\nCustom character creation and tests are now GUI-only (WIP). Launch GUI? (y/n): ");
        String answer = "n";
        try {
            answer = scanner.nextLine().trim();
        } catch (Exception ignored) {}
        if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
            System.out.println("Lancement de l'interface graphique (WIP)...");
            new RpgGui(alice, bob, dao);
            scanner.close();
            return;
        }

        scanner.close();
        System.out.println("Exiting demo. Bye!");
    }

    // CLI custom creation removed — use GUI instead.
}
