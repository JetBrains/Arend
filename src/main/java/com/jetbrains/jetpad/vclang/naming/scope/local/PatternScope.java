package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.abs.Abstract;

import java.util.List;
import java.util.function.Predicate;

public class PatternScope implements Scope {
  private final Scope myParent;
  private final List<? extends Abstract.Pattern> myPatterns;

  PatternScope(Scope parent, List<? extends Abstract.Pattern> patterns) {
    myParent = parent;
    myPatterns = patterns;
  }

  private Referable find(List<? extends Abstract.Pattern> args, Predicate<Referable> pred) {
    Scope globalScope = null;
    for (int i = args.size() - 1; i >= 0; i--) {
      List<? extends Abstract.Pattern> argArgs = args.get(i).getArguments();
      Referable ref = find(argArgs, pred);
      if (ref != null) {
        return ref;
      }
      if (argArgs.isEmpty()) {
        ref = args.get(i).getHeadReference();
        if (ref != null && !(ref instanceof GlobalReferable)) {
          if (globalScope == null) {
            globalScope = myParent.getGlobalScope();
          }
          if (globalScope.resolveName(ref.textRepresentation()) == null && pred.test(ref)) {
            return ref;
          }
        }
      }
    }
    return null;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    Referable ref = find(myPatterns, pred);
    return ref != null ? ref : myParent.find(pred);
  }

  @Override
  public Scope getGlobalScope() {
    return myParent.getGlobalScope();
  }
}
