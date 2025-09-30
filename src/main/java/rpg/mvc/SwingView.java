package rpg.mvc;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import rpg.builder.InvalidCharacterException;
import rpg.composite.Army;
import rpg.core.Character;
import rpg.core.CombatEngine;
import rpg.dao.CharacterDAO;
import rpg.decorator.FireResistance;
import rpg.decorator.Invisibility;
import rpg.decorator.Telepathy;
import rpg.observer.EventBus;
import rpg.settings.GameSettings;

public class SwingView extends View {
    private final GameController controller;
    private final EventBus eventBus;
    private final CharacterDAO dao;
    
    private JFrame frame;
    private JTabbedPane tabbedPane;
    
    // Character Management
    private JTextField nameField;
    private JSpinner strSpinner, agiSpinner, intSpinner;
    private JList<Character> characterList;
    private DefaultListModel<Character> characterListModel;
    
    // Combat
    private JComboBox<Character> fighter1Combo, fighter2Combo;
    private JTextArea combatLogArea;
    
    // Settings
    private JSpinner maxStatPointsSpinner, maxCharactersSpinner, maxGroupsSpinner;
    
    // Army Management
    private JList<Army> armyList;
    private DefaultListModel<Army> armyListModel;
    private JTextField armyNameField;
    
    // Command History
    private JTextArea historyArea;
    private CombatEngine combatEngine;

    public SwingView(GameController controller, EventBus eventBus, CharacterDAO dao) {
        super("SwingView");
        this.controller = controller;
        this.eventBus = eventBus;
        this.dao = dao;
        this.combatEngine = new CombatEngine(eventBus);
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("RPG Character Manager - TP2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        
        createCharacterTab();
        createCombatTab();
        createSettingsTab();
        createArmyTab();
        createHistoryTab();
        
        frame.add(tabbedPane);
    }

    private void createCharacterTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Creation form
        JPanel createPanel = new JPanel(new GridBagLayout());
        createPanel.setBorder(new TitledBorder("Create Character"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(5, 5, 5, 5);
        createPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        createPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        createPanel.add(new JLabel("Strength:"), gbc);
        gbc.gridx = 1;
        strSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        createPanel.add(strSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        createPanel.add(new JLabel("Agility:"), gbc);
        gbc.gridx = 1;
        agiSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        createPanel.add(agiSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        createPanel.add(new JLabel("Intelligence:"), gbc);
        gbc.gridx = 1;
        intSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        createPanel.add(intSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton createBtn = new JButton("Create Character");
        createBtn.addActionListener(e -> createCharacter());
        createPanel.add(createBtn, gbc);
        
        // Decorators
        JPanel decoratorPanel = new JPanel(new FlowLayout());
        decoratorPanel.setBorder(new TitledBorder("Add Decorators"));
        JButton invisBtn = new JButton("Add Invisibility");
        JButton fireBtn = new JButton("Add Fire Resistance");
        JButton teleBtn = new JButton("Add Telepathy");
        
        invisBtn.addActionListener(e -> addDecorator("invisibility"));
        fireBtn.addActionListener(e -> addDecorator("fire"));
        teleBtn.addActionListener(e -> addDecorator("telepathy"));
        
        decoratorPanel.add(invisBtn);
        decoratorPanel.add(fireBtn);
        decoratorPanel.add(teleBtn);
        
        // Character list
        characterListModel = new DefaultListModel<>();
        characterList = new JList<>(characterListModel);
        characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(characterList);
        listScroll.setBorder(new TitledBorder("Characters"));
        
        panel.add(createPanel, BorderLayout.NORTH);
        panel.add(decoratorPanel, BorderLayout.CENTER);
        panel.add(listScroll, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Characters", panel);
    }

    private void createCombatTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Combat setup
        JPanel setupPanel = new JPanel(new FlowLayout());
        setupPanel.setBorder(new TitledBorder("Combat Setup"));
        
        fighter1Combo = new JComboBox<>();
        fighter2Combo = new JComboBox<>();
        JButton fightBtn = new JButton("Start Combat");
        
        fightBtn.addActionListener(e -> startCombat());
        
        setupPanel.add(new JLabel("Fighter 1:"));
        setupPanel.add(fighter1Combo);
        setupPanel.add(new JLabel("vs"));
        setupPanel.add(fighter2Combo);
        setupPanel.add(fightBtn);
        
        // Combat log
        combatLogArea = new JTextArea(20, 60);
        combatLogArea.setEditable(false);
        combatLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(combatLogArea);
        logScroll.setBorder(new TitledBorder("Combat Log"));
        
        panel.add(setupPanel, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);
        
        tabbedPane.addTab("Combat", panel);
    }

    private void createSettingsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        GameSettings settings = GameSettings.getInstance();
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Max Stat Points:"), gbc);
        gbc.gridx = 1;
        maxStatPointsSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxStatPoints(), 1, 100, 1));
        panel.add(maxStatPointsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Max Characters Per Group:"), gbc);
        gbc.gridx = 1;
        maxCharactersSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxCharactersPerGroup(), 1, 50, 1));
        panel.add(maxCharactersSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Max Groups Per Army:"), gbc);
        gbc.gridx = 1;
        maxGroupsSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxGroupsPerArmy(), 1, 20, 1));
        panel.add(maxGroupsSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton applyBtn = new JButton("Apply Settings");
        applyBtn.addActionListener(e -> applySettings());
        panel.add(applyBtn, gbc);
        
        tabbedPane.addTab("Settings", panel);
    }

    private void createArmyTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Army creation
        JPanel createPanel = new JPanel(new FlowLayout());
        createPanel.setBorder(new TitledBorder("Create Army"));
        
        armyNameField = new JTextField(15);
        JButton createArmyBtn = new JButton("Create Army");
        createArmyBtn.addActionListener(e -> createArmy());
        
        createPanel.add(new JLabel("Army Name:"));
        createPanel.add(armyNameField);
        createPanel.add(createArmyBtn);
        
        // Army list
        armyListModel = new DefaultListModel<>();
        armyList = new JList<>(armyListModel);
        JScrollPane armyScroll = new JScrollPane(armyList);
        armyScroll.setBorder(new TitledBorder("Armies"));
        
        panel.add(createPanel, BorderLayout.NORTH);
        panel.add(armyScroll, BorderLayout.CENTER);
        
        tabbedPane.addTab("Armies", panel);
    }

