package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.children.ChildList;
import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
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

    @Override
    public boolean isExplicit() {
      if (getArgument() instanceof Argument) {
        return ((Argument) getArgument()).getExplicit();
      } else {
        return true;
      }
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

  public abstract static class Argument extends Expression implements Abstract.Argument {
    private final Property<Boolean> myExplicit = new ValueProperty<>(true);

    @Override
    public boolean getExplicit() {
      return myExplicit.get();
    }

    public Property<Boolean> isExplicit() {
      return myExplicit;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      throw new IllegalStateException();
    }
  }

  public static class NameArgument extends Argument implements Abstract.NameArgument {
    private final Property<String> myName = new ValueProperty<>();

    @Override
    public String getName() {
      return myName.get();
    }

    public Property<String> name() {
      return myName;
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class TypeArgument extends Argument implements Abstract.TypeArgument {
    private final ChildProperty<TypeArgument, Expression> myType = new ChildProperty<>(this);

    @Override
    public Expression getType() {
      return myType.get();
    }

    public Property<Expression> type() {
      return myType;
    }

    @Override
    public Node[] children() {
      return new Node[] { getType() };
    }
  }

  public static class TelescopeArgument extends TypeArgument implements Abstract.TelescopeArgument {
    private final ObservableList<String> myNames = new ObservableArrayList<>();

    @Override
    public ObservableList<String> getNames() {
      return myNames;
    }

    @Override
    public String getName(int index) {
      return myNames.get(index);
    }

    public ObservableList<String> names() {
      return myNames;
    }
  }

  public static class LamExpression extends Expression implements Abstract.LamExpression {
    private final ChildList<LamExpression, Argument> myArguments = new ChildList<>(this);
    private final ChildProperty<LamExpression, Expression> myBody = new ChildProperty<>(this);

    @Override
    public ObservableList<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public Argument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getBody() {
      return myBody.get();
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
      Node[] result = myArguments.toArray(new Node[myArguments.size() + 1]);
      result[myArguments.size()] = getBody();
      return result;
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
    private final ChildList<PiExpression, TypeArgument> myArguments = new ChildList<>(this);
    private final ChildProperty<PiExpression, Expression> myCodomain = new ChildProperty<>(this);

    @Override
    public ObservableList<TypeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public TypeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getCodomain() {
      return myCodomain.get();
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
      Node[] result = myArguments.toArray(new Node[myArguments.size() + 1]);
      result[myArguments.size()] = getCodomain();
      return result;
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
