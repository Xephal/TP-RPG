package rpg.ui;

import rpg.builder.CharacterBuilder;
import rpg.core.Character;
import rpg.core.Combat;
import rpg.decorator.Invisibility;
import rpg.decorator.FireResistance;
import rpg.decorator.Telepathy;
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

        // Center: log
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        panel.add(scroll, BorderLayout.CENTER);

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
                } catch (Exception ex) {
                    logArea.setText("Error: " + ex.getMessage());
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
                    Character decorated = new Invisibility(custom);
                    decorated = new FireResistance(decorated);
                    dao.save(decorated);
                    logArea.setText("Saved: " + decorated.getDescription() + "\nTotal saved: " + dao.findAll().size());
                } catch (Exception ex) {
                    logArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }
}
