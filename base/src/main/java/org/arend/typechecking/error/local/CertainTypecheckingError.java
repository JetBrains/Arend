package org.arend.typechecking.error.local;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;

public class CertainTypecheckingError extends TypecheckingError {
  public enum Kind {
    LEVEL_IGNORED(Level.WARNING_UNUSED, "\\level is ignored"),
    BODY_IGNORED(Level.WARNING_UNUSED, "Body is ignored"),
    AS_PATTERN_IGNORED(Level.WARNING_UNUSED, "As-pattern is ignored"),
    PATTERN_IGNORED(Level.WARNING_UNUSED, "Pattern is ignored"),
    PROPERTY_IGNORED(Level.WARNING_UNUSED, "\\property is ignored"),
    TOO_MANY_PATTERNS("Too many patterns"),
    EXPECTED_EXPLICIT_PATTERN("Expected an explicit pattern"),
    IMPLICIT_PATTERN(Level.WARNING, "All patterns must be explicit"),
    BODY_REQUIRED("Body is required"),
    DATA_WONT_BE_TRUNCATED(Level.WARNING_UNUSED, "The data type will not be truncated since it already fits in the specified universe"),
    USELESS_LEVEL(Level.WARNING_UNUSED, "Actual level is smaller than the specified one"),
    TRUNCATED_WITHOUT_UNIVERSE(Level.WARNING, "The data type cannot be truncated since its universe is not specified"),
    CASE_RESULT_TYPE("Cannot infer the result type"),
    COULD_BE_LEMMA(Level.WARNING, "Function can be declared as a lemma"),
    AXIOM_WITH_BODY(Level.WARNING, "An axiom should not have a body"),
    INSTANCE_TYPE("The type of an instance must be a class"),
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

  public CertainTypecheckingError(Kind kind, ConcreteSourceNode cause) {
    super(kind.level, kind.message, cause);
    this.kind = kind;
  }
}
