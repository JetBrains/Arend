package org.arend.ext.module;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class LongName implements Comparable<LongName> {
  private final List<String> path;

  public LongName(@NotNull List<String> path) {
    this.path = path;
  }

  public LongName(String... name) {
    this(Arrays.asList(name));
  }

  public static LongName fromString(String path) {
    return new LongName(path.split("\\."));
  }

  public List<String> toList() {
    return path;
  }

  public String[] toArray() {
    return path.toArray(new String[0]);
  }

  public int size() {
    return path.size();
  }

  public String getFirstName() {
    return path.isEmpty() ? null : path.get(0);
  }
  public String getLastName() {
    return path.isEmpty() ? null : path.get(path.size() - 1);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof LongName && path.equals(((LongName) o).path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return String.join(".", path);
  }

  @Override
  public int compareTo(@NotNull LongName longName) {
    List<String> theirPath = longName.path;
    Integer mySize = path.size();
    Integer theirSize = theirPath.size();
    if (!mySize.equals(theirSize)) return mySize.compareTo(theirSize);

    for (int i = 0; i < mySize; i++) {
      String myName = path.get(i);
      String theirName = theirPath.get(i);
      int cmp = myName.compareTo(theirName);
      if (cmp != 0) {
        return cmp;
      }
    }

    return 0;
  }
}
