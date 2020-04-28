package org.arend.core.expr.visitor;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ScopeDefinitionRenamer implements DefinitionRenamer {
  private final Scope myScope;
  private final Map<LocatedReferable, LongName> myPrefixes = new HashMap<>();

  public ScopeDefinitionRenamer(Scope scope) {
    myScope = CachingScope.make(scope);
  }

  @Override
  public @Nullable LongName getDefinitionPrefix(ArendRef arendRef) {
    if (!(arendRef instanceof LocatedReferable)) {
      return null;
    }

    LongName result = myPrefixes.computeIfAbsent((LocatedReferable) arendRef, ref -> {
      if (myScope.resolveName(ref.getRefName()) == ref) {
        return new LongName(Collections.emptyList());
      }

      List<String> list = new ArrayList<>();
      while (true) {
        LocatedReferable parent = ref.getLocatedReferableParent();
        if (parent == null || parent instanceof ModuleReferable) {
          Collections.reverse(list);
          ModulePath modulePath = parent != null ? ((ModuleReferable) parent).path : ref.getLocation();
          if (modulePath != null) {
            list.addAll(0, modulePath.toList());
          }
          break;
        } else {
          ref = parent;
          list.add(ref.getRefName());
          if (myScope.resolveName(ref.getRefName()) == ref) {
            Collections.reverse(list);
            break;
          }
        }
      }

      return new LongName(list);
    });

    return result.toList().isEmpty() ? null : result;
  }
}
