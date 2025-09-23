package rpg.dao;

import java.util.List;

public interface DAO<T> {
    void save(T item);
    T findByName(String name);
    List<T> findAll();
}
