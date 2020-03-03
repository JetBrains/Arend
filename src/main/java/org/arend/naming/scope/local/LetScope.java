package org.arend.naming.scope.local;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.ImportedScope;
import org.arend.naming.scope.Scope;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class LetScope implements Scope {
  private final Scope myParent;
  private final List<? extends Abstract.LetClause> myClauses;

  public LetScope(Scope parent, List<? extends Abstract.LetClause> clauses) {
    myParent = parent;
    myClauses = clauses;
  }

  private Referable find(Abstract.LetClausePattern pattern, Predicate<Referable> pred) {
    Referable ref = pattern.getReferable();
    if (ref != null) {
      if (pred.test(ref)) {
        return ref;
      }
    } else {
      List<? extends Abstract.LetClausePattern> patterns = pattern.getPatterns();
      for (int i = patterns.size() - 1; i >= 0; i--) {
        ref = find(patterns.get(i), pred);
        if (ref != null) {
          return ref;
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myClauses.size() - 1; i >= 0; i--) {
      Referable ref = myClauses.get(i).getReferable();
      if (ref != null) {
        if (pred.test(ref)) {
          return ref;
        }
      } else {
        Abstract.LetClausePattern pattern = myClauses.get(i).getPattern();
        if (pattern != null) {
          ref = find(pattern, pred);
          if (ref != null) {
            return ref;
          }
        }
      }
    }
    return myParent.find(pred);
  }

  private Referable resolveName(Abstract.LetClausePattern pattern, String name) {
    Referable ref = pattern.getReferable();
    if (ref != null) {
      if (ref.textRepresentation().equals(name)) {
        return ref;
      }
    } else {
      List<? extends Abstract.LetClausePattern> patterns = pattern.getPatterns();
      for (int i = patterns.size() - 1; i >= 0; i--) {
        ref = resolveName(patterns.get(i), name);
        if (ref != null) {
          return ref;
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    for (int i = myClauses.size() - 1; i >= 0; i--) {
      Referable ref = myClauses.get(i).getReferable();
      if (ref != null) {
        if (ref.textRepresentation().equals(name)) {
          return ref;
        }
      } else {
        Abstract.LetClausePattern pattern = myClauses.get(i).getPattern();
        if (pattern != null) {
          ref = resolveName(pattern, name);
          if (ref != null) {
            return ref;
          }
        }
      }
    }
    return myParent.resolveName(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return myParent.resolveNamespace(name, onlyInternal);
  }

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return myParent.getGlobalSubscopeWithoutOpens();
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
