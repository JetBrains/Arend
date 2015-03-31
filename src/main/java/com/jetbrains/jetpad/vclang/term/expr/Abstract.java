package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;

import java.util.List;

public class Abstract {
  public static interface Expression {
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    public void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped);
  }

  public static interface Argument extends Expression {
    boolean getExplicit();
  }

  public static interface NameArgument extends Argument {
    String getName();
  }

  public static interface TypeArgument extends Argument {
    Expression getType();
  }

  public static interface TelescopeArgument extends TypeArgument {
    List<String> getNames();
    String getName(int index);
  }

  public static interface AppExpression extends Expression {
    final static int PREC = 10;
    Expression getFunction();
    Expression getArgument();
    boolean isExplicit();
  }

  public static interface DefCallExpression extends Expression {
    final static int PREC = 11;
    Definition getDefinition();
  }

  public static interface IndexExpression extends Expression {
    final static int PREC = 11;
    int getIndex();
  }

  public static interface LamExpression extends Expression {
    final static int PREC = 5;
    List<? extends Argument> getArguments();
    Argument getArgument(int index);
    Expression getBody();
  }

  public static interface NatExpression extends Expression {
    final static int PREC = 11;
  }

  public static interface NelimExpression extends Expression {
    final static int PREC = 11;
  }

  public static interface PiExpression extends Expression {
    final static int PREC = 6;
    List<? extends TypeArgument> getArguments();
    TypeArgument getArgument(int index);
    Expression getCodomain();
  }

  public static interface SucExpression extends Expression {
    final static int PREC = 11;
  }

  public static interface UniverseExpression extends Expression {
    final static int PREC = 11;
    int getLevel();
  }

  public static interface VarExpression extends Expression {
    final static int PREC = 11;
    String getName();
  }

  public static interface ZeroExpression extends Expression {
    final static int PREC = 11;
  }

  public static interface HoleExpression extends Expression {
    final static int PREC = 11;
  }
}
