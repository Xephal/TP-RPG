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
import rpg.decorator.CharacterDecorator;
import rpg.decorator.FireResistance;
import rpg.decorator.Invisibility;
import rpg.decorator.Telepathy;
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
    // Character Management (ajoute cette ligne)
    private JButton deleteBtn;

    // Decorators checkboxes
    private JCheckBox invisibilityBox;
    private JCheckBox fireResistanceBox;
    private JCheckBox telepathyBox;

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
            JOptionPane.showMessageDialog(frame, "Select a character to delete.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Delete '" + selected.getName() + "' ?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        try {
            boolean ok = dao.remove(selected);
            if (!ok) {
                JOptionPane.showMessageDialog(frame, "Deletion failed (not found).", "Delete",
                        JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, "Error while deleting: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
                // NEW: activer/désactiver le bouton delete selon la sélection
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
        deleteBtn = new JButton("Delete Selected"); // NEW
        deleteBtn.setEnabled(false); // NEW: désactivé tant qu’aucune sélection
        deleteBtn.addActionListener(e -> deleteSelectedCharacter()); // NEW: action de suppression
        leftBottom.add(addBtn); // NEW
        leftBottom.add(deleteBtn); // NEW
        leftPanel.add(leftBottom, BorderLayout.SOUTH); // NEW

        // Right column: Character details/editor
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Character Details"));
        rightPanel.setPreferredSize(new Dimension(400, 0));

        // Character form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Strength:"), gbc);
        gbc.gridx = 1;
        strSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        formPanel.add(strSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Agility:"), gbc);
        gbc.gridx = 1;
        agiSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        formPanel.add(agiSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
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

        // Create layout similar to InteractiveBattleReplay
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Combat display (left side) - style noir/vert comme le replay
        combatLogArea = new JTextArea(20, 50);
        combatLogArea.setEditable(false);
        combatLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        combatLogArea.setBackground(Color.BLACK);
        combatLogArea.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(combatLogArea);
        logScroll.setBorder(new TitledBorder("Combat Replay"));

        // Status panel (right side) - identique au replay
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

        // Turn counter (sans révéler le total pour créer du suspense)
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Max Stat Points:"), gbc);
        gbc.gridx = 1;
        maxStatPointsSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxStatPoints(), 1, 100, 1));
        panel.add(maxStatPointsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Max Characters Per Group:"), gbc);
        gbc.gridx = 1;
        maxCharactersSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxCharactersPerGroup(), 1, 50, 1));
        panel.add(maxCharactersSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Max Groups Per Army:"), gbc);
        gbc.gridx = 1;
        maxGroupsSpinner = new JSpinner(new SpinnerNumberModel(settings.getMaxGroupsPerArmy(), 1, 20, 1));
        panel.add(maxGroupsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
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
                    if (child instanceof CharacterTreeNode ctn
                            && ctn.getCharacter().getName().equals(toRemove.getName())) {
                        partyNode.remove(k);
                    }
                }
            }
        }
        treeModel.reload(rootNode);
    }

    private void updateCharacterInArmyTree(Character updatedCharacter) {
        // Parcourt l'arbre et met à jour les CharacterTreeNode qui portent ce nom
        boolean updated = false;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode armyNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            for (int j = 0; j < armyNode.getChildCount(); j++) {
                DefaultMutableTreeNode partyNode = (DefaultMutableTreeNode) armyNode.getChildAt(j);
                for (int k = 0; k < partyNode.getChildCount(); k++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) partyNode.getChildAt(k);
                    if (child instanceof CharacterTreeNode ctn
                            && ctn.getCharacter().getName().equals(updatedCharacter.getName())) {
                        // Remplacer le nœud par un nouveau avec le personnage mis à jour
                        partyNode.remove(k);
                        partyNode.insert(new CharacterTreeNode(updatedCharacter), k);
                        updated = true;
                    }
                }
            }
        }
        if (updated) {
            treeModel.reload(rootNode);
        }
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
            JOptionPane.showMessageDialog(frame, "Please select an Army to add a Party to", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        // Can only add parties to Army nodes
        if (!(selectedNode instanceof ArmyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Parties can only be added to Armies", "Invalid Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check max groups per army limit
        int currentPartyCount = selectedNode.getChildCount();
        int maxGroupsPerArmy = GameSettings.getInstance().getMaxGroupsPerArmy();
        if (currentPartyCount >= maxGroupsPerArmy) {
            JOptionPane.showMessageDialog(frame,
                    "Cannot add party: Maximum parties per army is " + maxGroupsPerArmy +
                            ". This army already has " + currentPartyCount + " parties.",
                    "Limit Exceeded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String partyName = JOptionPane.showInputDialog(frame, "Enter Party name:", "Create Party",
                JOptionPane.PLAIN_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, "Please select a Party to add a Character to", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        // Can only add characters to Party nodes
        if (!(selectedNode instanceof PartyTreeNode)) {
            JOptionPane.showMessageDialog(frame, "Characters can only be added to Parties", "Invalid Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check max characters per group limit
        int currentCharacterCount = selectedNode.getChildCount();
        int maxCharactersPerGroup = GameSettings.getInstance().getMaxCharactersPerGroup();
        if (currentCharacterCount >= maxCharactersPerGroup) {
            JOptionPane.showMessageDialog(frame,
                    "Cannot add character: Maximum characters per party is " + maxCharactersPerGroup +
                            ". This party already has " + currentCharacterCount + " characters.",
                    "Limit Exceeded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show character selection dialog
        java.util.List<Character> availableCharacters = dao.findAll();
        if (availableCharacters.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No characters available. Create some characters first!",
                    "No Characters", JOptionPane.WARNING_MESSAGE);
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
                characters[0]);

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
            JOptionPane.showMessageDialog(frame, "Please select a node to remove", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        // Cannot remove root
        if (selectedNode == rootNode) {
            JOptionPane.showMessageDialog(frame, "Cannot remove the root node", "Invalid Operation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to remove '" + selectedNode.getUserObject() + "'?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);

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
            TreePath armyPath = new TreePath(new Object[] { rootNode, rootNode.getChildAt(i) });
            hierarchyTree.expandPath(armyPath);
        }
    }

    private void createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create the history tree
        historyRootNode = new DefaultMutableTreeNode("Battle History");
        historyTreeModel = new DefaultTreeModel(historyRootNode);
        battleHistoryTree = new JTree(historyTreeModel);

        // Configure tree
        battleHistoryTree.setRootVisible(false);
        battleHistoryTree.setShowsRootHandles(true);
        battleHistoryTree.setCellRenderer(new BattleHistoryTreeRenderer());

        // Add double-click listener for battle details
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

        // Button panel
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

        // Instructions
        JPanel instructionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        instructionsPanel.add(new JLabel(
                "Double-click battle to see details. Select battle and click 'Replay' for interactive mode."));

        panel.add(treeScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(instructionsPanel, BorderLayout.SOUTH);

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
                    JOptionPane.showMessageDialog(frame, "Character created successfully with decorators!", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Create character with selected decorators
                    String name = nameField.getText().trim();
                    int str = (Integer) strSpinner.getValue();
                    int agi = (Integer) agiSpinner.getValue();
                    int intel = (Integer) intSpinner.getValue();

                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "Please enter a character name", "Validation Error",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    Character baseCharacter = controller.buildCharacter(name, str, agi, intel);
                    Character characterWithDecorators = applySelectedDecorators(baseCharacter);
                    dao.save(characterWithDecorators);
                    JOptionPane.showMessageDialog(frame, "Character created successfully!", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
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

                // Update character in army tree as well
                updateCharacterInArmyTree(newCharacterWithDecorators);

                JOptionPane.showMessageDialog(frame, "Character updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
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
                    JOptionPane.showMessageDialog(frame, "Please enter a character name first", "Name Required",
                            JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, "Error creating character: " + e.getMessage(), "Creation Error",
                    JOptionPane.ERROR_MESSAGE);
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

            // Update character in army tree as well
            updateCharacterInArmyTree(newCharacterWithDecorators);

            // Update the character list model without losing selection
            for (int i = 0; i < characterListModel.size(); i++) {
                Character character = characterListModel.get(i);
                if (character.getName().equals(newCharacterWithDecorators.getName())) {
                    characterListModel.set(i, newCharacterWithDecorators);
                    break;
                }
            }

        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error updating character: " + e.getMessage(), "Update Error",
                    JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, "Please select two fighters", "Combat Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Initialize the live battle interface (style comme le replay)
        initializeLiveBattle(f1, f2);

        // Create new battle history and simulate combat (but don't display yet)
        liveBattleHistory = battleHistoryManager.startNewBattle(f1, f2);
        combatEngine.setCurrentBattle(liveBattleHistory);

        // Simulate combat completely (to get all actions)
        Character winner = combatEngine.simulate(f1, f2);
        liveBattleHistory.setWinner(winner);

        // Clear the battle link
        combatEngine.setCurrentBattle(null);

        // Start progressive animation of the battle
        startLiveBattleAnimation();

        // Refresh the history display
        refreshBattleHistory();
    }

    private void initializeLiveBattle(Character f1, Character f2) {
        // Clear and setup the combat log avec style replay
        combatLogArea.setText("Starting battle replay...\n");
        combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());

        // Initialize HP values (same formula as CombatEngine)
        liveFighter1MaxHP = Math.max(10, f1.getStrength() * 10 + f1.getIntelligence() * 2);
        liveFighter2MaxHP = Math.max(10, f2.getStrength() * 10 + f2.getIntelligence() * 2);
        liveFighter1HP = liveFighter1MaxHP;
        liveFighter2HP = liveFighter2MaxHP;

        // Setup HP bars with fighter names
        liveFighter1HPBar.setMaximum(liveFighter1MaxHP);
        liveFighter1HPBar.setValue(liveFighter1HP);
        liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
        liveFighter1HPBar.setForeground(Color.GREEN);

        liveFighter2HPBar.setMaximum(liveFighter2MaxHP);
        liveFighter2HPBar.setValue(liveFighter2HP);
        liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);
        liveFighter2HPBar.setForeground(Color.GREEN);

        // Reset turn counter
        currentTurn = 0;
        liveTurnLabel.setText("Tour: " + currentTurn);
    }

    private void updateLiveBattleStatus(String actionDescription) {
        // Update turn counter
        liveTurnLabel.setText("Tour: " + currentTurn);

        // Extract HP values from action description
        // Format: "Q attacks C for 5 damage (hpA=55, hpB=51)"
        if (actionDescription.contains("hpA=") && actionDescription.contains("hpB=")) {
            try {
                // Extract hpA
                int hpAStart = actionDescription.indexOf("hpA=") + 4;
                int hpAEnd = actionDescription.indexOf(",", hpAStart);
                if (hpAEnd == -1)
                    hpAEnd = actionDescription.indexOf(")", hpAStart);
                liveFighter1HP = Integer.parseInt(actionDescription.substring(hpAStart, hpAEnd));

                // Extract hpB
                int hpBStart = actionDescription.indexOf("hpB=") + 4;
                int hpBEnd = actionDescription.indexOf(")", hpBStart);
                liveFighter2HP = Integer.parseInt(actionDescription.substring(hpBStart, hpBEnd));

                // Update HP bars
                liveFighter1HPBar.setValue(liveFighter1HP);
                liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
                liveFighter1HPBar.setForeground(liveFighter1HP > liveFighter1MaxHP * 0.3 ? Color.GREEN : Color.RED);

                liveFighter2HPBar.setValue(liveFighter2HP);
                liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);
                liveFighter2HPBar.setForeground(liveFighter2HP > liveFighter2MaxHP * 0.3 ? Color.GREEN : Color.RED);

            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                // Si l'extraction échoue, on continue sans mettre à jour les HP
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
        // Army created but not added to old list anymore
        armyNameField.setText("");
        eventBus.notifyObservers("ARMY_CREATED", army.getName());

        // Suggest using the new tree interface
        JOptionPane.showMessageDialog(frame, "Army created! Use the new Army tab for hierarchical management.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
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

    // Battle history tree node classes
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

    // Battle history tree cell renderer
    private static class BattleHistoryTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof BattleHistoryNode) {
                setIcon(null); // Battle icon handled in toString
            } else if (value instanceof ActionNode) {
                setIcon(null); // Action icon handled in toString
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

        int newMaxStatPoints = (Integer) maxStatPointsSpinner.getValue();
        int newMaxCharactersPerGroup = (Integer) maxCharactersSpinner.getValue();
        int newMaxGroupsPerArmy = (Integer) maxGroupsSpinner.getValue();

        // Validation des conflits existants
        StringBuilder conflicts = new StringBuilder();

        // Vérifier les stats des personnages existants
        if (newMaxStatPoints < settings.getMaxStatPoints()) {
            java.util.List<Character> conflictingCharacters = new java.util.ArrayList<>();
            for (Character c : dao.findAll()) {
                int totalStats = c.getStrength() + c.getAgility() + c.getIntelligence();
                if (totalStats > newMaxStatPoints) {
                    conflictingCharacters.add(c);
                }
            }
            if (!conflictingCharacters.isEmpty()) {
                conflicts.append("- ").append(conflictingCharacters.size())
                        .append(" character(s) exceed the new max stat points limit (")
                        .append(newMaxStatPoints).append("):\n");
                for (Character c : conflictingCharacters) {
                    int total = c.getStrength() + c.getAgility() + c.getIntelligence();
                    conflicts.append("  • ").append(c.getName())
                            .append(" (Total: ").append(total).append(")\n");
                }
            }
        }

        // Vérifier les armées existantes avec trop de parties
        if (newMaxGroupsPerArmy < settings.getMaxGroupsPerArmy()) {
            int conflictingArmies = 0;
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                DefaultMutableTreeNode armyNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                if (armyNode.getChildCount() > newMaxGroupsPerArmy) {
                    conflictingArmies++;
                }
            }
            if (conflictingArmies > 0) {
                conflicts.append("- ").append(conflictingArmies)
                        .append(" army/armies exceed the new max parties per army limit (")
                        .append(newMaxGroupsPerArmy).append(")\n");
            }
        }

        // Vérifier les parties existantes avec trop de personnages
        if (newMaxCharactersPerGroup < settings.getMaxCharactersPerGroup()) {
            int conflictingParties = 0;
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                DefaultMutableTreeNode armyNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                for (int j = 0; j < armyNode.getChildCount(); j++) {
                    DefaultMutableTreeNode partyNode = (DefaultMutableTreeNode) armyNode.getChildAt(j);
                    if (partyNode.getChildCount() > newMaxCharactersPerGroup) {
                        conflictingParties++;
                    }
                }
            }
            if (conflictingParties > 0) {
                conflicts.append("- ").append(conflictingParties)
                        .append(" party/parties exceed the new max characters per party limit (")
                        .append(newMaxCharactersPerGroup).append(")\n");
            }
        }

        // Si des conflits existent, demander confirmation
        if (conflicts.length() > 0) {
            String message = "Warning: Applying these settings will conflict with existing data:\n\n" +
                    conflicts.toString() +
                    "\nDo you want to proceed anyway? Existing data will remain but may violate the new rules.";

            int choice = JOptionPane.showConfirmDialog(frame, message, "Settings Conflict",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice != JOptionPane.YES_OPTION) {
                return; // Annuler l'application des paramètres
            }
        }

        // Appliquer les nouveaux paramètres
        settings.setMaxStatPoints(newMaxStatPoints);
        settings.setMaxCharactersPerGroup(newMaxCharactersPerGroup);
        settings.setMaxGroupsPerArmy(newMaxGroupsPerArmy);

        String successMessage = "Settings applied successfully";
        if (conflicts.length() > 0) {
            successMessage += "\n\nNote: Some existing data may now violate the new rules. Consider reviewing and updating as needed.";
        }

        JOptionPane.showMessageDialog(frame, successMessage, "Settings", JOptionPane.INFORMATION_MESSAGE);
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
        // Old method - now redirects to new system
        refreshBattleHistory();
    }

    private void replayHistory() {
        combatLogArea.append("\n--- Replaying commands ---\n");
        combatEngine.getCommandHistory().replay();
        combatLogArea.append("--- Replay complete ---\n");
    }

    // New advanced battle history methods
    private void refreshBattleHistory() {
        historyRootNode.removeAllChildren();

        for (BattleHistory battle : battleHistoryManager.getAllBattles()) {
            BattleHistoryNode battleNode = new BattleHistoryNode(battle);

            // Add action nodes
            for (BattleAction action : battle.getActions()) {
                ActionNode actionNode = new ActionNode(action);
                battleNode.add(actionNode);
            }

            historyRootNode.add(battleNode);
        }

        historyTreeModel.reload();

        // Expand all battles by default
        for (int i = 0; i < historyRootNode.getChildCount(); i++) {
            TreePath battlePath = new TreePath(new Object[] { historyRootNode, historyRootNode.getChildAt(i) });
            battleHistoryTree.expandPath(battlePath);
        }
    }

    private void startLiveBattleAnimation() {
        if (liveBattleHistory == null || liveBattleHistory.getActions().isEmpty()) {
            combatLogArea.append("No actions to display.\n");
            return;
        }

        // Stop any existing animation
        if (liveAnimationTimer != null && liveAnimationTimer.isRunning()) {
            liveAnimationTimer.stop();
        }

        // Set flag to ignore Observer events during animation
        isAnimatingCombat = true;

        // Clear the combat log
        combatLogArea.setText("");

        // Reset animation state
        currentActionIndex = 0;
        liveFighter1HP = liveFighter1MaxHP;
        liveFighter2HP = liveFighter2MaxHP;
        currentTurn = 0;

        // Update initial HP bars
        updateLiveBattleHP();

        // Start animation timer (1 second delay between actions)
        liveAnimationTimer = new Timer(1000, e -> {
            if (currentActionIndex < liveBattleHistory.getActions().size()) {
                displayNextLiveAction();
                currentActionIndex++;
            } else {
                // Animation finished
                liveAnimationTimer.stop();

                // Display winner
                Character winner = liveBattleHistory.getWinner();
                if (winner != null) {
                    combatLogArea.append("\n=== Winner: " + winner.getName() + " ===\n");
                }
                combatLogArea.append(
                        "Battle saved to history with " + liveBattleHistory.getActions().size() + " actions.\n");

                // Re-enable Observer events
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

        // Update turn counter if necessary
        if (action.getRound() > currentTurn) {
            currentTurn = action.getRound();
            liveTurnLabel.setText("Turn " + currentTurn);
        }

        // Display the action in combat log
        String actionText = "Turn " + action.getRound() + ": " + action.getDescription();
        combatLogArea.append(actionText + "\n");
        combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());

        // Update HP based on action type
        updateLiveActionHP(action);

        // Update HP bars
        updateLiveBattleHP();
    }

    private void updateLiveActionHP(BattleAction action) {
        // Use the damage directly from the action
        int damage = action.getDamage();

        if (damage > 0) {
            // Determine which fighter took damage based on target
            Character target = action.getTarget();

            if (target.getName().equals(liveBattleHistory.getFighter1().getName())) {
                // Fighter1 takes damage
                liveFighter1HP = Math.max(0, liveFighter1HP - damage);
            } else if (target.getName().equals(liveBattleHistory.getFighter2().getName())) {
                // Fighter2 takes damage
                liveFighter2HP = Math.max(0, liveFighter2HP - damage);
            }
        }
    }

    private void updateLiveBattleHP() {
        // Update HP bars with current HP values
        if (liveFighter1HPBar != null && liveFighter2HPBar != null) {
            // Set the actual HP values (not percentages)
            liveFighter1HPBar.setValue(liveFighter1HP);
            liveFighter2HPBar.setValue(liveFighter2HP);

            // Update HP text
            liveFighter1HPBar.setString(liveFighter1HP + "/" + liveFighter1MaxHP + " HP");
            liveFighter2HPBar.setString(liveFighter2HP + "/" + liveFighter2MaxHP + " HP");

            // Calculate percentages for color determination
            int hp1Percentage = liveFighter1MaxHP > 0 ? (liveFighter1HP * 100) / liveFighter1MaxHP : 0;
            int hp2Percentage = liveFighter2MaxHP > 0 ? (liveFighter2HP * 100) / liveFighter2MaxHP : 0;

            // Update colors based on HP percentage
            liveFighter1HPBar.setForeground(getHPColor(hp1Percentage));
            liveFighter2HPBar.setForeground(getHPColor(hp2Percentage));
        }
    }

    private Color getHPColor(int hpPercentage) {
        if (hpPercentage > 60) {
            return new Color(0, 200, 0); // Vert vif
        } else if (hpPercentage > 30) {
            return new Color(255, 200, 0); // Jaune/orange
        } else {
            return new Color(220, 0, 0); // Rouge
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
            JOptionPane.showMessageDialog(frame, "Please select a battle to replay", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        if (!(selectedNode instanceof BattleHistoryNode)) {
            JOptionPane.showMessageDialog(frame, "Please select a battle (not an action) to replay",
                    "Invalid Selection", JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            battleHistoryManager.clearHistory();
            refreshBattleHistory();
        }
    }

    private void exportSelectedBattle() {
        TreePath selectionPath = battleHistoryTree.getSelectionPath();
        if (selectionPath == null) {
            JOptionPane.showMessageDialog(frame, "Please select a battle to export", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();

        if (!(selectedNode instanceof BattleHistoryNode)) {
            JOptionPane.showMessageDialog(frame, "Please select a battle to export", "Invalid Selection",
                    JOptionPane.WARNING_MESSAGE);
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
        details.append("Winner: ").append(battle.getWinner() != null ? battle.getWinner().getName() : "Unknown")
                .append("\n");
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
                action.getDescription());

        JOptionPane.showMessageDialog(frame, details, "Action Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openInteractiveReplay(BattleHistory battle) {
        // Launch the interactive replay window with callback to refresh history
        InteractiveBattleReplay replayWindow = new InteractiveBattleReplay(
                frame,
                battle,
                battleHistoryManager,
                () -> refreshBattleHistory() // Callback to refresh history tree when variant is saved
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
                    // Ignore combat actions during animation (will be displayed progressively)
                    if (isAnimatingCombat) {
                        return;
                    }
                    // Format the action like in the replay (with turn number)
                    currentTurn++;
                    String actionText = "[" + currentTurn + "] " + data.toString();
                    combatLogArea.append(actionText + "\n");
                    combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());

                    // Update HP bars and turn counter (extract HP from action text)
                    updateLiveBattleStatus(data.toString());
                    break;
                case "COMBAT_START":
                    // Ignore combat start during animation
                    if (isAnimatingCombat) {
                        return;
                    }
                    combatLogArea.append("=== " + data.toString() + " ===\n");
                    break;
                case "COMBAT_END":
                    // Ignore combat end during animation
                    if (isAnimatingCombat) {
                        return;
                    }
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