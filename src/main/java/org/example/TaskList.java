package org.example;

import java.util.ArrayList;
import java.util.List;

public class TaskList<T> {
    private final List<T> items;

    private int pos;

    public TaskList() {
        this.items = new ArrayList<>();
        pos = 0;
    }

    public TaskList(List<T> item) {
        this.items = new ArrayList<>(item);
        pos = 0;
    }

    public synchronized boolean addItem(T task) {
        return items.add(task);
    }

    public synchronized T getTask() {
        if (pos >= items.size()) {
            return null;
        } else {
            return items.get(pos++);
        }
    }

    public synchronized void reset() {
        pos = 0;
    }

    public T getItem(int index) {
        return items.get(index);
    }

    public List<T> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }
}
