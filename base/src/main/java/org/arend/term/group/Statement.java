package org.arend.term.group;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;

import java.util.List;

public interface Statement {
  default Group getGroup() {
    return null;
  }

  default NamespaceCommand getNamespaceCommand() {
    return null;
  }

  default Abstract.LevelParameters getPLevelsDefinition() {
    return null;
  }

  default Abstract.LevelParameters getHLevelsDefinition() {
    return null;
  }

  default void addReferables(List<Referable> referables, Scope.Kind kind) {
    if (kind == Scope.Kind.EXPR) {
      Group subgroup = getGroup();
      if (subgroup != null) {
        LexicalScope.addSubgroup(subgroup, referables);
      }
    } else {
      Abstract.LevelParameters params = kind == Scope.Kind.PLEVEL ? getPLevelsDefinition() : getHLevelsDefinition();
      if (params != null) {
        referables.addAll(params.getReferables());
      }
    }
  }

  default Referable resolveRef(String name, Scope.Kind kind) {
    if (kind == Scope.Kind.EXPR) {
      Group subgroup = getGroup();
      if (subgroup != null) {
        return LexicalScope.resolveRef(subgroup, name);
      }
    } else {
      Abstract.LevelParameters params = kind == Scope.Kind.PLEVEL ? getPLevelsDefinition() : getHLevelsDefinition();
      if (params != null) {
        for (Referable ref : params.getReferables()) {
          if (ref.getRefName().equals(name)) {
            return ref;
          }
        }
      }
    }
    return null;
  }
}
