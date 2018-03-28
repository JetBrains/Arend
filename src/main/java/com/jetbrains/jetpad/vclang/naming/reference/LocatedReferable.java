package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nullable;
import java.util.List;

public interface LocatedReferable extends GlobalReferable {
  @Nullable ModulePath getLocation();
  @Nullable LocatedReferable getLocatedReferableParent();

  class Helper {
    public static ModulePath getLocation(LocatedReferable referable, List<String> fullName) {
      LocatedReferable parent = referable.getLocatedReferableParent();
      if (parent == null) {
        return referable.getLocation();
      }

      ModulePath location = getLocation(parent, fullName);
      fullName.add(referable.textRepresentation());
      return location;
    }
  }
}
