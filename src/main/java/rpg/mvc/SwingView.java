package rpg.mvc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import rpg.builder.InvalidCharacterException;
import rpg.composite.Army;
import rpg.core.Character;
import rpg.core.CombatEngine;
import rpg.dao.DAO;

// Nouveaux décorateurs
import rpg.decorator.CharacterDecorator;
import rpg.decorator.Surcharge;
import rpg.decorator.Furtivite;
import rpg.decorator.Soin;
import rpg.decorator.BouleDeFeu;

import rpg.history.AdvancedBattleHistoryManager;
import rpg.history.BattleAction;
import rpg.history.BattleHistory;
import rpg.observer.EventBus;
import rpg.settings.GameSettings;

public class SwingView extends View {
    private final GameController controller;
    private final EventBus eventBus;
    private final DAO<Character> dao;

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
    private JLabel liveTurnLabel;
    private JProgressBar liveFighter1HPBar;
    private JProgressBar liveFighter2HPBar;
    private int liveFighter1HP, liveFighter2HP;
    private int liveFighter1MaxHP, liveFighter2MaxHP;
    private int currentTurn = 0;
    private Timer liveAnimationTimer;
    private BattleHistory liveBattleHistory;
    private int currentActionIndex = 0;
    private boolean isAnimatingCombat = false;

    // Settings
    private JSpinner maxStatPointsSpinner, maxCharactersSpinner, maxGroupsSpinner;

    // Army Management
    private JTree hierarchyTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTextField armyNameField;

    // Command History
    private JTree battleHistoryTree;
    private DefaultTreeModel historyTreeModel;
    private DefaultMutableTreeNode historyRootNode;
    private AdvancedBattleHistoryManager battleHistoryManager;
    private CombatEngine combatEngine;

    // Character editing state
    private Character currentEditingCharacter;
    private boolean isCreatingNew;
    private Character temporaryCharacter; // For creation with decorators

    // UI buttons for character management
    private JButton saveBtn;
    private JButton cancelBtn;
    private JButton deleteBtn;

    private JCheckBox surchargeBox;
    private JCheckBox furtiviteBox;
    private JCheckBox soinBox;
    private JCheckBox bouleDeFeuBox;

