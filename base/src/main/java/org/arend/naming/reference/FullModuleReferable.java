package org.arend.naming.reference;

import org.arend.module.FullModulePath;
import org.jetbrains.annotations.Nullable;

public class FullModuleReferable extends ModuleReferable implements LocatedReferable {
  public FullModuleReferable(FullModulePath path) {
    super(path);
  }

  @Nullable
  @Override
  public FullModulePath getLocation() {
    return (FullModulePath) path;
  }

  @Nullable
  @Override
  public LocatedReferable getLocatedReferableParent() {
    return null;
  }
}
