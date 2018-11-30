package org.arend.typechecking.visitor;

import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReferablesCollector extends VoidConcreteExpressionVisitor<Void> {
  private final Set<? extends TCReferable> myReferables;
  private final Set<TCReferable> myResult = new HashSet<>();

  public ReferablesCollector(Set<? extends TCReferable> referables) {
    myReferables = referables;
  }

  public Set<TCReferable> getResult() {
    return myResult;
  }

  public static Set<TCReferable> getReferables(Concrete.Expression expr, Set<? extends TCReferable> referables) {
    if (referables.isEmpty()) {
      return Collections.emptySet();
    }

    ReferablesCollector collector = new ReferablesCollector(referables);
    expr.accept(collector, null);
    return collector.myResult;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent() instanceof TCReferable && myReferables.contains(expr.getReferent())) {
      myResult.add((TCReferable) expr.getReferent());
    }
    return null;
  }
}
