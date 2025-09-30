package rpg.mvc;

import rpg.builder.CharacterBuilder;
import rpg.command.CommandHistory;
import rpg.core.Character;
import rpg.dao.CharacterDAO;
import rpg.observer.EventBus;

public class GameController extends Controller {
    private final CharacterDAO dao;
    private final CommandHistory commandHistory;

    public GameController(EventBus eventBus, CharacterDAO dao) {
        super(eventBus);
        this.dao = dao;
        this.commandHistory = new CommandHistory();
    }

    public Character createCharacter(String name, int strength, int agility, int intelligence) {
        CharacterBuilder builder = new CharacterBuilder();
        Character character = builder.setName(name)
                .setStrength(strength)
                .setAgility(agility)
                .setIntelligence(intelligence)
                .build();
        
        dao.save(character);
        eventBus.notifyObservers("CHARACTER_CREATED", character);
        return character;
    }

    public void listCharacters() {
        eventBus.notifyObservers("LIST_CHARACTERS", dao.findAll());
    }

    public CommandHistory getCommandHistory() {
        return commandHistory;
    }
}