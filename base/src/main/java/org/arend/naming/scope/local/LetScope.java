package org.arend.naming.scope.local;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.DelegateScope;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class LetScope extends DelegateScope {
  private final List<? extends Abstract.LetClause> myClauses;

  public LetScope(Scope parent, List<? extends Abstract.LetClause> clauses) {
    super(parent);
    myClauses = clauses;
  }

  private Referable find(Abstract.Pattern pattern, Predicate<Referable> pred) {
    Referable ref = pattern.getHeadReference();
    if (ref != null) {
      if (pred.test(ref)) {
        return ref;
      }
    } else {
      List<? extends Abstract.Pattern> patterns = pattern.getArguments();
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
        Abstract.Pattern pattern = myClauses.get(i).getPattern();
        if (pattern != null) {
          ref = find(pattern, pred);
          if (ref != null) {
            return ref;
          }
        }
      }
    }
    return parent.find(pred);
  }

  private Referable resolveName(Abstract.Pattern pattern, String name) {
    Referable ref = pattern.getHeadReference();
    if (ref != null) {
      if (ref.textRepresentation().equals(name)) {
        return ref;
      }
    } else {
      List<? extends Abstract.Pattern> patterns = pattern.getArguments();
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
  public Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    if (kind == Referable.RefKind.EXPR || kind == null) {
      for (int i = myClauses.size() - 1; i >= 0; i--) {
        Referable ref = myClauses.get(i).getReferable();
        if (ref != null) {
          if (ref.textRepresentation().equals(name)) {
            return ref;
          }
        } else {
          Abstract.Pattern pattern = myClauses.get(i).getPattern();
          if (pattern != null) {
            ref = resolveName(pattern, name);
            if (ref != null) {
              return ref;
            }
          }
        }
      }
    }
    return parent.resolveName(name, kind);
  }
}
