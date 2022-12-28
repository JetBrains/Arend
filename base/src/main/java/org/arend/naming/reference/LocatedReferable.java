package org.arend.naming.reference;

import org.arend.ext.module.LongName;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface LocatedReferable extends GlobalReferable {
  @Nullable ModuleLocation getLocation();
  @Nullable LocatedReferable getLocatedReferableParent();

  default @NotNull LocatedReferable getTypecheckable() {
    return this;
  }

  default @NotNull String getDescription() {
    return "";
  }

  @NotNull
  @Override
  default LongName getRefLongName() {
    List<String> longName = new ArrayList<>();
    LocatedReferable.Helper.getLocation(this, longName);
    return new LongName(longName);
  }

  class Helper {
    public static ModuleLocation getLocation(LocatedReferable referable, List<String> fullName) {
      LocatedReferable parent = referable.getLocatedReferableParent();
      if (parent == null) {
        return referable.getLocation();
      }

      ModuleLocation location = getLocation(parent, fullName);
      fullName.add(referable.textRepresentation());
      return location;
    }

    public static Scope resolveNamespace(LocatedReferable locatedReferable, ModuleScopeProvider moduleScopeProvider) {
      LocatedReferable parent = locatedReferable.getLocatedReferableParent();
      if (parent == null) {
        ModuleLocation moduleLocation = locatedReferable.getLocation();
        if (moduleLocation == null) {
          return null;
        }
        return moduleScopeProvider.forModule(moduleLocation.getModulePath());
      } else {
        Scope scope = resolveNamespace(parent, moduleScopeProvider);
        return scope == null ? null : scope.resolveNamespace(locatedReferable.textRepresentation(), true);
      }
    }

    public static Referable resolveReferable(LocatedReferable locatedReferable, ModuleScopeProvider moduleScopeProvider) {
      LocatedReferable parent = locatedReferable.getLocatedReferableParent();
      if (parent == null) {
        return locatedReferable;
      } else {
        Scope scope = resolveNamespace(parent, moduleScopeProvider);
        return scope == null ? null : scope.resolveName(locatedReferable.textRepresentation());
      }
    }
  }
}
