package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;

import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}

  public interface Expression extends SourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped);
  }

  public interface Argument extends SourceNode, PrettyPrintable {
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

  public interface ElimExpression extends Expression {
    byte PREC = -8;
    enum ElimType { ELIM, CASE }
    ElimType getElimType();
    Expression getExpression();
    List<? extends Clause> getClauses();
    Clause getClause(int index);
  }

  public interface Clause extends SourceNode {
    String getName();
    List<? extends Argument> getArguments();
    Argument getArgument(int index);
    Definition.Arrow getArrow();
    Expression getExpression();
  }

  public interface Binding extends SourceNode {
    String getName();
  }

  public interface Definition extends Binding {
    enum Arrow { LEFT, RIGHT }
    enum Fixity { PREFIX, INFIX }
    enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

    class Precedence {
      public Associativity associativity;
      public byte priority;

      public Precedence(Associativity associativity, byte priority) {
        this.associativity = associativity;
        this.priority = priority;
      }

      @Override
      public String toString() {
        String result = "infix";
        if (associativity == Associativity.LEFT_ASSOC) {
          result += "l";
        }
        if (associativity == Associativity.RIGHT_ASSOC) {
          result += "r";
        }
        return result + " " + priority;
      }
    }

    Precedence DEFAULT_PRECEDENCE = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

    Universe getUniverse();
    Precedence getPrecedence();
    Fixity getFixity();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface FunctionDefinition extends Definition {
    Definition.Arrow getArrow();
    Expression getTerm();
    List<? extends TelescopeArgument> getArguments();
    TelescopeArgument getArgument(int index);
    Expression getResultType();
  }

  public interface DataDefinition extends Definition {
    List<? extends TypeArgument> getParameters();
    TypeArgument getParameter(int index);
    List<? extends Constructor> getConstructors();
    Constructor getConstructor(int index);
  }

  public interface Constructor extends Definition {
    List<? extends TypeArgument> getArguments();
    TypeArgument getArgument(int index);
    DataDefinition getDataType();
  }
}
