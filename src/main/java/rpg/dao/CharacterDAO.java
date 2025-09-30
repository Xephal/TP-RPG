package rpg.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import rpg.core.Character;

public class CharacterDAO implements DAO<Character> {
    private final List<Character> storage = new ArrayList<>();

    @Override
    public void save(Character item) {
        storage.add(item);
    }

    @Override
    public Character findByName(String name) {
        for (Character c : storage) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    @Override
    public List<Character> findAll() {
        return new ArrayList<>(storage);
    }

    public List<Character> findAllSortedByPower() {
        return storage.stream()
                .sorted(Comparator.comparingInt(Character::getPowerLevel).reversed())
                .collect(Collectors.toList());
    }

    public List<Character> findAllSortedByName() {
        return storage.stream()
                .sorted(Comparator.comparing(Character::getName))
                .collect(Collectors.toList());
    }

    public boolean remove(Character item) {
        return storage.remove(item);
    }

    public boolean update(Character oldCharacter, Character newCharacter) {
        int index = storage.indexOf(oldCharacter);
        if (index >= 0) {
            storage.set(index, newCharacter);
            return true;
        }
        return false;
    }
}
