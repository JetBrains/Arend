package org.arend.term.concrete;

import org.arend.naming.reference.Referable;
import org.arend.typechecking.visitor.VoidConcreteVisitor;

import java.util.Set;

public class LocalFreeReferableVisitor extends VoidConcreteVisitor<Void> {
  private final Set<? extends Referable> myReferables;
  private Referable myFound;

  public LocalFreeReferableVisitor(Set<? extends Referable> referables) {
    myReferables = referables;
  }

  public Referable getFound() {
    return myFound;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (myReferables.contains(expr.getReferent())) {
      myFound = expr.getReferent();
    }
    return null;
  }
}
