package ac.at.tuwien.inso.ble.utils;

import java.util.LinkedList;

/**
 * List with limited number of elements.
 * If an element is added when the list is already full, the last element is removed.
 *
 * @author Manuel Heinzl
 */
public class LimitedList<E> extends LinkedList<E> {

    private int limit;

    public LimitedList(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) {
            super.remove();
        }
        return true;
    }
}