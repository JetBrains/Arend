package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}

  // Arguments

  public interface Argument extends SourceNode {
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

  // Expressions

  public interface Expression extends SourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(List<Binding> context, com.jetbrains.jetpad.vclang.core.expr.Expression wellTyped);
  }

  public interface ArgumentExpression extends SourceNode {
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

  public interface ModuleCallExpression extends Expression {
    byte PREC = 12;
    ModulePath getPath();
    Definition getModule();
  }

  public interface DefCallExpression extends Expression {
    byte PREC = 12;
    String getName();
    Expression getExpression();
    Definition getReferent();
  }

  public interface ClassExtExpression extends Expression {
    byte PREC = 12;
    Expression getBaseClassExpression();
    Collection<? extends ClassFieldImpl> getStatements();
  }

  public interface ClassFieldImpl extends SourceNode {
    String getImplementedFieldName();
    Definition getImplementedField();
    Expression getImplementation();
  }

  public static ClassDefinition getUnderlyingClassDef(Expression expr) {
    if (expr instanceof DefCallExpression) {
      Definition definition = ((DefCallExpression) expr).getReferent();
      if (definition instanceof ClassDefinition) {
        return (ClassDefinition) definition;
      }
      if (definition instanceof ClassView) {
        return (ClassDefinition) ((ClassView) definition).getUnderlyingClassDefCall().getReferent();
      }
    }

    if (expr instanceof ClassExtExpression) {
      return getUnderlyingClassDef(((ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  public static ClassView getUnderlyingClassView(Expression expr) {
    if (expr instanceof DefCallExpression) {
      Definition definition = ((DefCallExpression) expr).getReferent();
      if (definition instanceof ClassView) {
        return (ClassView) definition;
      }
    }

    if (expr instanceof ClassExtExpression) {
      return getUnderlyingClassView(((ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  public interface NewExpression extends Expression {
    byte PREC = 11;
    Expression getExpression();
  }

  public interface LamExpression extends Expression {
    byte PREC = -5;
    List<? extends Argument> getArguments();
    Expression getBody();
  }

  public interface LetClause extends Function {
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

  public interface BinOpExpression extends DefCallExpression {
    Expression getLeft();
    Expression getRight();
  }

  public static class BinOpSequenceElem {
    public DefCallExpression binOp;
    public Expression argument;

    public BinOpSequenceElem(DefCallExpression binOp, Expression argument) {
      this.binOp = binOp;
      this.argument = argument;
    }
  }

  public interface BinOpSequenceExpression extends Expression {
    byte PREC = 0;
    Expression getLeft();
    List<BinOpSequenceElem> getSequence();
  }

  public interface UniverseExpression extends Expression {
    byte PREC = 12;

    class Universe {
      public int pLevel;
      public int hLevel;

      public static final int NOT_TRUNCATED = -20;
      public static final int PROP = -1;
      public static final int SET = 0;

      public Universe(int pLevel, int hLevel) {
        this.pLevel = pLevel;
        this.hLevel = hLevel;
      }

      public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) return false;
        Universe u = (Universe)obj;
        return pLevel == u.pLevel && hLevel == u.hLevel;
      }

      @Override
      public String toString() {
        if (hLevel == PROP) return "\\Prop";
        if (hLevel == SET) return "\\Set" + pLevel;
        return "\\" + (hLevel == NOT_TRUNCATED ? "" : hLevel + "-") + "Type" + pLevel;
      }
    }

    Universe getUniverse();
  }

  public interface PolyUniverseExpression extends Expression {
    byte PREC = 12;

    Expression getPLevel();
    int getHLevel();
  }

  public interface InferHoleExpression extends Expression {
    byte PREC = 12;
  }

  public interface ErrorExpression extends Expression {
    byte PREC = 12;
    Expression getExpr();
  }

  public interface ElimCaseExpression extends Expression {
    byte PREC = -8;
    List<? extends Expression> getExpressions();
    List<? extends Clause> getClauses();
  }

  public interface CaseExpression extends ElimCaseExpression {
    String FUNCTION_NAME = "\\caseF";
    String ARGUMENT_NAME = "\\caseA";
  }

  public interface ElimExpression extends ElimCaseExpression {
  }

  public interface ProjExpression extends Expression {
    byte PREC = 12;
    Expression getExpression();
    int getField();
  }

  public interface PatternContainer {
    List<? extends Pattern> getPatterns();
  }

  public interface Clause extends SourceNode, PatternContainer {
    Definition.Arrow getArrow();
    Expression getExpression();
  }

  public interface NumericLiteral extends Expression {
    int getNumber();
  }

  // Definitions

  public interface ReferableSourceNode extends SourceNode {
    String getName();
  }

  public interface Definition extends ReferableSourceNode {
    enum Arrow { LEFT, RIGHT }
    Precedence getPrecedence();
    Definition getParent();
    boolean isStatic();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface Function extends ReferableSourceNode {
    Definition.Arrow getArrow();
    Expression getTerm();
    List<? extends Argument> getArguments();
    Expression getResultType();
  }

  public interface ClassField extends Definition {
    Expression getResultType();

    @Override
    ClassDefinition getParent();
  }

  public interface Implementation extends Definition {
    ClassField getImplementedField();
    Expression getImplementation();

    @Override
    ClassDefinition getParent();
  }

  public interface FunctionDefinition extends Definition, Function {
    Collection<? extends Statement> getStatements();
  }

  public interface DataDefinition extends Definition {
    List<? extends TypeArgument> getParameters();
    List<? extends Constructor> getConstructors();
    Collection<? extends Condition> getConditions();
    Expression getUniverse();
  }

  public interface SuperClass extends SourceNode {
    Expression getSuperClass();
  }

  public interface ClassDefinition extends Definition {
    List<? extends TypeArgument> getPolyParameters();
    Collection<? extends SuperClass> getSuperClasses();
    Collection<? extends ClassField> getFields();
    Collection<? extends Implementation> getImplementations();

    Collection<? extends Statement> getGlobalStatements();
    Collection<? extends Definition> getInstanceDefinitions();
  }

  // ClassViews

  public interface ClassView extends Definition {
    DefCallExpression getUnderlyingClassDefCall();
    String getClassifyingFieldName();
    ClassField getClassifyingField();
    List<? extends ClassViewField> getFields();
  }

  public interface ClassViewField extends Definition {
    String getUnderlyingFieldName();
    ClassField getUnderlyingField();
    ClassView getOwnView();
  }

  public interface ClassViewInstance extends Definition {
    boolean isDefault();
    List<? extends Argument> getArguments();
    Expression getTerm();
  }

  // Statements

  public interface Statement extends SourceNode {
    <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface DefineStatement extends Statement {
    Definition getDefinition();
  }

  public interface NamespaceCommandStatement extends Statement {
    enum Kind { OPEN, EXPORT }

    Kind getKind();
    ModulePath getModulePath();
    List<String> getPath();

    Definition getResolvedClass();

    boolean isHiding();
    List<String> getNames();
  }

  // Patterns

  public interface PatternArgument extends SourceNode {
    boolean isHidden();
    boolean isExplicit();
    Pattern getPattern();
  }

  public interface Pattern extends SourceNode {
    void setWellTyped(com.jetbrains.jetpad.vclang.core.pattern.Pattern pattern);
  }

  public interface NamePattern extends Pattern {
    String getName();
  }

  public interface ConstructorPattern extends Pattern {
    List<? extends PatternArgument> getArguments();
    String getConstructorName();
  }

  public interface AnyConstructorPattern extends Pattern {}

  public interface Constructor extends Definition {
    List<? extends PatternArgument> getPatterns();
    List<? extends TypeArgument> getArguments();
    DataDefinition getDataType();
  }

  public interface Condition extends SourceNode {
    List<? extends PatternArgument> getPatterns();
    String getConstructorName();
    Expression getTerm();
    void setWellTyped(com.jetbrains.jetpad.vclang.core.definition.Condition condition);
  }


  public static class Precedence {
    public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

    public static Precedence DEFAULT = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

    public final Associativity associativity;
    public final byte priority;

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
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Precedence that = (Precedence) o;
      return priority == that.priority && associativity == that.associativity;
    }

    @Override
    public int hashCode() {
      return  31 * associativity.hashCode() + (int) priority;
    }
  }
}
