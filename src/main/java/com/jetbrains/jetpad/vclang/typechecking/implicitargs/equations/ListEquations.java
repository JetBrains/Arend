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
  private interface Equation {
    TypeCheckingError abstractBinding(Binding binding);
    void subst(Binding binding, Expression subst);
    void solveIn(ListEquations equations);
  }

  private static class CmpEquation implements Equation {
    Expression expr1;
    Expression expr2;
    CMP cmp;
    Abstract.SourceNode sourceNode;

    public CmpEquation(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
      this.expr1 = expr1;
      this.expr2 = expr2;
      this.cmp = cmp;
      this.sourceNode = sourceNode;
    }

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

    @Override
    public TypeCheckingError abstractBinding(Binding binding) {
      if (expr1.findBinding(binding) || expr2.findBinding(binding)) {
        return new SolveEquationsError(expr1, expr2, binding, sourceNode);
      } else {
        return null;
      }
    }

    @Override
    public void subst(Binding binding, Expression subst) {
      expr1 = expr1.subst(binding, subst);
      expr2 = expr2.subst(binding, subst);
    }

    @Override
    public void solveIn(ListEquations equations) {
      if (!CompareVisitor.compare(equations, cmp, expr1.normalize(NormalizeVisitor.Mode.NF), expr2.normalize(NormalizeVisitor.Mode.NF), sourceNode)) {
        equations.myErrorReporter.report(new SolveEquationsError(expr1.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr2.normalize(NormalizeVisitor.Mode.HUMAN_NF), null, sourceNode));
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
    ReferenceExpression ref1 = expr1.toReference();
    ReferenceExpression ref2 = expr2.toReference();
    boolean isInf1 = ref1 != null && ref1.getBinding() instanceof InferenceBinding;
    boolean isInf2 = ref2 != null && ref2.getBinding() instanceof InferenceBinding;
    if (isInf1 && isInf2 && ref1.getBinding() == ref2.getBinding()) {
      return true;
    }

    if (isInf1 && !isInf2) {
      addSolution((InferenceBinding) ref1.getBinding(), expr2);
    } else
    if (isInf2 && !isInf1) {
      addSolution((InferenceBinding) ref2.getBinding(), expr1);
    } else {
      myEquations.add(new CmpEquation(expr1, expr2, cmp, sourceNode));
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
      InferenceBinding binding = null;
      Expression subst = null;
      for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<InferenceBinding, Expression> entry = it.next();
        if (bindings.remove(entry.getKey())) {
          was = true;
          it.remove();
          subst = entry.getValue();
          if (subst.findBinding(entry.getKey())) {
            entry.getKey().reportError(myErrorReporter, subst);
          } else {
            Expression expectedType = entry.getKey().getType().subst(result);
            Expression actualType = subst.getType().subst(result);
            if (expectedType != null && CompareVisitor.compare(this, CMP.GE, expectedType.normalize(NormalizeVisitor.Mode.NF), actualType.normalize(NormalizeVisitor.Mode.NF), entry.getKey().getSourceNode())) {
              binding = entry.getKey();
            } else {
              entry.getKey().reportError(myErrorReporter, subst);
            }
          }
          break;
        }
      }

      if (binding != null) {
        result.add(binding, subst);
        subst(binding, subst);
      }
    } while (was);

    return result;
  }

  public void subst(Binding binding, Expression subst) {
    for (Iterator<Map.Entry<InferenceBinding, Expression>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = it.next();
      Expression expr = entry.getValue().subst(binding, subst).normalize(NormalizeVisitor.Mode.NF);
      entry.setValue(expr);
      ReferenceExpression ref = expr.toReference();
      if (ref != null) {
        Binding binding1 = ref.getBinding();
        if (binding1 instanceof InferenceBinding) {
          it.remove();
          if (binding1 != entry.getKey()) {
            add(Reference(entry.getKey()), expr, CMP.EQ, entry.getKey().getSourceNode());
          }
        }
      }
    }

    for (int i = myEquations.size() - 1; i >= 0; i--) {
      Equation equation = myEquations.get(i);
      myEquations.remove(i);
      equation.subst(binding, subst);
      equation.solveIn(this);
    }
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter) {
    myErrorReporter.reportTo(errorReporter);
    myErrorReporter.getErrorList().clear();
  }
}
