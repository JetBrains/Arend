package org.arend.term.concrete;

import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;

import java.util.List;
import java.util.Set;

public class FreeReferablesVisitor extends SearchConcreteVisitor<Void, TCReferable> {
  private final Set<? extends TCReferable> myReferables;

  public FreeReferablesVisitor(Set<? extends TCReferable> referables) {
    myReferables = referables;
  }

  @Override
  public TCReferable visitPattern(Concrete.Pattern pattern, Void params) {
    if (pattern instanceof Concrete.NamePattern) {
      Referable ref = ((Concrete.NamePattern) pattern).getReferable();
      if (ref instanceof TCReferable && myReferables.contains(ref)) {
        return (TCReferable) ref;
      }
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      Referable ref = ((Concrete.ConstructorPattern) pattern).getConstructor();
      if (ref instanceof TCReferable && myReferables.contains(ref)) {
        return (TCReferable) ref;
      }
    }
    return super.visitPattern(pattern, params);
  }

  @Override
  public TCReferable visitParameter(Concrete.Parameter parameter, Void params) {
    for (Referable ref : parameter.getReferableList()) {
      if (ref instanceof TCReferable && myReferables.contains(ref)) {
        return (TCReferable) ref;
      }
    }
    return null;
  }

  @Override
  public TCReferable visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    return ref instanceof TCReferable && myReferables.contains(ref) ? (TCReferable) ref : null;
  }

  @Override
  public TCReferable visitClassFieldImpl(Concrete.ClassFieldImpl fieldImpl, Void params) {
    Referable ref = fieldImpl.getImplementedField();
    return ref instanceof TCReferable && myReferables.contains(ref) ? (TCReferable) ref : super.visitClassFieldImpl(fieldImpl, params);
  }
}
