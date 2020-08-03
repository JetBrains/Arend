package org.arend.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// Singleton list that supports set
public class SingletonList<E> extends AbstractList<E> implements RandomAccess {
  public E element;

  public SingletonList(E obj) {
    element = obj;
  }

  public @NotNull Iterator<E> iterator() {
    return new Iterator<>() {
      private boolean hasNext = true;
      public boolean hasNext() {
        return hasNext;
      }
      public E next() {
        if (hasNext) {
          hasNext = false;
          return element;
        }
        throw new NoSuchElementException();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
      @Override
      public void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        if (hasNext) {
          hasNext = false;
          action.accept(element);
        }
      }
    };
  }

  public int size() {
    return 1;
  }

  public boolean contains(Object obj) {
    return Objects.equals(obj, element);
  }

  public E get(int index) {
    if (index != 0)
      throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
    return element;
  }

  @Override
  public E set(int index, E element) {
    if (index != 0)
      throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
    E e = this.element;
    this.element = element;
    return e;
  }

  @Override
  public int indexOf(Object o) {
    return Objects.equals(o, element) ? 0 : -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    return Objects.equals(o, element) ? 0 : -1;
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    action.accept(element);
  }

  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    element = operator.apply(element);
  }

  @Override
  public void sort(Comparator<? super E> c) {
  }

  @Override
  public Spliterator<E> spliterator() {
    return new Spliterator<>() {
      long est = 1;

      @Override
      public Spliterator<E> trySplit() {
        return null;
      }

      @Override
      public boolean tryAdvance(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer);
        if (est > 0) {
          est--;
          consumer.accept(element);
          return true;
        }
        return false;
      }

      @Override
      public void forEachRemaining(Consumer<? super E> consumer) {
        tryAdvance(consumer);
      }

      @Override
      public long estimateSize() {
        return est;
      }

      @Override
      public int characteristics() {
        int value = (element != null) ? Spliterator.NONNULL : 0;

        return value | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE |
          Spliterator.DISTINCT | Spliterator.ORDERED;
      }
    };
  }

  @Override
  public int hashCode() {
    return 31 + Objects.hashCode(element);
  }
}
