package rpg.mvc;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.*;
import rpg.builder.InvalidCharacterException;
import rpg.composite.Army;
import rpg.core.Character;
import rpg.core.CombatEngine;
import rpg.dao.CharacterDAO;
import rpg.decorator.CharacterDecorator;
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
    private JTree hierarchyTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTextField armyNameField;
    
    // Command History
    private JTextArea historyArea;
    private CombatEngine combatEngine;
    
    // Character editing state
    private Character currentEditingCharacter;
    private boolean isCreatingNew;
    private Character temporaryCharacter; // For creation with decorators
    
    // UI buttons for character management
    private JButton saveBtn;
    private JButton cancelBtn;
    
    // Decorators checkboxes
    private JCheckBox invisibilityBox;
    private JCheckBox fireResistanceBox;
    private JCheckBox telepathyBox;

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
        
        // Left column: Character list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Characters"));
        leftPanel.setPreferredSize(new Dimension(300, 0));
        
        characterListModel = new DefaultListModel<>();
        characterList = new JList<>(characterListModel);
        characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        characterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Character selected = characterList.getSelectedValue();
                if (selected != null) {
                    loadCharacterToEdit(selected);
                }
            }
        });
        
        JScrollPane listScroll = new JScrollPane(characterList);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        
        JButton addBtn = new JButton("Add New Character");
        addBtn.addActionListener(e -> startNewCharacterCreation());
        leftPanel.add(addBtn, BorderLayout.SOUTH);
        
        // Right column: Character details/editor
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Character Details"));
        rightPanel.setPreferredSize(new Dimension(400, 0));
        
        // Character form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Strength:"), gbc);
        gbc.gridx = 1;
        strSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        formPanel.add(strSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Agility:"), gbc);
        gbc.gridx = 1;
        agiSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        formPanel.add(agiSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Intelligence:"), gbc);
        gbc.gridx = 1;
        intSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        formPanel.add(intSpinner, gbc);
        
        rightPanel.add(formPanel, BorderLayout.NORTH);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        saveBtn = new JButton("Save Character");
        cancelBtn = new JButton("Cancel");
        saveBtn.addActionListener(e -> saveCurrentCharacter());
        cancelBtn.addActionListener(e -> cancelEdit());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        // Decorators section
        JPanel decoratorPanel = new JPanel();
        decoratorPanel.setLayout(new BoxLayout(decoratorPanel, BoxLayout.Y_AXIS));
        decoratorPanel.setBorder(new TitledBorder("Decorators"));
        
        invisibilityBox = new JCheckBox("Invisibility");
        fireResistanceBox = new JCheckBox("Fire Resistance");
        telepathyBox = new JCheckBox("Telepathy");
        
        // Add action listeners for immediate application
        invisibilityBox.addActionListener(e -> updateDecorators());
        fireResistanceBox.addActionListener(e -> updateDecorators());
        telepathyBox.addActionListener(e -> updateDecorators());
        
        decoratorPanel.add(invisibilityBox);
        decoratorPanel.add(Box.createVerticalStrut(5));
        decoratorPanel.add(fireResistanceBox);
        decoratorPanel.add(Box.createVerticalStrut(5));
        decoratorPanel.add(telepathyBox);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(decoratorPanel, BorderLayout.CENTER);
        rightPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Add both panels to main panel
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        
        // Initially clear the right panel
        clearCharacterForm();
        
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
        
        // Top panel with creation controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new TitledBorder("Army Management"));
        
        armyNameField = new JTextField(15);
        JButton createArmyBtn = new JButton("Create Army");
        JButton createPartyBtn = new JButton("Add Party");
        JButton addCharacterBtn = new JButton("Add Character");
        JButton removeBtn = new JButton("Remove Selected");
        
        createArmyBtn.addActionListener(e -> createArmyInTree());
        createPartyBtn.addActionListener(e -> addPartyToSelected());
        addCharacterBtn.addActionListener(e -> addCharacterToSelected());
        removeBtn.addActionListener(e -> removeSelectedNode());
        
        topPanel.add(new JLabel("Army Name:"));
        topPanel.add(armyNameField);
        topPanel.add(createArmyBtn);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(createPartyBtn);
        topPanel.add(addCharacterBtn);
        topPanel.add(removeBtn);
        
        // Create the tree
        rootNode = new DefaultMutableTreeNode("Armies");
        treeModel = new DefaultTreeModel(rootNode);
        hierarchyTree = new JTree(treeModel);
        
        // Configure tree appearance
        hierarchyTree.setRootVisible(false); // Hide root to show armies as top level
        hierarchyTree.setShowsRootHandles(true);
        hierarchyTree.setEditable(false);
        
        // Custom cell renderer for different node types
        hierarchyTree.setCellRenderer(new CustomTreeCellRenderer());
        
        // Add mouse listener for double-click actions
        hierarchyTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
        });
        
        JScrollPane treeScroll = new JScrollPane(hierarchyTree);
        treeScroll.setBorder(new TitledBorder("Army Hierarchy"));
        
        // Instructions panel
        JPanel instructionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionsPanel.add(new JLabel("Double-click to expand/collapse. Right-click for context menu."));
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(treeScroll, BorderLayout.CENTER);
        panel.add(instructionsPanel, BorderLayout.SOUTH);
        
        tabbedPane.addTab("Armies", panel);
    }
    
    // Tree management methods
    private void createArmyInTree() {
        String name = armyNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter army name", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Army army = new Army(name);
        ArmyTreeNode armyNode = new ArmyTreeNode(army);
        rootNode.add(armyNode);
        treeModel.reload(rootNode);
        
        // Expand the root to show the new army
        hierarchyTree.expandPath(new TreePath(rootNode.getPath()));
        
        armyNameField.setText("");
        eventBus.notifyObservers("ARMY_CREATED", army.getName());
    }
    
    private void addPartyToSelected() {
        TreePath selectionPath = hierarchyTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select an Army to add a Party to", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        
        // Can only add parties to Army nodes
        if (!(selectedNode instanceof ArmyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Parties can only be added to Armies", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String partyName = JOptionPane.showInputDialog(frame, "Enter Party name:", "Create Party", JOptionPane.PLAIN_MESSAGE);
        if (partyName != null && !partyName.trim().isEmpty()) {
            PartyTreeNode partyNode = new PartyTreeNode(partyName.trim());
            selectedNode.add(partyNode);
            treeModel.reload(selectedNode);
            
            // Expand the army to show the new party
            hierarchyTree.expandPath(selectionPath);
            
            // Refresh display to show updated power totals
            refreshTreeDisplay();
        }
    }
    
    private void addCharacterToSelected() {
        TreePath selectionPath = hierarchyTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select a Party to add a Character to", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        
        // Can only add characters to Party nodes
        if (!(selectedNode instanceof PartyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Characters can only be added to Parties", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Show character selection dialog
        java.util.List<Character> availableCharacters = dao.findAll();
        if (availableCharacters.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No characters available. Create some characters first!", "No Characters", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Character[] characters = availableCharacters.toArray(new Character[0]);
        Character selectedCharacter = (Character) JOptionPane.showInputDialog(
            frame,
            "Select a character to add:",
            "Add Character",
            JOptionPane.PLAIN_MESSAGE,
            null,
            characters,
            characters[0]
        );
        
        if (selectedCharacter != null) {
            CharacterTreeNode characterNode = new CharacterTreeNode(selectedCharacter);
            selectedNode.add(characterNode);
            treeModel.reload(selectedNode);
            
            // Expand the party to show the new character
            hierarchyTree.expandPath(selectionPath);
            
            // Refresh display to show updated power totals
            refreshTreeDisplay();
        }
    }
    
    private void removeSelectedNode() {
        TreePath selectionPath = hierarchyTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select a node to remove", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        
        // Cannot remove root
        if (selectedNode == rootNode) {
            JOptionPane.showMessageDialog(frame, "Cannot remove the root node", "Invalid Operation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(
            frame,
            "Are you sure you want to remove '" + selectedNode.getUserObject() + "'?",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            parent.remove(selectedNode);
            treeModel.reload(parent);
            
            // Refresh display to show updated power totals
            refreshTreeDisplay();
        }
    }
    
    private void handleDoubleClick() {
        TreePath selectionPath = hierarchyTree.getSelectionPath();
        if (selectionPath != null) {
            if (hierarchyTree.isExpanded(selectionPath)) {
                hierarchyTree.collapsePath(selectionPath);
            } else {
                hierarchyTree.expandPath(selectionPath);
            }
        }
    }
    
    private void refreshTreeDisplay() {
        // Force the tree to repaint to show updated power calculations
        treeModel.reload(rootNode);
        
        // Expand all armies by default
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            TreePath armyPath = new TreePath(new Object[]{rootNode, rootNode.getChildAt(i)});
            hierarchyTree.expandPath(armyPath);
        }
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

    private void startNewCharacterCreation() {
        currentEditingCharacter = null;
        temporaryCharacter = null;
        
        // Clear form first
        nameField.setText("");
        strSpinner.setValue(5);
        agiSpinner.setValue(5);
        intSpinner.setValue(5);
        
        // Clear decorator checkboxes
        invisibilityBox.setSelected(false);
        fireResistanceBox.setSelected(false);
        telepathyBox.setSelected(false);
        
        // Then set to creation mode and enable fields
        isCreatingNew = true;
        nameField.setEnabled(true);
        strSpinner.setEnabled(true);
        agiSpinner.setEnabled(true);
        intSpinner.setEnabled(true);
        
        // Enable decorator checkboxes
        invisibilityBox.setEnabled(true);
        fireResistanceBox.setEnabled(true);
        telepathyBox.setEnabled(true);
        
        // Show save/cancel buttons for creation
        saveBtn.setText("Save Character");
        saveBtn.setVisible(true);
        cancelBtn.setVisible(true);
    }

    private void loadCharacterToEdit(Character character) {
        isCreatingNew = false;
        currentEditingCharacter = character;
        temporaryCharacter = null;
        
        nameField.setText(character.getName());
        strSpinner.setValue(character.getStrength());
        agiSpinner.setValue(character.getAgility());
        intSpinner.setValue(character.getIntelligence());
        
        // Update decorator checkboxes based on current character
        updateDecoratorCheckboxes(character);
        
        nameField.setEnabled(false); // Can't change name of existing character
        strSpinner.setEnabled(true);
        agiSpinner.setEnabled(true);
        intSpinner.setEnabled(true);
        
        // Enable decorator checkboxes
        invisibilityBox.setEnabled(true);
        fireResistanceBox.setEnabled(true);
        telepathyBox.setEnabled(true);
        
        // Show update button for editing mode
        saveBtn.setText("Update Character");
        saveBtn.setVisible(true);
        cancelBtn.setVisible(true);
    }

    private void saveCurrentCharacter() {
        try {
            if (isCreatingNew) {
                if (temporaryCharacter != null) {
                    // Save the temporary character with all decorators
                    dao.save(temporaryCharacter);
                    JOptionPane.showMessageDialog(frame, "Character created successfully with decorators!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Create character with selected decorators
                    String name = nameField.getText().trim();
                    int str = (Integer) strSpinner.getValue();
                    int agi = (Integer) agiSpinner.getValue();
                    int intel = (Integer) intSpinner.getValue();
                    
                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "Please enter a character name", "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    Character baseCharacter = controller.buildCharacter(name, str, agi, intel);
                    Character characterWithDecorators = applySelectedDecorators(baseCharacter);
                    dao.save(characterWithDecorators);
                    JOptionPane.showMessageDialog(frame, "Character created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                refreshCharacterList();
                clearCharacterForm();
            } else {
                // Update mode - get current stats and apply decorators
                int str = (Integer) strSpinner.getValue();
                int agi = (Integer) agiSpinner.getValue();
                int intel = (Integer) intSpinner.getValue();
                
                // Create updated character with new stats
                Character baseCharacter = findBaseCharacter(currentEditingCharacter);
                Character newBaseCharacter = controller.buildCharacter(baseCharacter.getName(), str, agi, intel);
                
                // Apply selected decorators
                Character newCharacterWithDecorators = applySelectedDecorators(newBaseCharacter);
                
                // Update in database
                dao.update(currentEditingCharacter, newCharacterWithDecorators);
                currentEditingCharacter = newCharacterWithDecorators;
                
                JOptionPane.showMessageDialog(frame, "Character updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshCharacterList();
                
                // Maintain selection after update
                if (currentEditingCharacter != null) {
                    for (int i = 0; i < characterListModel.size(); i++) {
                        Character character = characterListModel.get(i);
                        if (character.getName().equals(currentEditingCharacter.getName())) {
                            characterList.setSelectedIndex(i);
                            loadCharacterToEdit(character);
                            break;
                        }
                    }
                }
            }
            
        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelEdit() {
        clearCharacterForm();
        characterList.clearSelection();
    }

    private void clearCharacterForm() {
        nameField.setText("");
        strSpinner.setValue(5);
        agiSpinner.setValue(5);
        intSpinner.setValue(5);
        
        nameField.setEnabled(false);
        strSpinner.setEnabled(false);
        agiSpinner.setEnabled(false);
        intSpinner.setEnabled(false);
        
        // Clear and disable decorator checkboxes
        invisibilityBox.setSelected(false);
        fireResistanceBox.setSelected(false);
        telepathyBox.setSelected(false);
        invisibilityBox.setEnabled(false);
        fireResistanceBox.setEnabled(false);
        telepathyBox.setEnabled(false);
        
        currentEditingCharacter = null;
        temporaryCharacter = null;
        isCreatingNew = false;
        
        // Hide save/cancel buttons
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
    }

    private void updateDecoratorCheckboxes(Character character) {
        // Check which decorators are applied
        boolean hasInvisibility = hasDecorator(character, Invisibility.class);
        boolean hasFireResistance = hasDecorator(character, FireResistance.class);
        boolean hasTelepathy = hasDecorator(character, Telepathy.class);
        
        // Temporarily disable events to avoid recursive calls
        invisibilityBox.setSelected(hasInvisibility);
        fireResistanceBox.setSelected(hasFireResistance);
        telepathyBox.setSelected(hasTelepathy);
    }
    
    private boolean hasDecorator(Character character, Class<?> decoratorClass) {
        Character current = character;
        while (current instanceof CharacterDecorator) {
            if (decoratorClass.isInstance(current)) {
                return true;
            }
            current = ((CharacterDecorator) current).getWrappedCharacter();
        }
        return false;
    }
    
    private void updateDecorators() {
        if (isCreatingNew) {
            // For new character creation, update temporary character
            updateTemporaryCharacterDecorators();
        } else if (currentEditingCharacter != null) {
            // For existing character, apply changes immediately
            updateExistingCharacterDecorators();
        }
    }
    
    private void updateTemporaryCharacterDecorators() {
        try {
            // Create base character if it doesn't exist
            if (temporaryCharacter == null) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a character name first", "Name Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int str = (Integer) strSpinner.getValue();
                int agi = (Integer) agiSpinner.getValue();
                int intel = (Integer) intSpinner.getValue();
                
                temporaryCharacter = controller.buildCharacter(name, str, agi, intel);
            }
            
            // Get base character without decorators
            Character baseCharacter = findBaseCharacter(temporaryCharacter);
            
            // Apply selected decorators
            Character decoratedCharacter = applySelectedDecorators(baseCharacter);
            temporaryCharacter = decoratedCharacter;
            
        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error creating character: " + e.getMessage(), "Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateExistingCharacterDecorators() {
        try {
            // Get base character stats from spinners
            int str = (Integer) strSpinner.getValue();
            int agi = (Integer) agiSpinner.getValue();
            int intel = (Integer) intSpinner.getValue();
            
            // Create new base character with current stats
            Character baseCharacter = findBaseCharacter(currentEditingCharacter);
            Character newBaseCharacter = controller.buildCharacter(baseCharacter.getName(), str, agi, intel);
            
            // Apply selected decorators
            Character newCharacterWithDecorators = applySelectedDecorators(newBaseCharacter);
            
            // Update in database
            dao.update(currentEditingCharacter, newCharacterWithDecorators);
            currentEditingCharacter = newCharacterWithDecorators;
            
            // Update the character list model without losing selection
            for (int i = 0; i < characterListModel.size(); i++) {
                Character character = characterListModel.get(i);
                if (character.getName().equals(newCharacterWithDecorators.getName())) {
                    characterListModel.set(i, newCharacterWithDecorators);
                    break;
                }
            }
            
        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error updating character: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private Character applySelectedDecorators(Character baseCharacter) {
        Character result = baseCharacter;
        
        if (invisibilityBox.isSelected()) {
            result = new Invisibility(result);
        }
        if (fireResistanceBox.isSelected()) {
            result = new FireResistance(result);
        }
        if (telepathyBox.isSelected()) {
            result = new Telepathy(result);
        }
        
        return result;
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
        // Army created but not added to old list anymore
        armyNameField.setText("");
        eventBus.notifyObservers("ARMY_CREATED", army.getName());
        
        // Suggest using the new tree interface
        JOptionPane.showMessageDialog(frame, "Army created! Use the new Army tab for hierarchical management.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Custom tree node classes
    private static class ArmyTreeNode extends DefaultMutableTreeNode {
        private final Army army;
        
        public ArmyTreeNode(Army army) {
            super(army.getName());
            this.army = army;
        }
        
        public Army getArmy() {
            return army;
        }
        
        @Override
        public String toString() {
            int totalPower = calculateTotalPower();
            return "[ARMY] " + army.getName() + " (" + getChildCount() + " parties, Power: " + totalPower + ")";
        }
        
        private int calculateTotalPower() {
            int total = 0;
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof PartyTreeNode) {
                    total += ((PartyTreeNode) child).calculateTotalPower();
                }
            }
            return total;
        }
    }
    
    private static class PartyTreeNode extends DefaultMutableTreeNode {
        private final String partyName;
        
        public PartyTreeNode(String partyName) {
            super(partyName);
            this.partyName = partyName;
        }
        
        public String getPartyName() {
            return partyName;
        }
        
        @Override
        public String toString() {
            int totalPower = calculateTotalPower();
            return "[PARTY] " + partyName + " (" + getChildCount() + " characters, Power: " + totalPower + ")";
        }
        
        public int calculateTotalPower() {
            int total = 0;
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CharacterTreeNode) {
                    total += ((CharacterTreeNode) child).getCharacter().getPowerLevel();
                }
            }
            return total;
        }
    }
    
    private static class CharacterTreeNode extends DefaultMutableTreeNode {
        private final Character character;
        
        public CharacterTreeNode(Character character) {
            super(character.getName());
            this.character = character;
        }
        
        public Character getCharacter() {
            return character;
        }
        
        @Override
        public String toString() {
            return "[CHAR] " + character.getName() + " (Power: " + character.getPowerLevel() + ")";
        }
    }
    
    // Custom tree cell renderer
    private static class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            if (value instanceof ArmyTreeNode) {
                setIcon(null); // Army icon handled in toString
            } else if (value instanceof PartyTreeNode) {
                setIcon(null); // Party icon handled in toString
            } else if (value instanceof CharacterTreeNode) {
                setIcon(null); // Character icon handled in toString
            }
            
            return this;
        }
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
    
    private Character findBaseCharacter(Character character) {
        // Strip all decorators to find the base character
        Character current = character;
        while (current instanceof CharacterDecorator) {
            current = ((CharacterDecorator) current).getWrappedCharacter();
        }
        return current;
    }
    
    private Character reapplyDecorators(Character originalDecorated, Character newBase) {
        // Get the decorator chain from the original character
        java.util.List<String> decorators = new java.util.ArrayList<>();
        Character current = originalDecorated;
        
        while (current instanceof CharacterDecorator) {
            CharacterDecorator decorator = (CharacterDecorator) current;
            if (decorator instanceof Invisibility) {
                decorators.add(0, "invisibility");
            } else if (decorator instanceof FireResistance) {
                decorators.add(0, "fire");
            } else if (decorator instanceof Telepathy) {
                decorators.add(0, "telepathy");
            }
            current = decorator.getWrappedCharacter();
        }
        
        // Reapply decorators to new base
        Character result = newBase;
        for (String decoratorType : decorators) {
            switch (decoratorType) {
                case "invisibility":
                    result = new Invisibility(result);
                    break;
                case "fire":
                    result = new FireResistance(result);
                    break;
                case "telepathy":
                    result = new Telepathy(result);
                    break;
            }
        }
        
        return result;
    }
}