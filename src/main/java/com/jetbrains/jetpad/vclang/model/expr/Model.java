package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class Model {
  public static abstract class Expression extends Node implements Abstract.Expression {
    private Property<com.jetbrains.jetpad.vclang.term.expr.Expression> myWellTypedExpr = new ValueProperty<>();

    public Property<com.jetbrains.jetpad.vclang.term.expr.Expression> wellTypedExpr() {
      return myWellTypedExpr;
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped) {
      myWellTypedExpr.set(wellTyped);
    }
  }

  public static class AppExpression extends Expression implements Abstract.AppExpression {
    private final ChildProperty<AppExpression, Expression> myFunction = new ChildProperty<>(this);
    private final ChildProperty<AppExpression, Expression> myArgument = new ChildProperty<>(this);

    @Override
    public Expression getFunction() {
      return myFunction.get();
    }

    @Override
    public Expression getArgument() {
      return myArgument.get();
    }

    public Property<Expression> function() {
      return myFunction;
    }

    public Property<Expression> argument() {
      return myArgument;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[] { getFunction(), getArgument() };
    }
  }

  // TODO: Replace myName with a list of variables
  public static class Argument extends Expression {
    private final Property<Boolean> myExplicit = new ValueProperty<>(true);
    private final Property<String> myName = new ValueProperty<>();
    private final ChildProperty<Argument, Expression> myType = new ChildProperty<>(this);

    public Boolean getExplicit() {
      return myExplicit.get();
    }

    public String getName() {
      return myName.get();
    }

    public Expression getType() {
      return myType.get();
    }

    public Property<Boolean> isExplicit() {
      return myExplicit;
    }

    public Property<String> name() {
      return myName;
    }

    public Property<Expression> type() {
      return myType;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      throw new IllegalStateException();
    }

    @Override
    public Node[] children() {
      return new Node[] { getType() };
    }
  }

  public static class LamExpression extends Expression implements Abstract.LamExpression {
    private final Property<String> myVariable = new ValueProperty<>();
    private final ChildProperty<LamExpression, Expression> myBody = new ChildProperty<>(this);

    @Override
    public String getVariable() {
      return myVariable.get();
    }

    @Override
    public Expression getBody() {
      return myBody.get();
    }

    public Property<String> variable() {
      return myVariable;
    }

    public Property<Expression> body() {
      return myBody;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[] { getBody() };
    }
  }

  public static class NatExpression extends Expression implements Abstract.NatExpression {
    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNat(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class NelimExpression extends Expression implements Abstract.NelimExpression {
    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNelim(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class ParensExpression extends Expression {
    private final ChildProperty<ParensExpression, Expression> myExpression = new ChildProperty<>(this);

    public Expression getExpression() {
      return myExpression.get();
    }

    public Property<Expression> expression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return myExpression.get().accept(visitor, params);
    }

    public static Expression parens(boolean p, Expression expr) {
      if (p) {
        ParensExpression pexpr = new ParensExpression();
        pexpr.myExpression.set(expr);
        return pexpr;
      } else {
        return expr;
      }
    }

    @Override
    public Node[] children() {
      return new Node[] { getExpression() };
    }
  }

  public static class PiExpression extends Expression implements Abstract.PiExpression {
    private final ChildProperty<PiExpression, Expression> myDomain = new ChildProperty<>(this);
    private final ChildProperty<PiExpression, Expression> myCodomain = new ChildProperty<>(this);

    @Override
    public boolean isExplicit() {
      if (myDomain.get() instanceof Argument) {
        return ((Argument) myDomain.get()).getExplicit();
      } else {
        return true;
      }
    }

    @Override
    public String getVariable() {
      if (myDomain.get() instanceof Argument) {
        return ((Argument) myDomain.get()).getName();
      } else {
        return null;
      }
    }

    @Override
    public Expression getDomain() {
      if (myDomain.get() instanceof Argument) {
        return ((Argument) myDomain.get()).getType();
      } else {
        return myDomain.get();
      }
    }

    @Override
    public Expression getCodomain() {
      return myCodomain.get();
    }

    public Property<Expression> domain() {
      return myDomain;
    }

    public Property<Expression> codomain() {
      return myCodomain;
    }
    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[] { myDomain.get(), getCodomain() };
    }
  }

  public static class SucExpression extends Expression implements Abstract.SucExpression {
    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSuc(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class UniverseExpression extends Expression implements Abstract.UniverseExpression {
    private final Property<Integer> myLevel = new ValueProperty<>();

    @Override
    public int getLevel() {
      return myLevel.get();
    }

    public Property<Integer> level() {
      return myLevel;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitUniverse(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class VarExpression extends Expression implements Abstract.VarExpression {
    private final Property<String> myName = new ValueProperty<>();

    @Override
    public String getName() {
      return myName.get();
    }

    public Property<String> name() {
      return myName;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitVar(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class ZeroExpression extends Expression implements Abstract.ZeroExpression {
    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitZero(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }
}
