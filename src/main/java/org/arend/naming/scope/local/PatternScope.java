package org.arend.naming.scope.local;

import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.ImportedScope;
import org.arend.naming.scope.Scope;
import org.arend.term.abs.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class PatternScope implements Scope {
  private final Scope myParent;
  private final List<? extends Abstract.Pattern> myPatterns;

  public PatternScope(Scope parent, List<? extends Abstract.Pattern> patterns) {
    myParent = parent;
    myPatterns = patterns;
  }

  private Referable find(List<? extends Abstract.Pattern> args, Predicate<Referable> pred) {
    Scope globalScope = null;
    for (int i = args.size() - 1; i >= 0; i--) {
      List<? extends Abstract.TypedReferable> asPatterns = args.get(i).getAsPatterns();
      for (int j = asPatterns.size() - 1; j >= 0; j--) {
        Referable ref = asPatterns.get(j).getReferable();
        if (ref != null && pred.test(ref)) {
          return ref;
        }
      }

      List<? extends Abstract.Pattern> argArgs = args.get(i).getArguments();
      Referable ref = find(argArgs, pred);
      if (ref != null) {
        return ref;
      }
      if (argArgs.isEmpty()) {
        ref = args.get(i).getHeadReference();
        if (ref != null && !(ref instanceof GlobalReferable)) {
          if (globalScope == null) {
            globalScope = myParent.getGlobalSubscope();
          }
          Referable resolved = globalScope.resolveName(ref.textRepresentation());
          if ((resolved == null || !(resolved instanceof GlobalReferable && ((GlobalReferable) resolved).getKind() == GlobalReferable.Kind.CONSTRUCTOR)) && pred.test(ref)) {
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

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Referable ref = find(myPatterns, ref2 -> ref2.textRepresentation().equals(name));
    return ref != null ? ref : myParent.resolveName(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return myParent.resolveNamespace(name, onlyInternal);
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    return myParent.getGlobalSubscope();
  }

  @Nonnull
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
