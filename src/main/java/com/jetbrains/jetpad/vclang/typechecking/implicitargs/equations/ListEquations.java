package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.IgnoreBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationsError;
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
    Abstract.SourceNode sourceNode;
    /*
    List<Binding> bindings = Collections.emptyList();

    void abstractBinding(Binding binding) {
      if (expr1.findBinding(binding) || expr2.findBinding(binding)) {
        if (bindings.isEmpty()) {
          bindings = new ArrayList<>(3);
        }
        bindings.add(binding);
      }
    }
    */

    TypeCheckingError abstractBinding(Binding binding) {
      if (expr1.findBinding(binding) || expr2.findBinding(binding)) {
        return new SolveEquationsError(expr1, expr2, binding, sourceNode);
      } else {
        return null;
      }
    }
  }

  private final List<Equation> myEquations = new ArrayList<>();
  // TODO: add <=, => solutions
  private final Map<InferenceBinding, Expression> mySolutions = new HashMap<>();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();

  @Override
  public boolean add(Equations equations) {
    if (equations.isEmpty()) {
      return true;
    }
    if (equations instanceof ListEquations) {
      myEquations.addAll(((ListEquations) equations).myEquations);
      addSolutions(((ListEquations) equations).mySolutions);
      ((ListEquations) equations).myErrorReporter.reportTo(myErrorReporter);
    } else {
      return false;
    }
    return true;
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    boolean isInf1 = expr1 instanceof ReferenceExpression && ((ReferenceExpression) expr1).getBinding() instanceof InferenceBinding;
    boolean isInf2 = expr2 instanceof ReferenceExpression && ((ReferenceExpression) expr2).getBinding() instanceof InferenceBinding;
    if (isInf1 && isInf2 && ((ReferenceExpression) expr1).getBinding() == ((ReferenceExpression) expr2).getBinding()) {
      return true;
    }

    if (isInf1 && !isInf2) {
      addSolution((InferenceBinding) ((ReferenceExpression) expr1).getBinding(), expr2);
    } else
    if (isInf2 && !isInf1) {
      addSolution((InferenceBinding) ((ReferenceExpression) expr2).getBinding(), expr1);
    } else {
      Equation equation = new Equation();
      equation.expr1 = expr1;
      equation.expr2 = expr2;
      equation.cmp = cmp;
      equation.sourceNode = sourceNode;
      myEquations.add(equation);
    }

    return true;
  }

  private void addSolution(InferenceBinding binding, Expression expression) {
    if (!(binding instanceof IgnoreBinding)) {
      Expression expr = mySolutions.get(binding);
      if (expr != null) {
        if (!CompareVisitor.compare(this, CMP.EQ, expression, expr, binding.getSourceNode())) {
          binding.reportError(myErrorReporter, expression, expr);
        }
      } else {
        mySolutions.put(binding, expression);
      }
    }
  }

  private void addSolutions(Map<InferenceBinding, Expression> solutions) {
    for (Map.Entry<InferenceBinding, Expression> entry : solutions.entrySet()) {
      addSolution(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    myEquations.clear();
    mySolutions.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty() && mySolutions.isEmpty() && myErrorReporter.getErrorList().isEmpty();
  }

  @Override
  public void abstractBinding(Binding binding) {
    for (Iterator<Equation> it = myEquations.iterator(); it.hasNext(); ) {
      Equation equation = it.next();
      TypeCheckingError error = equation.abstractBinding(binding);
      if (error != null) {
        myErrorReporter.report(error);
        it.remove();
      }
    }

    for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = it.next();
      if (entry.getValue().findBinding(binding)) {
        myErrorReporter.report(new SolveEquationsError(Reference(entry.getKey()), entry.getValue(), binding, entry.getKey().getSourceNode()));
        it.remove();
      }
    }
  }

  @Override
  public Equations newInstance() {
    return new ListEquations();
  }

  @Override
  public Substitution getInferenceVariables(Set<InferenceBinding> bindings) {
    Substitution result = new Substitution();
    if (mySolutions.isEmpty() || bindings.isEmpty()) {
      return result;
    }

    boolean was;
    do {
      was = false;
      Substitution substitution = new Substitution();
      for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<InferenceBinding, Expression> entry = it.next();
        if (bindings.remove(entry.getKey())) {
          was = true;
          it.remove();
          Expression subst = entry.getValue();
          if (subst.findBinding(entry.getKey())) {
            entry.getKey().reportError(myErrorReporter, subst);
          } else {
            // TODO: There are a lot of bugs
            // Expression expectedType = entry.getKey().getType();
            // Expression actualType = subst.getType();
            // if (expectedType != null && actualType != null && CompareVisitor.compare(this, CMP.GE, expectedType.normalize(NormalizeVisitor.Mode.NF), actualType.normalize(NormalizeVisitor.Mode.NF), entry.getKey().getSourceNode())) {
              substitution.add(entry.getKey(), subst);
            // } else {
            //   entry.getKey().reportError(myErrorReporter, subst);
            // }
            break;
          }
        }
      }

      result.add(substitution);
      subst(substitution);
    } while (was);

    return result;
  }

  public void subst(Substitution substitution) {
    for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = it.next();
      Expression expr = entry.getValue().subst(substitution).normalize(NormalizeVisitor.Mode.NF);
      entry.setValue(expr);
      if (expr instanceof ReferenceExpression) {
        Binding binding = ((ReferenceExpression) expr).getBinding();
        if (binding instanceof InferenceBinding) {
          it.remove();
          if (binding != entry.getKey()) {
            add(Reference(entry.getKey()), expr, CMP.EQ, entry.getKey().getSourceNode());
          }
        }
      }
    }

    int size = myEquations.size();
    while (size-- > 0) {
      Equation equation = myEquations.get(0);
      myEquations.remove(0);
      Expression expr1 = equation.expr1.subst(substitution);
      Expression expr2 = equation.expr2.subst(substitution);
      if (!CompareVisitor.compare(this, equation.cmp, expr1.normalize(NormalizeVisitor.Mode.NF), expr2.normalize(NormalizeVisitor.Mode.NF), equation.sourceNode)) {
        myErrorReporter.report(new TypeCheckingError("Cannot solve equation:\nFirst expression: " + expr1.normalize(NormalizeVisitor.Mode.HUMAN_NF) + "Second expression: " + expr2.normalize(NormalizeVisitor.Mode.HUMAN_NF), equation.sourceNode));
      }
    }
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter) {
    myErrorReporter.reportTo(errorReporter);
    myErrorReporter.getErrorList().clear();
  }
}
