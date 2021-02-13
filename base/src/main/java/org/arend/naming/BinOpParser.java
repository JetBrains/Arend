package org.arend.naming;

import org.arend.error.ParsingError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.Precedence;
import org.arend.ext.error.NameResolverError;
import org.arend.naming.error.PrecedenceError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.renamer.Renamer;
import org.arend.term.Fixity;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BinOpParser {
  private final ErrorReporter myErrorReporter;
  private final List<StackElem> myStack;

  public BinOpParser(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
    myStack = new ArrayList<>();
  }

  private static class StackElem {
    public Concrete.Expression expression;
    public Precedence precedence;

    StackElem(Concrete.Expression expression, Precedence precedence) {
      this.expression = expression;
      this.precedence = precedence;
    }
  }

  public Concrete.Expression parse(Concrete.BinOpSequenceExpression expr) {
    List<Concrete.BinOpSequenceElem> sequence = expr.getSequence();
    Concrete.BinOpSequenceElem first = sequence.get(0);
    if (first.fixity == Fixity.INFIX || first.fixity == Fixity.POSTFIX) {
      LocalReferable firstArg = new LocalReferable(Renamer.UNNAMED);
      List<Concrete.BinOpSequenceElem> newSequence = new ArrayList<>(sequence.size() + 1);
      newSequence.add(new Concrete.BinOpSequenceElem(new Concrete.ReferenceExpression(expr.getData(), firstArg)));
      newSequence.addAll(sequence);
      return new Concrete.LamExpression(expr.getData(), Collections.singletonList(new Concrete.NameParameter(expr.getData(), true, firstArg)), parse(new Concrete.BinOpSequenceExpression(expr.getData(), newSequence, expr.getClauses())));
    }

    for (Concrete.BinOpSequenceElem elem : sequence) {
      Concrete.ReferenceExpression reference = elem.expression instanceof Concrete.ReferenceExpression ? (Concrete.ReferenceExpression) elem.expression : null;
      if (reference == null && elem.fixity != Fixity.NONFIX && elem.expression instanceof Concrete.AppExpression && ((Concrete.AppExpression) elem.expression).getFunction() instanceof Concrete.ReferenceExpression) {
        reference = (Concrete.ReferenceExpression) ((Concrete.AppExpression) elem.expression).getFunction();
      }
      Precedence precedence = reference != null && reference.getReferent() instanceof GlobalReferable ? ((GlobalReferable) reference.getReferent()).getPrecedence() : null;

      if (reference != null && (elem.fixity == Fixity.INFIX || elem.fixity == Fixity.POSTFIX || elem.fixity == Fixity.UNKNOWN && precedence != null && precedence.isInfix)) {
        if (precedence == null) {
          precedence = Precedence.DEFAULT;
        }
        push(reference, precedence, elem.fixity == Fixity.POSTFIX && !precedence.isInfix);
        if (elem.expression instanceof Concrete.AppExpression) {
          for (Concrete.Argument argument : ((Concrete.AppExpression) elem.expression).getArguments()) {
            push(argument.expression, argument.isExplicit());
          }
        }
      } else {
        push(elem.expression, elem.isExplicit);
      }
    }

    Concrete.Expression result = rollUp();
    return result instanceof Concrete.AppExpression && result.getData() != expr.getData() ? Concrete.AppExpression.make(expr.getData(), ((Concrete.AppExpression) result).getFunction(), ((Concrete.AppExpression) result).getArguments()) : result;
  }

  public void push(Concrete.Expression expression, boolean isExplicit) {
    if (myStack.isEmpty()) {
      if (!isExplicit) {
        // This should never happen if the binOp expression is correct
        myErrorReporter.report(new ParsingError("Expected an explicit expression", expression));
      }
      myStack.add(new StackElem(expression, null));
      return;
    }

    StackElem topElem = myStack.get(myStack.size() - 1);
    if (topElem.precedence == null || !isExplicit) {
      topElem.expression = Concrete.AppExpression.make(topElem.expression.getData(), topElem.expression, expression, isExplicit);
    } else {
      myStack.add(new StackElem(expression, null));
    }
  }

  public void push(Concrete.ReferenceExpression reference, @NotNull Precedence precedence, boolean isPostfix) {
    if (myStack.isEmpty()) {
      myStack.add(new StackElem(reference, precedence));
      return;
    }

    while (true) {
      StackElem topElem = myStack.get(myStack.size() - 1);
      if (topElem.precedence != null) {
        myErrorReporter.report(new NameResolverError("Expected an expression after an infix operator", topElem.expression));
        return;
      }

      StackElem nextElem = myStack.size() == 1 ? null : myStack.get(myStack.size() - 2);
      if (nextElem == null || nextElem.precedence.priority < precedence.priority || nextElem.precedence.priority == precedence.priority && nextElem.precedence.associativity == Precedence.Associativity.RIGHT_ASSOC && (isPostfix || precedence.associativity == Precedence.Associativity.RIGHT_ASSOC)) {
        if (isPostfix) {
          myStack.set(myStack.size() - 1, new StackElem(Concrete.AppExpression.make(reference.getData(), reference, topElem.expression, true), null));
        } else {
          myStack.add(new StackElem(reference, precedence));
        }
        return;
      }

      if (!(nextElem.precedence.priority > precedence.priority || nextElem.precedence.associativity == Precedence.Associativity.LEFT_ASSOC && (isPostfix || precedence.associativity == Precedence.Associativity.LEFT_ASSOC))) {
        myErrorReporter.report(new PrecedenceError(getOperator(nextElem.expression), reference.getReferent(), reference));
      }

      foldTop();
    }
  }

  private GlobalReferable getOperator(Concrete.Expression expr) {
    if (expr instanceof Concrete.AppExpression) {
      expr = ((Concrete.AppExpression) expr).getFunction();
    }
    return (GlobalReferable) ((Concrete.ReferenceExpression) expr).getReferent();
  }

  private void foldTop() {
    StackElem topElem = myStack.remove(myStack.size() - 1);
    if (topElem.precedence != null && myStack.size() > 1) {
      StackElem nextElem = myStack.get(myStack.size() - 2);
      myErrorReporter.report(new NameResolverError("The operator " + getOperator(topElem.expression) + " [" + topElem.precedence + "] of a section must have lower precedence than that of the operand, namely " + getOperator(nextElem.expression) + " [" + nextElem.precedence + "]", topElem.expression));
      topElem = myStack.remove(myStack.size() - 1);
    }
    StackElem midElem = myStack.remove(myStack.size() - 1);
    StackElem botElem = myStack.isEmpty() ? null : myStack.remove(myStack.size() - 1);

    if (botElem == null) {
      if (topElem.precedence != null) {
        myStack.add(new StackElem(Concrete.AppExpression.make(midElem.expression.getData(), topElem.expression, midElem.expression, true), null));
      } else {
        Referable leftRef = new LocalReferable(Renamer.UNNAMED);
        myStack.add(new StackElem(new Concrete.LamExpression(midElem.expression.getData(), Collections.singletonList(new Concrete.NameParameter(midElem.expression.getData(), true, leftRef)), makeBinOp(new Concrete.ReferenceExpression(midElem.expression.getData(), leftRef), midElem.expression, topElem.expression)), null));
      }
    } else {
      myStack.add(new StackElem(makeBinOp(botElem.expression, midElem.expression, topElem.expression), null));
    }
  }

  private static Concrete.Expression makeBinOp(Concrete.Expression left, Concrete.Expression var, Concrete.Expression right) {
    Concrete.Expression expr = Concrete.AppExpression.make(var.getData(), var, left, true);
    return right == null ? expr : Concrete.AppExpression.make(var.getData(), expr, right, true);
  }

  public Concrete.Expression rollUp() {
    while (myStack.size() > 1) {
      foldTop();
    }
    Concrete.Expression result = myStack.get(0).expression;
    myStack.clear();
    return result;
  }
}
