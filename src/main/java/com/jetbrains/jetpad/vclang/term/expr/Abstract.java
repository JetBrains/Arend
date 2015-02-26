package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;

public class Abstract {
  public static interface Expression {
    abstract <T> T accept(AbstractExpressionVisitor<? extends T> visitor);
  }

  public static interface AppExpression extends Expression {
    final static int PREC = 10;
    Expression getFunction();
    Expression getArgument();
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
    String getVariable();
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
    boolean isExplicit();
    String getVariable();
    Expression getLeft();
    Expression getRight();
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
}
