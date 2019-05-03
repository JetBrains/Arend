package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.PiExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.naming.reference.TCFieldReferable;

import java.util.Collections;
import java.util.List;

public class ClassField extends Definition {
  private final ClassDefinition myParentClass;
  private boolean myProperty;
  private PiExpression myType;
  private Expression myTypeLevel;
  private boolean myHideable;
  private boolean myCovariant;

  public ClassField(TCFieldReferable referable, ClassDefinition parentClass) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myParentClass = parentClass;
  }

  public ClassField(TCFieldReferable referable, ClassDefinition parentClass, PiExpression type, Expression typeLevel) {
    super(referable, TypeCheckingStatus.NO_ERRORS);
    myParentClass = parentClass;
    myType = type;
    myTypeLevel = typeLevel;
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

  public Expression getTypeLevel() {
    return myTypeLevel;
  }

  public void setTypeLevel(Expression typeLevel) {
    myTypeLevel = typeLevel;
  }

  public boolean isProperty() {
    return myProperty;
  }

  public void setIsProperty() {
    myProperty = true;
  }

  public boolean isTypeClass() {
    return myParentClass.isTypeClassField(this);
  }

  @Override
  public int getVisibleParameter() {
    return myHideable ? 0 : -1;
  }

  @Override
  public boolean isHideable() {
    return myHideable;
  }

  public void setHideable(boolean isHideable) {
    myHideable = isHideable;
  }

  public boolean isCovariant() {
    return myCovariant;
  }

  public void setCovariant(boolean isCovariant) {
    myCovariant = isCovariant;
  }

  @Override
  public List<Boolean> getGoodThisParameters() {
    return Collections.singletonList(true);
  }

  @Override
  public boolean isGoodParameter(int index) {
    return index == 0;
  }

  @Override
  public List<TypeClassParameterKind> getTypeClassParameters() {
    return myParentClass.isRecord() ? Collections.emptyList() : Collections.singletonList(TypeClassParameterKind.YES);
  }

  @Override
  public TypeClassParameterKind getTypeClassParameterKind(int index) {
    return index == 0 && !myParentClass.isRecord() ? TypeClassParameterKind.YES : TypeClassParameterKind.NO;
  }

  @Override
  public DependentLink getParameters() {
    return myType.getParameters();
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
