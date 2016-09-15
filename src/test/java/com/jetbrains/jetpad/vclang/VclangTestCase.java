package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;

public abstract class VclangTestCase {
  protected final ListErrorReporter internalErrorReporter = new ListErrorReporter();

  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);

  @SafeVarargs
  protected final void assertThatErrorsAre(Matcher<? super GeneralError>... matchers) {
    assertThat(errorList, Matchers.contains(matchers));
  }


  private static ErrorFormatter ERROR_FORMATTER = new ErrorFormatter(SourceInfoProvider.TRIVIAL);

  protected static Matcher<? super List<? extends GeneralError>> containsErrors(final int n) {
    return new TypeSafeDiagnosingMatcher<List<? extends GeneralError>>() {
      @Override
      protected boolean matchesSafely(List<? extends GeneralError> errors, Description description) {
        if (errors.size() == 0) {
          description.appendText("there were no errors");
        } else {
          description.appendText("there were errors:\n").appendText(ERROR_FORMATTER.printErrors(errors));
        }
        return errors.size() == n;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("expected number of errors: ").appendValue(n);
      }
    };
  }
}
