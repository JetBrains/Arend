package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
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
import com.jetbrains.jetpad.vclang.typechecking.exprorder.StandardOrder;

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

  private interface Solution {
    TypeCheckingError abstractBinding(InferenceBinding infBinding, Binding binding);
    Expression solve(ListEquations equations, InferenceBinding binding, Substitution substitution);
    void subst(Binding binding, Expression subst);
  }

  private static class ExpressionSolution implements Solution {
    public Expression expression;

    public ExpressionSolution(Expression expression) {
      this.expression = expression;
    }

    @Override
    public TypeCheckingError abstractBinding(InferenceBinding infBinding, Binding binding) {
      if (expression.findBinding(binding)) {
        return new SolveEquationsError(Reference(infBinding), expression, binding, infBinding.getSourceNode());
      }
      return null;
    }

    @Override
    public Expression solve(ListEquations equations, InferenceBinding binding, Substitution substitution) {
      return expression;
    }

    @Override
    public void subst(Binding binding, Expression subst) {
      expression = expression.subst(binding, subst);
    }
  }

  private static class EqSetSolution implements Solution {
    public List<Expression> geSet;
    public List<Expression> leSet;

    @Override
    public TypeCheckingError abstractBinding(InferenceBinding infBinding, Binding binding) {
      for (Expression expr : geSet) {
        if (expr.findBinding(binding)) {
          return new SolveEquationsError(Reference(infBinding), expr, binding, infBinding.getSourceNode());
        }
      }
      for (Expression expr : leSet) {
        if (expr.findBinding(binding)) {
          return new SolveEquationsError(Reference(infBinding), expr, binding, infBinding.getSourceNode());
        }
      }
      return null;
    }

    @Override
    public Expression solve(ListEquations equations, InferenceBinding binding, Substitution substitution) {
      if (geSet.isEmpty()) {
        Expression result = leSet.get(0).subst(substitution).normalize(NormalizeVisitor.Mode.NF);
        for (int i = 1; i < leSet.size(); i++) {
          Expression expr = leSet.get(i).subst(substitution).normalize(NormalizeVisitor.Mode.NF);
          if (!CompareVisitor.compare(equations, CMP.LE, result, expr, null)) {
            binding.reportErrorInfer(equations.getErrorReporter(), result, expr);
            return null;
          }
        }
        return result;
      } else {
        Expression result = geSet.get(0);
        if (geSet.size() > 1) {
          StandardOrder order = new StandardOrder();
          for (int i = 1; i < geSet.size(); i++) {
            result = order.max(result, geSet.get(i));
          }
        }

        result = result.subst(substitution).normalize(NormalizeVisitor.Mode.NF);
        if (!leSet.isEmpty()) {
          for (Expression expr : leSet) {
            expr = expr.subst(substitution).normalize(NormalizeVisitor.Mode.NF);
            if (!CompareVisitor.compare(equations, CMP.LE, result, expr, null)) {
              binding.reportErrorInfer(equations.getErrorReporter(), result, expr);
              return null;
            }
          }
        }
        return result;
      }
    }

    @Override
    public void subst(Binding binding, Expression subst) {
      for (int i = 0; i < geSet.size(); i++) {
        geSet.set(i, geSet.get(i).subst(binding, subst));
      }
      for (int i = 0; i < leSet.size(); i++) {
        leSet.set(i, leSet.get(i).subst(binding, subst));
      }
    }
  }

  private final List<Equation> myEquations = new ArrayList<>();
  private final Map<InferenceBinding, Solution> mySolutions = new HashMap<>();
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();

  public ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

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
      addSolution((InferenceBinding) ref1.getBinding(), cmp, expr2);
    } else
    if (isInf2 && !isInf1) {
      addSolution((InferenceBinding) ref2.getBinding(), cmp.not(), expr1);
    } else {
      myEquations.add(new CmpEquation(expr1, expr2, cmp, sourceNode));
    }

    return true;
  }

  private void addSolution(InferenceBinding binding, CMP cmp, Expression expr) {
    if (cmp == CMP.EQ) {
      addSolution(binding, new ExpressionSolution(expr));
    } else {
      List<Expression> expressions = new ArrayList<>();
      expressions.add(expr);
      EqSetSolution sol = new EqSetSolution();
      if (cmp == CMP.GE) {
        sol.geSet = expressions;
        sol.leSet = Collections.emptyList();
      } else {
        sol.geSet = Collections.emptyList();
        sol.leSet = expressions;
      }
      addSolution(binding, sol);
    }
  }

  private void addSolution(InferenceBinding binding, Solution sol2) {
    Solution sol1 = mySolutions.get(binding);
    if (sol1 != null) {
      if (sol1 instanceof ExpressionSolution) {
        Expression expr1 = ((ExpressionSolution) sol1).expression.normalize(NormalizeVisitor.Mode.NF);
        if (sol2 instanceof ExpressionSolution) {
          Expression expr2 = ((ExpressionSolution) sol2).expression.normalize(NormalizeVisitor.Mode.NF);
          if (!CompareVisitor.compare(this, CMP.EQ, expr2, expr1, binding.getSourceNode())) {
            binding.reportErrorInfer(myErrorReporter, expr2, expr1);
          }
        } else
        if (sol2 instanceof EqSetSolution) {
          for (Expression expr : ((EqSetSolution) sol2).geSet) {
            expr = expr.normalize(NormalizeVisitor.Mode.NF);
            if (!CompareVisitor.compare(this, CMP.GE, expr1, expr, binding.getSourceNode())) {
              binding.reportErrorInfer(myErrorReporter, expr1, expr);
            }
          }
          for (Expression expr : ((EqSetSolution) sol2).leSet) {
            expr = expr.normalize(NormalizeVisitor.Mode.NF);
            if (!CompareVisitor.compare(this, CMP.LE, expr1, expr, binding.getSourceNode())) {
              binding.reportErrorInfer(myErrorReporter, expr1, expr);
            }
          }
        } else {
          throw new IllegalStateException();
        }
      } else
      if (sol1 instanceof EqSetSolution) {
        if (sol2 instanceof ExpressionSolution) {
          Expression expr2 = ((ExpressionSolution) sol2).expression.normalize(NormalizeVisitor.Mode.NF);
          mySolutions.put(binding, sol2);
          for (Expression expr : ((EqSetSolution) sol1).geSet) {
            expr = expr.normalize(NormalizeVisitor.Mode.NF);
            if (!CompareVisitor.compare(this, CMP.GE, expr2, expr, binding.getSourceNode())) {
              binding.reportErrorInfer(myErrorReporter, expr2, expr);
            }
          }
          for (Expression expr : ((EqSetSolution) sol1).leSet) {
            expr = expr.normalize(NormalizeVisitor.Mode.NF);
            if (!CompareVisitor.compare(this, CMP.LE, expr2, expr, binding.getSourceNode())) {
              binding.reportErrorInfer(myErrorReporter, expr2, expr);
            }
          }
        } else
        if (sol2 instanceof EqSetSolution) {
          if (!((EqSetSolution) sol2).geSet.isEmpty()) {
            if (((EqSetSolution) sol1).geSet.isEmpty()) {
              ((EqSetSolution) sol1).geSet = new ArrayList<>();
            }
            ((EqSetSolution) sol1).geSet.addAll(((EqSetSolution) sol2).geSet);
          }
          if (!((EqSetSolution) sol2).leSet.isEmpty()) {
            if (((EqSetSolution) sol1).leSet.isEmpty()) {
              ((EqSetSolution) sol1).leSet = new ArrayList<>();
            }
            ((EqSetSolution) sol1).leSet.addAll(((EqSetSolution) sol2).leSet);
          }
        } else {
          throw new IllegalStateException();
        }
      } else {
        throw new IllegalStateException();
      }
    } else {
      mySolutions.put(binding, sol2);
    }
  }

  private void addSolutions(Map<InferenceBinding, Solution> solutions) {
    for (Map.Entry<InferenceBinding, Solution> entry : solutions.entrySet()) {
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

    for (Iterator<Map.Entry<InferenceBinding, Solution>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Solution> entry = it.next();
      TypeCheckingError error = entry.getValue().abstractBinding(entry.getKey(), binding);
      if (error != null) {
        myErrorReporter.report(error);
        it.remove();
      }
    }
  }

  @Override
  public Equations newInstance() {
    return new ListEquations();
  }

  @Override
  public Substitution getInferenceVariables(Set<InferenceBinding> bindings, boolean onlyPreciseSolutions) {
    Substitution result = new Substitution();
    if (mySolutions.isEmpty() || bindings.isEmpty()) {
      return result;
    }

    boolean was;
    do {
      was = false;
      InferenceBinding binding = null;
      Expression subst = null;
      for (Iterator<Map.Entry<InferenceBinding, Solution>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<InferenceBinding, Solution> entry = it.next();
        if (onlyPreciseSolutions && entry.getValue() instanceof EqSetSolution) {
          continue;
        }

        if (bindings.remove(entry.getKey())) {
          was = true;
          it.remove();
          subst = entry.getValue().solve(this, entry.getKey(), result);
          if (subst != null) {
            if (subst.findBinding(entry.getKey())) {
              entry.getKey().reportErrorInfer(myErrorReporter, subst);
            } else {
              Expression expectedType = entry.getKey().getType().subst(result);
              Expression actualType = subst.getType().subst(result);
              if (CompareVisitor.compare(this, CMP.GE, expectedType.normalize(NormalizeVisitor.Mode.NF), actualType.normalize(NormalizeVisitor.Mode.NF), entry.getKey().getSourceNode())) {
                binding = entry.getKey();
              } else {
                entry.getKey().reportErrorMismatch(myErrorReporter, expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), actualType.normalize(NormalizeVisitor.Mode.HUMAN_NF), subst);
              }
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
    for (Iterator<Map.Entry<InferenceBinding, Solution>> it = mySolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, Solution> entry = it.next();
      Solution solution = entry.getValue();
      solution.subst(binding, subst);
      if (solution instanceof ExpressionSolution) {
        ReferenceExpression expr = ((ExpressionSolution) solution).expression.normalize(NormalizeVisitor.Mode.NF).toReference();
        if (expr != null) {
          Binding binding1 = expr.getBinding();
          if (binding1 instanceof InferenceBinding) {
            it.remove();
            if (binding1 != entry.getKey()) {
              add(Reference(entry.getKey()), expr, CMP.EQ, entry.getKey().getSourceNode());
            }
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
