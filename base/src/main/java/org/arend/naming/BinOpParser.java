package org.arend.naming;

import org.arend.error.ParsingError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.Precedence;
import org.arend.naming.error.PrecedenceError;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.renamer.Renamer;
import org.arend.term.Fixity;
import org.arend.term.concrete.Concrete;
import org.arend.util.Pair;
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

  private enum Modifier { NEW, EVAL, PEVAL }

  private static class StackElem {
    public List<Pair<Object, Modifier>> modifiers;
    public Concrete.Expression expression;
    public Precedence precedence;

    StackElem(Concrete.Expression expression, Precedence precedence) {
      modifiers = Collections.emptyList();
      this.expression = expression;
      this.precedence = precedence;
    }

    StackElem(Modifier modifier, Object data) {
      modifiers = new ArrayList<>();
      modifiers.add(new Pair<>(data, modifier));
      this.expression = null;
      this.precedence = null;
    }

    void foldModifiers() {
      if (expression == null) {
        return;
      }
      for (int i = modifiers.size() - 1; i >= 0; i--) {
        switch (modifiers.get(i).proj2) {
          case NEW:
            expression = new Concrete.NewExpression(modifiers.get(i).proj1, expression);
            break;
          case EVAL:
            expression = new Concrete.EvalExpression(modifiers.get(i).proj1, false, expression);
            break;
          case PEVAL:
            expression = new Concrete.EvalExpression(modifiers.get(i).proj1, true, expression);
            break;
        }
      }
      modifiers = Collections.emptyList();
    }

    GlobalReferable getOperator() {
      Concrete.Expression expr = expression;
      if (expr instanceof Concrete.AppExpression) {
        expr = ((Concrete.AppExpression) expr).getFunction();
      }
      return (GlobalReferable) ((Concrete.ReferenceExpression) expr).getReferent();
    }
  }

  public Concrete.Expression parse(Concrete.BinOpSequenceExpression expr) {
    List<Concrete.BinOpSequenceElem> sequence = expr.getSequence();
    Concrete.BinOpSequenceElem first = sequence.get(0);
    if (first.getFixity() == Fixity.INFIX || first.getFixity() == Fixity.POSTFIX) {
      LocalReferable firstArg = new LocalReferable(Renamer.UNNAMED);
      List<Concrete.BinOpSequenceElem> newSequence = new ArrayList<>(sequence.size() + 1);
      newSequence.add(new Concrete.ExplicitBinOpSequenceElem(new Concrete.ReferenceExpression(expr.getData(), firstArg)));
      newSequence.addAll(sequence);
      return new Concrete.LamExpression(expr.getData(), Collections.singletonList(new Concrete.NameParameter(expr.getData(), true, firstArg)), parse(new Concrete.BinOpSequenceExpression(expr.getData(), newSequence)));
    }

    for (Concrete.BinOpSequenceElem elem : sequence) {
      Concrete.ReferenceExpression reference = elem.getExpression() instanceof Concrete.ReferenceExpression ? (Concrete.ReferenceExpression) elem.getExpression() : null;
      if (reference == null && elem.getFixity() != Fixity.NONFIX && elem.getExpression() instanceof Concrete.AppExpression && ((Concrete.AppExpression) elem.getExpression()).getFunction() instanceof Concrete.ReferenceExpression) {
        reference = (Concrete.ReferenceExpression) ((Concrete.AppExpression) elem.getExpression()).getFunction();
      }
      Precedence precedence = reference != null && reference.getReferent() instanceof GlobalReferable ? ((GlobalReferable) reference.getReferent()).getPrecedence() : null;

      if (reference != null && (elem.getFixity() == Fixity.INFIX || elem.getFixity() == Fixity.POSTFIX || elem.getFixity() == Fixity.UNKNOWN && precedence != null && precedence.isInfix)) {
        if (precedence == null) {
          precedence = Precedence.DEFAULT;
        }
        push(reference, precedence, elem.getFixity() == Fixity.POSTFIX && !precedence.isInfix);
        if (elem.getExpression() instanceof Concrete.AppExpression) {
          for (Concrete.Argument argument : ((Concrete.AppExpression) elem.getExpression()).getArguments()) {
            push(argument.expression, argument.isExplicit());
          }
        }
      } else {
        push(elem);
      }
    }

    return rollUp();
  }

  public void push(Concrete.BinOpSequenceElem elem) {
    if (elem instanceof Concrete.ExplicitBinOpSequenceElem) {
      push(elem.getExpression(), true);
    } else if (elem instanceof Concrete.ImplicitBinOpSequenceElem) {
      push(elem.getExpression(), false);
    } else {
      StackElem topElem = myStack.isEmpty() ? null : myStack.get(myStack.size() - 1);
      boolean lastIsExpr = topElem != null && topElem.expression != null && topElem.precedence == null;
      if (elem instanceof Concrete.ClausesBinOpSequenceElem || !lastIsExpr && elem instanceof Concrete.CoclausesBinOpSequenceElem || lastIsExpr && (elem instanceof Concrete.NewBinOpSequenceElem || elem instanceof Concrete.EvalBinOpSequenceElem)) {
        myErrorReporter.report(new ParsingError("Unexpected element", elem.getData()));
      } else if (lastIsExpr) {
        topElem.expression = Concrete.ClassExtExpression.make(elem.getData(), topElem.expression, ((Concrete.CoclausesBinOpSequenceElem) elem).coclauses);
        topElem.foldModifiers();
      } else {
        Modifier modifier = elem instanceof Concrete.NewBinOpSequenceElem ? Modifier.NEW : elem instanceof Concrete.EvalBinOpSequenceElem && ((Concrete.EvalBinOpSequenceElem) elem).isPEval ? Modifier.PEVAL : Modifier.EVAL;
        if (topElem == null || topElem.precedence != null) {
          myStack.add(new StackElem(modifier, elem.getData()));
        } else {
          topElem.modifiers.add(new Pair<>(elem.getData(), modifier));
        }
      }
    }
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
    if (topElem.precedence != null && isExplicit) {
      myStack.add(new StackElem(expression, null));
    } else if (topElem.expression == null && !isExplicit) {
      myErrorReporter.report(new ParsingError("Expected an explicit expression", expression));
    } else {
      topElem.expression = topElem.expression == null ? expression : Concrete.AppExpression.make(topElem.expression.getData(), topElem.expression, expression, isExplicit);
    }
  }

  public void push(Concrete.ReferenceExpression reference, @NotNull Precedence precedence, boolean isPostfix) {
    if (myStack.isEmpty()) {
      myStack.add(new StackElem(reference, precedence));
      return;
    }

    while (true) {
      StackElem topElem = myStack.get(myStack.size() - 1);
      if (topElem.precedence != null || topElem.expression == null) {
        myErrorReporter.report(new ParsingError("Expected an expression", reference));
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
        myErrorReporter.report(new PrecedenceError(nextElem.getOperator(), (GlobalReferable) reference.getReferent(), reference));
      }

      foldTop();
    }
  }

  private void foldTop() {
    StackElem topElem = myStack.remove(myStack.size() - 1);
    if (topElem.expression == null) {
      myErrorReporter.report(new ParsingError("Expected an expression", topElem.modifiers.get(topElem.modifiers.size() - 1).proj1));
      return;
    }
    topElem.foldModifiers();
    if (topElem.precedence != null && myStack.size() > 1) {
      StackElem nextElem = myStack.get(myStack.size() - 2);
      myErrorReporter.report(new ParsingError("The operator " + topElem.getOperator() + " [" + topElem.precedence + "] of a section must have lower precedence than that of the operand, namely " + nextElem.getOperator() + " [" + nextElem.precedence + "]", topElem.expression));
      topElem = myStack.remove(myStack.size() - 1);
    }

    StackElem midElem = myStack.remove(myStack.size() - 1);
    midElem.foldModifiers();
    StackElem botElem = myStack.isEmpty() ? null : myStack.remove(myStack.size() - 1);
    if (botElem != null) {
      botElem.foldModifiers();
    }

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
    myStack.get(0).foldModifiers();
    Concrete.Expression result = myStack.get(0).expression;
    myStack.clear();
    return result;
  }
}
