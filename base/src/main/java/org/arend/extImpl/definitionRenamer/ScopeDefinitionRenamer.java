package org.arend.extImpl.definitionRenamer;

import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.reference.ArendRef;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.FieldReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ScopeDefinitionRenamer implements DefinitionRenamer {
  private final Scope myScope;

  public ScopeDefinitionRenamer(Scope scope) {
    myScope = CachingScope.make(scope);
  }

  @Override
  public @Nullable LongName renameDefinition(ArendRef arendRef) {
    if (!(arendRef instanceof LocatedReferable)) {
      return null;
    }

    if (myScope.resolveName(arendRef.getRefName()) == arendRef) {
      return new LongName(Collections.emptyList());
    }

    LocatedReferable ref = (LocatedReferable) arendRef;
    List<String> list = new ArrayList<>();
    list.add(ref.getRepresentableName());
    while (true) {
      LocatedReferable parent = ref.getLocatedReferableParent();
      if ((ref.getKind() == GlobalReferable.Kind.CONSTRUCTOR || ref instanceof FieldReferable && !((FieldReferable) ref).isParameterField()) && parent != null && parent.getKind() == GlobalReferable.Kind.TYPECHECKABLE) {
        parent = parent.getLocatedReferableParent();
      }
      if (parent == null || parent instanceof ModuleReferable) {
        Collections.reverse(list);
        ModulePath modulePath;
        if (parent != null) {
          modulePath = ((ModuleReferable) parent).path;
        } else {
          ModuleLocation location = ref.getLocation();
          modulePath = location == null ? null : location.getModulePath();
        }
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

    return list.size() == 1 ? null : new LongName(list);
  }
}
