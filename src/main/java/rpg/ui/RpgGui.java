package rpg.ui;

import rpg.builder.CharacterBuilder;
import rpg.core.Character;
import rpg.core.Combat;
import rpg.decorator.Furtivite;
import rpg.decorator.Surcharge;
import rpg.dao.CharacterDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RpgGui {
    private final Character alice;
    private final Character bob;
    private final rpg.dao.CharacterDAO dao;

    public RpgGui(Character alice, Character bob, CharacterDAO dao) {
        this.alice = alice;
        this.bob = bob;
        this.dao = dao;
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    private void createAndShowGui() {
        JFrame frame = new JFrame("RPG Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);

        JPanel panel = new JPanel(new BorderLayout());

        // Top: form
        JPanel form = new JPanel(new GridLayout(5, 2));
        JTextField nameField = new JTextField("Custom");
        JSpinner str = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        JSpinner agi = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        JSpinner intel = new JSpinner(new SpinnerNumberModel(5, 0, 30, 1));
        String[] opponents = {"Alice", "Bob"};
        JComboBox<String> opponentBox = new JComboBox<>(opponents);

        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Strength:")); form.add(str);
        form.add(new JLabel("Agility:")); form.add(agi);
        form.add(new JLabel("Intelligence:")); form.add(intel);
        form.add(new JLabel("Opponent:")); form.add(opponentBox);

        panel.add(form, BorderLayout.NORTH);

        // Center: 3 columns -> Saved list | Combat Log | Results
        JPanel center = new JPanel(new GridLayout(1, 3, 8, 8));

        // Left: saved characters list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Character c : dao.findAll()) {
            listModel.addElement(c.getDescription() + " -> Power=" + c.getPowerLevel());
        }
        JList<String> savedList = new JList<>(listModel);
        JScrollPane leftScroll = new JScrollPane(savedList);
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Saved"), BorderLayout.NORTH);
        leftPanel.add(leftScroll, BorderLayout.CENTER);

        // Middle: combat log
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane centerScroll = new JScrollPane(logArea);
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.add(new JLabel("Combat Log"), BorderLayout.NORTH);
        midPanel.add(centerScroll, BorderLayout.CENTER);

        // Right: results / summary
        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        JScrollPane rightScroll = new JScrollPane(resultsArea);
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Results"), BorderLayout.NORTH);
        rightPanel.add(rightScroll, BorderLayout.CENTER);

        center.add(leftPanel);
        center.add(midPanel);
        center.add(rightPanel);
        panel.add(center, BorderLayout.CENTER);

        // Bottom: buttons
        JPanel buttons = new JPanel();
        JButton fightBtn = new JButton("Fight");
        JButton createBtn = new JButton("Create & Save");
        buttons.add(createBtn);
        buttons.add(fightBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        fightBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nm = nameField.getText().trim();
                int s = (Integer) str.getValue();
                int a = (Integer) agi.getValue();
                int it = (Integer) intel.getValue();
                CharacterBuilder cb = new CharacterBuilder();
                try {
                    Character custom = cb.setName(nm).setStrength(s).setAgility(a).setIntelligence(it).build();
                    Character opp = opponentBox.getSelectedItem().equals("Alice") ? alice : bob;
                    String log = Combat.simulateWithLog(custom, opp);
                    logArea.setText(log);
                    resultsArea.setText("Last fight: " + custom.getName() + " vs " + opp.getName() + "\nWinner: " + (log.contains("Winner:") ? log.substring(log.lastIndexOf("Winner:")) : "n/a"));
                } catch (Exception ex) {
                    logArea.setText("Error: " + ex.getMessage());
                    resultsArea.setText("Error");
                }
            }
        });

        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nm = nameField.getText().trim();
                int s = (Integer) str.getValue();
                int a = (Integer) agi.getValue();
                int it = (Integer) intel.getValue();
                CharacterBuilder cb = new CharacterBuilder();
                try {
                    Character custom = cb.setName(nm).setStrength(s).setAgility(a).setIntelligence(it).build();
                    // simple decorator demo
                    Character decorated = new Furtivite(custom);
                    decorated = new Surcharge(decorated);
                    dao.save(decorated);
                    // refresh saved list
                    listModel.clear();
                    for (Character c : dao.findAll()) {
                        listModel.addElement(c.getDescription() + " -> Power=" + c.getPowerLevel());
                    }
                    logArea.setText("Saved: " + decorated.getDescription());
                    resultsArea.setText("Total saved: " + dao.findAll().size());
                } catch (Exception ex) {
                    logArea.setText("Error: " + ex.getMessage());
                    resultsArea.setText("Error");
                }
            }
        });

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }
}
