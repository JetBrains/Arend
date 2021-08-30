package org.arend.extImpl.definitionRenamer;

import org.arend.core.definition.Definition;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.reference.ArendRef;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ModuleReferable;
import org.arend.naming.reference.TCReferable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConflictDefinitionRenamer extends VoidExpressionVisitor<Void> implements DefinitionRenamer {
  // Names that we already met
  private final Set<String> myNames = new HashSet<>();

  // If this map contains a pair (s,d), then s is in myNames and d is not in myDefLongNames.
  // If s is in myNames, but not in this map, then we already found a conflict.
  private final Map<String, CoreDefinition> myNotRenamedDefs = new HashMap<>();

  // The result map
  private final Map<TCReferable, LongName> myDefLongNames = new HashMap<>();

  @Override
  public @Nullable LongName renameDefinition(ArendRef ref) {
    return ref instanceof TCReferable ? myDefLongNames.get(ref) : null;
  }

  private void rename(Definition definition) {
    LocatedReferable ref = definition.getRef().getLocatedReferableParent();
    if (ref == null || ref instanceof ModuleReferable) {
      ModulePath modulePath;
      if (ref != null) {
        modulePath = ((ModuleReferable) ref).path;
      } else {
        ModuleLocation location = definition.getRef().getLocation();
        modulePath = location == null ? null : location.getModulePath();
      }
      if (modulePath != null) {
        List<String> fullName = new ArrayList<>(modulePath.size() + 1);
        fullName.addAll(modulePath.toList());
        fullName.add(definition.getReferable().getRepresentableName());
        myDefLongNames.put(definition.getRef(), new LongName(fullName));
      }
    } else {
      List<String> list = new ArrayList<>();
      while (ref != null && !(ref instanceof ModuleReferable)) {
        list.add(ref.getRefName());
        ref = ref.getLocatedReferableParent();
      }
      Collections.reverse(list);
      list.add(definition.getReferable().getRepresentableName());
      myDefLongNames.put(definition.getRef(), new LongName(list));
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    String name = expr.getDefinition().getName();
    if (!myNames.add(name)) {
      CoreDefinition definition = myNotRenamedDefs.get(name);
      if (definition != expr.getDefinition()) { // if expr.getDefinition() is not yet renamed, we shouldn't rename it now since there are no conflicts yet
        if (definition != null) { // if definition is not null, then we found a conflict
          myNotRenamedDefs.remove(name);
          if (definition instanceof Definition) {
            rename((Definition) definition);
          }
        }
        if (!myDefLongNames.containsKey(expr.getDefinition().getRef())) {
          rename(expr.getDefinition());
        }
      }
    } else {
      myNotRenamedDefs.put(name, expr.getDefinition());
    }
    return super.visitDefCall(expr, params);
  }
}
