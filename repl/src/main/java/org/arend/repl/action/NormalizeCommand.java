package org.arend.repl.action;

import org.arend.ext.core.ops.NormalizationMode;
import org.arend.repl.ReplApi;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class NormalizeCommand implements ReplCommand {
  private final @NotNull NormalizationMode myMode;

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Normalize the given expression into " + myMode.name();
  }

  public NormalizeCommand(@NotNull NormalizationMode mode) {
    myMode = mode;
  }

  @Override
  public void invoke(@NotNull String line, @NotNull ReplApi api, @NotNull Supplier<@NotNull String> scanner) {
    var expr = api.preprocessExpr(line);
    if (api.checkErrors() || expr == null) return;
    var result = api.checkExpr(expr, null);
    if (result == null) return;
    api.println(result.expression.normalize(myMode));
  }
}
