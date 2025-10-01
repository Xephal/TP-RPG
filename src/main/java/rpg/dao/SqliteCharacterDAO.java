package rpg.dao;

import rpg.core.Character;
import rpg.decorator.CharacterDecorator;
import rpg.decorator.FireResistance;
import rpg.decorator.Invisibility;
import rpg.decorator.Telepathy;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SqliteCharacterDAO implements DAO<Character> {

    public SqliteCharacterDAO() {
        // rien
    }

    // --------------------- Helpers décorateurs ---------------------
    private String serializeDecorators(Character c) {
        // remonte la chaine et enregistre l'ordre d'application (du bas vers le haut)
        List<String> list = new ArrayList<>();
        Character cur = c;
        while (cur instanceof CharacterDecorator) {
            if (cur instanceof Invisibility) list.add("Invisibility");
            else if (cur instanceof FireResistance) list.add("FireResistance");
            else if (cur instanceof Telepathy) list.add("Telepathy");
            cur = ((CharacterDecorator) cur).getWrappedCharacter();
        }
        // on stocke de haut en bas (ordre d’application), ça n’a pas d’importance tant qu’on réapplique dans le même ordre
        java.util.Collections.reverse(list);
        return String.join(",", list);
    }

    private Character unwrap(Character c) {
        Character cur = c;
        while (cur instanceof CharacterDecorator) {
            cur = ((CharacterDecorator) cur).getWrappedCharacter();
        }
        return cur;
    }

    private Character reapply(String decoratorsCsv, Character base) {
        if (decoratorsCsv == null || decoratorsCsv.isBlank()) return base;
        for (String d : decoratorsCsv.split(",")) {
            switch (d.trim()) {
                case "Invisibility" -> base = new Invisibility(base);
                case "FireResistance" -> base = new FireResistance(base);
                case "Telepathy" -> base = new Telepathy(base);
                default -> { /* inconnu => ignorer */ }
            }
        }
        return base;
    }

    // --------------------- CRUD ---------------------
    @Override
    public void save(Character item) {
        String sql = "INSERT OR REPLACE INTO character(name, strength, agility, intelligence, decorators) VALUES(?,?,?,?,?)";
        Character base = unwrap(item);
        String decorators = serializeDecorators(item);
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, base.getName());
            ps.setInt(2, base.getStrength());
            ps.setInt(3, base.getAgility());
            ps.setInt(4, base.getIntelligence());
            ps.setString(5, decorators);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Character findByName(String name) {
        String sql = "SELECT name,strength,agility,intelligence,decorators FROM character WHERE name=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Character base = new Character(
                        rs.getString("name"),
                        rs.getInt("strength"),
                        rs.getInt("agility"),
                        rs.getInt("intelligence")
                );
                return reapply(rs.getString("decorators"), base);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByName failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Character> findAll() {
        String sql = "SELECT name,strength,agility,intelligence,decorators FROM character ORDER BY name";
        List<Character> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Character base = new Character(
                        rs.getString("name"),
                        rs.getInt("strength"),
                        rs.getInt("agility"),
                        rs.getInt("intelligence")
                );
                out.add(reapply(rs.getString("decorators"), base));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
        return out;
    }

    // --------------------- Bonus: mêmes méthodes que ton CharacterDAO mémoire ---------------------

    public boolean remove(Character item) {
        String sql = "DELETE FROM character WHERE name=?";
        String name = unwrap(item).getName();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("remove failed: " + e.getMessage(), e);
        }
    }

    public boolean update(Character oldCharacter, Character newCharacter) {
        // on s’appuie sur la clé = name. Si tu veux permettre rename, il faut une clé numérique et un UPDATE spécifique.
        // Ici : on écrase la ligne du même name
        save(newCharacter);
        return true;
    }

    public List<Character> findAllSortedByPower() {
        // plus simple : on récupère tout puis on trie côté Java (pour tenir compte des décorateurs)
        return findAll().stream()
                .sorted(Comparator.comparingInt(Character::getPowerLevel).reversed())
                .collect(Collectors.toList());
    }

    public List<Character> findAllSortedByName() {
        return findAll().stream()
                .sorted(Comparator.comparing(Character::getName))
                .collect(Collectors.toList());
    }
}
