package org.arend.util;

import java.util.List;

public class LongName {
  private final List<String> myPath;

  public LongName(List<String> path) {
    myPath = path;
  }

  public List<String> toList() {
    return myPath;
  }

  public String[] toArray() {
    return myPath.toArray(new String[0]);
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
}
