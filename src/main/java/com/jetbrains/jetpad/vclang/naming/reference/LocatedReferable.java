package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.scopeprovider.ModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nullable;
import java.util.List;

public interface LocatedReferable extends GlobalReferable {
  @Nullable ModulePath getLocation();
  @Nullable LocatedReferable getLocatedReferableParent();
  @Nullable LocatedReferable getUnderlyingReference();

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

    public static Scope resolveNamespace(LocatedReferable locatedReferable, ModuleScopeProvider moduleScopeProvider) {
      LocatedReferable parent = locatedReferable.getLocatedReferableParent();
      if (parent == null) {
        ModulePath modulePath = locatedReferable.getLocation();
        if (modulePath == null) {
          return null;
        }
        return moduleScopeProvider.forModule(modulePath);
      } else {
        Scope scope = resolveNamespace(parent, moduleScopeProvider);
        return scope == null ? null : scope.resolveNamespace(locatedReferable.textRepresentation());
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
