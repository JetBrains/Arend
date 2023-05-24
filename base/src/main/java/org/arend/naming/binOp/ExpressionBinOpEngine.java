package org.arend.naming.binOp;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.renamer.Renamer;
import org.arend.ext.reference.Fixity;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionBinOpEngine implements BinOpEngine<Concrete.Expression> {

  private static final ExpressionBinOpEngine engine = new ExpressionBinOpEngine();

  private ExpressionBinOpEngine() {
  }

  @Override
  public @Nullable Referable getReferable(@NotNull Concrete.Expression elem) {
    return elem instanceof Concrete.ReferenceExpression ? ((Concrete.ReferenceExpression) elem).getReferent()
            : elem instanceof Concrete.AppExpression && ((Concrete.AppExpression) elem).getFunction() instanceof Concrete.ReferenceExpression ? ((Concrete.ReferenceExpression) ((Concrete.AppExpression) elem).getFunction()).getReferent()
            : null;
  }

  @Override
  public @NotNull Concrete.Expression wrapSequence(Object data, Concrete.@NotNull Expression base, List<@NotNull Pair<? extends Concrete.Expression, Boolean>> explicitComponents) {
    return Concrete.AppExpression.make(data, base, explicitComponents.stream().map((pair) -> new Concrete.Argument(pair.proj1, pair.proj2)).collect(Collectors.toList()));
  }


  @Override
  public @NotNull Concrete.Expression augmentWithLeftReferable(Object data, @NotNull Referable leftRef, Concrete.@NotNull Expression mid, Concrete.Expression right) {
    return new Concrete.LamExpression(data, Collections.singletonList(new Concrete.NameParameter(data, true, leftRef)), BinOpParser.makeBinOp(new Concrete.ReferenceExpression(data, leftRef), mid, right, this));
  }

  @Override
  public @NotNull String getPresentableComponentName() {
    return "expression";
  }

  public static @NotNull Concrete.Expression parse(@NotNull Concrete.BinOpSequenceExpression expression, @Nullable ErrorReporter reporter) {
    Concrete.BinOpSequenceElem<Concrete.Expression> first = expression.getSequence().get(0);
    if (first.fixity == Fixity.INFIX || first.fixity == Fixity.POSTFIX) {
      LocalReferable firstArg = new LocalReferable(Renamer.UNNAMED);
      List<Concrete.BinOpSequenceElem<Concrete.Expression>> newSequence = new ArrayList<>(expression.getSequence().size() + 1);
      newSequence.add(new Concrete.BinOpSequenceElem<>(new Concrete.ReferenceExpression(expression.getData(), firstArg)));
      newSequence.addAll(expression.getSequence());
      return new Concrete.LamExpression(expression.getData(), Collections.singletonList(new Concrete.NameParameter(expression.getData(), true, firstArg)), parse(new Concrete.BinOpSequenceExpression(expression.getData(), newSequence, expression.getClauses()), reporter));
    }

    Concrete.Expression parsed = new BinOpParser<>(reporter, engine).parse(expression.getSequence());
    return parsed instanceof Concrete.AppExpression && parsed.getData() != expression.getData()
        ? Concrete.AppExpression.make(expression.getData(), ((Concrete.AppExpression) parsed).getFunction(), ((Concrete.AppExpression) parsed).getArguments())
        : parsed;
  }
}
