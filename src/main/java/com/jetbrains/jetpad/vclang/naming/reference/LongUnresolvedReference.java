package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;
import java.util.List;

public class LongUnresolvedReference extends UnresolvedReference {
  private final List<String> myNames;

  public LongUnresolvedReference(@Nonnull String name, @Nonnull List<String> names) {
    super(name);
    myNames = names;
  }

  public List<String> getNames() {
    return myNames;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName());
    for (String name : myNames) {
      builder.append('.').append(name);
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LongUnresolvedReference that = (LongUnresolvedReference) o;

    return getName().equals(that.getName()) && myNames.equals(that.myNames);
  }

  @Override
  public int hashCode() {
    int result = getName().hashCode();
    result = 31 * result + myNames.hashCode();
    return result;
  }
}
