package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class Matchers {
  public static Matcher<? super GeneralError> typecheckingError(final Class<? extends LocalTypeCheckingError> type) {
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
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
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
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

  public static Matcher<GeneralError> goal(final int expectedSize) {
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
        if (error instanceof GoalError) {
          description.appendText("goal with ");
          int size = ((GoalError) error).context.size();
          if (size == 0) {
            description.appendText("empty context");
          } else {
            description.appendText("context of size ").appendValue(size);
          }
          return size == expectedSize;
        } else {
          description.appendText("not a goal");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a goal with ");
        if (expectedSize == 0) {
          description.appendText("empty context");
        } else {
          description.appendText("context of size ").appendValue(expectedSize);
        }
      }
    };
  }

  public abstract static class TypeCheckingErrorMatcher extends TypeSafeDiagnosingMatcher<GeneralError> {
    @Override
    protected boolean matchesSafely(GeneralError generalError, Description description) {
      if (generalError instanceof TypeCheckingError) {
        description.appendText("TC error ");
        return matchesTypeCheckingError(((TypeCheckingError) generalError).localError, description);
      } else {
        description.appendText("not a TC error");
        return false;
      }
    }

    protected abstract boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description);
  }
}
