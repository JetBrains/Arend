package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.IgnoreBinding;
import com.jetbrains.jetpad.vclang.term.definition.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class ListEquations implements Equations {
  private static class Equation {
    Expression expr1;
    Expression expr2;
    CMP cmp;
    // TODO: check for abstracted bindings
    List<Binding> bindings = Collections.emptyList();

    void abstractBinding(Binding binding) {
      if (expr1.findBinding(binding) || expr2.findBinding(binding)) {
        if (bindings.isEmpty()) {
          bindings = new ArrayList<>(3);
        }
        bindings.add(binding);
      }
    }
  }

  private final List<Equation> myEquations = new ArrayList<>();
  // TODO: add <=, => solutions
  private final Map<InferenceBinding, Expression> mySolutions = new HashMap<>();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();

  @Override
  public void add(Equations equations) {
    if (equations.isEmpty()) {
      return;
    }
    if (equations instanceof ListEquations) {
      myEquations.addAll(((ListEquations) equations).myEquations);
      mySolutions.putAll(((ListEquations) equations).mySolutions);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void add(Expression expr1, Expression expr2, CMP cmp) {
    boolean isInf1 = expr1 instanceof ReferenceExpression && ((ReferenceExpression) expr1).getBinding() instanceof InferenceBinding;
    boolean isInf2 = expr2 instanceof ReferenceExpression && ((ReferenceExpression) expr2).getBinding() instanceof InferenceBinding;
    if (!isInf1 && !isInf2) {
      Equation equation = new Equation();
      equation.expr1 = expr1;
      equation.expr2 = expr2;
      equation.cmp = cmp;
      myEquations.add(equation);
    } else {
      if (isInf1 && isInf2) {
        if (((ReferenceExpression) expr1).getBinding() == ((ReferenceExpression) expr2).getBinding()) {
          return;
        }
      }
      if (isInf1) {
        addSolution((InferenceBinding) ((ReferenceExpression) expr1).getBinding(), expr2);
      }
      if (isInf2) {
        addSolution((InferenceBinding) ((ReferenceExpression) expr2).getBinding(), expr1);
      }
    }
  }

  private void addSolution(InferenceBinding binding, Expression expression) {
    if (!(binding instanceof IgnoreBinding)) {
      mySolutions.put(binding, expression);
    }
  }

  @Override
  public void clear() {
    myEquations.clear();
    mySolutions.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty() && mySolutions.isEmpty();
  }

  @Override
  public void abstractBinding(Binding binding) {
    for (Equation equation : myEquations) {
      equation.abstractBinding(binding);
    }
  }

  @Override
  public Equations newInstance() {
    return new ListEquations();
  }

  @Override
  public Substitution getInferenceVariables(List<InferenceBinding> bindings0) {
    List<InferenceBinding> bindings = new ArrayList<>(bindings0);
    Substitution result = new Substitution();
    if (mySolutions.isEmpty()) {
      return result;
    }

    boolean was;
    do {
      was = false;
      Substitution substitution = new Substitution();
      for (int i = 0; i < bindings.size(); i++) {
        Expression subst = mySolutions.get(bindings.get(i));
        if (subst != null) {
          mySolutions.remove(bindings.get(i));
          if (subst.findBinding(bindings.get(i))) {
            // TODO: Add SourceNode
            myErrorReporter.report(new TypeCheckingError("Cannot infer variable " + bindings.get(i).getName() + "\nExpected expression: " + subst, null));
          } else {
            substitution.add(bindings.get(i), subst);
          }
          bindings.remove(i--);
          was = true;
        }
      }
      result.add(substitution);
      subst(substitution);
    } while (was);

    return result;
  }

  public void subst(Substitution substitution) {
    Map<InferenceBinding, Expression> solution = new HashMap<>();
    for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = it.next();
      Expression expr = entry.getValue().subst(substitution).normalize(NormalizeVisitor.Mode.NF);
      entry.setValue(expr);
      if (expr instanceof ReferenceExpression) {
        Binding binding = ((ReferenceExpression) expr).getBinding();
        if (binding == entry.getKey()) {
          it.remove();
        } else
        if (binding instanceof InferenceBinding) {
          solution.put((InferenceBinding) binding, Reference(entry.getKey()));
        }
      }
    }
    mySolutions.putAll(solution);

    int size = myEquations.size();
    while (size-- > 0) {
      Equation equation = myEquations.get(0);
      myEquations.remove(0);
      Expression expr1 = equation.expr1.subst(substitution);
      Expression expr2 = equation.expr2.subst(substitution);
      if (!CompareVisitor.compare(this, equation.cmp, expr1.normalize(NormalizeVisitor.Mode.NF), expr2.normalize(NormalizeVisitor.Mode.NF))) {
        // TODO: Add SourceNode
        myErrorReporter.report(new TypeCheckingError("Cannot solve equation:\nFirst expression: " + expr1.normalize(NormalizeVisitor.Mode.HUMAN_NF) + "Second expression: " + expr2.normalize(NormalizeVisitor.Mode.HUMAN_NF), null));
      }
    }
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter) {
    myErrorReporter.reportTo(errorReporter);
    myErrorReporter.getErrorList().clear();
  }
}
