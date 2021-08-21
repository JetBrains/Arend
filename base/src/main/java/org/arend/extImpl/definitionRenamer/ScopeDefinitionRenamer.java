package org.arend.extImpl.definitionRenamer;

import org.arend.ext.module.LongReference;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopeDefinitionRenamer implements DefinitionRenamer {
  private final Scope myScope;

  public ScopeDefinitionRenamer(Scope scope) {
    myScope = CachingScope.make(scope);
  }

  @Override
  public @Nullable LongReference renameDefinition(ArendRef arendRef) {
    if (!(arendRef instanceof LocatedReferable)) {
      return null;
    }

    if (myScope.resolveName(arendRef.getRefName()) == arendRef) {
      return LongReference.EMPTY;
    }

    LocatedReferable ref = (LocatedReferable) arendRef;
    List<ArendRef> list = new ArrayList<>();
    list.add(ref);
    while (true) {
      LocatedReferable parent = ref.getLocatedReferableParent();
      if ((ref.getKind() == GlobalReferable.Kind.CONSTRUCTOR || ref instanceof FieldReferable && !((FieldReferable) ref).isParameterField()) && parent != null && parent.getKind().isTypecheckable()) {
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
          list.add(0, parent);
        }
        break;
      } else {
        ref = parent;
        list.add(ref);
        if (myScope.resolveName(ref.getRefName()) == ref) {
          Collections.reverse(list);
          break;
        }
      }
    }

    return list.size() == 1 ? null : new LongReference(list);
  }
}
