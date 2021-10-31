package org.arend.repl.action;

import org.arend.core.definition.Definition;
import org.arend.core.expr.visitor.SizeExpressionVisitor;
import org.arend.naming.reference.TCDefReferable;
import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SizeCommand implements ExpressionArgumentCommand {
  public static final SizeCommand INSTANCE = new SizeCommand();

  private SizeCommand() {}

  @Override
  public @NotNull String description() {
    return "Compute size of a definition or an expression";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException {
    Concrete.Expression expr = api.preprocessExpr(line);
    if (api.checkErrors() || expr == null) return;
    if (expr instanceof Concrete.ReferenceExpression && ((Concrete.ReferenceExpression) expr).getReferent() instanceof TCDefReferable) {
      Definition def = ((TCDefReferable) ((Concrete.ReferenceExpression) expr).getReferent()).getTypechecked();
      if (def == null) {
        api.eprintln("[ERROR] Definition was not typechecked yet");
      } else {
        api.println(SizeExpressionVisitor.getSize(def));
      }
    } else {
      api.checkExpr(expr, null, result -> {
        if (result != null) api.println(SizeExpressionVisitor.getSize(result.expression));
      });
    }
  }
}
