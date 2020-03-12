package org.arend.term.abs;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.ClassFieldKind;
import org.arend.term.FunctionKind;
import org.arend.term.NamespaceCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {
    @NotNull SourceNode getTopmostEquivalentSourceNode();
    @Nullable SourceNode getParentSourceNode();

    default boolean isLocal() {
      return true;
    }
  }

  public static abstract class SourceNodeImpl implements SourceNode {
    @NotNull
    @Override
    public SourceNode getTopmostEquivalentSourceNode() {
      return this;
    }

    @Nullable
    @Override
    public SourceNode getParentSourceNode() {
      return null;
    }
  }

  public interface Parameter extends SourceNode, org.arend.naming.reference.Parameter {
    @Nullable Object getData();
    @Nullable Expression getType();
  }

  public interface FieldParameter extends Parameter {
    boolean isClassifying();

    @Override
    default boolean isLocal() {
      return false;
    }
  }

  public interface Clause extends SourceNode {
    @Nullable Object getData();
    @NotNull List<? extends Pattern> getPatterns();
  }

  public interface FunctionClause extends Clause {
    @Nullable Expression getExpression();
  }

  public interface ConstructorClause extends Clause {
    @NotNull Collection<? extends Constructor> getConstructors();
  }

  public interface TypedReferable extends SourceNode {
    @Nullable Object getData();
    @Nullable Referable getReferable();
    @Nullable Expression getType();
  }

  public interface Pattern extends SourceNode {
    @Nullable Object getData();
    boolean isUnnamed();
    boolean isExplicit();
    @Nullable Integer getNumber();
    @Nullable Referable getHeadReference();
    @NotNull List<? extends Pattern> getArguments();
    @Nullable Expression getType();
    @NotNull List<? extends TypedReferable> getAsPatterns();
  }

  public interface Reference extends org.arend.naming.reference.Reference, SourceNode {
  }

  public interface LongReference extends Reference {
    @Nullable Reference getHeadReference();
    @NotNull Collection<? extends Reference> getTailReferences();
  }

  // Holder

  public interface ParametersHolder extends SourceNode {
    @NotNull List<? extends Parameter> getParameters();
  }

  public interface LetClausesHolder extends SourceNode {
    @NotNull Collection<? extends LetClause> getLetClauses();
  }

  public interface EliminatedExpressionsHolder extends ParametersHolder {
    @Nullable Collection<? extends Reference> getEliminatedExpressions();
  }

  public interface ClassReferenceHolder extends SourceNode {
    @Nullable ClassReferable getClassReference();
    @NotNull Collection<? extends CoClauseElement> getCoClauseElements();
  }

  public interface NamespaceCommandHolder extends SourceNode, NamespaceCommand {
    @Nullable LongReference getOpenedReference();

    @Override
    default boolean isLocal() {
      return false;
    }
  }

  // Expression

  public enum EvalKind { EVAL, PEVAL }

  public static final int INFINITY_LEVEL = -33;

  public interface Expression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(@NotNull AbstractExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  public interface CaseArgument extends SourceNode {
    @Nullable Expression getExpression();
    @Nullable Referable getReferable();
    @Nullable Expression getType();
    @Nullable Reference getEliminatedReference();
  }

  public interface CaseArgumentsHolder extends SourceNode {
    @NotNull List<? extends CaseArgument> getCaseArguments();
  }

  public interface BinOpSequenceElem extends SourceNode {
    /* @NotNull */ @Nullable Expression getExpression();
    boolean isVariable();
    boolean isExplicit();
  }

  public interface Argument extends SourceNode {
    boolean isExplicit();
    /* @NotNull */ @Nullable Expression getExpression();
  }

  public interface ClassElement extends SourceNode {
  }

  public interface CoClauseElement extends ClassElement {
    /* @NotNull */ @Nullable Reference getImplementedField();
  }

  public interface ClassFieldImpl extends CoClauseElement, ParametersHolder, ClassReferenceHolder {
    @Override @NotNull Collection<? extends ClassFieldImpl> getCoClauseElements();
    @Nullable Object getData();
    @Nullable Object getPrec();
    /* @NotNull */ @Nullable Expression getImplementation();
    boolean hasImplementation();
  }

  public interface CoClauseFunctionReference extends ClassFieldImpl {
    @Nullable LocatedReferable getFunctionReference();
  }

  public interface LetClausePattern extends SourceNode {
    boolean isIgnored();
    @Nullable Referable getReferable();
    @Nullable Abstract.Expression getType();
    @NotNull List<? extends LetClausePattern> getPatterns();
  }

  public interface LetClause extends ParametersHolder {
    @Nullable LetClausePattern getPattern();
    @Nullable Referable getReferable();
    @Nullable Expression getResultType();
    /* @NotNull */ @Nullable Expression getTerm();
  }

  public interface LevelExpression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  // Definition

  public interface ReferableDefinition extends SourceNode {
    /* @NotNull */ @Nullable LocatedReferable getReferable();

    @Override
    default boolean isLocal() {
      return false;
    }
  }

  public interface Definition extends ReferableDefinition {
    @Nullable ClassReferable getEnclosingClass();
    @Override @NotNull LocatedReferable getReferable();
    <R> R accept(AbstractDefinitionVisitor<? extends R> visitor);
  }

  public interface FunctionDefinition extends Definition, EliminatedExpressionsHolder, ClassReferenceHolder {
    @Nullable Expression getResultType();
    @Nullable Expression getResultTypeLevel();
    @Nullable Expression getTerm();
    @Override @NotNull Collection<? extends Reference> getEliminatedExpressions();
    @NotNull Collection<? extends FunctionClause> getClauses();
    @NotNull Collection<? extends LocatedReferable> getUsedDefinitions();
    boolean withTerm();
    boolean isCowith();
    FunctionKind getFunctionKind();
    @Nullable Reference getImplementedField();
  }

  public interface DataDefinition extends Definition, EliminatedExpressionsHolder {
    boolean isTruncated();
    @Nullable Expression getUniverse();
    @NotNull Collection<? extends ConstructorClause> getClauses();
    @NotNull Collection<? extends LocatedReferable> getUsedDefinitions();
  }

  public interface ClassDefinition extends Definition, ParametersHolder, ClassReferenceHolder  {
    @Override @NotNull ClassReferable getReferable();
    @Override @NotNull List<? extends FieldParameter> getParameters();
    @Override @NotNull Collection<? extends ClassFieldImpl> getCoClauseElements();
    boolean isRecord();
    boolean withoutClassifying();
    @NotNull Collection<? extends Reference> getSuperClasses();
    @NotNull Collection<? extends ClassElement> getClassElements();
    @NotNull Collection<? extends LocatedReferable> getUsedDefinitions();
  }

  public interface Constructor extends ReferableDefinition, EliminatedExpressionsHolder {
    @Override @NotNull LocatedReferable getReferable();
    @Override @NotNull Collection<? extends Reference> getEliminatedExpressions();
    @NotNull Collection<? extends FunctionClause> getClauses();
    @Nullable Expression getResultType();
  }

  public interface ClassField extends ClassElement, ReferableDefinition, ParametersHolder {
    ClassFieldKind getClassFieldKind();
    /* @NotNull */ @Nullable Expression getResultType();
    @Nullable Expression getResultTypeLevel();
  }

  public interface OverriddenField extends ClassElement, ParametersHolder {
    @Nullable Object getData();
    /* @NotNull */ @Nullable Reference getOverriddenField();
    /* @NotNull */ @Nullable Expression getResultType();
    @Nullable Expression getResultTypeLevel();
  }
}
