package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;

import java.util.List;
import java.util.Map;

public class Abstract {
  public interface Expression {
    byte PREC = -12;
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

  public interface InferHoleExpression extends Expression {
    byte PREC = 12;
  }

  public interface ErrorExpression extends Expression {
    byte PREC = 12;
    Expression getExpr();
  }

  public interface Definition extends PrettyPrintable {
    enum Arrow { LEFT, RIGHT }
    Universe getUniverse();
    Definition checkTypes(Map<String, Definition> globalContext, List<Binding> localContext, List<TypeCheckingError> errors);
  }
}
