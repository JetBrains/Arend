package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.PiExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.naming.reference.TCFieldReferable;

import java.util.List;

public class ClassField extends Definition {
  private final ClassDefinition myParentClass;
  private PiExpression myType;

  public ClassField(TCFieldReferable referable, ClassDefinition parentClass) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myParentClass = parentClass;
  }

  public ClassField(TCFieldReferable referable, ClassDefinition parentClass, PiExpression type) {
    super(referable, TypeCheckingStatus.NO_ERRORS);
    myParentClass = parentClass;
    myType = type;
  }

  @Override
  public TCFieldReferable getReferable() {
    return (TCFieldReferable) super.getReferable();
  }

  public ClassDefinition getParentClass() {
    return myParentClass;
  }

  public void setType(PiExpression type) {
    myType = type;
  }

  public PiExpression getType(Sort sortArgument) {
    return sortArgument == Sort.STD ? myType : new SubstVisitor(new ExprSubstitution(), sortArgument.toLevelSubstitution()).visitPi(myType, null);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    PiExpression type = getType(sortArgument);
    params.add(type.getParameters());
    return type.getCodomain();
  }

  @Override
  public Expression getDefCall(Sort sortArgument, List<Expression> args) {
    return FieldCallExpression.make(this, sortArgument, args.get(0));
  }
}
