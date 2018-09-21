package org.arend.typechecking;

import org.arend.core.definition.Definition;
import org.arend.error.Error;
import org.arend.error.GeneralError;
import org.arend.naming.error.DuplicateNameError;
import org.arend.naming.error.NotInScopeError;
import org.arend.naming.error.WrongReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.ProxyError;
import org.arend.typechecking.error.local.*;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Collection;

public class Matchers {
  public static Matcher<? super GeneralError> typecheckingError() {
    return typecheckingError(LocalError.class);
  }

  public static Matcher<? super GeneralError> typecheckingError(final Class<? extends LocalError> type) {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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

  public static Matcher<? super GeneralError> duplicateName(Error.Level level, String name) {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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

  public static Matcher<? super GeneralError> duplicateInstanceError() {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
        if (error instanceof DuplicateInstanceError) {
          description.appendText("Duplicate instance");
          return true;
        } else {
          description.appendText("not a 'Duplicate instance' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Duplicate instance' error");
      }
    };
  }

  public static Matcher<? super GeneralError> wrongReferable() {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
        if (error instanceof FieldsImplementationError && ((FieldsImplementationError) error).alreadyImplemented == ((FieldsImplementationError) error).alreadyImplemented && ((FieldsImplementationError) error).fields.equals(fields)) {
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

  public static Matcher<? super GeneralError> instanceInference(TCReferable classRef) {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
        if (error instanceof InstanceInferenceError && ((InstanceInferenceError) error).classRef.equals(classRef)) {
          description.appendText("Instance inference for class '" + ((InstanceInferenceError) error).classRef.textRepresentation() + "'");
          return true;
        } else {
          description.appendText(error instanceof InstanceInferenceError ? "'Instance inference for class " + ((InstanceInferenceError) error).classRef.textRepresentation() + "' error" : "not a 'Instance inference' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Instance inference for class " + classRef + "' error");
      }
    };
  }

  public static Matcher<? super GeneralError> instanceInference(Definition definition) {
    return instanceInference(definition.getReferable());
  }

  public static Matcher<? super GeneralError> missingClauses(int clauses) {
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
        if (error instanceof MissingClausesError && ((MissingClausesError) error).getMissingClauses().size() == clauses) {
          description.appendText("Missing " + clauses + " clauses");
          return true;
        } else {
          description.appendText(error instanceof MissingClausesError ? "'Missing " + ((MissingClausesError) error).getMissingClauses().size() + " clauses' error" : "not a 'Missing clauses' error");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a 'Missing " + clauses + " clauses' error");
      }
    };
  }

  public static Matcher<? super GeneralError> error() {
    return new TypeSafeDiagnosingMatcher<GeneralError>() {
      @Override
      protected boolean matchesSafely(GeneralError error, Description description) {
        if (error.level == Error.Level.ERROR) {
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
        if (error.level == Error.Level.WARNING) {
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
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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
    return new LocalErrorMatcher() {
      @Override
      protected boolean matchesLocalError(LocalError error, Description description) {
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

  public abstract static class LocalErrorMatcher extends TypeSafeDiagnosingMatcher<GeneralError> {
    @Override
    protected boolean matchesSafely(GeneralError generalError, Description description) {
      if (generalError instanceof ProxyError) {
        description.appendText("Local error ");
        return matchesLocalError(((ProxyError) generalError).localError, description);
      } else {
        description.appendText("not a local error");
        return false;
      }
    }

    protected abstract boolean matchesLocalError(LocalError error, Description description);
  }
}
