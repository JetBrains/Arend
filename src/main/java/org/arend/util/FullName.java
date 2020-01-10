package org.arend.util;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.LocatedReferable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FullName {
  public ModulePath modulePath;
  public LongName longName;

  public FullName(ModulePath modulePath, LongName longName) {
    this.modulePath = modulePath;
    this.longName = longName;
  }

  public FullName(LocatedReferable referable) {
    List<String> name = new ArrayList<>();
    modulePath = LocatedReferable.Helper.getLocation(referable, name);
    longName = new LongName(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FullName fullName = (FullName) o;
    return Objects.equals(modulePath, fullName.modulePath) &&
      Objects.equals(longName, fullName.longName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modulePath, longName);
  }

  @Override
  public String toString() {
    return modulePath + "::" + longName;
  }
}
