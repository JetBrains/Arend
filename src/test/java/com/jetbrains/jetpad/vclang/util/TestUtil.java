package com.jetbrains.jetpad.vclang.util;

import com.jetbrains.jetpad.vclang.ErrorFormatter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUtil {
  public static final ErrorFormatter ERROR_FORMATTER = new ErrorFormatter(SourceInfoProvider.TRIVIAL);

  public static void assertErrorListSize(Collection<? extends GeneralError> errors, int expected) {
    if (expected >= 0) {
      assertEquals(ERROR_FORMATTER.printErrors(errors), expected, errors.size());
    } else {
      assertErrorListNotEmpty(errors);
    }
  }

  public static void assertErrorListIsEmpty(Collection<? extends GeneralError> errors) {
    assertTrue(ERROR_FORMATTER.printErrors(errors), errors.isEmpty());
  }

  public static void assertErrorListNotEmpty(Collection<? extends GeneralError> errors) {
    assertFalse(ERROR_FORMATTER.printErrors(errors), errors.isEmpty());
  }
}
