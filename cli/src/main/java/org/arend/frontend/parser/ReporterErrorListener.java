package org.arend.frontend.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.jetbrains.annotations.NotNull;

/**
 * An implementation of {@link BaseErrorListener} that reports syntax errors
 * from ANTLR to {@link ErrorReporter}.
 */
public class ReporterErrorListener extends BaseErrorListener {
  private final @NotNull ErrorReporter myErrorReporter;
  private final @NotNull ModulePath myModulePath;

  public ReporterErrorListener(@NotNull ErrorReporter errorReporter, @NotNull ModulePath modulePath) {
    myErrorReporter = errorReporter;
    myModulePath = modulePath;
  }

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
    myErrorReporter.report(new ParserError(new Position(myModulePath, line, pos, 1), msg));
  }
}
