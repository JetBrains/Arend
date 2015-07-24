package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}
  public interface PrettyPrintableSourceNode extends SourceNode, PrettyPrintable {}

  public interface Expression extends PrettyPrintableSourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped);
  }

  public interface Argument extends PrettyPrintableSourceNode {
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
  }

  public interface ArgumentExpression extends PrettyPrintableSourceNode {
    Expression getExpression();
    boolean isExplicit();
    boolean isHidden();
  }

  public interface AppExpression extends Expression {
    byte PREC = 11;
    Expression getFunction();
    ArgumentExpression getArgument();
  }

  public static Expression getFunction(Expression expr, List<ArgumentExpression> arguments) {
    while (expr instanceof AppExpression) {
      arguments.add(((AppExpression) expr).getArgument());
      expr = ((AppExpression) expr).getFunction();
    }
    Collections.reverse(arguments);
    return expr;
  }

  public interface DefCallExpression extends Expression {
    byte PREC = 12;
    Expression getExpression();
    com.jetbrains.jetpad.vclang.term.definition.Definition getDefinition();
    String getName();
    Definition.Fixity getFixity();
  }

  public interface ClassExtExpression extends Expression {
    byte PREC = 12;
    com.jetbrains.jetpad.vclang.term.definition.ClassDefinition getBaseClass();
    Collection<? extends FunctionDefinition> getDefinitions();
  }

  public interface NewExpression extends Expression {
    byte PREC = 11;
    Expression getExpression();
  }

  public interface IndexExpression extends Expression {
    byte PREC = 12;
    int getIndex();
  }

  public interface LamExpression extends Expression {
    byte PREC = -5;
    List<? extends Argument> getArguments();
    Expression getBody();
  }

  public interface LetClause extends Function, PrettyPrintable {
  }

  public interface LetExpression extends Expression {
    byte PREC = -9;

    List<? extends LetClause> getClauses();
    Expression getExpression();
  }

  public interface TupleExpression extends Expression {
    byte PREC = 12;
    List<? extends Expression> getFields();
  }

  public interface SigmaExpression extends Expression {
    byte PREC = -3;
    List<? extends TypeArgument> getArguments();
  }

  public interface PiExpression extends Expression {
    byte PREC = -4;
    List<? extends TypeArgument> getArguments();
    Expression getCodomain();
  }

  public interface BinOpExpression extends Expression {
    com.jetbrains.jetpad.vclang.term.definition.Definition getBinOp();
    ArgumentExpression getLeft();
    ArgumentExpression getRight();
  }

  public interface UniverseExpression extends Expression {
    byte PREC = 12;
    Universe getUniverse();
  }

  public interface VarExpression extends Expression {
    byte PREC = 12;
    String getName();
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
    Clause getOtherwise();
  }

  public interface ProjExpression extends Expression {
    byte PREC = 12;
    Expression getExpression();
    int getField();
  }

  public interface Clause extends PrettyPrintableSourceNode {
    String getName();
    Definition.Fixity getFixity();
    List<? extends NameArgument> getArguments();
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

      @Override
      public boolean equals(Object obj) {
        return this == obj || obj instanceof Precedence && associativity == ((Precedence) obj).associativity && priority == ((Precedence) obj).priority;
      }
    }

    static Precedence DEFAULT_PRECEDENCE = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

    Universe getUniverse();
    Precedence getPrecedence();
    Fixity getFixity();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface Function extends Binding {
    Definition.Arrow getArrow();
    Expression getTerm();
    List<? extends Argument> getArguments();
    Expression getResultType();
  }

  public interface FunctionDefinition extends Definition, Function {
    boolean isAbstract();
    boolean isOverridden();
  }

  public interface DataDefinition extends Definition {
    List<? extends TypeArgument> getParameters();
    List<? extends Constructor> getConstructors();
  }

  public interface ClassDefinition extends Definition {
    List<? extends Definition> getPublicFields();
  }

  public interface Constructor extends Definition {
    List<? extends TypeArgument> getArguments();
    DataDefinition getDataType();
  }
}
