package rpg.mvc;

import rpg.observer.EventBus;

public abstract class Controller {
    protected final EventBus eventBus;

    public Controller(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}