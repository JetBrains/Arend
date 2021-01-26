package org.arend.repl.action;

import org.arend.core.expr.Expression;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class ShowTypeCommand implements ReplCommand, ExpressionArgumentCommand {
  public static final @NotNull ShowTypeCommand INSTANCE = new ShowTypeCommand();

  private ShowTypeCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Show the synthesized type of a given expression";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    var expr = api.preprocessExpr(line);
    if (api.checkErrors() || expr == null) return;
    api.checkExpr(expr, null, result -> {
      if (result == null) return;
      Expression type = result.expression.getType();
      if (type != null) api.println(api.prettyExpr(new StringBuilder(), api.normalize(type)));
      else api.eprintln("[ERROR] Unable to synthesize a type, sorry.");
    });
  }
}
