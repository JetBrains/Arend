package org.arend.library;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LibraryDependency implements Comparable<LibraryDependency> {
  public final @NotNull String name;

  public LibraryDependency(@NotNull String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LibraryDependency that = (LibraryDependency) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(LibraryDependency another) {
    int r = name.compareToIgnoreCase(another.name);
    return r != 0 ? r : name.compareTo(another.name);
  }
}
