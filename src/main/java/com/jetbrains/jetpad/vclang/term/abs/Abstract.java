package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface Parameter {
    @Nullable Object getData();
    boolean isExplicit();
    @Nonnull List<? extends Referable> getReferableList();
    @Nullable Expression getType();
  }

  public interface FunctionBody {
    @Nullable Object getData();
    @Nullable Expression getTerm();
    @Nonnull Collection<? extends Expression> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
  }

  public interface FunctionClause {
    @Nullable Object getData();
    @Nonnull Collection<? extends Pattern> getPatterns();
    @Nullable Expression getExpression();
  }

  public interface ConstructorClause {
    @Nullable Object getData();
    @Nonnull Collection<? extends Pattern> getPatterns();
    @Nonnull Collection<? extends Constructor> getConstructors();
  }

  public interface Pattern {
    @Nullable Object getData();
    boolean isEmpty();
    boolean isExplicit();
    @Nullable Referable getHeadReference();
    @Nonnull Collection<? extends Pattern> getArguments();
  }

  // Expression

  public static final int INFINITY_LEVEL = -33;

  public interface Expression {
    @Nullable Object getData();
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  public interface FieldAcc {
    @Nullable Object getData();
    @Nullable Referable getFieldReference();
    int getProjIndex();
  }

  public interface BinOpSequenceElem {
    @Nonnull Referable getBinOpReference();
    @Nullable Expression getArgument();
  }

  public interface Argument {
    boolean isExplicit();
    @Nonnull Expression getExpression();
  }

  public interface ClassFieldImpl {
    @Nullable Object getData();
    @Nonnull Referable getImplementedField();
    /* @Nonnull */ @Nullable Expression getImplementation();
  }

  public interface LetClause {
    @Nonnull Referable getReferable();
    @Nonnull Collection<? extends Parameter> getParameters();
    @Nullable Expression getResultType();
    /* @Nonnull */ @Nullable Expression getTerm();
  }

  public interface LevelExpression {
    @Nullable Object getData();
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  // Definition

  public interface Definition {
    @Nonnull GlobalReferable getReferable();
    <R> R accept(AbstractDefinitionVisitor<? extends R> visitor);
  }

  public interface FunctionDefinition extends Definition {
    @Nonnull Collection<? extends Parameter> getParameters();
    @Nullable Expression getResultType();
    /* @Nonnull */ @Nullable FunctionBody getBody();
  }

  public interface DataDefinition extends Definition {
    @Nonnull Collection<? extends Parameter> getParameters();
    @Nullable Collection<? extends Expression> getEliminatedExpressions();
    boolean isTruncated();
    @Nullable Expression getUniverse();
    @Nonnull Collection<? extends ConstructorClause> getClauses();
  }

  public interface ClassDefinition extends Definition {
    @Nonnull Collection<? extends Parameter> getParameters();
    @Nonnull Collection<? extends Expression> getSuperClasses();
    @Nonnull Collection<? extends ClassField> getClassFields();
    @Nonnull Collection<? extends ClassFieldImpl> getClassFieldImpls();
  }

  public interface Constructor {
    @Nonnull GlobalReferable getReferable();
    @Nonnull Collection<? extends Parameter> getParameters();
    @Nonnull Collection<? extends Expression> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
  }

  public interface ClassField {
    @Nonnull GlobalReferable getReferable();
    /* @Nonnull */ @Nullable Expression getResultType();
  }
}
