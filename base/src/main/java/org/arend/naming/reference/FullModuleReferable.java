package org.arend.naming.reference;

import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FullModuleReferable extends ModuleReferable implements LocatedReferable {
  private final ModuleLocation myLocation;

  public FullModuleReferable(ModuleLocation location) {
    super(location.getModulePath());
    myLocation = location;
  }

  @Nullable
  @Override
  public ModuleLocation getLocation() {
    return myLocation;
  }

  @Nullable
  @Override
  public LocatedReferable getLocatedReferableParent() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FullModuleReferable that = (FullModuleReferable) o;
    return Objects.equals(myLocation, that.myLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myLocation);
  }
}
