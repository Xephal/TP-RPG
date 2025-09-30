package rpg.mvc;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rpg.history.BattleAction;

public class ActionModificationDialog extends JDialog {
    private final BattleAction originalAction;
    private BattleAction modifiedAction;
    private boolean confirmed;
    
    // UI Components
    private JTextField damageField;
    private JTextArea descriptionArea;
    private JComboBox<String> actionTypeCombo;
    private JLabel actorLabel;
    private JLabel targetLabel;
    
    public ActionModificationDialog(Frame parent, BattleAction action) {
        super(parent, "Modify Action #" + action.getRound() + " (Auto-Generated)", true);
        this.originalAction = action;
        this.modifiedAction = null;
        this.confirmed = false;
        
        initializeUI();
        loadActionData();
        setupEventHandlers();
        
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Actor (read-only)
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Actor:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        actorLabel = new JLabel(originalAction.getActor().getName());
        actorLabel.setFont(actorLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(actorLabel, gbc);
        
        // Target (read-only)
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Target:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        targetLabel = new JLabel(originalAction.getTarget().getName());
        targetLabel.setFont(targetLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(targetLabel, gbc);
        
        // Action Type
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Action Type:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        actionTypeCombo = new JComboBox<>(new String[]{"attacks", "defends", "uses power", "critical hit", "misses"});
        mainPanel.add(actionTypeCombo, gbc);
        
        // Damage
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Damage:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        damageField = new JTextField();
        damageField.setColumns(10);
        mainPanel.add(damageField, gbc);
        
        // Description (read-only, auto-generated)
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(new JLabel("Description (Auto):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(getBackground());
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        mainPanel.add(scrollPane, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton confirmBtn = new JButton("Apply Changes");
        JButton cancelBtn = new JButton("Cancel");
        JButton resetBtn = new JButton("Reset to Original");
        
        buttonPanel.add(resetBtn);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(confirmBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Event handlers for buttons
        confirmBtn.addActionListener(e -> confirmChanges());
        cancelBtn.addActionListener(e -> dispose());
        resetBtn.addActionListener(e -> loadActionData());
    }
    
    private void loadActionData() {
        actionTypeCombo.setSelectedItem(originalAction.getActionType());
        damageField.setText(String.valueOf(originalAction.getDamage()));
        // Trigger description update
        updateDescriptionPreview();
    }
    
    private void setupEventHandlers() {
        // Update description automatically when other fields change
        ActionListener updateDescription = e -> updateDescriptionPreview();
        
        actionTypeCombo.addActionListener(updateDescription);
        damageField.addCaretListener(e -> updateDescriptionPreview());
    }
    
    private void updateDescriptionPreview() {
        try {
            String actionType = (String) actionTypeCombo.getSelectedItem();
            int damage = Integer.parseInt(damageField.getText());
            
            String newDescription = String.format("%s %s %s for %d damage", 
                originalAction.getActor().getName(),
                actionType,
                originalAction.getTarget().getName(),
                damage);
                
            // Always update the description since it's auto-generated
            descriptionArea.setText(newDescription);
        } catch (NumberFormatException ex) {
            // Keep current description if damage is invalid
        }
    }
    
    private void confirmChanges() {
        try {
            String actionType = (String) actionTypeCombo.getSelectedItem();
            int damage = Integer.parseInt(damageField.getText());
            String description = descriptionArea.getText();
            
            if (damage < 0) {
                JOptionPane.showMessageDialog(this, "Damage cannot be negative!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (description.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Description cannot be empty!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create modified action
            modifiedAction = new BattleAction(
                originalAction.getRound(),
                originalAction.getActor(),
                originalAction.getTarget(),
                actionType,
                description,
                damage
            );
            
            confirmed = true;
            dispose();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for damage!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public BattleAction getModifiedAction() {
        return modifiedAction;
    }
    
    public BattleAction getOriginalAction() {
        return originalAction;
    }
}