package rpg.mvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import rpg.core.Character;
import rpg.history.AdvancedBattleHistoryManager;
import rpg.history.BattleAction;
import rpg.history.BattleHistory;

public class InteractiveBattleReplay extends JDialog {
    public interface VariantSavedCallback {
        void onVariantSaved();
    }
    
    private final BattleHistory originalBattle;
    private BattleHistory currentBattle;
    private final AdvancedBattleHistoryManager historyManager;
    private final VariantSavedCallback callback;
    private int currentActionIndex;
    private boolean isPlaying;
    private Timer replayTimer;
    
    // UI Components
    private JTextArea combatDisplay;
    private JLabel turnLabel;
    private JProgressBar f1HPBar;
    private JProgressBar f2HPBar;
    private JButton playPauseBtn;
    private JButton stepBackBtn;
    private JButton stepForwardBtn;
    private JButton resetBtn;
    private JButton saveVariantBtn;
    private JSlider speedSlider;
    private JLabel statusLabel;
    private JLabel actionDetailsLabel;
    
    // Character state tracking
    private int fighter1HP;
    private int fighter2HP;
    private int fighter1MaxHP;
    private int fighter2MaxHP;
    
    public InteractiveBattleReplay(Frame parent, BattleHistory battle, AdvancedBattleHistoryManager historyManager, VariantSavedCallback callback) {
        super(parent, "Interactive Battle Replay (v2.0) - " + battle.getBattleName(), true);
        this.originalBattle = battle;
        this.currentBattle = cloneBattle(battle);
        this.historyManager = historyManager;
        this.callback = callback;
        this.currentActionIndex = 0;
        this.isPlaying = false;
        
        initializeCharacterStats();
        initializeUI();
        setupEventHandlers();
        resetReplay();
        
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initializeCharacterStats() {
        Character f1 = originalBattle.getFighter1();
        Character f2 = originalBattle.getFighter2();
        
        // Calculate initial HP (same formula as CombatEngine)
        fighter1MaxHP = Math.max(10, f1.getStrength() * 10 + f1.getIntelligence() * 2);
        fighter2MaxHP = Math.max(10, f2.getStrength() * 10 + f2.getIntelligence() * 2);
        
        fighter1HP = fighter1MaxHP;
        fighter2HP = fighter2MaxHP;
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Top panel - Battle info
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.NORTH);
        
        // Center panel - Combat display
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel - Controls
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Battle Information"));
        
        String battleInfo = String.format(
            "Battle: %s vs %s | Date: %s | Original Winner: %s",
            originalBattle.getFighter1().getName(),
            originalBattle.getFighter2().getName(),
            originalBattle.getTimestamp(),
            originalBattle.getWinner() != null ? originalBattle.getWinner().getName() : "Unknown"
        );
        
        JLabel infoLabel = new JLabel(battleInfo);
        infoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(infoLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Combat log display
        combatDisplay = new JTextArea();
        combatDisplay.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        combatDisplay.setEditable(false);
        combatDisplay.setBackground(Color.BLACK);
        combatDisplay.setForeground(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(combatDisplay);
        scrollPane.setBorder(new TitledBorder("Combat Replay"));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Right panel - Character status
        JPanel statusPanel = createStatusPanel();
        panel.add(statusPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Battle Status"));
        panel.setPreferredSize(new Dimension(200, 0));
        
        // Turn counter (sans révéler le total pour créer du suspense)
        turnLabel = new JLabel("Tour: 0", SwingConstants.CENTER);
        turnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(turnLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        // Character HP displays
        JLabel f1Label = new JLabel(originalBattle.getFighter1().getName() + ":");
        f1Label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(f1Label);
        
        f1HPBar = new JProgressBar(0, fighter1MaxHP);
        f1HPBar.setValue(fighter1HP);
        f1HPBar.setStringPainted(true);
        f1HPBar.setString(fighter1HP + " / " + fighter1MaxHP);
        f1HPBar.setForeground(Color.GREEN);
        panel.add(f1HPBar);
        
        panel.add(Box.createVerticalStrut(5));
        
        JLabel f2Label = new JLabel(originalBattle.getFighter2().getName() + ":");
        f2Label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(f2Label);
        
        f2HPBar = new JProgressBar(0, fighter2MaxHP);
        f2HPBar.setValue(fighter2HP);
        f2HPBar.setStringPainted(true);
        f2HPBar.setString(fighter2HP + " / " + fighter2MaxHP);
        f2HPBar.setForeground(Color.GREEN);
        panel.add(f2HPBar);
        
        panel.add(Box.createVerticalStrut(10));
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Replay Controls"));
        
        // Playback controls
        JPanel playbackPanel = new JPanel(new FlowLayout());
        
        resetBtn = new JButton("<< Reset");
        stepBackBtn = new JButton("< Step Back");
        playPauseBtn = new JButton("> Play");
        stepForwardBtn = new JButton("> Step Forward");
        
        playbackPanel.add(resetBtn);
        playbackPanel.add(stepBackBtn);
        playbackPanel.add(playPauseBtn);
        playbackPanel.add(stepForwardBtn);
        
        // Speed control
        JPanel speedPanel = new JPanel(new FlowLayout());
        speedPanel.add(new JLabel("Speed:"));
        speedSlider = new JSlider(1, 10, 5);
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedPanel.add(speedSlider);
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout());
        JButton modifyBtn = new JButton("[*] Modify Action");
        saveVariantBtn = new JButton("[S] Save Variant");
        saveVariantBtn.setEnabled(false);
        
        modifyBtn.addActionListener(e -> modifyCurrentAction());
        saveVariantBtn.addActionListener(e -> saveVariant());
        
        actionPanel.add(modifyBtn);
        actionPanel.add(saveVariantBtn);
        
        // Status
        statusLabel = new JLabel("Ready to replay battle");
        
        panel.add(playbackPanel, BorderLayout.WEST);
        panel.add(speedPanel, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.EAST);
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Replay timer
        replayTimer = new Timer(1000, e -> stepForward());
        
        // Control buttons
        playPauseBtn.addActionListener(e -> togglePlayPause());
        stepBackBtn.addActionListener(e -> stepBack());
        stepForwardBtn.addActionListener(e -> stepForward());
        resetBtn.addActionListener(e -> resetReplay());
        
        // Speed slider
        speedSlider.addChangeListener(e -> {
            int speed = speedSlider.getValue();
            int delay = 2000 - (speed * 180); // 1820ms to 200ms
            replayTimer.setDelay(delay);
        });
    }
    
    private void togglePlayPause() {
        if (isPlaying) {
            pauseReplay();
        } else {
            playReplay();
        }
    }
    
    private void playReplay() {
        if (currentActionIndex >= currentBattle.getActions().size()) {
            return; // Battle finished
        }
        
        isPlaying = true;
        playPauseBtn.setText("|| Pause");
        replayTimer.start();
        statusLabel.setText("Playing replay...");
    }
    
    private void pauseReplay() {
        isPlaying = false;
        playPauseBtn.setText("> Play");
        replayTimer.stop();
        statusLabel.setText("Replay paused");
    }
    
    private void stepForward() {
        List<BattleAction> actions = currentBattle.getActions();
        if (currentActionIndex >= actions.size()) {
            pauseReplay();
            statusLabel.setText("Replay completed");
            return;
        }
        
        BattleAction action = actions.get(currentActionIndex);
        executeAction(action);
        currentActionIndex++;
        updateDisplay();
    }
    
    private void stepBack() {
        if (currentActionIndex <= 0) {
            return;
        }
        
        pauseReplay();
        currentActionIndex--;
        replayFromStart();
    }
    
    private void resetReplay() {
        pauseReplay();
        currentActionIndex = 0;
        initializeCharacterStats();
        combatDisplay.setText("Starting battle replay...\n");
        updateDisplay();
        statusLabel.setText("Ready to replay battle");
    }
    
    private void executeAction(BattleAction action) {
        // Apply damage to the correct character
        if (action.getTarget().equals(originalBattle.getFighter2())) {
            fighter2HP = Math.max(0, fighter2HP - action.getDamage());
        } else {
            fighter1HP = Math.max(0, fighter1HP - action.getDamage());
        }
        
        // Add to combat log
        String logEntry = String.format("[%d] %s\n", 
            action.getRound(), 
            action.getDescription());
        combatDisplay.append(logEntry);
        
        // Auto-scroll to bottom
        combatDisplay.setCaretPosition(combatDisplay.getDocument().getLength());
    }
    
    private void replayFromStart() {
        initializeCharacterStats();
        combatDisplay.setText("Starting battle replay...\n");
        
        // Use currentBattle to get potentially modified actions
        List<BattleAction> actionsToReplay = currentBattle.getActions();
        for (int i = 0; i < currentActionIndex && i < actionsToReplay.size(); i++) {
            BattleAction action = actionsToReplay.get(i);
            executeAction(action);
        }
        
        updateDisplay();
    }
    
    private void updateDisplay() {
        // Update turn display (juste le tour actuel, pas le total)
        if (turnLabel != null) {
            turnLabel.setText("Tour: " + currentActionIndex);
        }
        
        // Update HP bars with current battle HP
        if (f1HPBar != null) {
            f1HPBar.setValue(fighter1HP);
            f1HPBar.setString(fighter1HP + " / " + fighter1MaxHP);
            f1HPBar.setForeground(fighter1HP > fighter1MaxHP * 0.3 ? Color.GREEN : Color.RED);
        }
        
        if (f2HPBar != null) {
            f2HPBar.setValue(fighter2HP);
            f2HPBar.setString(fighter2HP + " / " + fighter2MaxHP);
            f2HPBar.setForeground(fighter2HP > fighter2MaxHP * 0.3 ? Color.GREEN : Color.RED);
        }
        
        // Update button states
        List<BattleAction> actions = currentBattle.getActions();
        stepBackBtn.setEnabled(currentActionIndex > 0);
        stepForwardBtn.setEnabled(currentActionIndex < actions.size());
        playPauseBtn.setEnabled(currentActionIndex < actions.size());
    }
    
    private BattleHistory cloneBattle(BattleHistory original) {
        // Create a copy of the battle for modifications
        BattleHistory clone = new BattleHistory(original.getFighter1(), original.getFighter2());
        clone.setWinner(original.getWinner());
        
        // Copy all actions using the direct access method
        List<BattleAction> originalActions = original.getActions();
        for (BattleAction action : originalActions) {
            clone.addAction(new BattleAction(
                action.getRound(),
                action.getActor(),
                action.getTarget(),
                action.getActionType(),
                action.getDescription(),
                action.getDamage()
            ));
        }
        
        return clone;
    }
    
    private void recalculateBattleFromModification(int modifiedActionIndex) {
        System.out.println("Starting recalculation from modification at index " + modifiedActionIndex);
        
        // Recalculate HP from the beginning up to (but not including) the modified action
        initializeCharacterStats();
        
        List<BattleAction> actions = currentBattle.getActions();
        
        // Apply all actions up to (but not including) the modified one - update their descriptions too
        for (int i = 0; i < modifiedActionIndex; i++) {
            if (i < actions.size()) {
                BattleAction action = actions.get(i);
                applyActionToHP(action);
                
                // Update even unchanged actions to show both fighters' HP
                updateActionDescription(action, i);
                
                System.out.println("Applied unchanged action " + (i+1) + ". HP: F1=" + fighter1HP + ", F2=" + fighter2HP);
            }
        }
        
        // Now apply the modified action
        if (modifiedActionIndex < actions.size()) {
            BattleAction modifiedAction = actions.get(modifiedActionIndex);
            applyActionToHP(modifiedAction);
            
            // Update the modified action's description with correct HP
            updateActionDescription(modifiedAction, modifiedActionIndex);
            
            System.out.println("Applied modified action " + (modifiedActionIndex+1) + ". HP: F1=" + fighter1HP + ", F2=" + fighter2HP);
        }
        
        // Check if battle should end after the modified action
        if (fighter1HP <= 0 || fighter2HP <= 0) {
            // Battle ends here - remove all subsequent actions
            currentBattle.removeActionsFrom(modifiedActionIndex + 1);
            
            // Update winner
            Character newWinner = fighter1HP > fighter2HP ? originalBattle.getFighter1() : originalBattle.getFighter2();
            currentBattle.setWinner(newWinner);
            
            System.out.println("Battle ended early due to modification. New winner: " + newWinner.getName());
            return;
        }
        
        // If battle continues, regenerate subsequent actions
        regenerateActionsFrom(modifiedActionIndex + 1);
    }
    
    private void updateActionDescription(BattleAction action, int actionIndex) {
        // Create proper description with both fighters' HP info
        String hpSuffix = " (hpA=" + fighter1HP + ", hpB=" + fighter2HP + ")";
        
        // Build complete description
        String baseDescription = action.getActor().getName() + " " + action.getActionType() + " " + 
                               action.getTarget().getName() + " for " + action.getDamage() + " damage";
        String newDescription = baseDescription + hpSuffix;
        
        // Create a new action with the updated description
        BattleAction updatedAction = new BattleAction(
            action.getRound(),
            action.getActor(),
            action.getTarget(),
            action.getActionType(),
            newDescription,
            action.getDamage()
        );
        
        currentBattle.replaceAction(actionIndex, updatedAction);
        
        System.out.println("Updated action " + (actionIndex + 1) + ": " + newDescription);
    }
    
    private void smoothUpdateFromModification(int modifiedActionIndex) {
        System.out.println("Setting up for manual navigation from action " + (modifiedActionIndex + 1));
        
        // Keep everything before the modification exactly as it was
        // Update ALL lines from the modification point onward with new HP values
        
        String currentText = combatDisplay.getText();
        String[] lines = currentText.split("\n");
        
        // Build new text: keep lines before modification, update from modification onward
        StringBuilder newText = new StringBuilder();
        
        // Always keep the header
        newText.append("Starting battle replay...").append("\n");
        
        // Keep all action lines before the modification exactly as they were
        // Note: lines[0] is header, so action i is at lines[i+1]
        for (int i = 0; i < modifiedActionIndex; i++) {
            int lineIndex = i + 1; // +1 because lines[0] is the header
            if (lineIndex < lines.length) {
                newText.append(lines[lineIndex]).append("\n");
            }
        }
        
        // Now update ALL lines from the modification point onward with correct HP values
        List<BattleAction> actions = currentBattle.getActions();
        for (int i = modifiedActionIndex; i < actions.size(); i++) {
            BattleAction action = actions.get(i);
            if (action != null) {
                newText.append("[").append(i + 1).append("] ").append(action.getDescription()).append("\n");
            }
        }
        
        // Set the complete updated text
        combatDisplay.setText(newText.toString());
        
        // Set current position to show all the updated content
        currentActionIndex = actions.size();
        
        // Update display elements
        updateDisplay();
        
        // Auto-scroll to the modified line
        combatDisplay.setCaretPosition(combatDisplay.getDocument().getLength());
        
        // Set status to indicate the update is complete
        int updatedActions = actions.size() - modifiedActionIndex;
        statusLabel.setText("Timeline updated! " + updatedActions + " actions recalculated. Use Step Back/Forward to navigate.");
    }
    
    private void applyActionToHP(BattleAction action) {
        if (action.getTarget().equals(originalBattle.getFighter2())) {
            fighter2HP = Math.max(0, fighter2HP - action.getDamage());
        } else {
            fighter1HP = Math.max(0, fighter1HP - action.getDamage());
        }
    }
    
    private void regenerateActionsFrom(int startIndex) {
        // Remove all actions from startIndex onward
        currentBattle.removeActionsFrom(startIndex);
        
        // Simulate the rest of the battle
        int actionNumber = startIndex + 1;
        int turn = startIndex;
        
        System.out.println("Regenerating battle from action " + actionNumber + ". Current HP: F1=" + fighter1HP + ", F2=" + fighter2HP);
        
        while (fighter1HP > 0 && fighter2HP > 0 && actionNumber <= 50) { // Limit to prevent infinite loops
            Character attacker, target;
            
            if (turn % 2 == 0) {
                attacker = originalBattle.getFighter1();
                target = originalBattle.getFighter2();
            } else {
                attacker = originalBattle.getFighter2();
                target = originalBattle.getFighter1();
            }
            
            // Calculate damage (similar to CombatEngine logic)
            int attackPower = attacker.getStrength() + (int)(Math.random() * 10);
            int damage = Math.max(1, attackPower - target.getAgility() / 2);
            
            // Apply damage
            if (target.equals(originalBattle.getFighter2())) {
                fighter2HP = Math.max(0, fighter2HP - damage);
            } else {
                fighter1HP = Math.max(0, fighter1HP - damage);
            }
            
            // Create action description with both fighters' HP info
            String description = String.format("%s attacks %s for %d damage (hpA=%d, hpB=%d)",
                attacker.getName(),
                target.getName(),
                damage,
                fighter1HP,
                fighter2HP
            );
            
            // Create and add new action
            BattleAction newAction = new BattleAction(
                actionNumber,
                attacker,
                target,
                "attacks",
                description,
                damage
            );
            
            currentBattle.addAction(newAction);
            
            actionNumber++;
            turn++;
        }
        
        // Update winner
        Character newWinner = fighter1HP > fighter2HP ? originalBattle.getFighter1() : originalBattle.getFighter2();
        currentBattle.setWinner(newWinner);
        
        System.out.println("Battle regeneration complete. Final HP: F1=" + fighter1HP + ", F2=" + fighter2HP);
        System.out.println("New winner: " + newWinner.getName());
    }
    
    private void modifyCurrentAction() {
        if (currentActionIndex <= 0 || currentActionIndex > currentBattle.getActions().size()) {
            JOptionPane.showMessageDialog(this, "No action selected to modify!", "No Action", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the action to modify (previous action since currentActionIndex is after execution)
        BattleAction actionToModify = currentBattle.getActions().get(currentActionIndex - 1);
        
        // Open modification dialog
        ActionModificationDialog dialog = new ActionModificationDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), 
            actionToModify
        );
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            BattleAction modifiedAction = dialog.getModifiedAction();
            int actionIndex = currentActionIndex - 1;
            
            // Debug output
            System.out.println("=== MODIFYING ACTION ===");
            System.out.println("Original: " + actionToModify.getDescription());
            System.out.println("Modified: " + modifiedAction.getDescription());
            System.out.println("Action index: " + actionIndex);
            System.out.println("Original damage: " + actionToModify.getDamage() + " -> New damage: " + modifiedAction.getDamage());
            
            // Replace the action in current battle
            currentBattle.replaceAction(actionIndex, modifiedAction);
            
            // Recalculate the battle from this point (but don't reset display)
            recalculateBattleFromModification(actionIndex);
            
            // Smoothly update the display from the modification point only
            smoothUpdateFromModification(actionIndex);
            
            // Enable save variant button
            saveVariantBtn.setEnabled(true);
            statusLabel.setText("Timeline updated from action " + (actionIndex + 1) + "!");
            
            System.out.println("========================");
        }
    }
    
    private void saveVariant() {
        if (currentBattle.equals(originalBattle)) {
            JOptionPane.showMessageDialog(this, "No modifications to save!", "No Changes", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String variantName = JOptionPane.showInputDialog(this, 
            "Enter a name for this battle variant:", 
            "Save Battle Variant", 
            JOptionPane.QUESTION_MESSAGE);
            
        if (variantName != null && !variantName.trim().isEmpty()) {
            // Create a new battle history as variant
            BattleHistory variant = cloneBattle(currentBattle);
            
            // Add " (Variant: name)" to distinguish from original
            String originalName = variant.getBattleName();
            if (!originalName.contains("(Variant:")) {
                // Only add variant suffix if not already a variant
                variant = new BattleHistory(variant.getFighter1(), variant.getFighter2()) {
                    @Override
                    public String getBattleName() {
                        return originalName + " (Variant: " + variantName.trim() + ")";
                    }
                };
                
                // Copy actions and winner
                for (BattleAction action : currentBattle.getActions()) {
                    variant.addAction(action);
                }
                variant.setWinner(currentBattle.getWinner());
            }
            
            // Save the variant
            historyManager.addBattle(variant);
            
            saveVariantBtn.setEnabled(false);
            statusLabel.setText("Variant '" + variantName + "' saved successfully!");
            
            // Refresh main history if callback available
            if (callback != null) {
                callback.onVariantSaved();
            }
            
            JOptionPane.showMessageDialog(this, 
                "Battle variant '" + variantName + "' saved to history!", 
                "Variant Saved", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}