    private void createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        
        historyArea = new JTextArea(20, 60);
        historyArea.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(new TitledBorder("Command History"));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshBtn = new JButton("Refresh History");
        JButton replayBtn = new JButton("Replay Commands");
        JButton clearBtn = new JButton("Clear History");
        
        refreshBtn.addActionListener(e -> refreshHistory());
        replayBtn.addActionListener(e -> replayHistory());
        clearBtn.addActionListener(e -> clearHistory());
        
        buttonPanel.add(refreshBtn);
        buttonPanel.add(replayBtn);
        buttonPanel.add(clearBtn);
        
        panel.add(historyScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("History", panel);
    }

    private void createCharacter() {
        try {
            String name = nameField.getText().trim();
            int str = (Integer) strSpinner.getValue();
            int agi = (Integer) agiSpinner.getValue();
            int intel = (Integer) intSpinner.getValue();
            
            controller.createCharacter(name, str, agi, intel);
            refreshCharacterList();
            
            nameField.setText("");
            strSpinner.setValue(5);
            agiSpinner.setValue(5);
            intSpinner.setValue(5);
            
        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addDecorator(String type) {
        Character selected = characterList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(frame, "Please select a character first", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Character decorated = selected;
        switch (type) {
            case "invisibility":
                decorated = new Invisibility(selected);
                break;
            case "fire":
                decorated = new FireResistance(selected);
                break;
            case "telepathy":
                decorated = new Telepathy(selected);
                break;
        }
        
        // Update in DAO
        dao.save(decorated);
        refreshCharacterList();
        eventBus.notifyObservers("DECORATOR_APPLIED", decorated.getDescription());
    }

    private void startCombat() {
        Character f1 = (Character) fighter1Combo.getSelectedItem();
        Character f2 = (Character) fighter2Combo.getSelectedItem();
        
        if (f1 == null || f2 == null) {
            JOptionPane.showMessageDialog(frame, "Please select two fighters", "Combat Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        combatLogArea.setText("Starting combat...\n");
        Character winner = combatEngine.simulate(f1, f2);
        combatLogArea.append("\nWinner: " + winner.getName() + "\n");
        refreshHistory();
    }

    private void createArmy() {
        String name = armyNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter army name", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Army army = new Army(name);
        armyListModel.addElement(army);
        armyNameField.setText("");
        eventBus.notifyObservers("ARMY_CREATED", army.getName());
    }

    private void applySettings() {
        GameSettings settings = GameSettings.getInstance();
        settings.setMaxStatPoints((Integer) maxStatPointsSpinner.getValue());
        settings.setMaxCharactersPerGroup((Integer) maxCharactersSpinner.getValue());
        settings.setMaxGroupsPerArmy((Integer) maxGroupsSpinner.getValue());
        
        JOptionPane.showMessageDialog(frame, "Settings applied successfully", "Settings", JOptionPane.INFORMATION_MESSAGE);
        eventBus.notifyObservers("SETTINGS_CHANGED", "Settings updated");
    }

    private void refreshCharacterList() {
        characterListModel.clear();
        fighter1Combo.removeAllItems();
        fighter2Combo.removeAllItems();
        
        for (Character c : dao.findAll()) {
            characterListModel.addElement(c);
            fighter1Combo.addItem(c);
            fighter2Combo.addItem(c);
        }
    }

    private void refreshHistory() {
        historyArea.setText("");
        for (String command : combatEngine.getCommandHistory().getHistory()) {
            historyArea.append(command + "\n");
        }
    }

    private void replayHistory() {
        combatLogArea.append("\n--- Replaying commands ---\n");
        combatEngine.getCommandHistory().replay();
        combatLogArea.append("--- Replay complete ---\n");
    }

    private void clearHistory() {
        combatEngine.getCommandHistory().clear();
        historyArea.setText("");
        combatLogArea.append("History cleared.\n");
    }

    @Override
    public void render() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            refreshCharacterList();
        });
    }

    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, message, "Message", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void update(String eventType, Object data) {
        SwingUtilities.invokeLater(() -> {
            switch (eventType) {
                case "CHARACTER_CREATED":
                    refreshCharacterList();
                    break;
                case "COMBAT_ACTION":
                    combatLogArea.append(data.toString() + "\n");
                    combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());
                    break;
                case "COMBAT_START":
                    combatLogArea.append("=== " + data.toString() + " ===\n");
                    break;
                case "COMBAT_END":
                    combatLogArea.append("=== " + data.toString() + " ===\n");
                    break;
                default:
                    // Handle other events
                    break;
            }
        });
    }
}