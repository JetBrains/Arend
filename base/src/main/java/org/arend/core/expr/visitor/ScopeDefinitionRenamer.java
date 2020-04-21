package org.arend.core.expr.visitor;

import org.arend.core.definition.Definition;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ScopeDefinitionRenamer implements DefinitionRenamer {
  private final Scope myScope;
  private final Map<Definition, LongName> myPrefixes = new HashMap<>();

  public ScopeDefinitionRenamer(Scope scope) {
    myScope = CachingScope.make(scope);
  }

  @Override
  public @Nullable LongName getDefinitionPrefix(CoreDefinition definition) {
    if (!(definition instanceof Definition)) {
      return null;
    }

    LongName result = myPrefixes.computeIfAbsent((Definition) definition, def -> {
      LocatedReferable ref = ((Definition) definition).getReferable();
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
