package rpg.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import rpg.core.Character;

public class BattleHistory {
    private final String battleId;
    private final Character fighter1;
    private final Character fighter2;
    private final LocalDateTime timestamp;
    private final List<BattleAction> actions;
    private Character winner;
    private String battleName;
    
    public BattleHistory(Character fighter1, Character fighter2) {
        this.battleId = "BATTLE_" + System.currentTimeMillis();
        this.fighter1 = fighter1;
        this.fighter2 = fighter2;
        this.timestamp = LocalDateTime.now();
        this.actions = new ArrayList<>();
        this.battleName = fighter1.getName() + " vs " + fighter2.getName();
    }
    
    public void addAction(BattleAction action) {
        actions.add(action);
    }
    
    public void setWinner(Character winner) {
        this.winner = winner;
    }
    
    public void setBattleName(String name) {
        this.battleName = name;
    }
    
    // Getters
    public String getBattleId() { return battleId; }
    public Character getFighter1() { return fighter1; }
    public Character getFighter2() { return fighter2; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<BattleAction> getActions() { return new ArrayList<>(actions); }
    public Character getWinner() { return winner; }
    public String getBattleName() { return battleName; }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public String getSummary() {
        return String.format("[%s] %s - Winner: %s (%d actions)", 
            getFormattedTimestamp(), 
            battleName, 
            winner != null ? winner.getName() : "Unknown",
            actions.size());
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}