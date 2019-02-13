package org.arend.library;

import javax.annotation.Nonnull;
import java.util.Objects;

public class LibraryDependency {
  public final @Nonnull String name;

  public LibraryDependency(@Nonnull String name) {
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
}
