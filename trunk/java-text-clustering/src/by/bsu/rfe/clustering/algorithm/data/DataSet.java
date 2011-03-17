package by.bsu.rfe.clustering.algorithm.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;

public class DataSet<E extends DataElement> implements Iterable<E> {

    private final List<E> _elements;

    public DataSet() {
        _elements = new ArrayList<E>();
    }

    public void addElement(E element) {
        Preconditions.checkNotNull(element, "Data Element is null");
        _elements.add(element);
    }

    public List<E> getElements() {
        return Collections.unmodifiableList(_elements);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new ElementIterator();
    }

    public int size() {
        return _elements.size();
    }

    private class ElementIterator implements Iterator<E> {

        private final Iterator<E> _delegate;

        private ElementIterator() {
            _delegate = _elements.iterator();
        }

        @Override
        public boolean hasNext() {
            return _delegate.hasNext();
        }

        @Override
        public E next() {
            return _delegate.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() is not supported");
        }
    }
}
