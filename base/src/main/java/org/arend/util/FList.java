package org.arend.util;

import java.util.Objects;

public class FList<E> {
  private final E myHead;
  private final FList<E> myTail;

  public FList(E head, FList<E> tail) {
    this.myHead = head;
    this.myTail = tail;
  }

  public E getHead() {
    return myHead;
  }

  public FList<E> getTail() {
    return myTail;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FList<?> fList = (FList<?>) o;
    return Objects.equals(myHead, fList.myHead) &&
      Objects.equals(myTail, fList.myTail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myHead, myTail);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (FList<E> list = this; list != null; list = list.myTail) {
      if (list != this) {
        builder.append(", ");
      }
      builder.append(list.myHead);
    }
    builder.append("]");
    return builder.toString();
  }
}
