package rpg.composite;

import java.util.ArrayList;
import java.util.List;

public class Army implements GroupComponent {
    private final String name;
    private final List<GroupComponent> children = new ArrayList<>();

    public Army(String name) {
        this.name = name;
    }

    @Override
    public void add(GroupComponent component) {
        children.add(component);
    }

    @Override
    public void remove(GroupComponent component) {
        children.remove(component);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getTotalPower() {
        int total = 0;
        for (GroupComponent child : children) {
            total += child.getTotalPower();
        }
        return total;
    }

    @Override
    public List<GroupComponent> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public boolean isComposite() {
        return true;
    }
}