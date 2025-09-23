package rpg.settings;

import rpg.core.Character;

public class GameSettings {
    private static final GameSettings INSTANCE = new GameSettings();

    private int maxStatPoints = 30;

    private GameSettings() {}

    public static GameSettings getInstance() {
        return INSTANCE;
    }

    public int getMaxStatPoints() {
        return maxStatPoints;
    }

    public void setMaxStatPoints(int maxStatPoints) {
        this.maxStatPoints = maxStatPoints;
    }

    public boolean isValid(Character c) {
        int sum = c.getStrength() + c.getAgility() + c.getIntelligence();
        return sum <= maxStatPoints;
    }
}
