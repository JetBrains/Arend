package org.arend.typechecking;

import org.arend.core.expr.Expression;
import org.arend.error.GeneralError;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.NotInScopeError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.local.*;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Matchers {
  public static Matcher<? super GeneralError> typecheckingError() {
    return typecheckingError(LocalError.class);
  }

  public static Matcher<? super GeneralError> typecheckingError(final Class<? extends LocalError> type) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (type.isInstance(error)) {
          description.appendText(type.getName());
          return true;
        } else {
          description.appendText("not a " + type.getName());
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a " + type.getName());
      }
    };
  }

  public static Matcher<? super GeneralError> typeMismatchError() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
          if (error instanceof TypeMismatchError) {
            description.appendText("type mismatch");
            return true;
          } else {
            description.appendText("not a type mismatch");
            return false;
          }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a type mismatch");
      }
    };
  }

  public static Matcher<? super GeneralError> notInScope(String name) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof NotInScopeError && ((NotInScopeError) error).name.equals(name)) {
          description.appendText("Not in scope '" + name + "'");
          return true;
        } else {
          description.appendText(error instanceof NotInScopeError ? "'Not in scope: " + ((NotInScopeError) error).name + "' error" : "not a 'Not in scope' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Not in scope: " + name + "' error");
      }
    };
  }

  public static Matcher<? super GeneralError> duplicateName(GeneralError.Level level, String name) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof DuplicateNameError && error.level.equals(level) && ((DuplicateNameError) error).referable.textRepresentation().equals(name)) {
          description.appendText("Duplicate name '" + name + "'");
          return true;
        } else {
          description.appendText(error instanceof DuplicateNameError ? "'Duplicate name: " + ((DuplicateNameError) error).referable.textRepresentation() + (error.level.equals(level) ? "" : "[" + error.level + "]") + "' error" : "not a 'Duplicate name' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Duplicate name: " + name + "' error");
      }
    };
  }

  public static Matcher<? super GeneralError> wrongReferable() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof WrongReferable) {
          description.appendText("Wrong referable '" + ((WrongReferable) error).referable.textRepresentation() + "'");
          return true;
        } else {
          description.appendText("not a 'Wrong referable' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Wrong referable' error");
      }
    };
  }

  public static Matcher<? super GeneralError> fieldsImplementation(boolean alreadyImplemented, Collection<? extends GlobalReferable> fields) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof FieldsImplementationError && ((FieldsImplementationError) error).alreadyImplemented == alreadyImplemented && ((FieldsImplementationError) error).fields.equals(fields)) {
          description.appendText(error.toString());
          return true;
        } else {
          description.appendText(error instanceof FieldsImplementationError ? "'Fields " + ((FieldsImplementationError) error).fields + " are " + (((FieldsImplementationError) error).alreadyImplemented ? "already" : "not") + " implemented' error" : "not a 'Fields implementation' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Fields " + fields + " are " + (alreadyImplemented ? "already" : "not") + " implemented' error");
      }
    };
  }

  public static Matcher<? super GeneralError> instanceInference(TCReferable classRef, Expression classifyingExpression) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (!(error instanceof InstanceInferenceError)) {
          description.appendText("not a 'Instance inference' error");
          return false;
        }

        InstanceInferenceError instanceInferenceError = (InstanceInferenceError) error;
        if (!instanceInferenceError.classRef.equals(classRef)) {
          description.appendText("'Instance inference for class " + instanceInferenceError.classRef.textRepresentation() + "' error");
          return false;
        }

        if (!Objects.equals(instanceInferenceError.classifyingExpression, classifyingExpression)) {
          description.appendText("'Instance inference for class " + instanceInferenceError.classRef.textRepresentation() + (instanceInferenceError.classifyingExpression == null ? " without classifying expression" : " with classifying expression " + instanceInferenceError.classifyingExpression) + "' error");
          return false;
        }

        description.appendText("Instance inference for class '" + instanceInferenceError.classRef.textRepresentation() + "'");
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Instance inference for class " + classRef + "' error " + (classifyingExpression == null ? "without classifying expression" : "with classifying expression " + classifyingExpression));
      }
    };
  }

  public static Matcher<? super GeneralError> instanceInference(TCReferable definition) {
    return instanceInference(definition, null);
  }

  public static Matcher<? super GeneralError> argInferenceError() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof ArgInferenceError) {
          description.appendText("Argument inference");
          return true;
        } else {
          description.appendText("not an 'Argument inference' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be an 'Argument inference' error");
      }
    };
  }

  public static Matcher<? super GeneralError> missingClauses(int clauses) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof MissingClausesError && ((MissingClausesError) error).missingClauses.size() == clauses) {
          description.appendText("Missing " + clauses + " clauses");
          return true;
        } else {
          description.appendText(error instanceof MissingClausesError ? "'Missing " + ((MissingClausesError) error).missingClauses.size() + " clauses' error" : "not a 'Missing clauses' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Missing " + clauses + " clauses' error");
      }
    };
  }

  public static Matcher<? super GeneralError> cycle(GlobalReferable... refs) {
    List<GlobalReferable> referables = Arrays.asList(refs);
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof CycleError && (referables.isEmpty() || ((CycleError) error).cycle.equals(referables))) {
          description.appendText("Cycle error: " + ((CycleError) error).cycle);
          return true;
        } else {
          description.appendText(error instanceof CycleError ? "Cycle error: " + ((CycleError) error).cycle : "not a cycle error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Cycle error: " + referables + "'");
      }
    };
  }

  public static Matcher<? super GeneralError> error() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error.level == GeneralError.Level.ERROR) {
          description.appendText("error");
          return true;
        } else {
          description.appendText("not an error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be an error");
      }
    };
  }

  public static Matcher<? super GeneralError> warning() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error.level == GeneralError.Level.WARNING || error.level == GeneralError.Level.WEAK_WARNING) {
          description.appendText("warning");
          return true;
        } else {
          description.appendText("not a warning");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a warning");
      }
    };
  }

  public static Matcher<GeneralError> goal(final int contextSize) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof GoalError) {
          description.appendText("goal with ");
          int size = ((GoalError) error).context.size();
          if (size == 0) {
            description.appendText("empty context");
          } else {
            description.appendText("context of size ").appendValue(size);
          }
          return size == contextSize;
        } else {
          description.appendText("not a goal");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a goal with ");
        if (contextSize == 0) {
          description.appendText("empty context");
        } else {
          description.appendText("context of size ").appendValue(contextSize);
        }
      }
    };
  }

  public static Matcher<GeneralError> hasErrors(GlobalReferable cause) {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error instanceof HasErrors) {
          description.appendText("has errors with ");
          Referable actualCause = ((Concrete.ReferenceExpression) ((HasErrors) error).cause).getReferent();
          description.appendText(actualCause == cause ? "the write " : "a wrong ");
          description.appendText("cause");
          return actualCause == cause;
        } else {
          description.appendText("not a has errors");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a has errors with the write target");
      }
    };
  }
}
