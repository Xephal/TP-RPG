package rpg.history;

import java.util.ArrayList;
import java.util.List;
import rpg.core.Character;

public class AdvancedBattleHistoryManager {
    private final List<BattleHistory> battles;
    private final int maxBattles;
    
    public AdvancedBattleHistoryManager() {
        this.battles = new ArrayList<>();
        this.maxBattles = 50; // Keep last 50 battles
    }
    
    public BattleHistory startNewBattle(Character fighter1, Character fighter2) {
        BattleHistory battle = new BattleHistory(fighter1, fighter2);
        addBattle(battle);
        return battle;
    }
    
    public void addBattle(BattleHistory battle) {
        battles.add(0, battle); // Add at beginning for newest first
        
        // Keep only max battles
        while (battles.size() > maxBattles) {
            battles.remove(battles.size() - 1);
        }
    }
    
    public List<BattleHistory> getAllBattles() {
        return new ArrayList<>(battles);
    }
    
    public BattleHistory getBattle(String battleId) {
        return battles.stream()
            .filter(battle -> battle.getBattleId().equals(battleId))
            .findFirst()
            .orElse(null);
    }
    
    public void saveBattleVariant(BattleHistory originalBattle, List<BattleAction> modifiedActions, Character newWinner) {
        // Create a new battle history with modified actions
        BattleHistory variant = new BattleHistory(originalBattle.getFighter1(), originalBattle.getFighter2());
        variant.setBattleName(originalBattle.getBattleName() + " (Variant)");
        
        // Add all modified actions
        for (BattleAction action : modifiedActions) {
            variant.addAction(action);
        }
        variant.setWinner(newWinner);
        
        addBattle(variant);
    }
    
    public void clearHistory() {
        battles.clear();
    }
    
    public int getBattleCount() {
        return battles.size();
    }
}