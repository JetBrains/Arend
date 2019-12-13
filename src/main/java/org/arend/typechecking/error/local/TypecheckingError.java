package org.arend.typechecking.error.local;

import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

public class TypecheckingError extends LocalError {
  public enum Kind {
    LEVEL_IGNORED(Level.WEAK_WARNING, "\\level is ignored"),
    BODY_IGNORED(Level.WEAK_WARNING, "Body is ignored"),
    AS_PATTERN_IGNORED(Level.WEAK_WARNING, "As-pattern is ignored"),
    PATTERN_IGNORED(Level.WEAK_WARNING, "Pattern is ignored"),
    REDUNDANT_CLAUSE(Level.WEAK_WARNING, "Clause is redundant"),
    TOO_MANY_PATTERNS("Too many patterns"),
    EXPECTED_EXPLICIT_PATTERN("Expected an explicit pattern"),
    IMPLICIT_PATTERN(Level.WARNING, "All patterns must be explicit"),
    BODY_REQUIRED("Body is required"),
    DATA_WONT_BE_TRUNCATED(Level.WEAK_WARNING, "The data type will not be truncated since it already fits in the specified universe"),
    USELESS_LEVEL(Level.WEAK_WARNING, "Actual level is smaller than the specified one"),
    TRUNCATED_WITHOUT_UNIVERSE(Level.WARNING, "The data type cannot be truncated since its universe is not specified"),
    EXPECTED_EXPLICIT("Expected an explicit argument"),
    EXPECTED_IMPLICIT("Expected an implicit argument"),
    CASE_RESULT_TYPE("Cannot infer the result type"),
    LEMMA_LEVEL("The level of a lemma must be \\Prop"),
    PROPERTY_LEVEL("The level of a property must be \\Prop"),
    REDUNDANT_COCLAUSE(Level.WEAK_WARNING, "Coclause is redundant"),
    NO_CLASSIFYING_IGNORED(Level.WEAK_WARNING, "\\noclassifying is ignored");

    private final Level level;
    private final String message;

    Kind(Level level, String message) {
      this.level = level;
      this.message = message;
    }

    Kind(String message) {
      this.level = Level.ERROR;
      this.message = message;
    }
  }

  public final Concrete.SourceNode cause;
  public final Kind kind;

  public TypecheckingError(@Nonnull Level level, String message, @Nonnull Concrete.SourceNode cause) {
    super(level, message);
    this.cause = cause;
    this.kind = null;
  }

  public TypecheckingError(String message, @Nonnull Concrete.SourceNode cause) {
    super(Level.ERROR, message);
    this.cause = cause;
    this.kind = null;
  }

  public TypecheckingError(Kind kind, @Nonnull Concrete.SourceNode cause) {
    super(kind.level, kind.message);
    this.cause = cause;
    this.kind = kind;
  }

  @Override
  public Concrete.SourceNode getCauseSourceNode() {
    return cause;
  }

  @Override
  public Stage getStage() {
    return Stage.TYPECHECKER;
  }
}
