package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}

  // Parameters

  public interface Parameter extends SourceNode {
    boolean getExplicit();
  }

  public interface NameParameter extends Parameter, ReferableSourceNode {
    String getName();
  }

  public interface TypeParameter extends Parameter {
    Expression getType();
  }

  public interface TelescopeParameter extends TypeParameter {
    List<? extends ReferableSourceNode> getReferableList();
  }

  // Expressions

  public interface Expression extends SourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(Map<ReferableSourceNode, Binding> context, com.jetbrains.jetpad.vclang.core.expr.Expression wellTyped);
  }

  public interface Argument extends SourceNode {
    Expression getExpression();
    boolean isExplicit();
  }

  public interface AppExpression extends Expression {
    byte PREC = 11;
    Expression getFunction();
    Argument getArgument();
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
    List<? extends Parameter> getParameters();
    Expression getBody();
  }

  public interface LetClause extends ReferableSourceNode {
    Expression getTerm();
    List<? extends Parameter> getParameters();
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
    List<? extends TypeParameter> getParameters();
  }

  public interface PiExpression extends Expression {
    byte PREC = -4;
    List<? extends TypeParameter> getParameters();
    Expression getCodomain();
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
    public final ReferenceExpression binOp;
    public final Expression argument;

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
    List<? extends FunctionClause> getClauses();
  }

  public interface ProjExpression extends Expression {
    byte PREC = 12;
    Expression getExpression();
    int getField();
  }

  public interface Clause extends SourceNode {
    List<? extends Pattern> getPatterns();
  }

  public interface FunctionClause extends Clause {
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

  public static Collection<? extends Parameter> getParameters(Abstract.Definition definition) {
    if (definition instanceof Abstract.FunctionDefinition) {
      return ((FunctionDefinition) definition).getParameters();
    }
    if (definition instanceof Abstract.DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof Abstract.Constructor) {
      return ((Constructor) definition).getParameters();
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

  public interface DefinitionCollection {
    @Nonnull Collection<? extends Definition> getGlobalDefinitions();
  }

  public interface FunctionBody extends SourceNode {}

  public interface TermFunctionBody extends FunctionBody {
    Expression getTerm();
  }

  public interface ElimBody extends SourceNode {
    List<? extends ReferenceExpression> getEliminatedReferences();
    List<? extends FunctionClause> getClauses();
  }

  public interface ElimFunctionBody extends FunctionBody, ElimBody {
  }

  public interface FunctionDefinition extends Definition, DefinitionCollection {
    FunctionBody getBody();
    List<? extends Parameter> getParameters();
    Expression getResultType();
  }

  public interface DataDefinition extends Definition {
    List<? extends TypeParameter> getParameters();
    List<? extends ReferenceExpression> getEliminatedReferences();
    List<? extends ConstructorClause> getConstructorClauses();
    boolean isTruncated();
    UniverseExpression getUniverse();
  }

  public interface ConstructorClause extends Clause {
    List<? extends Constructor> getConstructors();
  }

  public interface Constructor extends Definition, ElimBody {
    DataDefinition getDataType();
    List<? extends TypeParameter> getParameters();
  }

  public interface SuperClass extends SourceNode {
    Expression getSuperClass();
  }

  public interface ClassDefinition extends Definition, DefinitionCollection {
    List<? extends TypeParameter> getPolyParameters();
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
    List<? extends Parameter> getParameters();
    ReferenceExpression getClassView();
    Definition getClassifyingDefinition();
    Collection<? extends ClassFieldImpl> getClassFieldImpls();
  }

  // Patterns

  public interface Pattern extends SourceNode {
    byte PREC = 11;
    boolean isExplicit();
  }

  public interface NamePattern extends Pattern, ReferableSourceNode {
    String getName();
  }

  public interface ConstructorPattern extends Pattern {
    List<? extends Pattern> getArguments();
    String getConstructorName();
    Abstract.Constructor getConstructor();
  }

  public interface EmptyPattern extends Pattern {}


  public static class Precedence {
    public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

    public static final Precedence DEFAULT = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

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
