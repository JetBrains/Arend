package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.DerivedInferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedBindings;
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedEquations;

import java.util.*;

public class TwoStageEquations implements Equations {
  private List<Equation> myEquations;
  private Map<InferenceBinding, Expression> mySolutions;
  private final LevelEquations myLevelEquations;
  private final List<InferenceBinding> myUnsolvedVariables;
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();

  public TwoStageEquations() {
    myEquations = new ArrayList<>();
    mySolutions = new HashMap<>();
    myLevelEquations = new LevelEquations();
    myUnsolvedVariables = Collections.emptyList();
  }

  @Override
  public boolean add(Equations equations) {
    if (equations.isEmpty()) {
      return true;
    }
    if (!(equations instanceof TwoStageEquations)) {
      return false;
    }
    TwoStageEquations eq = (TwoStageEquations) equations;

    for (Map.Entry<InferenceBinding, Expression> entry : eq.mySolutions.entrySet()) {
      addSolution(entry.getKey(), entry.getValue());
    }
    myEquations.addAll(eq.myEquations);
    myLevelEquations.add(eq.myLevelEquations);
    return true;
  }

  private void addSolution(InferenceBinding binding, Expression expression) {
    Expression expr = mySolutions.get(binding);
    if (expr != null) {
      myEquations.add(new Equation(expr, expression, CMP.EQ, binding.getSourceNode()));
    } else {
      mySolutions.put(binding, expression);
    }
  }

  private void addEquation(Type type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode) {
    ReferenceExpression ref1 = type instanceof Expression ? ((Expression) type).toReference() : null;
    ReferenceExpression ref2 = expr.toReference();
    InferenceBinding inf1 = ref1 != null && ref1.getBinding() instanceof InferenceBinding ? (InferenceBinding) ref1.getBinding() : null;
    InferenceBinding inf2 = ref2 != null && ref2.getBinding() instanceof InferenceBinding ? (InferenceBinding) ref2.getBinding() : null;

    if (inf1 == inf2 && inf1 != null) {
      return;
    }

    if (inf1 != null && inf2 != null && cmp == CMP.EQ) {
      addSolution(inf1, ref2);
      addSolution(inf2, ref1);
      return;
    }

    if (inf1 != null && inf2 == null || inf2 != null && inf1 == null) {
      InferenceBinding cInf = inf1 != null ? inf1 : inf2;
      Type cType = inf1 != null ? expr : type;

      if (cType instanceof Expression) {
        Expression cExpr = (Expression) cType;
        if (cExpr.toPi() == null && cExpr.toUniverse() == null && cExpr.toClassCall() == null) {
          cmp = CMP.EQ;
        }
      }

      if (cmp == CMP.EQ) {
        assert cType instanceof Expression;
        addSolution(cInf, (Expression) cType);
        return;
      }

      if (inf1 != null) {
        cmp = cmp.not();
      }

      DependentLink piParams = cType.getPiParameters();
      if (piParams.hasNext()) {
        DerivedInferenceBinding newInf = new DerivedInferenceBinding(cInf.getName() + "-cod", new UniverseExpression(new Sort(Level.INFINITY, Level.INFINITY)), cInf);
        myUnsolvedVariables.add(newInf);
        Expression newRef = new ReferenceExpression(newInf);
        addSolution(cInf, new PiExpression(piParams, newRef));
        addEquation(cType.getPiCodomain(), newRef, cmp, sourceNode);
        return;
      }

      SortMax sorts = cType.toSorts();
      if (sorts != null) {
        DerivedInferenceBinding lpInf = new DerivedInferenceBinding(cInf.getName() + "-lp", new DataCallExpression(Preprelude.LVL), cInf);
        DerivedInferenceBinding lhInf = new DerivedInferenceBinding(cInf.getName() + "-lh", new DataCallExpression(Preprelude.CNAT), cInf);
        myUnsolvedVariables.add(lpInf);
        myUnsolvedVariables.add(lhInf);
        Level lp = new Level(lpInf);
        Level lh = new Level(lhInf);
        addSolution(cInf, new UniverseExpression(new Sort(lp, lh)));
        if (cmp == CMP.LE) {
          sorts.getPLevel().isLessOrEquals(lp, this, sourceNode);
          sorts.getHLevel().isLessOrEquals(lh, this, sourceNode);
        } else {
          Sort sort = sorts.toSort();
          myLevelEquations.add(lp, sort.getPLevel(), CMP.LE, sourceNode);
          myLevelEquations.add(lh, sort.getHLevel(), CMP.LE, sourceNode);
        }
        return;
      }
    }

    myEquations.add(new Equation(type, expr, cmp, sourceNode));
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    addEquation(expr1, expr2, cmp, sourceNode);
    return true;
  }

