package org.arend.repl.action;

import org.arend.ext.core.ops.NormalizationMode;
import org.arend.repl.ReplApi;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

public final class NormalizeCommand extends ReplCommand {
  private final @NotNull NormalizationMode myMode;

  public NormalizeCommand(@NotNull String command, @NotNull NormalizationMode mode) {
    super(command);
    myMode = mode;
  }

  @Override
  protected void doInvoke(@NotNull String line, @NotNull ReplApi api, @NotNull Scanner scanner) {
    var expr = api.preprocessExpr(line);
    if (api.checkErrors() || expr == null) return;
    var result = api.checkExpr(expr, null);
    if (result == null) return;
    api.println(result.expression.normalize(myMode));
  }
}
