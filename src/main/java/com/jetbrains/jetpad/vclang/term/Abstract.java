package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
  }

  public interface TypeParameter extends Parameter {
    @Nonnull Expression getType();
  }

  public interface TelescopeParameter extends TypeParameter {
    @Nonnull List<? extends ReferableSourceNode> getReferableList();
  }

  // Expressions

  public interface Expression extends SourceNode {
    byte PREC = -12;
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params);
    void setWellTyped(Map<ReferableSourceNode, Binding> context, com.jetbrains.jetpad.vclang.core.expr.Expression wellTyped);
  }

  public interface Argument extends SourceNode {
    @Nonnull Expression getExpression();
    boolean isExplicit();
  }

  public interface AppExpression extends Expression {
    byte PREC = 11;
    @Nonnull Expression getFunction();
    @Nonnull Argument getArgument();
  }

  public interface ModuleCallExpression extends Expression {
    byte PREC = 12;
    @Nonnull ModulePath getPath();
    @Nullable Definition getModule();
  }

  public interface ReferenceExpression extends Expression {
    byte PREC = 12;
    @Nullable String getName();
    @Nullable Expression getExpression();
    @Nullable ReferableSourceNode getReferent();
  }

  public interface InferenceReferenceExpression extends Expression {
    @Nonnull InferenceVariable getVariable();
  }

  public interface ClassExtExpression extends Expression {
    byte PREC = 12;
    @Nonnull Expression getBaseClassExpression();
    @Nonnull Collection<? extends ClassFieldImpl> getStatements();
  }

  public interface ClassFieldImpl extends SourceNode {
    @Nonnull String getImplementedFieldName();
    @Nonnull ClassField getImplementedField();
    @Nonnull Expression getImplementation();
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
    @Nonnull Expression getExpression();
  }

  public interface LamExpression extends Expression {
    byte PREC = -5;
    @Nonnull List<? extends Parameter> getParameters();
    @Nonnull Expression getBody();
  }

  public interface LetClause extends ReferableSourceNode {
    @Nonnull Expression getTerm();
    @Nonnull List<? extends Parameter> getParameters();
    @Nullable Expression getResultType();
  }

  public interface LetExpression extends Expression {
    byte PREC = -9;

    @Nonnull List<? extends LetClause> getClauses();
    @Nonnull Expression getExpression();
  }

  public interface TupleExpression extends Expression {
    byte PREC = 12;
    @Nonnull List<? extends Expression> getFields();
  }

  public interface SigmaExpression extends Expression {
    byte PREC = -3;
    @Nonnull List<? extends TypeParameter> getParameters();
  }

  public interface PiExpression extends Expression {
    byte PREC = -4;
    @Nonnull List<? extends TypeParameter> getParameters();
    @Nonnull Expression getCodomain();
  }

  public interface BinOpExpression extends ReferenceExpression {
    @Nonnull Expression getLeft();
    @Nullable Expression getRight();

    @Nonnull
    @Override
    ReferableSourceNode getReferent();

    @Override
    default Expression getExpression() {
      return null;
    }
  }

  public static class BinOpSequenceElem {
    @Nonnull public final ReferenceExpression binOp;
    @Nullable public final Expression argument;

    public BinOpSequenceElem(@Nonnull ReferenceExpression binOp, @Nullable Expression argument) {
      this.binOp = binOp;
      this.argument = argument;
    }
  }

  public interface BinOpSequenceExpression extends Expression {
    byte PREC = 0;
    @Nonnull Expression getLeft();
    @Nonnull List<BinOpSequenceElem> getSequence();
  }

  public interface UniverseExpression extends Expression {
    byte PREC = 12;

    @Nullable LevelExpression getPLevel();
    @Nullable LevelExpression getHLevel();
  }

  public interface InferHoleExpression extends Expression {
    byte PREC = 12;
  }

  public interface GoalExpression extends Expression {
    byte PREC = 12;
    @Nullable String getName();
    @Nullable Expression getExpression();
  }

  public interface CaseExpression extends Expression {
    byte PREC = -8;
    @Nonnull List<? extends Expression> getExpressions();
    @Nonnull List<? extends FunctionClause> getClauses();
  }

  public interface ProjExpression extends Expression {
    byte PREC = 12;
    @Nonnull Expression getExpression();
    int getField();
  }

  public interface PatternContainer {
    @Nullable List<? extends Pattern> getPatterns();
  }

  public interface Clause extends SourceNode, PatternContainer {
  }

  public interface FunctionClause extends Clause {
    @Nullable Expression getExpression();

    @Nonnull
    @Override
    List<? extends Pattern> getPatterns();
  }

  public interface NumericLiteral extends Expression {
    int getNumber();
  }

  // Level expressions

  public interface LevelExpression extends SourceNode {
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface InferVarLevelExpression extends LevelExpression {
    @Nonnull InferenceLevelVariable getVariable();
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
    @Nonnull LevelExpression getExpression();
  }

  public interface MaxLevelExpression extends LevelExpression {
    @Nonnull LevelExpression getLeft();
    @Nonnull LevelExpression getRight();
  }

  // Definitions

  public interface ReferableSourceNode extends SourceNode {
    @Nullable default String getName() {
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
    @Nonnull Precedence getPrecedence();
    @Nullable Definition getParentDefinition();
    boolean isStatic();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface ClassField extends Definition {
    @Nonnull Expression getResultType();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface Implementation extends Definition {
    @Nonnull ClassField getImplementedField();
    @Nonnull Expression getImplementation();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface DefinitionCollection {
    @Nonnull Collection<? extends Definition> getGlobalDefinitions();
  }

  public interface FunctionBody extends SourceNode {}

  public interface TermFunctionBody extends FunctionBody {
    @Nonnull Expression getTerm();
  }

  public interface ElimBody extends SourceNode {
    @Nonnull List<? extends ReferenceExpression> getEliminatedReferences();
    @Nonnull List<? extends FunctionClause> getClauses();
  }

  public interface ElimFunctionBody extends FunctionBody, ElimBody {
  }

  public interface FunctionDefinition extends Definition, DefinitionCollection {
    @Nonnull FunctionBody getBody();
    @Nonnull List<? extends Parameter> getParameters();
    @Nullable Expression getResultType();
  }

  public interface DataDefinition extends Definition {
    @Nonnull List<? extends TypeParameter> getParameters();
    @Nullable List<? extends ReferenceExpression> getEliminatedReferences();
    @Nonnull List<? extends ConstructorClause> getConstructorClauses();
    boolean isTruncated();
    @Nullable UniverseExpression getUniverse();
  }

  public interface ConstructorClause extends Clause {
    @Nonnull List<? extends Constructor> getConstructors();
  }

  public interface Constructor extends Definition, ElimBody {
    @Nonnull DataDefinition getDataType();
    @Nonnull List<? extends TypeParameter> getParameters();
  }

  public interface SuperClass extends SourceNode {
    @Nonnull Expression getSuperClass();
  }

  public interface ClassDefinition extends Definition, DefinitionCollection {
    @Nonnull List<? extends TypeParameter> getPolyParameters();
    @Nonnull Collection<? extends SuperClass> getSuperClasses();
    @Nonnull Collection<? extends ClassField> getFields();
    @Nonnull Collection<? extends Implementation> getImplementations();
    @Nonnull Collection<? extends Definition> getInstanceDefinitions();
  }

  // ClassViews

  public interface ClassView extends Definition {
    @Nonnull ReferenceExpression getUnderlyingClassReference();
    @Nonnull String getClassifyingFieldName();
    @Nullable ClassField getClassifyingField();
    @Nonnull List<? extends ClassViewField> getFields();
  }

  public interface ClassViewField extends Definition {
    @Nonnull String getUnderlyingFieldName();
    @Nullable ClassField getUnderlyingField();
    @Nonnull ClassView getOwnView();
  }

  public interface ClassViewInstance extends Definition {
    boolean isDefault();
    @Nonnull List<? extends Parameter> getParameters();
    @Nonnull ReferenceExpression getClassView();
    @Nonnull Definition getClassifyingDefinition();
    @Nonnull Collection<? extends ClassFieldImpl> getClassFieldImpls();
  }

  // Patterns

  public interface Pattern extends SourceNode {
    byte PREC = 11;
    boolean isExplicit();
  }

  public interface NamePattern extends Pattern, ReferableSourceNode {
  }

  public interface ConstructorPattern extends Pattern, PatternContainer {
    @Nonnull String getConstructorName();
    @Nullable Abstract.Constructor getConstructor();

    @Nonnull
    @Override
    List<? extends Pattern> getPatterns();
  }

  public interface EmptyPattern extends Pattern {}


  public static class Precedence {
    public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

    public static final Precedence DEFAULT = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

    public final @Nonnull Associativity associativity;
    public final byte priority;

    public Precedence(@Nonnull Associativity associativity, byte priority) {
      this.associativity = associativity;
      this.priority = priority;
    }

    public Precedence(byte prec) {
      this.associativity = Associativity.NON_ASSOC;
      this.priority = prec;
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
