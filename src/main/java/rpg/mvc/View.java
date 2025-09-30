package rpg.mvc;

import rpg.observer.Observer;

public abstract class View implements Observer {
    protected String viewName;

    public View(String viewName) {
        this.viewName = viewName;
    }

    public abstract void render();
    public abstract void showMessage(String message);
}