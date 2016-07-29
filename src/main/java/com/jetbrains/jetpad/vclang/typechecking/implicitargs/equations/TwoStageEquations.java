package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.DerivedInferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationError;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedBindings;
import com.jetbrains.jetpad.vclang.typechecking.error.UnsolvedEquations;

import java.util.*;

public class TwoStageEquations implements Equations {
  private List<Equation> myEquations;
  private Map<InferenceBinding, Expression> mySolutions;
  private final Map<LevelInferenceBinding, Binding> myBases;
  private final LevelEquations<LevelInferenceBinding> myLevelEquations;
  private final List<InferenceBinding> myUnsolvedVariables;
  private final ListErrorReporter myErrorReporter = new ListErrorReporter();

  public TwoStageEquations() {
    myEquations = new ArrayList<>();
    mySolutions = new HashMap<>();
    myBases = new HashMap<>();
    myLevelEquations = new LevelEquations<>();
    myUnsolvedVariables = new ArrayList<>(2);
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
    for (Map.Entry<LevelInferenceBinding, Binding> entry : eq.myBases.entrySet()) {
      addBase(entry.getKey(), entry.getValue(), entry.getKey().getSourceNode());
    }
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
        LevelInferenceBinding lpInf = new LevelInferenceBinding(cInf.getName() + "-lp", new DataCallExpression(Preprelude.LVL), cInf.getSourceNode());
        LevelInferenceBinding lhInf = new LevelInferenceBinding(cInf.getName() + "-lh", new DataCallExpression(Preprelude.CNAT), cInf.getSourceNode());
        myLevelEquations.addVariable(lpInf);
        myLevelEquations.addVariable(lhInf);
        Level lp = new Level(lpInf);
        Level lh = new Level(lhInf);
        addSolution(cInf, new UniverseExpression(new Sort(lp, lh)));
        if (cmp == CMP.LE) {
          sorts.getPLevel().isLessOrEquals(lp, this, sourceNode);
          sorts.getHLevel().isLessOrEquals(lh, this, sourceNode);
        } else {
          Sort sort = sorts.toSort();
          if (!sort.getPLevel().isInfinity()) {
            addLevelEquation(lpInf, sort.getPLevel().getVar(), sort.getPLevel().getConstant(), sourceNode);
          }
          if (!sort.getHLevel().isInfinity()) {
            addLevelEquation(lhInf, sort.getHLevel().getVar(), sort.getHLevel().getConstant(), sourceNode);
          }
        }
        return;
      }
    }

    myEquations.add(new Equation(type, expr, cmp, sourceNode));
  }

  private void addBase(LevelInferenceBinding var, Binding base, Abstract.SourceNode sourceNode) {
    Binding base1 = myBases.get(var);
    if (base1 == null) {
      myBases.put(var, base);
    } else {
      List<LevelEquation<Binding>> equations = new ArrayList<>(2);
      equations.add(new LevelEquation<>(base, var, 0));
      equations.add(new LevelEquation<>(base1, var, 0));
      myErrorReporter.report(new SolveEquationsError(equations, sourceNode));
    }
  }

  private void addLevelEquation(Binding var1, Binding var2, Integer constant, Abstract.SourceNode sourceNode) {
    if (!(var1 instanceof LevelInferenceBinding) && !(var2 instanceof LevelInferenceBinding)) {
      if (var1 != var2 || constant < 0) {
        myErrorReporter.report(new SolveEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant)), sourceNode));
      }
      return;
    }

    if (var1 != null && var2 instanceof LevelInferenceBinding) {
      Binding base = var1 instanceof LevelInferenceBinding ? myBases.get(var1) : var1;
      if (base != null) {
        addBase((LevelInferenceBinding) var2, base, sourceNode);
      }
    }

    myLevelEquations.addEquation(new LevelEquation<>(var1 instanceof LevelInferenceBinding ? (LevelInferenceBinding) var1 : null, var2 instanceof LevelInferenceBinding ? (LevelInferenceBinding) var2 : null, constant));
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode) {
    addEquation(expr1, expr2, cmp, sourceNode);
    return true;
  }

  @Override
  public boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode) {
    if (level1.isInfinity() && level2.isInfinity() || level1.isInfinity() && cmp == CMP.GE || level2.isInfinity() && cmp == CMP.LE) {
      return true;
    }
    if (level1.isInfinity()) {
      addLevelEquation(null, level2.getVar(), null, sourceNode);
      return true;
    }
    if (level2.isInfinity()) {
      addLevelEquation(null, level1.getVar(), null, sourceNode);
      return true;
    }

    if (cmp == CMP.LE || cmp == CMP.EQ) {
      addLevelEquation(level1.getVar(), level2.getVar(), level2.getConstant() - level1.getConstant(), sourceNode);
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), sourceNode);
    }
    return true;
  }

  @Override
  public boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode) {
    addEquation(type, expr, CMP.LE, sourceNode);
    return true;
  }

  @Override
  public boolean addVariable(LevelInferenceBinding var) {
    myLevelEquations.addVariable(var);
    return true;
  }

  @Override
  public void clear() {
    myEquations.clear();
    mySolutions.clear();
    myBases.clear();
    myLevelEquations.clear();
    myUnsolvedVariables.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty() && mySolutions.isEmpty() && myLevelEquations.isEmpty() && myUnsolvedVariables.isEmpty() && myBases.isEmpty();
  }

  @Override
  public void abstractBinding(Binding binding) {
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.type.findBinding(binding) || equation.expr.findBinding(binding)) {
        myErrorReporter.report(new SolveEquationError<>(equation.type, equation.expr, binding, equation.sourceNode));
        iterator.remove();
      }
    }

    for (Iterator<Map.Entry<InferenceBinding, Expression>> iterator = mySolutions.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<InferenceBinding, Expression> entry = iterator.next();
      if (entry.getValue().findBinding(binding)) {
        myErrorReporter.report(new SolveEquationError<>(new ReferenceExpression(entry.getKey()), entry.getValue(), binding, entry.getKey().getSourceNode()));
        iterator.remove();
      }
    }
  }

  @Override
  public Equations newInstance() {
    return new TwoStageEquations();
  }

  @Override
  public Substitution solve(Set<InferenceBinding> bindings, boolean isFinal) {
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
          update(binding, solution, substitution);
          substitution.add(binding, solution);
          mySolutions.remove(binding);
          iterator.remove();
        }
      }
      subst(substitution);
      result.exprSubst.add(substitution);
    } while (!substitution.getDomain().isEmpty());

    if (isFinal) {
      Map<LevelInferenceBinding, Integer> solution = new HashMap<>();
      LevelInferenceBinding var = myLevelEquations.solve(solution);
      if (var != null) {
        myErrorReporter.report(new SolveEquationsError(new ArrayList<LevelEquation<? extends Binding>>(myLevelEquations.getEquations()), var.getSourceNode()));
      }
      for (Map.Entry<LevelInferenceBinding, Integer> entry : solution.entrySet()) {
        Integer constant = entry.getValue();
        result.levelSubst.add(entry.getKey(), constant == null ? Level.INFINITY : new Level(myBases.get(entry.getKey()), -constant));
      }
    }

    return result;
  }

  private void update(InferenceBinding binding, Expression expr, ExprSubstitution subst) {
    if (expr.findBinding(binding)) {
      binding.reportErrorInfer(myErrorReporter, expr);
      return;
    }

    Expression expectedType = binding.getType().subst(subst);
    Type actualType = expr.getType().subst(subst, new LevelSubstitution());
    if (!actualType.isLessOrEquals(expectedType.normalize(NormalizeVisitor.Mode.NF), this, binding.getSourceNode())) {
      if (actualType instanceof Expression) {
        actualType = ((Expression) actualType).normalize(NormalizeVisitor.Mode.HUMAN_NF);
      }
      binding.reportErrorMismatch(myErrorReporter, expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), actualType, expr);
    }
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