    public SwingView(GameController controller, EventBus eventBus, DAO<Character> dao) {
        super("SwingView");
        this.controller = controller;
        this.eventBus = eventBus;
        this.dao = dao;
        this.combatEngine = new CombatEngine(eventBus);
        this.battleHistoryManager = new AdvancedBattleHistoryManager();
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

    private void deleteSelectedCharacter() {
        rpg.core.Character selected = characterList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(frame, "Select a character to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Delete '" + selected.getName() + "' ?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = dao.remove(selected);
            if (!ok) {
                JOptionPane.showMessageDialog(frame, "Deletion failed (not found).", "Delete", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // UI refresh
            refreshCharacterList();
            clearCharacterForm();
            deleteBtn.setEnabled(false);

            // Supprimer aussi du tree "Armies"
            removeCharacterFromArmyTree(selected);

            JOptionPane.showMessageDialog(frame, "Character deleted.", "Delete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error while deleting: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
                rpg.core.Character selected = characterList.getSelectedValue();
                if (deleteBtn != null) {
                    deleteBtn.setEnabled(selected != null);
                }
                if (selected != null) {
                    loadCharacterToEdit(selected);
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(characterList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        // BOTTOM LEFT: Add + Delete
        JButton addBtn = new JButton("Add New Character");
        addBtn.addActionListener(e -> startNewCharacterCreation());
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        deleteBtn = new JButton("Delete Selected");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedCharacter());
        leftBottom.add(addBtn);
        leftBottom.add(deleteBtn);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

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

        // Decorators section: anciens + nouveaux
        JPanel decoratorPanel = new JPanel();
        decoratorPanel.setLayout(new BoxLayout(decoratorPanel, BoxLayout.Y_AXIS));
        decoratorPanel.setBorder(new TitledBorder("Skills / Decorators"));

        // Nouvelles compétences
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.setBorder(new TitledBorder("New"));

        surchargeBox = new JCheckBox("Surcharge");
        furtiviteBox = new JCheckBox("Furtivité");
        soinBox = new JCheckBox("Soin");
        bouleDeFeuBox = new JCheckBox("Boule de Feu");
        newPanel.add(surchargeBox);
        newPanel.add(Box.createVerticalStrut(5));
        newPanel.add(furtiviteBox);
        newPanel.add(Box.createVerticalStrut(5));
        newPanel.add(soinBox);
        newPanel.add(Box.createVerticalStrut(5));
        newPanel.add(bouleDeFeuBox);

        // Listeners
        surchargeBox.addActionListener(e -> updateDecorators());
        furtiviteBox.addActionListener(e -> updateDecorators());
        soinBox.addActionListener(e -> updateDecorators());
        bouleDeFeuBox.addActionListener(e -> updateDecorators());

        // Layout right panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel decoratorsColumns = new JPanel();
        decoratorsColumns.setLayout(new GridBagLayout());
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(5, 10, 5, 10);
        dgbc.gridx = 0; dgbc.gridy = 0; dgbc.fill = GridBagConstraints.HORIZONTAL; dgbc.weightx = 1;
        dgbc.gridx = 1; dgbc.gridy = 0;
        decoratorsColumns.add(newPanel, dgbc);

        centerPanel.add(decoratorsColumns, BorderLayout.CENTER);
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

        // Create layout similar to InteractiveBattleReplay
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Combat display (left side)
        combatLogArea = new JTextArea(20, 50);
        combatLogArea.setEditable(false);
        combatLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        combatLogArea.setBackground(Color.BLACK);
        combatLogArea.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(combatLogArea);
        logScroll.setBorder(new TitledBorder("Combat Replay"));

        // Status panel (right side)
        JPanel statusPanel = createLiveBattleStatusPanel();

        centerPanel.add(logScroll, BorderLayout.CENTER);
        centerPanel.add(statusPanel, BorderLayout.EAST);

        panel.add(setupPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        tabbedPane.addTab("Combat", panel);
    }

    private JPanel createLiveBattleStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Battle Status"));
        panel.setPreferredSize(new Dimension(200, 0));

        // Turn counter
        liveTurnLabel = new JLabel("Tour: 0", SwingConstants.CENTER);
        liveTurnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        liveTurnLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(liveTurnLabel);

        panel.add(Box.createVerticalStrut(10));

        // Fighter 1 HP
        JLabel f1Label = new JLabel("Fighter 1:");
        f1Label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(f1Label);

        liveFighter1HPBar = new JProgressBar(0, 100);
        liveFighter1HPBar.setValue(100);
        liveFighter1HPBar.setStringPainted(true);
        liveFighter1HPBar.setString("100 / 100");
        liveFighter1HPBar.setForeground(Color.GREEN);
        panel.add(liveFighter1HPBar);

        panel.add(Box.createVerticalStrut(5));

        // Fighter 2 HP
        JLabel f2Label = new JLabel("Fighter 2:");
        f2Label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(f2Label);

        liveFighter2HPBar = new JProgressBar(0, 100);
        liveFighter2HPBar.setValue(100);
        liveFighter2HPBar.setStringPainted(true);
        liveFighter2HPBar.setString("100 / 100");
        liveFighter2HPBar.setForeground(Color.GREEN);
        panel.add(liveFighter2HPBar);

        return panel;
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

    private void removeCharacterFromArmyTree(Character toRemove) {
        // Parcourt l’arbre et supprime les CharacterTreeNode qui portent ce nom
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode armyNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            for (int j = 0; j < armyNode.getChildCount(); j++) {
                DefaultMutableTreeNode partyNode = (DefaultMutableTreeNode) armyNode.getChildAt(j);
                for (int k = partyNode.getChildCount() - 1; k >= 0; k--) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) partyNode.getChildAt(k);
                    if (child instanceof CharacterTreeNode ctn && ctn.getCharacter().getName().equals(toRemove.getName())) {
                        partyNode.remove(k);
                    }
                }
            }
        }
        treeModel.reload(rootNode);
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

        hierarchyTree.setRootVisible(false);
        hierarchyTree.setShowsRootHandles(true);
        hierarchyTree.setEditable(false);

        // Custom cell renderer
        hierarchyTree.setCellRenderer(new CustomTreeCellRenderer());

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

        if (!(selectedNode instanceof ArmyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Parties can only be added to Armies", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String partyName = JOptionPane.showInputDialog(frame, "Enter Party name:", "Create Party", JOptionPane.PLAIN_MESSAGE);
        if (partyName != null && !partyName.trim().isEmpty()) {
            PartyTreeNode partyNode = new PartyTreeNode(partyName.trim());
            selectedNode.add(partyNode);
            treeModel.reload(selectedNode);
            hierarchyTree.expandPath(selectionPath);
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

        if (!(selectedNode instanceof PartyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Characters can only be added to Parties", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
            hierarchyTree.expandPath(selectionPath);
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
        treeModel.reload(rootNode);
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            TreePath armyPath = new TreePath(new Object[]{rootNode, rootNode.getChildAt(i)});
            hierarchyTree.expandPath(armyPath);
        }
    }

    private void createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());

        historyRootNode = new DefaultMutableTreeNode("Battle History");
        historyTreeModel = new DefaultTreeModel(historyRootNode);
        battleHistoryTree = new JTree(historyTreeModel);

        battleHistoryTree.setRootVisible(false);
        battleHistoryTree.setShowsRootHandles(true);
        battleHistoryTree.setCellRenderer(new BattleHistoryTreeRenderer());

        battleHistoryTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleBattleDoubleClick();
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(battleHistoryTree);
        treeScroll.setBorder(new TitledBorder("Battle History"));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshBtn = new JButton("Refresh");
        JButton replayBtn = new JButton("Replay Battle");
        JButton clearBtn = new JButton("Clear History");
        JButton exportBtn = new JButton("Export Battle");

        refreshBtn.addActionListener(e -> refreshBattleHistory());
        replayBtn.addActionListener(e -> replaySelectedBattle());
        clearBtn.addActionListener(e -> clearBattleHistory());
        exportBtn.addActionListener(e -> exportSelectedBattle());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(replayBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(exportBtn);

        JPanel instructionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionsPanel.add(new JLabel("Double-click battle to see details. Select battle and click 'Replay' for interactive mode."));

        panel.add(treeScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(instructionsPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("History", panel);
    }

    private void startNewCharacterCreation() {
        currentEditingCharacter = null;
        temporaryCharacter = null;

        nameField.setText("");
        strSpinner.setValue(5);
        agiSpinner.setValue(5);
        intSpinner.setValue(5);

        // Reset checkboxes
        setAllDecoratorCheckboxesEnabled(true);
        setAllDecoratorCheckboxesSelected(false);

        isCreatingNew = true;
        nameField.setEnabled(true);
        strSpinner.setEnabled(true);
        agiSpinner.setEnabled(true);
        intSpinner.setEnabled(true);

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

        updateDecoratorCheckboxes(character);

        nameField.setEnabled(false);
        strSpinner.setEnabled(true);
        agiSpinner.setEnabled(true);
        intSpinner.setEnabled(true);

        setAllDecoratorCheckboxesEnabled(true);

        saveBtn.setText("Update Character");
        saveBtn.setVisible(true);
        cancelBtn.setVisible(true);
    }

    private void saveCurrentCharacter() {
        try {
            if (isCreatingNew) {
                if (temporaryCharacter != null) {
                    dao.save(temporaryCharacter);
                    JOptionPane.showMessageDialog(frame, "Character created successfully with decorators!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
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
                int str = (Integer) strSpinner.getValue();
                int agi = (Integer) agiSpinner.getValue();
                int intel = (Integer) intSpinner.getValue();

                Character baseCharacter = findBaseCharacter(currentEditingCharacter);
                Character newBaseCharacter = controller.buildCharacter(baseCharacter.getName(), str, agi, intel);

                Character newCharacterWithDecorators = applySelectedDecorators(newBaseCharacter);

                dao.update(currentEditingCharacter, newCharacterWithDecorators);
                currentEditingCharacter = newCharacterWithDecorators;

                JOptionPane.showMessageDialog(frame, "Character updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshCharacterList();

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

        setAllDecoratorCheckboxesSelected(false);
        setAllDecoratorCheckboxesEnabled(false);

        currentEditingCharacter = null;
        temporaryCharacter = null;
        isCreatingNew = false;

        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
    }

    private void updateDecoratorCheckboxes(Character character) {
        // Détection exacte de la chaîne de décorateurs
        surchargeBox.setSelected(hasDecorator(character, Surcharge.class));
        furtiviteBox.setSelected(hasDecorator(character, Furtivite.class));
        soinBox.setSelected(hasDecorator(character, Soin.class));
        bouleDeFeuBox.setSelected(hasDecorator(character, BouleDeFeu.class));
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
            updateTemporaryCharacterDecorators();
        } else if (currentEditingCharacter != null) {
            updateExistingCharacterDecorators();
        }
    }

    private void updateTemporaryCharacterDecorators() {
        try {
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

            Character baseCharacter = findBaseCharacter(temporaryCharacter);
            Character decoratedCharacter = applySelectedDecorators(baseCharacter);
            temporaryCharacter = decoratedCharacter;

        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error creating character: " + e.getMessage(), "Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateExistingCharacterDecorators() {
        try {
            int str = (Integer) strSpinner.getValue();
            int agi = (Integer) agiSpinner.getValue();
            int intel = (Integer) intSpinner.getValue();

            Character baseCharacter = findBaseCharacter(currentEditingCharacter);
            Character newBaseCharacter = controller.buildCharacter(baseCharacter.getName(), str, agi, intel);

            Character newCharacterWithDecorators = applySelectedDecorators(newBaseCharacter);

            dao.update(currentEditingCharacter, newCharacterWithDecorators);
            currentEditingCharacter = newCharacterWithDecorators;

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

        // Ordre d’application déterministe:
        // D’abord anciens, ensuite nouveaux (ou inverse, ça reste cohérent du moment que c’est stable)
        // Ici: Legacy -> New

        if (surchargeBox.isSelected()) {
            result = new Surcharge(result);
        }
        if (furtiviteBox.isSelected()) {
            result = new Furtivite(result);
        }
        if (soinBox.isSelected()) {
            result = new Soin(result);
        }
        if (bouleDeFeuBox.isSelected()) {
            result = new BouleDeFeu(result);
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

        initializeLiveBattle(f1, f2);

        liveBattleHistory = battleHistoryManager.startNewBattle(f1, f2);
        combatEngine.setCurrentBattle(liveBattleHistory);

        Character winner = combatEngine.simulate(f1, f2);
        liveBattleHistory.setWinner(winner);

        combatEngine.setCurrentBattle(null);

        startLiveBattleAnimation();
        refreshBattleHistory();
    }

    private void initializeLiveBattle(Character f1, Character f2) {
        combatLogArea.setText("Starting battle replay...\n");
        combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());

        liveFighter1MaxHP = Math.max(10, f1.getStrength() * 10 + f1.getIntelligence() * 2);
        liveFighter2MaxHP = Math.max(10, f2.getStrength() * 10 + f2.getIntelligence() * 2);
        liveFighter1HP = liveFighter1MaxHP;
        liveFighter2HP = liveFighter2MaxHP;

        liveFighter1HPBar.setMaximum(liveFighter1MaxHP);
        liveFighter1HPBar.setValue(liveFighter1HP);
        liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
        liveFighter1HPBar.setForeground(Color.GREEN);

        liveFighter2HPBar.setMaximum(liveFighter2MaxHP);
        liveFighter2HPBar.setValue(liveFighter2HP);
        liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);
        liveFighter2HPBar.setForeground(Color.GREEN);

        currentTurn = 0;
        liveTurnLabel.setText("Tour: " + currentTurn);
    }

    private void updateLiveBattleStatus(String actionDescription) {
        liveTurnLabel.setText("Tour: " + currentTurn);

        if (actionDescription.contains("hpA=") && actionDescription.contains("hpB=")) {
            try {
                int hpAStart = actionDescription.indexOf("hpA=") + 4;
                int hpAEnd = actionDescription.indexOf(",", hpAStart);
                if (hpAEnd == -1) hpAEnd = actionDescription.indexOf(")", hpAStart);
                liveFighter1HP = Integer.parseInt(actionDescription.substring(hpAStart, hpAEnd));

                int hpBStart = actionDescription.indexOf("hpB=") + 4;
                int hpBEnd = actionDescription.indexOf(")", hpBStart);
                liveFighter2HP = Integer.parseInt(actionDescription.substring(hpBStart, hpBEnd));

                liveFighter1HPBar.setValue(liveFighter1HP);
                liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
                liveFighter1HPBar.setForeground(liveFighter1HP > liveFighter1MaxHP * 0.3 ? Color.GREEN : Color.RED);

                liveFighter2HPBar.setValue(liveFighter2HP);
                liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);
                liveFighter2HPBar.setForeground(liveFighter2HP > liveFighter2MaxHP * 0.3 ? Color.GREEN : Color.RED);

            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                System.err.println("Could not extract HP from: " + actionDescription);
            }
        }
    }

    private void createArmy() {
        String name = armyNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter army name", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Army army = new Army(name);
        armyNameField.setText("");
        eventBus.notifyObservers("ARMY_CREATED", army.getName());

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
                setIcon(null);
            } else if (value instanceof PartyTreeNode) {
                setIcon(null);
            } else if (value instanceof CharacterTreeNode) {
                setIcon(null);
            }

            return this;
        }
    }

    // Battle history node classes
    private static class BattleHistoryNode extends DefaultMutableTreeNode {
        private final BattleHistory battle;

        public BattleHistoryNode(BattleHistory battle) {
            super(battle.getSummary());
            this.battle = battle;
        }

        public BattleHistory getBattle() {
            return battle;
        }

        @Override
        public String toString() {
            return "[BATTLE] " + battle.getSummary();
        }
    }

    private static class ActionNode extends DefaultMutableTreeNode {
        private final BattleAction action;

        public ActionNode(BattleAction action) {
            super(action.getFormattedAction());
            this.action = action;
        }

        public BattleAction getAction() {
            return action;
        }

        @Override
        public String toString() {
            String modifiable = action.isModifiable() ? "[EDIT]" : "[FIXED]";
            return modifiable + " " + action.getFormattedAction();
        }
    }

    private static class BattleHistoryTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof BattleHistoryNode) {
                setIcon(null);
            } else if (value instanceof ActionNode) {
                setIcon(null);
                ActionNode actionNode = (ActionNode) value;
                if (!actionNode.getAction().isModifiable()) {
                    setForeground(java.awt.Color.GRAY);
                }
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
        refreshBattleHistory();
    }

    private void replayHistory() {
        combatLogArea.append("\n--- Replaying commands ---\n");
        combatEngine.getCommandHistory().replay();
        combatLogArea.append("--- Replay complete ---\n");
    }

    private void refreshBattleHistory() {
        historyRootNode.removeAllChildren();

        for (BattleHistory battle : battleHistoryManager.getAllBattles()) {
            BattleHistoryNode battleNode = new BattleHistoryNode(battle);

            for (BattleAction action : battle.getActions()) {
                ActionNode actionNode = new ActionNode(action);
                battleNode.add(actionNode);
            }

            historyRootNode.add(battleNode);
        }

        historyTreeModel.reload();

        for (int i = 0; i < historyRootNode.getChildCount(); i++) {
            TreePath battlePath = new TreePath(new Object[]{historyRootNode, historyRootNode.getChildAt(i)});
            battleHistoryTree.expandPath(battlePath);
        }
    }

    private void startLiveBattleAnimation() {
        if (liveBattleHistory == null || liveBattleHistory.getActions().isEmpty()) {
            combatLogArea.append("No actions to display.\n");
            return;
        }

        if (liveAnimationTimer != null && liveAnimationTimer.isRunning()) {
            liveAnimationTimer.stop();
        }

        isAnimatingCombat = true;
        combatLogArea.setText("");

        currentActionIndex = 0;
        liveFighter1HP = liveFighter1MaxHP;
        liveFighter2HP = liveFighter2MaxHP;
        currentTurn = 0;

        updateLiveBattleHP();

        liveAnimationTimer = new Timer(1000, e -> {
            if (currentActionIndex < liveBattleHistory.getActions().size()) {
                displayNextLiveAction();
                currentActionIndex++;
            } else {
                liveAnimationTimer.stop();
                Character winner = liveBattleHistory.getWinner();
                if (winner != null) {
                    combatLogArea.append("\n=== Winner: " + winner.getName() + " ===\n");
                }
                combatLogArea.append("Battle saved to history with " + liveBattleHistory.getActions().size() + " actions.\n");
                isAnimatingCombat = false;
            }
        });

        liveAnimationTimer.start();
    }

    private void displayNextLiveAction() {
        if (currentActionIndex >= liveBattleHistory.getActions().size()) {
            return;
        }

        BattleAction action = liveBattleHistory.getActions().get(currentActionIndex);

        if (action.getRound() > currentTurn) {
            currentTurn = action.getRound();
            liveTurnLabel.setText("Turn " + currentTurn);
        }

        String actionText = "Turn " + action.getRound() + ": " + action.getDescription();
        combatLogArea.append(actionText + "\n");
        combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());

        updateLiveActionHP(action);
        updateLiveBattleHP();
    }

    private void updateLiveActionHP(BattleAction action) {
        int damage = action.getDamage();

        if (damage > 0) {
            Character target = action.getTarget();

            if (target.getName().equals(liveBattleHistory.getFighter1().getName())) {
                liveFighter1HP = Math.max(0, liveFighter1HP - damage);
            } else if (target.getName().equals(liveBattleHistory.getFighter2().getName())) {
                liveFighter2HP = Math.max(0, liveFighter2HP - damage);
            }
        }
    }

    private void updateLiveBattleHP() {
        if (liveFighter1HPBar != null && liveFighter2HPBar != null) {
            liveFighter1HPBar.setValue(liveFighter1HP);
            liveFighter2HPBar.setValue(liveFighter2HP);

            liveFighter1HPBar.setString(liveFighter1HP + "/" + liveFighter1MaxHP + " HP");
            liveFighter2HPBar.setString(liveFighter2HP + "/" + liveFighter2MaxHP + " HP");

            int hp1Percentage = liveFighter1MaxHP > 0 ? (liveFighter1HP * 100) / liveFighter1MaxHP : 0;
            int hp2Percentage = liveFighter2MaxHP > 0 ? (liveFighter2HP * 100) / liveFighter2MaxHP : 0;

            liveFighter1HPBar.setForeground(getHPColor(hp1Percentage));
            liveFighter2HPBar.setForeground(getHPColor(hp2Percentage));
        }
    }

    private Color getHPColor(int hpPercentage) {
        if (hpPercentage > 60) {
            return new Color(0, 200, 0);
        } else if (hpPercentage > 30) {
            return new Color(255, 200, 0);
        } else {
            return new Color(220, 0, 0);
        }
    }

    private void handleBattleDoubleClick() {
        TreePath selectionPath = battleHistoryTree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

            if (selectedNode instanceof BattleHistoryNode) {
                BattleHistory battle = ((BattleHistoryNode) selectedNode).getBattle();
                showBattleDetails(battle);
            } else if (selectedNode instanceof ActionNode) {
                BattleAction action = ((ActionNode) selectedNode).getAction();
                showActionDetails(action);
            }
        }
    }

    private void replaySelectedBattle() {
        TreePath selectionPath = battleHistoryTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select a battle to replay", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        if (!(selectedNode instanceof BattleHistoryNode)) {
            JOptionPane.showMessageDialog(frame, "Please select a battle (not an action) to replay", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BattleHistory battle = ((BattleHistoryNode) selectedNode).getBattle();
        openInteractiveReplay(battle);
    }

    private void clearBattleHistory() {
        int result = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to clear all battle history?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            battleHistoryManager.clearHistory();
            refreshBattleHistory();
        }
    }

    private void exportSelectedBattle() {
        TreePath selectionPath = battleHistoryTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select a battle to export", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        if (!(selectedNode instanceof BattleHistoryNode)) {
            JOptionPane.showMessageDialog(frame, "Please select a battle to export", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BattleHistory battle = ((BattleHistoryNode) selectedNode).getBattle();
        exportBattleToText(battle);
    }

    private void showBattleDetails(BattleHistory battle) {
        StringBuilder details = new StringBuilder();
        details.append("Battle Details\n");
        details.append("=============\n");
        details.append("Battle: ").append(battle.getBattleName()).append("\n");
        details.append("Date: ").append(battle.getFormattedTimestamp()).append("\n");
        details.append("Fighter 1: ").append(battle.getFighter1().getName()).append("\n");
        details.append("Fighter 2: ").append(battle.getFighter2().getName()).append("\n");
        details.append("Winner: ").append(battle.getWinner() != null ? battle.getWinner().getName() : "Unknown").append("\n");
        details.append("Actions: ").append(battle.getActions().size()).append("\n\n");

        details.append("Action Details:\n");
        for (BattleAction action : battle.getActions()) {
            details.append(action.getFormattedAction()).append("\n");
        }

        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(frame, scrollPane, "Battle Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showActionDetails(BattleAction action) {
        String details = String.format(
                "Action Details\n" +
                        "Round: %d\n" +
                        "Actor: %s\n" +
                        "Target: %s\n" +
                        "Action: %s\n" +
                        "Damage: %d\n" +
                        "Modifiable: %s\n" +
                        "Description: %s",
                action.getRound(),
                action.getActor().getName(),
                action.getTarget().getName(),
                action.getActionType(),
                action.getDamage(),
                action.isModifiable() ? "Yes" : "No",
                action.getDescription()
        );

        JOptionPane.showMessageDialog(frame, details, "Action Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openInteractiveReplay(BattleHistory battle) {
        InteractiveBattleReplay replayWindow = new InteractiveBattleReplay(
                frame,
                battle,
                battleHistoryManager,
                () -> refreshBattleHistory()
        );
        replayWindow.setVisible(true);
    }

    private void exportBattleToText(BattleHistory battle) {
        StringBuilder export = new StringBuilder();
        export.append("Battle Export: ").append(battle.getBattleName()).append("\n");
        export.append("Date: ").append(battle.getFormattedTimestamp()).append("\n");
        export.append("==========================================\n\n");

        for (BattleAction action : battle.getActions()) {
            export.append(action.getFormattedAction()).append("\n");
        }

        export.append("\n==========================================\n");
        export.append("Winner: ").append(battle.getWinner() != null ? battle.getWinner().getName() : "Unknown");

        JTextArea textArea = new JTextArea(export.toString());
        textArea.setEditable(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.selectAll();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(frame, scrollPane, "Export Battle - Copy Text", JOptionPane.INFORMATION_MESSAGE);
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
                    if (isAnimatingCombat) {
                        return;
                    }
                    currentTurn++;
                    String actionText = "[" + currentTurn + "] " + data.toString();
                    combatLogArea.append(actionText + "\n");
                    combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());
                    updateLiveBattleStatus(data.toString());
                    break;
                case "COMBAT_START":
                    if (isAnimatingCombat) {
                        return;
                    }
                    combatLogArea.append("=== " + data.toString() + " ===\n");
                    break;
                case "COMBAT_END":
                    if (isAnimatingCombat) {
                        return;
                    }
                    combatLogArea.append("=== " + data.toString() + " ===\n");
                    break;
                default:
                    break;
            }
        });
    }

    private Character findBaseCharacter(Character character) {
        Character current = character;
        while (current instanceof CharacterDecorator) {
            current = ((CharacterDecorator) current).getWrappedCharacter();
        }
        return current;
    }

    @SuppressWarnings("unused")
    private Character reapplyDecorators(Character originalDecorated, Character newBase) {
        // Conserve l’ordre de la pile en partant de la base
        java.util.List<Class<?>> chain = new java.util.ArrayList<>();
        Character current = originalDecorated;
        while (current instanceof CharacterDecorator) {
            chain.add(0, current.getClass());
            current = ((CharacterDecorator) current).getWrappedCharacter();
        }

        Character result = newBase;
        for (Class<?> clazz : chain) {
            if (clazz == Surcharge.class) result = new Surcharge(result);
            else if (clazz == Furtivite.class) result = new Furtivite(result);
            else if (clazz == Soin.class) result = new Soin(result);
            else if (clazz == BouleDeFeu.class) result = new BouleDeFeu(result);
        }
        return result;
    }

    private void setAllDecoratorCheckboxesEnabled(boolean enabled) {
        surchargeBox.setEnabled(enabled);
        furtiviteBox.setEnabled(enabled);
        soinBox.setEnabled(enabled);
        bouleDeFeuBox.setEnabled(enabled);
    }

    private void setAllDecoratorCheckboxesSelected(boolean selected) {
        surchargeBox.setSelected(selected);
        furtiviteBox.setSelected(selected);
        soinBox.setSelected(selected);
        bouleDeFeuBox.setSelected(selected);
    }
}
