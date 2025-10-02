package rpg.mvc;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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

/**
 * Fenêtre d’édition d’une action avec support des compétences:
 * ATTACK, SURCHARGE, FURTIVITE, SOIN, BOULE_DE_FEU (et START/END/BUFF_FEU si besoin).
 * - Le champ dégâts n’est activé que pour ATTACK et BOULE_DE_FEU.
 * - La description est auto-générée en fonction du type sélectionné.
 */
public class ActionModificationDialog extends JDialog {
    private final BattleAction originalAction;
    private BattleAction modifiedAction;
    private boolean confirmed;

    // UI Components
    private JTextField damageField;
    private JTextArea descriptionArea;
    private JComboBox<ActionChoice> actionTypeCombo;
    private JLabel actorLabel;
    private JLabel targetLabel;

    // Mapping label affiché -> token interne stocké dans BattleAction.actionType
    private static class ActionChoice {
        final String label; // affichage
        final String token; // valeur persistée
        ActionChoice(String label, String token) {
            this.label = label;
            this.token = token;
        }
        @Override public String toString() { return label; }
    }

    private static final ActionChoice[] ACTIONS = new ActionChoice[] {
            new ActionChoice("Attaque",        "ATTACK"),
            new ActionChoice("Surcharge",      "SURCHARGE"),
            new ActionChoice("Furtivité",      "FURTIVITE"),
            new ActionChoice("Soin",           "SOIN"),
            new ActionChoice("Boule de Feu",   "BOULE_DE_FEU"),
            // Optionnels si tu veux les éditer aussi:
            new ActionChoice("Buff Feu (+X/2 attaques)", "BUFF_FEU"),
            new ActionChoice("Début (START)",  "START"),
            new ActionChoice("Fin (END)",      "END")
    };

    public ActionModificationDialog(Frame parent, BattleAction action) {
        super(parent, "Modify Action #" + action.getRound(), true);
        this.originalAction = action;
        this.modifiedAction = null;
        this.confirmed = false;

        initializeUI();
        loadActionData();
        setSize(520, 360);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Actor
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Acteur:"), gbc);
        gbc.gridx = 1;
        actorLabel = new JLabel(originalAction.getActor().getName());
        actorLabel.setFont(actorLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(actorLabel, gbc);

        // Target
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Cible:"), gbc);
        gbc.gridx = 1;
        targetLabel = new JLabel(originalAction.getTarget().getName());
        targetLabel.setFont(targetLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(targetLabel, gbc);

        // Type d’action
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Type d’action:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        actionTypeCombo = new JComboBox<>(ACTIONS);
        actionTypeCombo.addActionListener(e -> regenerateForType());
        mainPanel.add(actionTypeCombo, gbc);

        // Dégâts
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("Dégâts:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        damageField = new JTextField(10);
        mainPanel.add(damageField, gbc);

        // Description auto
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mainPanel.add(new JLabel("Description (Auto):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(descriptionArea);
        mainPanel.add(scroll, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Boutons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetBtn = new JButton("Reset to Original");
        JButton cancelBtn = new JButton("Cancel");
        JButton applyBtn = new JButton("Apply Changes");

        resetBtn.addActionListener(e -> loadActionData());
        cancelBtn.addActionListener(e -> { confirmed = false; modifiedAction = null; dispose(); });
        applyBtn.addActionListener(e -> confirmChanges());

        buttons.add(resetBtn);
        buttons.add(cancelBtn);
        buttons.add(applyBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void loadActionData() {
        // Sélectionner l’entrée correspondant au token original
        String token = originalAction.getActionType();
        int idx = 0;
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].token.equalsIgnoreCase(token)) { idx = i; break; }
        }
        actionTypeCombo.setSelectedIndex(idx);

        damageField.setText(String.valueOf(originalAction.getDamage()));
        descriptionArea.setText(originalAction.getDescription());

        regenerateForType();
    }

    private void regenerateForType() {
        ActionChoice choice = (ActionChoice) actionTypeCombo.getSelectedItem();
        if (choice == null) return;

        boolean needsDamage = "ATTACK".equals(choice.token) || "BOULE_DE_FEU".equals(choice.token);
        damageField.setEnabled(needsDamage);
        if (!needsDamage) {
            damageField.setText("0");
        }

        // Description FR auto
        String actor = originalAction.getActor().getName();
        String target = originalAction.getTarget().getName();
        int dmg = safeDamage();

        String desc;
        switch (choice.token) {
            case "ATTACK":
                desc = actor + " inflige " + dmg + " dégâts à " + target + ".";
                break;
            case "SURCHARGE":
                desc = actor + " prépare une attaque à 150% pour le prochain coup.";
                break;
            case "FURTIVITE":
                desc = actor + " devient furtif (esquive augmentée, 3 charges).";
                break;
            case "SOIN":
                desc = actor + " se soigne.";
                break;
            case "BOULE_DE_FEU":
                desc = actor + " lance Boule de Feu (" + dmg + " dégâts).";
                break;
            case "BUFF_FEU":
                desc = actor + " s’imbue de flammes: +X dégâts pendant 2 attaques.";
                break;
            case "START":
                desc = "Combat start: " + actor + " vs " + target;
                break;
            case "END":
                desc = "Fin du combat. Vainqueur: " + actor;
                break;
            default:
                desc = originalAction.getDescription();
        }
        descriptionArea.setText(desc);
    }

    private int safeDamage() {
        try {
            int v = Integer.parseInt(damageField.getText().trim());
            return Math.max(0, v);
        } catch (Exception e) {
            return 0;
        }
    }

    private void confirmChanges() {
        ActionChoice choice = (ActionChoice) actionTypeCombo.getSelectedItem();
        if (choice == null) return;

        int dmg = safeDamage();
        if (dmg < 0) {
            JOptionPane.showMessageDialog(this, "Les dégâts ne peuvent pas être négatifs.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String desc = descriptionArea.getText().trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La description ne peut pas être vide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        modifiedAction = new BattleAction(
                originalAction.getRound(),
                originalAction.getActor(),
                originalAction.getTarget(),
                choice.token,     // on stocke le token interne propre
                desc,
                dmg,
                true
        );
        confirmed = true;
        dispose();
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
