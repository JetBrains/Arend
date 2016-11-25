package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.parser.Precedence;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

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
    void setWellTyped(List<com.jetbrains.jetpad.vclang.term.context.binding.Binding> context, com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped);
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
    List<String> getPath();
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

  public interface BinOpExpression extends Expression {
    Definition getResolvedBinOp();
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
      public int myPLevel;
      public int myHLevel;

      public static final int NOT_TRUNCATED = -20;
      public static final int PROP = -1;
      public static final int SET = 0;

      public Universe(int PLevel, int HLevel) {
        myPLevel = PLevel;
        myHLevel = HLevel;
      }

      public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) return false;
        Universe u = (Universe)obj;
        return myPLevel == u.myPLevel && myHLevel == u.myHLevel;
      }

      @Override
      public String toString() {
        if (myHLevel == PROP) return "\\Prop";
        if (myHLevel == SET) return "\\Set" + myPLevel;
        return "\\" + (myHLevel == NOT_TRUNCATED ? "" : myHLevel + "-") + "Type" + myPLevel;
      }
    }

    Universe getUniverse();
  }

  public interface PolyUniverseExpression extends Expression {
    byte PREC = 12;

    Expression getPLevel();
    Expression getHLevel();
  }

  public interface TypeOmegaExpression extends Expression {
    byte PREC = 12;
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
    Definition getImplementedField();
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
    List<String> getModulePath();
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
    void setWellTyped(com.jetbrains.jetpad.vclang.term.pattern.Pattern pattern);
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
    void setWellTyped(com.jetbrains.jetpad.vclang.term.definition.Condition condition);
  }
}
