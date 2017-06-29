package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}

  // Arguments

  public interface Argument extends SourceNode {
    boolean getExplicit();
  }

  public interface NameArgument extends Argument {
    ReferableSourceNode getReferable();
  }

  public interface TypeArgument extends Argument {
    Expression getType();
  }

  public interface TelescopeArgument extends TypeArgument {
    List<? extends ReferableSourceNode> getReferableList();
  }

  // Expressions

  public interface Expression extends SourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(Map<ReferableSourceNode, Binding> context, com.jetbrains.jetpad.vclang.core.expr.Expression wellTyped);
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

  public interface ReferenceExpression extends Expression {
    byte PREC = 12;
    String getName();
    Expression getExpression();
    ReferableSourceNode getReferent();
  }

  public interface InferenceReferenceExpression extends Expression {
    InferenceVariable getVariable();
  }

  public interface ClassExtExpression extends Expression {
    byte PREC = 12;
    Expression getBaseClassExpression();
    Collection<? extends ClassFieldImpl> getStatements();
  }

  public interface ClassFieldImpl extends SourceNode {
    String getImplementedFieldName();
    ClassField getImplementedField();
    Expression getImplementation();
  }

  public static ClassDefinition getUnderlyingClassDef(Expression expr) {
    if (expr instanceof ReferenceExpression) {
      ReferableSourceNode definition = ((ReferenceExpression) expr).getReferent();
      if (definition instanceof ClassDefinition) {
        return (ClassDefinition) definition;
      }
      if (definition instanceof ClassView) {
        return (ClassDefinition) ((ClassView) definition).getUnderlyingClassReference().getReferent();
      }
    }

    if (expr instanceof ClassExtExpression) {
      return getUnderlyingClassDef(((ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  public static ClassView getUnderlyingClassView(Expression expr) {
    if (expr instanceof ReferenceExpression) {
      ReferableSourceNode definition = ((ReferenceExpression) expr).getReferent();
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

  public interface LetClause extends ReferableSourceNode {
    Expression getTerm();
    List<? extends Argument> getArguments();
    Expression getResultType();
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

  public static Expression getCodomain(Expression expr, List<TypeArgument> arguments) {
    Expression codomain = expr;

    while (codomain instanceof PiExpression) {
      PiExpression pi = (PiExpression)codomain;
      codomain = pi.getCodomain();
      arguments.addAll(pi.getArguments());
    }

    return codomain;
  }

  public interface BinOpExpression extends ReferenceExpression {
    Expression getLeft();
    Expression getRight();

    @Override
    Definition getReferent();

    @Override
    default Expression getExpression() {
      return null;
    }
  }

  public static class BinOpSequenceElem {
    public ReferenceExpression binOp;
    public Expression argument;

    public BinOpSequenceElem(ReferenceExpression binOp, Expression argument) {
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

    LevelExpression getPLevel();
    LevelExpression getHLevel();
  }

  public interface InferHoleExpression extends Expression {
    byte PREC = 12;
  }

  public interface ErrorExpression extends Expression {
    byte PREC = 12;
    Expression getExpr();
  }

  public interface CaseExpression extends Expression {
    byte PREC = -8;
    List<? extends Expression> getExpressions();
    List<? extends Clause> getClauses();
  }

  public interface ProjExpression extends Expression {
    byte PREC = 12;
    Expression getExpression();
    int getField();
  }

  public interface Clause extends SourceNode {
    List<? extends Pattern> getPatterns();
    Expression getExpression();
  }

  public interface NumericLiteral extends Expression {
    int getNumber();
  }

  // Level expressions

  public interface LevelExpression extends SourceNode {
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface InferVarLevelExpression extends LevelExpression {
    InferenceLevelVariable getVariable();
  }

  public interface PLevelExpression extends LevelExpression {
  }

  public interface HLevelExpression extends LevelExpression {
  }

  public interface InfLevelExpression extends LevelExpression {
  }

  public interface NumberLevelExpression extends LevelExpression {
    int getNumber();
  }

  public interface SucLevelExpression extends LevelExpression {
    LevelExpression getExpression();
  }

  public interface MaxLevelExpression extends LevelExpression {
    LevelExpression getLeft();
    LevelExpression getRight();
  }

  // Definitions

  public interface ReferableSourceNode extends SourceNode {
    default String getName() {
      return toString();
    }
  }

  public static Collection<? extends Abstract.Argument> getArguments(Abstract.Definition definition) {
    if (definition instanceof Abstract.FunctionDefinition) {
      return ((FunctionDefinition) definition).getArguments();
    }
    if (definition instanceof Abstract.DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof Abstract.Constructor) {
      return ((Constructor) definition).getArguments();
    }
    return null;
  }

  public interface Definition extends ReferableSourceNode {
    Precedence getPrecedence();
    Definition getParentDefinition();
    boolean isStatic();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface ClassField extends Definition {
    Expression getResultType();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface Implementation extends Definition {
    ClassField getImplementedField();
    Expression getImplementation();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface StatementCollection {
    Collection<? extends Statement> getGlobalStatements();
  }

  public interface FunctionBody extends SourceNode {}

  public interface TermFunctionBody extends FunctionBody {
    Expression getTerm();
  }

  public interface ElimFunctionBody extends FunctionBody {
    List<? extends ReferenceExpression> getExpressions();
    List<? extends Clause> getClauses();
  }

  public interface FunctionDefinition extends Definition, StatementCollection {
    FunctionBody getBody();
    List<? extends Argument> getArguments();
    Expression getResultType();
  }

  public interface DataDefinition extends Definition {
    List<? extends TypeArgument> getParameters();
    List<? extends ReferenceExpression> getEliminatedReferences();
    List<? extends Constructor> getConstructors();
    Collection<? extends Condition> getConditions();
    boolean isTruncated();
    Expression getUniverse();
  }

  public interface SuperClass extends SourceNode {
    Expression getSuperClass();
  }

  public interface ClassDefinition extends Definition, StatementCollection {
    List<? extends TypeArgument> getPolyParameters();
    Collection<? extends SuperClass> getSuperClasses();
    Collection<? extends ClassField> getFields();
    Collection<? extends Implementation> getImplementations();
    Collection<? extends Definition> getInstanceDefinitions();
  }

  // ClassViews

  public interface ClassView extends Definition {
    ReferenceExpression getUnderlyingClassReference();
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
    ReferenceExpression getClassView();
    Definition getClassifyingDefinition();
    Collection<? extends ClassFieldImpl> getClassFieldImpls();
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

  public interface Pattern extends SourceNode {
    byte PREC = 11;
    boolean isExplicit();
    void setWellTyped(com.jetbrains.jetpad.vclang.core.pattern.Pattern pattern);
  }

  public interface NamePattern extends Pattern {
    String getName();
    ReferableSourceNode getReferent();
  }

  public interface ConstructorPattern extends Pattern {
    List<Pattern> getArguments();
    String getConstructorName();
    Abstract.Constructor getConstructor();
  }

  public interface EmptyPattern extends Pattern {}

  public interface Constructor extends Definition {
    List<Pattern> getPatterns();
    List<? extends TypeArgument> getArguments();
    DataDefinition getDataType();
  }

  public interface Condition extends SourceNode {
    List<Pattern> getPatterns();
    String getConstructorName();
    Expression getTerm();
    void setWellTyped(ElimTreeNode condition);
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
