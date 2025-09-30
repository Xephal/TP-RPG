package rpg.history;

import rpg.core.Character;

public class BattleAction {
    private final int round;
    private final Character actor;
    private final Character target;
    private final String actionType;
    private final String description;
    private final int damage;
    private final boolean modifiable;
    
    public BattleAction(int round, Character actor, Character target, String actionType, String description, int damage) {
        this.round = round;
        this.actor = actor;
        this.target = target;
        this.actionType = actionType;
        this.description = description;
        this.damage = damage;
        this.modifiable = true; // By default, actions can be modified
    }
    
    public BattleAction(int round, Character actor, Character target, String actionType, String description, int damage, boolean modifiable) {
        this.round = round;
        this.actor = actor;
        this.target = target;
        this.actionType = actionType;
        this.description = description;
        this.damage = damage;
        this.modifiable = modifiable;
    }
    
    // Getters
    public int getRound() { return round; }
    public Character getActor() { return actor; }
    public Character getTarget() { return target; }
    public String getActionType() { return actionType; }
    public String getDescription() { return description; }
    public int getDamage() { return damage; }
    public boolean isModifiable() { return modifiable; }
    
    public String getFormattedAction() {
        return String.format("Round %d: %s %s %s (dmg: %d)", 
            round, 
            actor.getName(), 
            actionType, 
            target.getName(), 
            damage);
    }
    
    @Override
    public String toString() {
        return getFormattedAction();
    }
}