package org.arend.repl;

import org.arend.core.expr.Expression;
import org.arend.naming.scope.Scope;
import org.arend.repl.action.ReplAction;
import org.arend.repl.action.ReplCommand;
import org.arend.typechecking.result.TypecheckingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ReplApi {
  void registerAction(@NotNull ReplCommand action);

  boolean unregisterAction(@NotNull ReplAction action);

  void clearActions();

  /**
   * @return the scope of the REPL. It's mutable.
   */
  @NotNull List<Scope> getMergedScopes();

  /**
   * A replacement of {@link System#out#println(Object)} where it uses the
   * output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  void println(Object anything);

  /**
   * A replacement of {@link System#err#println(Object)} where it uses the
   * error output stream of the REPL.
   *
   * @param anything whose {@link Object#toString()} is invoked.
   */
  void eprintln(Object anything);

  @NotNull StringBuilder prettyExpr(@NotNull StringBuilder builder, @NotNull Expression expression);

  @Nullable TypecheckingResult checkExpr(@NotNull String text, @Nullable Expression expectedType);

  /**
   * Check and print errors.
   *
   * @return true if there is error(s).
   */
  boolean checkErrors();
}
