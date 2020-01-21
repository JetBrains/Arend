package org.arend.typechecking.error.local;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;

import javax.annotation.Nonnull;

public class CertainTypecheckingError extends TypecheckingError {
  public enum Kind {
    LEVEL_IGNORED(Level.WARNING_UNUSED, "\\level is ignored"),
    BODY_IGNORED(Level.WARNING_UNUSED, "Body is ignored"),
    AS_PATTERN_IGNORED(Level.WARNING_UNUSED, "As-pattern is ignored"),
    PATTERN_IGNORED(Level.WARNING_UNUSED, "Pattern is ignored"),
    REDUNDANT_CLAUSE(Level.WARNING_UNUSED, "Clause is redundant"),
    TOO_MANY_PATTERNS("Too many patterns"),
    EXPECTED_EXPLICIT_PATTERN("Expected an explicit pattern"),
    IMPLICIT_PATTERN(Level.WARNING, "All patterns must be explicit"),
    BODY_REQUIRED("Body is required"),
    DATA_WONT_BE_TRUNCATED(Level.WARNING_UNUSED, "The data type will not be truncated since it already fits in the specified universe"),
    USELESS_LEVEL(Level.WARNING_UNUSED, "Actual level is smaller than the specified one"),
    TRUNCATED_WITHOUT_UNIVERSE(Level.WARNING, "The data type cannot be truncated since its universe is not specified"),
    EXPECTED_EXPLICIT("Expected an explicit argument"),
    EXPECTED_IMPLICIT("Expected an implicit argument"),
    CASE_RESULT_TYPE("Cannot infer the result type"),
    LEMMA_LEVEL("The level of a lemma must be \\Prop"),
    PROPERTY_LEVEL("The level of a property must be \\Prop"),
    REDUNDANT_COCLAUSE(Level.WARNING_UNUSED, "Coclause is redundant"),
    NO_CLASSIFYING_IGNORED(Level.WARNING_UNUSED, "\\noclassifying is ignored");

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

  public final Kind kind;

  public CertainTypecheckingError(Kind kind, @Nonnull ConcreteSourceNode cause) {
    super(kind.level, kind.message, cause);
    this.kind = kind;
  }
}
