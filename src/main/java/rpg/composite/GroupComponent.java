package rpg.composite;

import java.util.List;

public interface GroupComponent {
    void add(GroupComponent component);
    void remove(GroupComponent component);
    String getName();
    int getTotalPower();
    List<GroupComponent> getChildren();
    boolean isComposite();
}