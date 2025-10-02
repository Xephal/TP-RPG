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
import java.util.Random;

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

    private static final int FURTIVITE_DODGE_PERCENT = 70; // Chance d'esquive quand furtif


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

    // Combat UI (turn-based)
    private JComboBox<Character> fighter1Combo, fighter2Combo;
    private JTextArea combatLogArea;
    private JLabel liveTurnLabel;
    private JProgressBar liveFighter1HPBar;
    private JProgressBar liveFighter2HPBar;

    // Action buttons
    private JButton startCombatBtn;
    private JButton attackBtn, surchargeBtn, furtiviteBtn, soinBtn, bouleDeFeuBtn;

    // Live values
    private int liveFighter1HP, liveFighter2HP;
    private int liveFighter1MaxHP, liveFighter2MaxHP;
    private int currentTurn = 0;

    // Optional animation timer used elsewhere (kept but not used for auto combat now)
    private Timer liveAnimationTimer;
    private BattleHistory liveBattleHistory; // not filled by auto engine anymore, we keep for future use
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
    private Character temporaryCharacter;

    // UI buttons for character management
    private JButton saveBtn;
    private JButton cancelBtn;
    private JButton deleteBtn;

    // Decorators (character build)
    private JCheckBox surchargeBox;
    private JCheckBox furtiviteBox;
    private JCheckBox soinBox;
    private JCheckBox bouleDeFeuBox;

    // RNG
    private static final Random RNG = new Random();

    // Turn-based state
    private enum Side { PLAYER, ENEMY }
    private boolean combatOngoing = false;
    private Character playerChar;
    private Character enemyChar;

    // Per-side buffs and state
    private static class BuffState {
        // Effets
        int fireballBonusTurns = 0;       // nombre de tours restants
        int fireballBonusAmount = 0;      // +X dégâts tant que fireballBonusTurns > 0
        boolean surchargeReady = false;   // prochain coup à 150%
        int dodgeCharges = 0;                // furtivité active pendant X tours

        // Cooldowns
        int cdSurcharge = 0;
        int cdFurtivite = 0;
        int cdSoin = 0;
        int cdBoule = 0;

        void tickCooldowns() {
            if (cdSurcharge > 0) cdSurcharge--;
            if (cdFurtivite > 0) cdFurtivite--;
            if (cdSoin > 0) cdSoin--;
            if (cdBoule > 0) cdBoule--;
        }
    }


    private BuffState playerBuff = new BuffState();
    private BuffState enemyBuff = new BuffState();

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
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        createCharacterTab();
        createCombatTab();
        createSettingsTab();
        createArmyTab();
        createHistoryTab();

        frame.add(tabbedPane);
    }

    private void refreshCharacterList() {
        if (characterListModel == null || fighter1Combo == null || fighter2Combo == null) return;

        characterListModel.clear();
        fighter1Combo.removeAllItems();
        fighter2Combo.removeAllItems();

        for (Character c : dao.findAll()) {
            characterListModel.addElement(c);
            fighter1Combo.addItem(c);
            fighter2Combo.addItem(c);
        }
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

            refreshCharacterList();
            clearCharacterForm();
            deleteBtn.setEnabled(false);
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
        rightPanel.setPreferredSize(new Dimension(450, 0));

        // Character form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(18);
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

        // Decorators section (new skills only)
        JPanel decoratorPanel = new JPanel();
        decoratorPanel.setLayout(new BoxLayout(decoratorPanel, BoxLayout.Y_AXIS));
        decoratorPanel.setBorder(new TitledBorder("Skills"));

        surchargeBox = new JCheckBox("Surcharge");
        furtiviteBox = new JCheckBox("Furtivité");
        soinBox = new JCheckBox("Soin");
        bouleDeFeuBox = new JCheckBox("Boule de Feu");

        surchargeBox.addActionListener(e -> { enforceSkillCheckboxLimitUI(); updateDecorators(); });
        furtiviteBox.addActionListener(e -> { enforceSkillCheckboxLimitUI(); updateDecorators(); });
        soinBox.addActionListener(e -> { enforceSkillCheckboxLimitUI(); updateDecorators(); });
        bouleDeFeuBox.addActionListener(e -> { enforceSkillCheckboxLimitUI(); updateDecorators(); });

        decoratorPanel.add(surchargeBox);
        decoratorPanel.add(Box.createVerticalStrut(5));
        decoratorPanel.add(furtiviteBox);
        decoratorPanel.add(Box.createVerticalStrut(5));
        decoratorPanel.add(soinBox);
        decoratorPanel.add(Box.createVerticalStrut(5));
        decoratorPanel.add(bouleDeFeuBox);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(buttonPanel, BorderLayout.NORTH);
        centerPanel.add(decoratorPanel, BorderLayout.CENTER);
        rightPanel.add(centerPanel, BorderLayout.CENTER);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

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
        startCombatBtn = new JButton("Start Turn-based Combat");

        startCombatBtn.addActionListener(e -> startTurnBasedCombat());

        setupPanel.add(new JLabel("Player:"));
        setupPanel.add(fighter1Combo);
        setupPanel.add(new JLabel("vs"));
        setupPanel.add(fighter2Combo);
        setupPanel.add(startCombatBtn);

        // Center layout
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Combat display (left side)
        combatLogArea = new JTextArea(22, 60);
        combatLogArea.setEditable(false);
        combatLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        combatLogArea.setBackground(Color.BLACK);
        combatLogArea.setForeground(Color.GREEN);
        JScrollPane logScroll = new JScrollPane(combatLogArea);
        logScroll.setBorder(new TitledBorder("Combat Log"));

        // Status panel (right side)
        JPanel statusPanel = createLiveBattleStatusPanel();

        // Actions panel (bottom)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.setBorder(new TitledBorder("Your Action"));

        attackBtn = new JButton("Attaque");
        surchargeBtn = new JButton("Surcharge");
        furtiviteBtn = new JButton("Furtivité");
        soinBtn = new JButton("Soin");
        bouleDeFeuBtn = new JButton("Boule de Feu");

        attackBtn.addActionListener(e -> onPlayerAttack());
        surchargeBtn.addActionListener(e -> onPlayerSurcharge());
        furtiviteBtn.addActionListener(e -> onPlayerFurtivite());
        soinBtn.addActionListener(e -> onPlayerSoin());
        bouleDeFeuBtn.addActionListener(e -> onPlayerBouleDeFeu());

        actionsPanel.add(attackBtn);
        actionsPanel.add(surchargeBtn);
        actionsPanel.add(furtiviteBtn);
        actionsPanel.add(soinBtn);
        actionsPanel.add(bouleDeFeuBtn);

        centerPanel.add(logScroll, BorderLayout.CENTER);
        centerPanel.add(statusPanel, BorderLayout.EAST);
        centerPanel.add(actionsPanel, BorderLayout.SOUTH);

        panel.add(setupPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        tabbedPane.addTab("Combat", panel);

        setActionButtonsEnabled(false);
    }

    private JPanel createLiveBattleStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("Battle Status"));
        panel.setPreferredSize(new Dimension(220, 0));

        liveTurnLabel = new JLabel("Tour: 0", SwingConstants.CENTER);
        liveTurnLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        liveTurnLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(liveTurnLabel);

        panel.add(Box.createVerticalStrut(10));

        JLabel f1Label = new JLabel("Player HP:");
        f1Label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(f1Label);

        liveFighter1HPBar = new JProgressBar(0, 100);
        liveFighter1HPBar.setValue(100);
        liveFighter1HPBar.setStringPainted(true);
        liveFighter1HPBar.setString("100 / 100");
        liveFighter1HPBar.setForeground(Color.GREEN);
        panel.add(liveFighter1HPBar);

        panel.add(Box.createVerticalStrut(5));

        JLabel f2Label = new JLabel("Enemy HP:");
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

    private void applySettings() {
        // Si tu as bien rpg.settings.GameSettings, on pousse les valeurs UI dedans.
        GameSettings settings = GameSettings.getInstance();
        settings.setMaxStatPoints((Integer) maxStatPointsSpinner.getValue());
        settings.setMaxCharactersPerGroup((Integer) maxCharactersSpinner.getValue());
        settings.setMaxGroupsPerArmy((Integer) maxGroupsSpinner.getValue());

        JOptionPane.showMessageDialog(frame, "Settings applied successfully", "Settings", JOptionPane.INFORMATION_MESSAGE);
        eventBus.notifyObservers("SETTINGS_CHANGED", "Settings updated");
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
        if (rootNode == null) return;
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

        rootNode = new DefaultMutableTreeNode("Armies");
        treeModel = new DefaultTreeModel(rootNode);
        hierarchyTree = new JTree(treeModel);

        hierarchyTree.setRootVisible(false);
        hierarchyTree.setShowsRootHandles(true);
        hierarchyTree.setEditable(false);

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
                action.getActor() != null ? action.getActor().getName() : "(?)",
                action.getTarget() != null ? action.getTarget().getName() : "(?)",
                action.getActionType() != null ? action.getActionType() : "(?)",
                action.getDamage(),
                action.isModifiable() ? "Yes" : "No",
                action.getDescription() != null ? action.getDescription() : ""
        );
        JOptionPane.showMessageDialog(frame, details, "Action Details", JOptionPane.INFORMATION_MESSAGE);
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
            } else {
                // Pas une bataille ni une action? On plie/déplie, histoire d’être utile quand même.
                if (battleHistoryTree.isExpanded(selectionPath)) {
                    battleHistoryTree.collapsePath(selectionPath);
                } else {
                    battleHistoryTree.expandPath(selectionPath);
                }
            }
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
            // Stop net si > 2
            if (!ensureSkillLimitOrWarn()) return;

            if (isCreatingNew) {
                String name = nameField.getText().trim();
                int str = (Integer) strSpinner.getValue();
                int agi = (Integer) agiSpinner.getValue();
                int intel = (Integer) intSpinner.getValue();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a character name", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Character base = controller.buildCharacter(name, str, agi, intel);
                Character decorated = applySelectedDecorators(base);

                // Validation des règles globales (incluant max skills si tu l’as ajouté)
                if (!rpg.settings.GameSettings.getInstance().isValid(decorated)) {
                    JOptionPane.showMessageDialog(frame, "Règles du jeu violées (compétences/stat).", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                dao.save(decorated);
                JOptionPane.showMessageDialog(frame, "Character created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                refreshCharacterList();
                clearCharacterForm();

            } else {
                // Update existant
                int str = (Integer) strSpinner.getValue();
                int agi = (Integer) agiSpinner.getValue();
                int intel = (Integer) intSpinner.getValue();

                Character baseOrig = findBaseCharacter(currentEditingCharacter);
                Character newBase = controller.buildCharacter(baseOrig.getName(), str, agi, intel);
                Character newDecorated = applySelectedDecorators(newBase);

                if (!rpg.settings.GameSettings.getInstance().isValid(newDecorated)) {
                    JOptionPane.showMessageDialog(frame, "Règles du jeu violées (compétences/stat).", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                dao.update(currentEditingCharacter, newDecorated);
                currentEditingCharacter = newDecorated;

                JOptionPane.showMessageDialog(frame, "Character updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshCharacterList();

                // Rester sélectionné
                for (int i = 0; i < characterListModel.size(); i++) {
                    Character c = characterListModel.get(i);
                    if (c.getName().equals(currentEditingCharacter.getName())) {
                        characterList.setSelectedIndex(i);
                        loadCharacterToEdit(c);
                        break;
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
            if (!ensureSkillLimitOrWarn()) return;

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

            Character base = findBaseCharacter(temporaryCharacter);
            Character decorated = applySelectedDecorators(base);

            if (!rpg.settings.GameSettings.getInstance().isValid(decorated)) {
                // On n’écrase pas temporaryCharacter avec un truc invalide
                return;
            }
            temporaryCharacter = decorated;

        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error creating character: " + e.getMessage(), "Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateExistingCharacterDecorators() {
        try {
            if (!ensureSkillLimitOrWarn()) return;

            int str = (Integer) strSpinner.getValue();
            int agi = (Integer) agiSpinner.getValue();
            int intel = (Integer) intSpinner.getValue();

            Character baseOrig = findBaseCharacter(currentEditingCharacter);
            Character newBase = controller.buildCharacter(baseOrig.getName(), str, agi, intel);
            Character newDecorated = applySelectedDecorators(newBase);

            if (!rpg.settings.GameSettings.getInstance().isValid(newDecorated)) {
                return; // ne pousse rien au DAO
            }

            dao.update(currentEditingCharacter, newDecorated);
            currentEditingCharacter = newDecorated;

            // refresh in list
            for (int i = 0; i < characterListModel.size(); i++) {
                Character c = characterListModel.get(i);
                if (c.getName().equals(newDecorated.getName())) {
                    characterListModel.set(i, newDecorated);
                    break;
                }
            }

        } catch (InvalidCharacterException e) {
            JOptionPane.showMessageDialog(frame, "Error updating character: " + e.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Character applySelectedDecorators(Character baseCharacter) {
        Character result = baseCharacter;
        if (surchargeBox.isSelected()) result = new rpg.decorator.Surcharge(result);
        if (furtiviteBox.isSelected()) result = new rpg.decorator.Furtivite(result);
        if (soinBox.isSelected()) result = new rpg.decorator.Soin(result);
        if (bouleDeFeuBox.isSelected()) result = new rpg.decorator.BouleDeFeu(result);
        return result;
    }



    // ======== TURN-BASED COMBAT LOGIC ========

    private void startTurnBasedCombat() {
        Character f1 = (Character) fighter1Combo.getSelectedItem();
        Character f2 = (Character) fighter2Combo.getSelectedItem();

        if (f1 == null || f2 == null) {
            JOptionPane.showMessageDialog(frame, "Please select two fighters", "Combat Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        playerChar = f1;
        enemyChar = f2;

        // Reset buffs
        playerBuff = new BuffState();
        enemyBuff = new BuffState();

        // Init HPs (ta règle actuelle)
        liveFighter1MaxHP = 300;
        liveFighter2MaxHP = 300;
        liveFighter1HP = liveFighter1MaxHP;
        liveFighter2HP = liveFighter2MaxHP;

        // UI status
        liveFighter1HPBar.setMaximum(liveFighter1MaxHP);
        liveFighter1HPBar.setValue(liveFighter1HP);
        liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
        liveFighter1HPBar.setForeground(Color.GREEN);

        liveFighter2HPBar.setMaximum(liveFighter2MaxHP);
        liveFighter2HPBar.setValue(liveFighter2HP);
        liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);
        liveFighter2HPBar.setForeground(Color.GREEN);

        currentTurn = 1;
        liveTurnLabel.setText("Tour: " + currentTurn);

        combatLogArea.setText("");
        logLine("=== Combat start: " + playerChar.getName() + " vs " + enemyChar.getName() + " ===");

        // >>> IMPORTANT: créer l’historique pour ce combat
        liveBattleHistory = battleHistoryManager.startNewBattle(playerChar, enemyChar);
        recordAction("START", playerChar, enemyChar, 0,
                "Combat start: " + playerChar.getName() + " vs " + enemyChar.getName(),
                false);

        // Rafraîchir l’onglet History tout de suite
        refreshBattleHistory();

        // Enable action buttons selon les compétences du joueur
        setActionButtonsEnabled(true);
        enableSkillButtonsFor(playerChar);

        combatOngoing = true;
    }

    private void recordAction(String actionType,
                              Character actor,
                              Character target,
                              int damage,
                              String description,
                              boolean modifiable) {
        if (liveBattleHistory == null) return;

        BattleAction action = new BattleAction(
                currentTurn,   // round
                actor,
                target,
                actionType,
                description,   // description vient AVANT damage
                damage,
                modifiable
        );

        liveBattleHistory.addAction(action);
        refreshBattleHistory();
    }



    private void onPlayerAttack() {
        if (!combatOngoing) return;

        // Dégâts = force uniquement
        int dmg = playerChar.getStrength();

        // Appliquer surcharge si active
        if (playerBuff.surchargeReady) {
            dmg = (int) (dmg * 1.5);
            playerBuff.surchargeReady = false;
        }

        // Ajouter bonus boule de feu si actif
        if (playerBuff.fireballBonusTurns > 0) {
            dmg += playerBuff.fireballBonusAmount;
            playerBuff.fireballBonusTurns--;
        }

        // Vérifier esquive ennemi (si furtivité active)
        boolean dodged = false;
        if (enemyBuff.dodgeCharges > 0) {
            int chance = enemyChar.getAgility() * 5; // exemple: 5% par point d’agi
            int roll = (int) (Math.random() * 100);
            if (roll < chance) {
                dodged = true;
            }
        }

        if (!dodged) {
            liveFighter2HP = Math.max(0, liveFighter2HP - dmg);
            logLine(playerChar.getName() + " frappe " + enemyChar.getName() + " pour " + dmg + " dégâts.");
            recordAction("ATTACK", playerChar, enemyChar, dmg,
                    playerChar.getName() + " inflige " + dmg + " dégâts à " + enemyChar.getName(),
                    true);
        } else {
            logLine(enemyChar.getName() + " esquive l’attaque !");
            recordAction("ATTACK", playerChar, enemyChar, 0,
                    enemyChar.getName() + " esquive l’attaque.",
                    true);
        }

        refreshHPBars();
        if (checkVictory()) return;

        // Fin du tour joueur → tour ennemi
        endPlayerTurnAndRunEnemy();
    }


    private void onPlayerSurcharge() {
        if (!combatOngoing) return;
        if (!hasDecorator(playerChar, rpg.decorator.Surcharge.class)) return;
        if (playerBuff.cdSurcharge > 0) {
            logLine("Surcharge en recharge (" + playerBuff.cdSurcharge + ").");
            return;
        }

        playerBuff.surchargeReady = true;
        playerBuff.cdSurcharge = 3;

        logLine(playerChar.getName() + " prépare une Surcharge pour le prochain coup !");
        recordAction("SURCHARGE", playerChar, enemyChar, 0,
                playerChar.getName() + " prépare une attaque à 150% pour le prochain coup.",
                true);

        endPlayerNoDamageActionAndPassToEnemy();
    }



    // Furtivité: base 30% + 2% par point d’AGI, cap à 90%.
    private int getFurtiviteDodgePercent(Character c) {
        int base = 30;
        int perAgi = 2;
        int pct = base + c.getAgility() * perAgi;
        return Math.max(0, Math.min(90, pct));
    }

    // Boule de Feu: dégâts immédiats = 5 + 2*INT
    private int getFireballImmediateDamage(Character caster) {
        return 5 + caster.getIntelligence() * 2;
    }

    // Bonus par tour pendant 2 tours = 1 + INT/3, cap à +10
    private int getFireballBonusPerTurn(Character caster) {
        return Math.min(10, 1 + caster.getIntelligence() / 3);
    }


    private void onPlayerFurtivite() {
        if (!combatOngoing) return;
        if (!hasDecorator(playerChar, rpg.decorator.Furtivite.class)) return;
        if (playerBuff.cdFurtivite > 0) {
            logLine("Furtivité en recharge (" + playerBuff.cdFurtivite + ").");
            return;
        }

        playerBuff.dodgeCharges = 3;  // 3 tours d’esquive
        playerBuff.cdFurtivite = 3;

        int chance = playerChar.getAgility() * 5; // % basé sur agi
        logLine(playerChar.getName() + " active Furtivité: " + chance + "% d’esquive pour 3 tours !");
        recordAction("FURTIVITE", playerChar, playerChar, 0,
                playerChar.getName() + " devient furtif: " + chance + "% d’esquive (3 tours).",
                true);

        endPlayerNoDamageActionAndPassToEnemy();
    }




    private void onPlayerSoin() {
        if (!combatOngoing) return;
        if (!hasDecorator(playerChar, rpg.decorator.Soin.class)) return;
        if (playerBuff.cdSoin > 0) {
            logLine("Soin en recharge (" + playerBuff.cdSoin + ").");
            return;
        }

        playerBuff.cdSoin = 3;

        int before = liveFighter1HP;
        liveFighter1HP = Math.min(liveFighter1MaxHP, liveFighter1HP + 30);
        int healed = liveFighter1HP - before;

        logLine(playerChar.getName() + " se soigne de " + healed + " PV.");
        recordAction("SOIN", playerChar, playerChar, 0,
                playerChar.getName() + " se soigne de " + healed + " PV.",
                true);

        refreshHPBars();
        endPlayerNoDamageActionAndPassToEnemy();
    }




    private void onPlayerBouleDeFeu() {
        if (!combatOngoing) return;
        if (!hasDecorator(playerChar, rpg.decorator.BouleDeFeu.class)) return;
        if (playerBuff.cdBoule > 0) {
            logLine("Boule de Feu en recharge (" + playerBuff.cdBoule + ").");
            return;
        }

        playerBuff.cdBoule = 3;

        int imm = 15 + playerChar.getIntelligence(); // dégâts immédiats
        boolean dodged = false;
        if (enemyBuff.dodgeCharges > 0) {
            int chance = enemyChar.getAgility() * 5;
            int roll = (int) (Math.random() * 100);
            if (roll < chance) dodged = true;
        }

        if (!dodged) {
            liveFighter2HP = Math.max(0, liveFighter2HP - imm);
            logLine("Boule de Feu inflige " + imm + " dégâts à " + enemyChar.getName());
            recordAction("BOULE_DE_FEU", playerChar, enemyChar, imm,
                    playerChar.getName() + " lance une Boule de Feu sur " + enemyChar.getName() + " (" + imm + " dégâts).",
                    true);
        } else {
            logLine(enemyChar.getName() + " esquive la Boule de Feu !");
            recordAction("BOULE_DE_FEU", playerChar, enemyChar, 0,
                    "Boule de Feu esquivée par " + enemyChar.getName(),
                    true);
        }

        // Bonus sur 2 attaques suivantes
        playerBuff.fireballBonusAmount = 5 + playerChar.getIntelligence() / 2;
        playerBuff.fireballBonusTurns = 2;

        logLine(playerChar.getName() + " gagne +" + playerBuff.fireballBonusAmount + " dégâts pour 2 attaques.");
        recordAction("BUFF_FEU", playerChar, playerChar, 0,
                playerChar.getName() + " est enflammé: +" + playerBuff.fireballBonusAmount + " dégâts pour 2 attaques.",
                true);

        refreshHPBars();
        if (checkVictory()) return;

        endPlayerTurnAndRunEnemy();
    }




    private void endPlayerNoDamageActionAndPassToEnemy() {
        // Conclude any charge transitions
        advanceEndOfActorTurn(Side.PLAYER);
        if (checkVictory()) return;
        enemyTurn();
    }

    private void enemyTurn() {
        if (!combatOngoing) return;

        setActionButtonsEnabled(false);
        // Simple AI: if low HP and has heal, sometimes heal; else sometimes Surcharge; else attack
        boolean enemyHasHeal = hasDecorator(enemyChar, Soin.class);
        boolean enemyHasSurcharge = hasDecorator(enemyChar, Surcharge.class);
        boolean enemyHasFurtivite = hasDecorator(enemyChar, Furtivite.class);
        boolean enemyHasBoule = hasDecorator(enemyChar, BouleDeFeu.class);

// Heal si bas PV et CD prêt
        if (enemyHasHeal && enemyBuff.cdSoin == 0 && liveFighter2HP <= (liveFighter2MaxHP * 45 / 100) && RNG.nextBoolean()) {
            int before = liveFighter2HP;
            liveFighter2HP = Math.min(liveFighter2MaxHP, liveFighter2HP + 30);
            enemyBuff.cdSoin = 4;
            logLine(enemyChar.getName() + " se soigne de " + (liveFighter2HP - before) + " PV.");
            refreshHPBars();
            if (checkVictory()) return;
            advanceEndOfActorTurn(Side.ENEMY);
            startNextPlayerTurn();
            return;
        }

// Boule de Feu si dispo
        if (enemyHasBoule && enemyBuff.cdBoule == 0 && RNG.nextInt(100) < 35) {
            int imm = getFireballImmediateDamage(enemyChar);
            applyDamageWithDodge(Side.PLAYER, imm, "Boule de Feu de " + enemyChar.getName());

            enemyBuff.fireballBonusTurns = 2;
            enemyBuff.fireballBonusAmount = getFireballBonusPerTurn(enemyChar);
            logLine(enemyChar.getName() + " s’imbue de flammes: +"
                    + enemyBuff.fireballBonusAmount + " dégâts pendant 2 attaques.");

            enemyBuff.cdBoule = 3;


            refreshHPBars();
            if (checkVictory()) return;
            advanceEndOfActorTurn(Side.ENEMY);
            startNextPlayerTurn();
            return;
        }


// Surcharge si dispo et pas déjà en cours/prête
        if (enemyHasSurcharge && enemyBuff.cdSurcharge == 0 && !enemyBuff.surchargeReady && RNG.nextBoolean()) {
            enemyBuff.surchargeReady = true;     // prêt pour SA prochaine attaque
            enemyBuff.cdSurcharge = 3;
            logLine(enemyChar.getName() + " se met en Surcharge. Prochain coup à 150%.");
            advanceEndOfActorTurn(Side.ENEMY);
            startNextPlayerTurn();
            return;
        }


// Furtivité si dispo et pas déjà active
        if (enemyHasFurtivite && enemyBuff.cdFurtivite == 0 && enemyBuff.dodgeCharges == 0 && RNG.nextInt(100) < 25) {
            enemyBuff.dodgeCharges = 3;
            enemyBuff.cdFurtivite = 3;
            int pct = getFurtiviteDodgePercent(enemyChar);
            logLine(enemyChar.getName() + " devient furtif: " + pct + "% d’esquive pendant 3 attaques.");
            advanceEndOfActorTurn(Side.ENEMY);
            startNextPlayerTurn();
            return;
        }

        // Default: attack
        doAttack(Side.ENEMY, Side.PLAYER, false);
        if (checkVictory()) return;

        startNextPlayerTurn();
    }

    private void startNextPlayerTurn() {
        currentTurn++;
        liveTurnLabel.setText("Tour: " + currentTurn);
        setActionButtonsEnabled(true);
        enableSkillButtonsFor(playerChar);
    }

    private void doAttack(Side attacker, Side defender, boolean isSkill) {
        Character atk = attacker == Side.PLAYER ? playerChar : enemyChar;
        Character def = defender == Side.PLAYER ? playerChar : enemyChar;
        BuffState atkBuff = attacker == Side.PLAYER ? playerBuff : enemyBuff;
        BuffState defBuff = defender == Side.PLAYER ? playerBuff : enemyBuff;


        // Dégâts de base: uniquement la Force, avec une variance proportionnelle à la Force
        int base = atk.getStrength() + RNG.nextInt((atk.getStrength() / 2) + 1);


        // Bonus Boule de Feu: s’applique et se consomme sur l’attaque
        if (atkBuff.fireballBonusTurns > 0) {
            base += Math.max(0, atkBuff.fireballBonusAmount);
            atkBuff.fireballBonusTurns--;            // on consomme UNE attaque boostée
            if (atkBuff.fireballBonusTurns == 0) {
                atkBuff.fireballBonusAmount = 0;     // proprement terminé
            }
        }



        // Surcharge ready multiplier
        if (atkBuff.surchargeReady) {
            base = (int) Math.round(base * 1.5);
            atkBuff.surchargeReady = false; // consommé
        }


        // Apply dodge (furtivité) on defender
        if (defBuff.dodgeCharges > 0) {
            int dodgePct = (def == playerChar) ? getFurtiviteDodgePercent(playerChar) : getFurtiviteDodgePercent(enemyChar);
            defBuff.dodgeCharges--; // on consomme 1 charge pour cette attaque entrante
            if (RNG.nextInt(100) < dodgePct) {
                logLine(atk.getName() + " attaque " + def.getName() + " mais " + def.getName() + " esquive (" + dodgePct + "%) !");
                advanceEndOfActorTurn(attacker);
                return;
            }
        }


        // Commit damage
        if (defender == Side.PLAYER) {
            liveFighter1HP = Math.max(0, liveFighter1HP - base);
        } else {
            liveFighter2HP = Math.max(0, liveFighter2HP - base);
        }
        logLine(atk.getName() + " frappe " + def.getName() + " pour " + base + " dégâts.");

        refreshHPBars();
        advanceEndOfActorTurn(attacker);
    }

    private void applyDamageWithDodge(Side targetSide, int dmg, String label) {
        BuffState targetBuff = targetSide == Side.PLAYER ? playerBuff : enemyBuff;
        Character target = targetSide == Side.PLAYER ? playerChar : enemyChar;

        if (targetBuff.dodgeCharges > 0) {
            int dodgePct = (target == playerChar) ? getFurtiviteDodgePercent(playerChar) : getFurtiviteDodgePercent(enemyChar);
            targetBuff.dodgeCharges--; // on consomme 1 charge
            if (RNG.nextInt(100) < dodgePct) {
                logLine(label + " est esquivée par " + target.getName() + " (" + dodgePct + "%) !");
                return;
            }
        }


        if (targetSide == Side.PLAYER) {
            liveFighter1HP = Math.max(0, liveFighter1HP - dmg);
        } else {
            liveFighter2HP = Math.max(0, liveFighter2HP - dmg);
        }
        logLine(label + " inflige " + dmg + " dégâts à " + target.getName() + ".");
    }

    private void advanceEndOfActorTurn(Side actor) {
        BuffState b = actor == Side.PLAYER ? playerBuff : enemyBuff;

        // On ne touche plus à la furtivité ici (elle est gérée lors des attaques reçues)

        // Cooldowns -1 à la fin du tour de l'acteur
        b.tickCooldowns();

        refreshHPBars();
    }



    private boolean checkVictory() {
        if (liveFighter1HP <= 0 || liveFighter2HP <= 0) {
            Character winner = (liveFighter1HP > 0) ? playerChar : enemyChar;
            if (liveBattleHistory != null) {
                liveBattleHistory.setWinner(winner);
                recordAction("END", winner, (winner == playerChar ? enemyChar : playerChar), 0,
                        "Fin du combat. Vainqueur: " + winner.getName(), false);
            }
            combatLogArea.append("\n=== Winner: " + winner.getName() + " ===\n");
            refreshBattleHistory();
            combatOngoing = false;
            setActionButtonsEnabled(false);
            return true;
        }
        return false;
    }

    private void enableSkillButtonsFor(Character c) {
        boolean hasSurcharge = hasDecorator(c, Surcharge.class);
        boolean hasFurtivite = hasDecorator(c, Furtivite.class);
        boolean hasSoin = hasDecorator(c, Soin.class);
        boolean hasBoule = hasDecorator(c, BouleDeFeu.class);

        // CD du joueur seulement (on n’active que pour le joueur)
        int cdS = playerBuff.cdSurcharge;
        int cdF = playerBuff.cdFurtivite;
        int cdO = playerBuff.cdSoin;
        int cdB = playerBuff.cdBoule;

        surchargeBtn.setEnabled(hasSurcharge && cdS == 0 && combatOngoing);
        furtiviteBtn.setEnabled(hasFurtivite && cdF == 0 && combatOngoing);
        soinBtn.setEnabled(hasSoin && cdO == 0 && combatOngoing);
        bouleDeFeuBtn.setEnabled(hasBoule && cdB == 0 && combatOngoing);

        // MàJ des labels pour indiquer le CD
        surchargeBtn.setText(cdS > 0 ? "Surcharge (CD " + cdS + ")" : "Surcharge");
        furtiviteBtn.setText(cdF > 0 ? "Furtivité (CD " + cdF + ")" : "Furtivité");
        soinBtn.setText(cdO > 0 ? "Soin (CD " + cdO + ")" : "Soin");
        bouleDeFeuBtn.setText(cdB > 0 ? "Boule de Feu (CD " + cdB + ")" : "Boule de Feu");
    }


    private void setActionButtonsEnabled(boolean enabled) {
        attackBtn.setEnabled(enabled);
        surchargeBtn.setEnabled(enabled);
        furtiviteBtn.setEnabled(enabled);
        soinBtn.setEnabled(enabled);
        bouleDeFeuBtn.setEnabled(enabled);
    }

    private void refreshHPBars() {
        if (liveFighter1HPBar != null && liveFighter2HPBar != null) {
            liveFighter1HPBar.setMaximum(liveFighter1MaxHP);
            liveFighter2HPBar.setMaximum(liveFighter2MaxHP);

            liveFighter1HPBar.setValue(liveFighter1HP);
            liveFighter2HPBar.setValue(liveFighter2HP);

            liveFighter1HPBar.setString(liveFighter1HP + " / " + liveFighter1MaxHP);
            liveFighter2HPBar.setString(liveFighter2HP + " / " + liveFighter2MaxHP);

            int p1 = liveFighter1MaxHP > 0 ? (liveFighter1HP * 100) / liveFighter1MaxHP : 0;
            int p2 = liveFighter2MaxHP > 0 ? (liveFighter2HP * 100) / liveFighter2MaxHP : 0;

            liveFighter1HPBar.setForeground(getHPColor(p1));
            liveFighter2HPBar.setForeground(getHPColor(p2));

            if (p1 <= 30) liveFighter1HPBar.setString(liveFighter1HPBar.getString() + " !");
            if (p2 <= 30) liveFighter2HPBar.setString(liveFighter2HPBar.getString() + " !");
        }
    }

    private void logLine(String line) {
        combatLogArea.append(line + "\n");
        combatLogArea.setCaretPosition(combatLogArea.getDocument().getLength());
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

    // ======== History tools (unchanged, but manual fights aren't auto-recorded) ========

    private void refreshHistory() {
        refreshBattleHistory();
    }

    private void replayHistory() {
        combatLogArea.append("\n--- Replaying commands ---\n");
        combatEngine.getCommandHistory().replay();
        combatLogArea.append("--- Replay complete ---\n");
    }

    private void refreshBattleHistory() {
        if (historyRootNode == null) return;
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

    private void displayNextLiveAction() {
        if (liveBattleHistory == null || currentActionIndex >= liveBattleHistory.getActions().size()) {
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
        refreshHPBars();
    }

    private void updateLiveActionHP(BattleAction action) {
        int damage = action.getDamage();

        if (damage > 0) {
            Character target = action.getTarget();

            if (target.getName().equals(playerChar != null ? playerChar.getName() : "")) {
                liveFighter1HP = Math.max(0, liveFighter1HP - damage);
            } else if (target.getName().equals(enemyChar != null ? enemyChar.getName() : "")) {
                liveFighter2HP = Math.max(0, liveFighter2HP - damage);
            }
        }
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

    private void openInteractiveReplay(BattleHistory battle) {
        InteractiveBattleReplay replayWindow = new InteractiveBattleReplay(
                frame,
                battle,
                battleHistoryManager,
                () -> refreshBattleHistory()
        );
        replayWindow.setVisible(true);
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

    // ======== Observer ========

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
        // We keep observer plumbing for other parts; manual combat does not rely on it
        SwingUtilities.invokeLater(() -> {
            switch (eventType) {
                case "CHARACTER_CREATED":
                    refreshCharacterList();
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

    // ======== Tree inner classes ========

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

    // Compte le nombre de compétences cochées
    private int selectedSkillsCount() {
        int n = 0;
        if (surchargeBox.isSelected()) n++;
        if (furtiviteBox.isSelected()) n++;
        if (soinBox.isSelected()) n++;
        if (bouleDeFeuBox.isSelected()) n++;
        return n;
    }

    // Affiche un warning et bloque si > 2
    private boolean ensureSkillLimitOrWarn() {
        int n = selectedSkillsCount();
        if (n > 2) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Un personnage ne peut avoir que 2 compétences maximum.",
                    "Sélection invalide",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        return true;
    }

    // Grise automatiquement les cases quand on atteint 2
    private void enforceSkillCheckboxLimitUI() {
        int n = selectedSkillsCount();
        boolean lockOthers = (n >= 2);
        surchargeBox.setEnabled(surchargeBox.isSelected() || !lockOthers);
        furtiviteBox.setEnabled(furtiviteBox.isSelected() || !lockOthers);
        soinBox.setEnabled(soinBox.isSelected() || !lockOthers);
        bouleDeFeuBox.setEnabled(bouleDeFeuBox.isSelected() || !lockOthers);
    }

    private void endPlayerTurnAndRunEnemy() {
        advanceEndOfActorTurn(Side.PLAYER);  // CD du joueur
        if (checkVictory()) return;

        enemyTurn();                         // IA joue, enregistre ses actions
        if (checkVictory()) return;

        advanceEndOfActorTurn(Side.ENEMY);   // CD de l’ennemi
        currentTurn++;
        liveTurnLabel.setText("Tour: " + currentTurn);
    }


}
