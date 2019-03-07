package org.arend.term.abs;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.ClassFieldKind;
import org.arend.term.Fixity;
import org.arend.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public static class ErrorData {
    public final Object cause;
    public final String message;

    public ErrorData(Object cause, String message) {
      this.cause = cause;
      this.message = message;
    }
  }

  public interface SourceNode {
    @Nonnull SourceNode getTopmostEquivalentSourceNode();
    @Nullable SourceNode getParentSourceNode();
    @Nullable ErrorData getErrorData();

    default boolean isLocal() {
      return true;
    }
  }

  public static abstract class SourceNodeImpl implements SourceNode {
    @Nonnull
    @Override
    public SourceNode getTopmostEquivalentSourceNode() {
      return this;
    }

    @Nullable
    @Override
    public SourceNode getParentSourceNode() {
      return null;
    }

    @Nullable
    @Override
    public ErrorData getErrorData() {
      return null;
    }
  }

  public interface Parameter extends SourceNode {
    @Nullable Object getData();
    boolean isExplicit();
    @Nonnull List<? extends Referable> getReferableList();
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
    @Nonnull List<? extends Pattern> getPatterns();
  }

  public interface FunctionClause extends Clause {
    @Nullable Expression getExpression();
  }

  public interface ConstructorClause extends Clause {
    @Nonnull Collection<? extends Constructor> getConstructors();
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
    @Nonnull List<? extends Pattern> getArguments();
    @Nullable Expression getType();
    @Nonnull List<? extends TypedReferable> getAsPatterns();
  }

  public interface Reference extends org.arend.naming.reference.Reference, SourceNode {
  }

  public interface LongReference extends Reference {
    @Nullable Reference getHeadReference();
    @Nonnull Collection<? extends Reference> getTailReferences();
  }

  // Holder

  public interface ParametersHolder extends SourceNode {
    @Nonnull List<? extends Parameter> getParameters();
  }

  public interface LetClausesHolder extends SourceNode {
    @Nonnull Collection<? extends LetClause> getLetClauses();
  }

  public interface EliminatedExpressionsHolder extends ParametersHolder {
    @Nullable Collection<? extends Reference> getEliminatedExpressions();
  }

  public interface ClassReferenceHolder extends SourceNode {
    @Nullable ClassReferable getClassReference();
    @Nonnull Collection<? extends ClassFieldImpl> getClassFieldImpls();
  }

  public interface NamespaceCommandHolder extends SourceNode, NamespaceCommand {
    @Nullable LongReference getOpenedReference();

    @Override
    default boolean isLocal() {
      return false;
    }
  }

  // Expression

  public static final int INFINITY_LEVEL = -33;

  public interface Expression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(@Nonnull AbstractExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  public interface CaseArgument extends SourceNode {
    @Nonnull Expression getExpression();
    @Nullable Referable getReferable();
    @Nullable Expression getType();
  }

  public interface CaseArgumentsHolder extends SourceNode {
    @Nonnull List<? extends CaseArgument> getCaseArguments();
  }

  public interface BinOpSequenceElem extends SourceNode {
    /* @Nonnull */ @Nullable Expression getExpression();
    @Nonnull Fixity getFixity();
    boolean isExplicit();
  }

  public interface Argument extends SourceNode {
    boolean isExplicit();
    /* @Nonnull */ @Nullable Expression getExpression();
  }

  public interface ClassFieldImpl extends ParametersHolder, ClassReferenceHolder {
    @Nullable Object getData();
    @Nullable Referable getImplementedField();
    /* @Nonnull */ @Nullable Expression getImplementation();
  }

  public interface LetClausePattern extends SourceNode {
    @Nullable Referable getReferable();
    @Nullable Abstract.Expression getType();
    @Nonnull List<? extends LetClausePattern> getPatterns();
  }

  public interface LetClause extends ParametersHolder {
    @Nullable LetClausePattern getPattern();
    @Nullable Referable getReferable();
    @Nullable Expression getResultType();
    /* @Nonnull */ @Nullable Expression getTerm();
  }

  public interface LevelExpression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  // Definition

  public interface ReferableDefinition extends SourceNode {
    /* @Nonnull */ @Nullable LocatedReferable getReferable();

    @Override
    default boolean isLocal() {
      return false;
    }
  }

  public interface Definition extends ReferableDefinition {
    @Nullable ClassReferable getEnclosingClass();
    @Override @Nonnull LocatedReferable getReferable();
    <R> R accept(AbstractDefinitionVisitor<? extends R> visitor);
  }

  public interface FunctionDefinition extends Definition, EliminatedExpressionsHolder, ClassReferenceHolder {
    @Nullable Expression getResultType();
    @Nullable Expression getResultTypeLevel();
    @Nullable Expression getTerm();
    @Override @Nonnull Collection<? extends Reference> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
    @Nonnull Collection<? extends LocatedReferable> getUsedDefinitions();
    boolean withTerm();
    boolean isCowith();
    boolean isCoerce();
    boolean isLevel();
    boolean isLemma();
    boolean isInstance();
  }

  public interface DataDefinition extends Definition, EliminatedExpressionsHolder {
    boolean isTruncated();
    @Nullable Expression getUniverse();
    @Nonnull Collection<? extends ConstructorClause> getClauses();
    @Nonnull Collection<? extends LocatedReferable> getUsedDefinitions();
  }

  public interface ClassDefinition extends Definition, ParametersHolder, ClassReferenceHolder  {
    @Override @Nonnull ClassReferable getReferable();
    @Override @Nonnull List<? extends FieldParameter> getParameters();
    boolean isRecord();
    @Nonnull Collection<? extends Reference> getSuperClasses();
    @Nonnull Collection<? extends ClassField> getClassFields();
    @Nullable Reference getUnderlyingClass();
    @Nonnull Collection<? extends LocatedReferable> getUsedDefinitions();
  }

  public interface Constructor extends ReferableDefinition, EliminatedExpressionsHolder {
    @Override @Nonnull LocatedReferable getReferable();
    @Override @Nonnull Collection<? extends Reference> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
    @Nullable Expression getResultType();
  }

  public interface ClassField extends ReferableDefinition, ParametersHolder {
    ClassFieldKind getClassFieldKind();
    /* @Nonnull */ @Nullable Expression getResultType();
    @Nullable Expression getResultTypeLevel();
  }

  public interface ClassFieldSynonym extends ReferableDefinition {
    /* @Nonnull */ @Nullable Reference getUnderlyingField();
  }
}
