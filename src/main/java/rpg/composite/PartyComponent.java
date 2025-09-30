package rpg.composite;

import java.util.Collections;
import java.util.List;
import rpg.core.Party;

public class PartyComponent implements GroupComponent {
    private final Party party;

    public PartyComponent(Party party) {
        this.party = party;
    }

    @Override
    public void add(GroupComponent component) {
        throw new UnsupportedOperationException("Cannot add to a leaf component");
    }

    @Override
    public void remove(GroupComponent component) {
        throw new UnsupportedOperationException("Cannot remove from a leaf component");
    }

    @Override
    public String getName() {
        return "Party(" + party.getMembers().size() + " members)";
    }

    @Override
    public int getTotalPower() {
        return party.totalPower();
    }

    @Override
    public List<GroupComponent> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    public Party getParty() {
        return party;
    }
}