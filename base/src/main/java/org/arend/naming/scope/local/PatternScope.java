package org.arend.naming.scope.local;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.DelegateScope;
import org.arend.naming.scope.Scope;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class PatternScope extends DelegateScope {
  private final List<? extends Abstract.Pattern> myPatterns;

  public PatternScope(Scope parent, List<? extends Abstract.Pattern> patterns) {
    super(parent);
    myPatterns = patterns;
  }

  private Referable find(List<? extends Abstract.Pattern> args, Predicate<Referable> pred) {
    Scope globalScope = null;
    for (int i = args.size() - 1; i >= 0; i--) {
      Abstract.TypedReferable asPattern = args.get(i).getAsPattern();
      Referable ref = asPattern == null ? null : asPattern.getReferable();
      if (ref != null && pred.test(ref)) {
        return ref;
      }

      List<? extends Abstract.Pattern> argArgs = args.get(i).getSequence();
      ref = find(argArgs, pred);
      if (ref != null) {
        return ref;
      }
      ref = args.get(i).getSingleReferable();
      if (ref != null) {
        if (!(ref instanceof GlobalReferable)) {
          if (globalScope == null) {
            globalScope = parent.getGlobalSubscope();
          }
          Referable resolved = globalScope.resolveName(ref.textRepresentation());
          if ((resolved == null || !(resolved instanceof GlobalReferable && ((GlobalReferable) resolved).getKind().isConstructor())) && pred.test(ref)) {
            return ref;
          }
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    Referable ref = context == null || context == ScopeContext.STATIC ? find(myPatterns, pred) : null;
    return ref != null ? ref : parent.find(pred, context);
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    Referable ref = context == null || context == ScopeContext.STATIC ? find(myPatterns, ref2 -> ref2.textRepresentation().equals(name)) : null;
    return ref != null ? ref : parent.resolveName(name, context);
  }
}
