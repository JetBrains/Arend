package org.arend.ext.module;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class LongName implements Comparable<LongName> {
  private final List<String> myPath;

  public LongName(List<String> path) {
    assert !path.isEmpty();
    myPath = path;
  }

  public LongName(String... name) {
    this(Arrays.asList(name));
  }

  public static LongName fromString(String path) {
    return new LongName(path.split("\\."));
  }

  public List<String> toList() {
    return myPath;
  }

  public String[] toArray() {
    return myPath.toArray(new String[0]);
  }

  public int size() {
    return myPath.size();
  }

  public String getLastName() {
    return myPath.isEmpty() ? null : myPath.get(myPath.size() - 1);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof LongName && myPath.equals(((LongName) o).myPath);
  }

  @Override
  public int hashCode() {
    return myPath.hashCode();
  }

  @Override
  public String toString() {
    return String.join(".", myPath);
  }

  @Override
  public int compareTo(@Nonnull LongName longName) {
    List<String> theirPath = longName.myPath;
    Integer mySize = myPath.size();
    Integer theirSize = theirPath.size();
    if (!mySize.equals(theirSize)) return mySize.compareTo(theirSize);

    for (int i = 0; i < mySize; i++) {
      String myName = myPath.get(i);
      String theirName = theirPath.get(i);
      int cmp = myName.compareTo(theirName);
      if (cmp != 0) {
        return cmp;
      }
    }

    return 0;
  }
}
