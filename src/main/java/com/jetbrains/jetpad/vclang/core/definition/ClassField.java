package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.PiExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;

import java.util.List;

public class ClassField extends Definition {
  private final ClassDefinition myParentClass; // TODO[classes]
  private PiExpression myType;

  public ClassField(TCReferable referable, ClassDefinition parentClass) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myParentClass = parentClass;
  }

  public ClassField(TCReferable referable, ClassDefinition parentClass, PiExpression type) {
    super(referable, TypeCheckingStatus.NO_ERRORS);
    myParentClass = parentClass;
    myType = type;
  }

  public ClassDefinition getParentClass() {
    return myParentClass;
  }

  public void setType(PiExpression type) {
    assert myType == null;
    myType = type;
  }

  public PiExpression getType(Sort sortArgument) {
    return sortArgument == Sort.STD ? myType : new SubstVisitor(new ExprSubstitution(), sortArgument.toLevelSubstitution()).visitPi(myType, null);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    return myType == null ? null : myType.subst(new ExprSubstitution(), sortArgument.toLevelSubstitution());
  }

  @Override
  public Expression getDefCall(Sort sortArgument, List<Expression> args) {
    return FieldCallExpression.make(this, args.get(0));
  }
}
