package rpg.mvc;

import java.util.List;
import rpg.core.Character;

public class ConsoleView extends View {
    public ConsoleView() {
        super("ConsoleView");
    }

    @Override
    public void render() {
        System.out.println("=== RPG Character Manager ===");
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void update(String eventType, Object data) {
        switch (eventType) {
            case "CHARACTER_CREATED":
                Character character = (Character) data;
                showMessage("Character created: " + character.getDescription());
                break;
            case "LIST_CHARACTERS":
                @SuppressWarnings("unchecked")
                List<Character> characters = (List<Character>) data;
                showMessage("Characters in database:");
                for (Character c : characters) {
                    showMessage("- " + c.toString());
                }
                break;
            case "COMBAT_ACTION":
                showMessage("Combat: " + data.toString());
                break;
            default:
                showMessage("Event: " + eventType + " - " + data);
        }
    }
}