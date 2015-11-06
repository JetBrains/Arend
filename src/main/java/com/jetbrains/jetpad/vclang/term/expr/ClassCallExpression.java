package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;
import java.util.Map;

public class ClassCallExpression extends DefCallExpression {
  private final Map<ClassField, OverrideElem> myElems;
  private final Universe myUniverse;

  public ClassCallExpression(ClassDefinition definition, Map<ClassField, OverrideElem> elems, Universe universe) {
    super(definition);
    myElems = elems;
    myUniverse = universe;
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    ClassField parent = getDefinition().getParentField();
    myElems.put(parent, new OverrideElem(null, thisExpr));
    return this;
  }

  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
  }

  public Map<ClassField, OverrideElem> getOverrideElems() {
    return myElems;
  }

  public Universe getUniverse() {
    return myUniverse;
  }

  @Override
  public UniverseExpression getType(List<Binding> context) {
    return new UniverseExpression(myUniverse);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitClassCall(this);
  }

  public static class OverrideElem {
    public Expression type;
    public Expression term;

    public OverrideElem(Expression type, Expression term) {
      this.type = type;
      this.term = term;
    }
  }
}