  @Override
  public boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode) {
    myLevelEquations.add(level1, level2, cmp, sourceNode);
    return true;
  }

  @Override
  public boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode) {
    addEquation(type, expr, CMP.LE, sourceNode);
    return true;
  }

  @Override
  public void clear() {
    myEquations.clear();
    mySolutions.clear();
    myLevelEquations.clear();
    myUnsolvedVariables.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty() && mySolutions.isEmpty() && myLevelEquations.isEmpty();
  }

  @Override
  public void abstractBinding(Binding binding) {
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.type.findBinding(binding) || equation.expr.findBinding(binding)) {
        myErrorReporter.report(new SolveEquationsError<>(equation.type, equation.expr, binding, equation.sourceNode));
        iterator.remove();
      }
    }

    for (Iterator<Map.Entry<InferenceBinding, Expression>> iterator = mySolutions.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = iterator.next();
      if (entry.getValue().findBinding(binding)) {
        myErrorReporter.report(new SolveEquationsError<>(new ReferenceExpression(entry.getKey()), entry.getValue(), binding, entry.getKey().getSourceNode()));
        iterator.remove();
      }
    }
  }

  @Override
  public Equations newInstance() {
    return new TwoStageEquations();
  }

  @Override
  public Substitution getInferenceVariables(Set<InferenceBinding> bindings, boolean isFinal) {
    bindings.addAll(myUnsolvedVariables);
    myUnsolvedVariables.clear();

    Substitution result = new Substitution();
    ExprSubstitution substitution = new ExprSubstitution();
    do {
      substitution.clear();
      for (Iterator<InferenceBinding> iterator = bindings.iterator(); iterator.hasNext(); ) {
        InferenceBinding binding = iterator.next();
        Expression solution = mySolutions.get(binding);
        if (solution != null) {
          substitution.add(binding, solution);
          iterator.remove();
        }
      }
      subst(substitution);
      result.exprSubst.add(substitution);
    } while (!substitution.getDomain().isEmpty());

    return result;
  }

  private void subst(ExprSubstitution substitution) {
    List<Equation> equations = myEquations;
    myEquations = new ArrayList<>(equations.size());
    for (Equation equation : equations) {
      addEquation(equation.type.subst(substitution, new LevelSubstitution()), equation.expr.subst(substitution), equation.cmp, equation.sourceNode);
    }

    Map<InferenceBinding, Expression> solutions = mySolutions;
    mySolutions = new HashMap<>();
    for (Map.Entry<InferenceBinding, Expression> entry : solutions.entrySet()) {
      mySolutions.put(entry.getKey(), entry.getValue().subst(substitution));
    }
  }

  @Override
  public void reportErrors(ErrorReporter errorReporter) {
    myErrorReporter.reportTo(errorReporter);

    if (!myEquations.isEmpty()) {
      errorReporter.report(new UnsolvedEquations(myEquations));
    }

    if (!mySolutions.isEmpty()) {
      errorReporter.report(new UnsolvedBindings(new ArrayList<>(mySolutions.keySet())));
    }

    myErrorReporter.getErrorList().clear();
    clear();
  }
}
