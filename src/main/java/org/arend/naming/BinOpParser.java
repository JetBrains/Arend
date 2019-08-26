package org.arend.naming;

import org.arend.error.ErrorReporter;
import org.arend.naming.error.NamingError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.Fixity;
import org.arend.term.Precedence;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
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
    for (Concrete.BinOpSequenceElem elem : expr.getSequence()) {
      Concrete.ReferenceExpression reference = elem.expression instanceof Concrete.ReferenceExpression ? (Concrete.ReferenceExpression) elem.expression : null;
      if (reference == null && elem.fixity != Fixity.NONFIX && elem.expression instanceof Concrete.AppExpression && ((Concrete.AppExpression) elem.expression).getFunction() instanceof Concrete.ReferenceExpression) {
        reference = (Concrete.ReferenceExpression) ((Concrete.AppExpression) elem.expression).getFunction();
      }
      Precedence precedence = reference != null && reference.getReferent() instanceof GlobalReferable ? ((GlobalReferable) reference.getReferent()).getPrecedence() : null;
      if (precedence == null && reference != null && reference.getReferent() instanceof GlobalReferable) {
        precedence = ((GlobalReferable) reference.getReferent()).getPrecedence();
      }

      if (reference != null && (elem.fixity == Fixity.INFIX || elem.fixity == Fixity.POSTFIX || elem.fixity == Fixity.UNKNOWN && precedence != null && precedence.isInfix)) {
        if (precedence == null) {
          precedence = Precedence.DEFAULT;
        }
        push(reference, precedence, elem.fixity == Fixity.POSTFIX);
        if (elem.expression instanceof Concrete.AppExpression) {
          for (Concrete.Argument argument : ((Concrete.AppExpression) elem.expression).getArguments()) {
            push(argument.expression, argument.isExplicit());
          }
        }
      } else {
        push(elem.expression, elem.isExplicit);
      }
    }

    return rollUp();
  }

  public void push(Concrete.Expression expression, boolean isExplicit) {
    if (myStack.isEmpty()) {
      if (!isExplicit) {
        myErrorReporter.report(new NamingError(NamingError.Kind.EXPECTED_EXPLICIT, expression));
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

  public void push(Concrete.ReferenceExpression reference, @Nonnull Precedence precedence, boolean isPostfix) {
    if (myStack.isEmpty()) {
      if (isPostfix) {
        myErrorReporter.report(new NamingError("Expected an argument before a postfix operator", reference));
      }
      myStack.add(new StackElem(reference, precedence));
      return;
    }

    while (true) {
      StackElem topElem = myStack.get(myStack.size() - 1);
      if (topElem.precedence != null) {
        myErrorReporter.report(new NamingError("Expected an expression after an infix operator", topElem.expression));
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
        String msg = "Precedence parsing error: cannot mix " + getOperator(nextElem.expression).textRepresentation() + " [" + nextElem.precedence + "] and " + reference.getReferent().textRepresentation() + " [" + precedence + "] in the same infix expression";
        myErrorReporter.report(new NamingError(msg, reference));
      }

      foldTop();
    }
  }

  private Referable getOperator(Concrete.Expression expr) {
    if (expr instanceof Concrete.AppExpression) {
      expr = ((Concrete.AppExpression) expr).getFunction();
    }
    return ((Concrete.ReferenceExpression) expr).getReferent();
  }

  private void foldTop() {
    StackElem topElem = myStack.remove(myStack.size() - 1);
    if (topElem.precedence != null && myStack.size() > 1) {
      StackElem nextElem = myStack.get(myStack.size() - 2);
      myErrorReporter.report(new NamingError("The operator " + getOperator(topElem.expression) + " [" + topElem.precedence + "] of a section must have lower precedence than that of the operand, namely " + getOperator(nextElem.expression) + " [" + nextElem.precedence + "]", topElem.expression));
      topElem = myStack.remove(myStack.size() - 1);
    }
    StackElem midElem = myStack.remove(myStack.size() - 1);
    StackElem botElem = myStack.isEmpty() ? null : myStack.remove(myStack.size() - 1);

    if (botElem == null) {
      if (topElem.precedence != null) {
        myStack.add(new StackElem(Concrete.AppExpression.make(midElem.expression.getData(), topElem.expression, midElem.expression, true), null));
      } else {
        Referable leftRef = new LocalReferable(null);
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
