package rpg.core;

import java.util.ArrayList;
import java.util.List;

public class Party {
    private final List<Character> members = new ArrayList<>();

    public void add(Character c) {
        members.add(c);
    }

    public void remove(Character c) {
        members.remove(c);
    }

    public int totalPower() {
        int sum = 0;
        for (Character c : members) sum += c.getPowerLevel();
        return sum;
    }

    public List<Character> getMembers() {
        return new ArrayList<>(members);
    }
}
