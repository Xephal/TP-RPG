package rpg.main;

import rpg.builder.InvalidCharacterException;
import rpg.composite.Army;
import rpg.composite.PartyComponent;
import rpg.core.Character;
import rpg.core.CombatEngine;
import rpg.core.Party;
import rpg.dao.CharacterDAO;
import rpg.decorator.FireResistance;
import rpg.decorator.Invisibility;
import rpg.decorator.Telepathy;
import rpg.mvc.ConsoleView;
import rpg.mvc.GameController;
import rpg.observer.EventBus;
import rpg.settings.GameSettings;

public class MainTP2 {
    public static void main(String[] args) {
        EventBus eventBus = new EventBus();
        CharacterDAO dao = new CharacterDAO();
        GameController controller = new GameController(eventBus, dao);
        ConsoleView view = new ConsoleView();
        
        eventBus.addObserver(view);
        
        view.render();
        view.showMessage("Demonstrating TP2 features...");

        GameSettings.getInstance().setMaxStatPoints(30);

        try {
            Character alice = controller.createCharacter("Alice", 8, 6, 8);
            Character bob = controller.createCharacter("Bob", 10, 5, 5);

            alice = new Invisibility(alice);
            bob = new FireResistance(new Telepathy(bob));

            view.showMessage("\nDecorated characters:");
            view.showMessage("Alice: " + alice.getDescription());
            view.showMessage("Bob: " + bob.getDescription());

            controller.listCharacters();

            Party party1 = new Party();
            party1.add(alice);
            party1.add(bob);

            PartyComponent partyComp = new PartyComponent(party1);
            Army army = new Army("Main Army");
            army.add(partyComp);

            view.showMessage("\nComposite pattern demo:");
            view.showMessage("Army: " + army.getName() + " - Total Power: " + army.getTotalPower());

            CombatEngine combat = new CombatEngine(eventBus);
            view.showMessage("\nCombat simulation with Command pattern:");
            Character winner = combat.simulate(alice, bob);

            view.showMessage("\nCommand history:");
            for (String action : combat.getCommandHistory().getHistory()) {
                view.showMessage("- " + action);
            }

        } catch (InvalidCharacterException e) {
            view.showMessage("Error: " + e.getMessage());
        }
    }
}