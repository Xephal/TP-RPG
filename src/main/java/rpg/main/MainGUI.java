package rpg.main;

import javax.swing.SwingUtilities;
import rpg.dao.CharacterDAO;
import rpg.mvc.ConsoleView;
import rpg.mvc.GameController;
import rpg.mvc.SwingView;
import rpg.observer.EventBus;
import rpg.settings.GameSettings;

public class MainGUI {
    public static void main(String[] args) {
        // Initialize core components
        EventBus eventBus = new EventBus();
        CharacterDAO dao = new CharacterDAO();
        GameController controller = new GameController(eventBus, dao);
        
        // Set default game settings
        GameSettings.getInstance().setMaxStatPoints(30);
        GameSettings.getInstance().setMaxCharactersPerGroup(10);
        GameSettings.getInstance().setMaxGroupsPerArmy(5);
        
        // Check if user wants console or GUI
        boolean useGUI = args.length > 0 && args[0].equalsIgnoreCase("gui");
        
        if (useGUI) {
            // Launch Swing GUI
            SwingUtilities.invokeLater(() -> {
                SwingView swingView = new SwingView(controller, eventBus, dao);
                eventBus.addObserver(swingView);
                swingView.render();
            });
        } else {
            // Launch Console version (backward compatibility)
            ConsoleView consoleView = new ConsoleView();
            eventBus.addObserver(consoleView);
            
            consoleView.render();
            consoleView.showMessage("RPG Character Manager - Console Mode");
            consoleView.showMessage("To launch GUI, run with 'gui' argument");
            consoleView.showMessage("Example: java -cp out rpg.main.MainGUI gui");
            
            // Create some demo characters
            try {
                controller.createCharacter("Alice", 8, 6, 8);
                controller.createCharacter("Bob", 10, 5, 5);
                controller.listCharacters();
            } catch (Exception e) {
                consoleView.showMessage("Error creating demo characters: " + e.getMessage());
            }
        }
    }
}