package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;

import java.util.List;

public class Abstract {
  public interface Expression {
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped);
  }

  public interface Argument {
    boolean getExplicit();
  }

  public interface NameArgument extends Argument {
    String getName();
  }

  public interface TypeArgument extends Argument {
    Expression getType();
  }

  public interface TelescopeArgument extends TypeArgument {
    List<String> getNames();
    String getName(int index);
  }

  public interface AppExpression extends Expression {
    byte PREC = 11;
    Expression getFunction();
    Expression getArgument();
    boolean isExplicit();
  }

  public interface DefCallExpression extends Expression {
    byte PREC = 12;
    Definition getDefinition();
  }

  public interface IndexExpression extends Expression {
    byte PREC = 12;
    int getIndex();
  }

  public interface LamExpression extends Expression {
    byte PREC = -5;
    List<? extends Argument> getArguments();
    Argument getArgument(int index);
    Expression getBody();
  }

  public interface NatExpression extends Expression {
    byte PREC = 12;
  }

  public interface NelimExpression extends Expression {
    byte PREC = 12;
  }

  public interface TupleExpression extends Expression {
    byte PREC = 12;
    List<? extends Expression> getFields();
    Expression getField(int index);
  }

  public interface SigmaExpression extends Expression {
    byte PREC = -3;
    List<? extends TypeArgument> getArguments();
    TypeArgument getArgument(int index);
  }

  public interface PiExpression extends Expression {
    byte PREC = -4;
    List<? extends TypeArgument> getArguments();
    TypeArgument getArgument(int index);
    Expression getCodomain();
  }

  public interface BinOpExpression extends Expression {
    Definition getBinOp();
    Expression getLeft();
    Expression getRight();
  }

  public interface SucExpression extends Expression {
    byte PREC = 12;
  }

  public interface UniverseExpression extends Expression {
    byte PREC = 12;
    Universe getUniverse();
  }

  public interface VarExpression extends Expression {
    byte PREC = 12;
    String getName();
  }

  public interface ZeroExpression extends Expression {
    byte PREC = 12;
  }

  public interface HoleExpression extends Expression {
    byte PREC = 12;
  }

  public interface InferHoleExpression extends HoleExpression {
    byte PREC = 12;
  }
}
