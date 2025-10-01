package rpg.dao;

import java.util.List;

public interface DAO<T> {
    void save(T item);
    T findByName(String name);
    List<T> findAll();

    // Ajouts pour SwingView (et autres couches) :
    default boolean update(T oldItem, T newItem) {
        // Implémentation par défaut : non supporté
        return false;
    }

    default boolean remove(T item) {
        // Implémentation par défaut : non supporté
        return false;
    }
}
