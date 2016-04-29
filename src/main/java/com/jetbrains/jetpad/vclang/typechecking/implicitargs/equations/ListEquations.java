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
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedBindings;
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedEquations;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.exprorder.StandardOrder;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;

public class ListEquations implements Equations {
  public interface Equation {
    TypeCheckingError abstractBinding(Binding binding);
    void subst(Binding binding, Expression subst);
    void solveIn(ListEquations equations);
  }

  public static class CmpEquation implements Equation {
    public Expression expr1;
    public Expression expr2;
    public CMP cmp;
    public Abstract.SourceNode sourceNode;

    public CmpEquation(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
      this.expr1 = expr1;
      this.expr2 = expr2;
      this.cmp = cmp;
      this.sourceNode = sourceNode;
    }

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

  private static class ExactSolution implements Solution {
    public Expression expression;

    public ExactSolution(Expression expression) {
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
          for (int i = 1; i < geSet.size(); i++) {
            Expression max = StandardOrder.getInstance().max(result, geSet.get(i));
            if (max != null) {
              result = max;
            } else {
              Expression result1 = result.subst(substitution).normalize(NormalizeVisitor.Mode.NF);
              Expression expr = geSet.get(i).subst(substitution).normalize(NormalizeVisitor.Mode.NF);
              if (!CompareVisitor.compare(equations, CMP.GE, result1, expr, null)) {
                binding.reportErrorInfer(equations.getErrorReporter(), result1, expr);
                return null;
              }
            }
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
  private final Map<InferenceBinding, ExactSolution> myExactSolutions = new LinkedHashMap<>();
  private final Map<InferenceBinding, EqSetSolution> myEqSolutions = new LinkedHashMap<>();
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
      addExactSolutions(((ListEquations) equations).myExactSolutions);
      addEqSetSolutions(((ListEquations) equations).myEqSolutions);
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
    if (!StandardOrder.getInstance().isComparable(expr)) {
      cmp = CMP.EQ;
    }

    if (expr.toLevel() != null) {
      if (expr.toLevel().isZero()) {
        /* TODO
        if (cmp == CMP.GE) {
          return;
        }
        */
        if (cmp == CMP.LE) {
          cmp = CMP.EQ;
        }
      }
      if (expr.toLevel().isInfinity()) {
        /* TODO
        if (cmp == CMP.LE) {
          return;
        }
        */
        if (cmp == CMP.GE) {
          cmp = CMP.EQ;
        }
      }
    }

    if (cmp == CMP.EQ) {
      addSolution(binding, new ExactSolution(expr));
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

  private void addSolution(InferenceBinding binding, ExactSolution sol2) {
    ExactSolution sol1 = myExactSolutions.get(binding);
    if (sol1 != null) {
      Expression expr1 = sol1.expression.normalize(NormalizeVisitor.Mode.NF);
      Expression expr2 = sol2.expression.normalize(NormalizeVisitor.Mode.NF);
      if (!CompareVisitor.compare(this, CMP.EQ, expr2, expr1, binding.getSourceNode())) {
        binding.reportErrorInfer(myErrorReporter, expr2, expr1);
      }
      return;
    }

    EqSetSolution sol1eq = myEqSolutions.get(binding);
    if (sol1eq != null) {
      myEqSolutions.remove(binding);
      myExactSolutions.put(binding, sol2);
      mergeSolutions(sol2, sol1eq, binding);
      return;
    }

    myExactSolutions.put(binding, sol2);
  }

  private void addSolution(InferenceBinding binding, EqSetSolution sol2) {
    ExactSolution sol1 = myExactSolutions.get(binding);
    if (sol1 != null) {
      mergeSolutions(sol1, sol2, binding);
      return;
    }

    EqSetSolution sol1eq = myEqSolutions.get(binding);
    if (sol1eq != null) {
      if (!sol2.geSet.isEmpty()) {
        if (sol1eq.geSet.isEmpty()) {
          sol1eq.geSet = new ArrayList<>();
        }
        sol1eq.geSet.addAll(sol2.geSet);
      }
      if (!sol2.leSet.isEmpty()) {
        if (sol1eq.leSet.isEmpty()) {
          sol1eq.leSet = new ArrayList<>();
        }
        sol1eq.leSet.addAll(sol2.leSet);
      }
      return;
    }

    myEqSolutions.put(binding, sol2);
  }

  private void mergeSolutions(ExactSolution sol1, EqSetSolution sol2, InferenceBinding binding) {
    Expression expr1 = sol1.expression.normalize(NormalizeVisitor.Mode.NF);
    for (Expression expr : sol2.geSet) {
      expr = expr.normalize(NormalizeVisitor.Mode.NF);
      if (!CompareVisitor.compare(this, CMP.GE, expr1, expr, binding.getSourceNode())) {
        binding.reportErrorInfer(myErrorReporter, expr1, expr);
      }
    }
    for (Expression expr : sol2.leSet) {
      expr = expr.normalize(NormalizeVisitor.Mode.NF);
      if (!CompareVisitor.compare(this, CMP.LE, expr1, expr, binding.getSourceNode())) {
        binding.reportErrorInfer(myErrorReporter, expr1, expr);
      }
    }
  }

  private void addExactSolutions(Map<InferenceBinding, ExactSolution> solutions) {
    for (Map.Entry<InferenceBinding, ExactSolution> entry : solutions.entrySet()) {
      addSolution(entry.getKey(), entry.getValue());
    }
  }

  private void addEqSetSolutions(Map<InferenceBinding, EqSetSolution> solutions) {
    for (Map.Entry<InferenceBinding, EqSetSolution> entry : solutions.entrySet()) {
      addSolution(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    myEquations.clear();
    myExactSolutions.clear();
    myEqSolutions.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty() && myExactSolutions.isEmpty() && myEqSolutions.isEmpty() && myErrorReporter.getErrorList().isEmpty();
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

    abstractSolutions(myExactSolutions, binding);
    abstractSolutions(myEqSolutions, binding);
  }

  private void abstractSolutions(Map<InferenceBinding, ? extends Solution> solutions, Binding binding) {
    for (Iterator<? extends Map.Entry<InferenceBinding, ? extends Solution>> it = solutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, ? extends Solution> entry = it.next();
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
    if (bindings.isEmpty() || myExactSolutions.isEmpty() && (myEqSolutions.isEmpty() || onlyPreciseSolutions)) {
      return result;
    }

    boolean was;
    do {
      was = false;
      InferenceBinding binding = null;
      Expression subst = null;
      // TODO: First solve variables that are not levels, then level variables
      for (Iterator<Map.Entry<InferenceBinding, ExactSolution>> it = myExactSolutions.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<InferenceBinding, ExactSolution> entry = it.next();

        if (bindings.remove(entry.getKey())) {
          was = true;
          it.remove();
          subst = entry.getValue().solve(this, entry.getKey(), result);
          if (update(entry.getKey(), subst, result)) {
            binding = entry.getKey();
          }
          break;
        }
      }

      if (!was && !onlyPreciseSolutions) {
        for (Iterator<Map.Entry<InferenceBinding, EqSetSolution>> it = myEqSolutions.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry<InferenceBinding, EqSetSolution> entry = it.next();

          if (bindings.remove(entry.getKey())) {
            was = true;
            it.remove();
            subst = entry.getValue().solve(this, entry.getKey(), result);
            if (update(entry.getKey(), subst, result)) {
              binding = entry.getKey();
            }
            break;
          }
        }
      }

      if (binding != null) {
        result.subst(binding, subst);
        result.add(binding, subst);
        subst(binding, subst);
      }
    } while (was);

    return result;
  }

  private boolean update(InferenceBinding binding, Expression subst, Substitution result) {
    if (subst != null) {
      if (subst.findBinding(binding)) {
        binding.reportErrorInfer(myErrorReporter, subst);
      } else {
        Expression expectedType = binding.getType().subst(result);
        Expression actualType = subst.getType().subst(result);
        if (CompareVisitor.compare(this, CMP.GE, expectedType.normalize(NormalizeVisitor.Mode.NF), actualType.normalize(NormalizeVisitor.Mode.NF), binding.getSourceNode())) {
          return true;
        } else {
          binding.reportErrorMismatch(myErrorReporter, expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), actualType.normalize(NormalizeVisitor.Mode.HUMAN_NF), subst);
        }
      }
    }
    return false;
  }

  public void subst(Binding binding, Expression subst) {
    for (Iterator<Map.Entry<InferenceBinding, ExactSolution>> it = myExactSolutions.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<InferenceBinding, ExactSolution> entry = it.next();
      ExactSolution solution = entry.getValue();
      solution.subst(binding, subst);
      ReferenceExpression expr = solution.expression.normalize(NormalizeVisitor.Mode.NF).toReference();
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

    for (Map.Entry<InferenceBinding, EqSetSolution> entry : myEqSolutions.entrySet()) {
      entry.getValue().subst(binding, subst);
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
    if (myErrorReporter.getErrorList().isEmpty()) {
      if (!myEquations.isEmpty()) {
        List<CmpEquation> equations = new ArrayList<>(myEquations.size());
        for (Equation equation : myEquations) {
          equations.add((CmpEquation) equation);
        }
        errorReporter.report(new UnsolvedEquations(equations));
      }

      if (!myExactSolutions.isEmpty() || !myEqSolutions.isEmpty()) {
        List<InferenceBinding> bindings = new ArrayList<>(myExactSolutions.size() + myEqSolutions.size());
        for (InferenceBinding binding : myExactSolutions.keySet()) {
          bindings.add(binding);
        }
        for (InferenceBinding binding : myEqSolutions.keySet()) {
          bindings.add(binding);
        }
        errorReporter.report(new UnsolvedBindings(bindings));
      }
    } else {
      myErrorReporter.getErrorList().clear();
    }

    clear();
  }
}
