package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingException;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchException;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

import java.util.List;

public class LamExpression extends Expression implements Abstract.LamExpression {
  private final String variable;
  private final Expression body;

  public LamExpression(String variable, Expression expression) {
    this.variable = variable;
    this.body = expression;
  }

  @Override
  public String getVariable() {
    return variable;
  }

  @Override
  public Expression getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof LamExpression)) return false;
    LamExpression other = (LamExpression)o;
    return body.equals(other.body);
  }

  @Override
  public String toString() {
    return "\\" + variable + " -> " + body;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitLam(this);
  }

  @Override
  public void checkType(List<Definition> context, Expression expected) throws TypeCheckingException {
    Expression expectedNormalized = expected.normalize();
    if (expectedNormalized instanceof PiExpression) {
      PiExpression type = (PiExpression)expectedNormalized;
      // TODO: This is ugly. Fix it.
      context.add(new FunctionDefinition(variable, new Signature(type.getLeft()), new VarExpression(variable)));
      body.checkType(context, type.getRight());
      context.remove(context.size() - 1);
    } else {
      throw new TypeMismatchException(expectedNormalized, new PiExpression(new VarExpression("_"), new VarExpression("_")), this);
    }
  }

  @Override
  public <T> T accept(AbstractExpressionVisitor<? extends T> visitor) {
    return visitor.visitLam(this);
  }
}
