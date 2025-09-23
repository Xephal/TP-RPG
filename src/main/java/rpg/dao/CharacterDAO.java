package rpg.dao;

import java.util.ArrayList;
import java.util.List;
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
}
