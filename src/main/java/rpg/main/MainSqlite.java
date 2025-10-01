package rpg.main;

import rpg.dao.Db;
import rpg.dao.SqliteCharacterDAO;
import rpg.mvc.GameController;
import rpg.mvc.SwingView;
import rpg.observer.EventBus;

public class MainSqlite {
    public static void main(String[] args) {
        try {
            // 1) schéma
            Db.initSchema();

            // 2) wiring
            SqliteCharacterDAO dao = new SqliteCharacterDAO();
            EventBus eventBus = new EventBus();
            GameController controller = new GameController(eventBus, dao);

            // 3) lance l’IHM Swing (tu peux aussi lancer ConsoleView si tu préfères)
            new SwingView(controller, eventBus, dao).render();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
