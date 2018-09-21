package org.arend.core.context;

import java.util.*;

public class Utils {
  public static void trimToSize(List<?> list, int size) {
    if (size < list.size()) {
      list.subList(size, list.size()).clear();
    }
  }

  public static class SetContextSaver<K> implements AutoCloseable {
    private final Set<K> mySet;
    private final Set<K> myOriginalSet;

    public SetContextSaver(Set<K> set) {
      mySet = set;
      myOriginalSet = new HashSet<>(mySet);
    }

    public SetContextSaver(Map<K, ?> map) {
      mySet = map.keySet();
      myOriginalSet = new HashSet<>(mySet);
    }

    @Override
    public void close() {
      mySet.retainAll(myOriginalSet);
    }
  }

  public static class ContextSaver implements AutoCloseable {
    private final List myContext;
    private final int myOldContextSize;

    public ContextSaver(List context) {
      myContext = context;
      myOldContextSize = context != null ? context.size() : 0;
    }

    public int getOriginalSize() {
      return myOldContextSize;
    }

    @Override
    public void close() {
      if (myContext != null) {
        trimToSize(myContext, myOldContextSize);
      }
    }
  }

  public static class CompleteContextSaver<T> implements AutoCloseable {
    private final List<T> myContext;
    private final List<T> myOldContext;

    public CompleteContextSaver(List<T> context) {
      myContext = context;
      myOldContext = new ArrayList<>(context);
    }

    public List<T> getCurrentContext() {
      return myContext;
    }

    public List<T> getOldContext() {
      return myOldContext;
    }

    @Override
    public void close() {
      myContext.clear();
      myContext.addAll(myOldContext);
    }
  }
}
