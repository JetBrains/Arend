package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.DerivedInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SimpleLevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.SolveEquationError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.SolveEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.SolveLevelEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class TwoStageEquations implements Equations {
  private final List<Equation> myEquations;
  private final LevelEquations<InferenceLevelVariable> myPLevelEquations;
  private final LevelEquations<InferenceLevelVariable> myBasedPLevelEquations;
  private final LevelEquations<InferenceLevelVariable> myHLevelEquations;
  private final LevelEquations<InferenceLevelVariable> myBasedHLevelEquations;
  private final CheckTypeVisitor myVisitor;
  private final List<InferenceVariable> myProps;
  private final List<Pair<InferenceLevelVariable, InferenceLevelVariable>> myBoundVariables;
  private final Map<LevelVariable, Set<InferenceLevelVariable>> myDependencyGraph;

  public TwoStageEquations(CheckTypeVisitor visitor) {
    myEquations = new ArrayList<>();
    myPLevelEquations = new LevelEquations<>();
    myBasedPLevelEquations = new LevelEquations<>();
    myHLevelEquations = new LevelEquations<>();
    myBasedHLevelEquations = new LevelEquations<>();
    myBoundVariables = new ArrayList<>();
    myDependencyGraph = new HashMap<>();
    myProps = new ArrayList<>();
    myVisitor = visitor;
  }

  private void addEquation(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    InferenceVariable inf1 = expr1.isInstance(InferenceReferenceExpression.class) ? expr1.cast(InferenceReferenceExpression.class).getVariable() : null;
    InferenceVariable inf2 = expr2.isInstance(InferenceReferenceExpression.class) ? expr2.cast(InferenceReferenceExpression.class).getVariable() : null;

    // expr1 == expr2 == ?x
    if (inf1 == inf2 && inf1 != null) {
      return;
    }

    if (inf1 == null && inf2 == null) {
      InferenceVariable variable = null;
      Expression result = null;

      // expr1 == field call
      FieldCallExpression fieldCall1 = expr1.checkedCast(FieldCallExpression.class);
      if (fieldCall1 != null && fieldCall1.getExpression().isInstance(InferenceReferenceExpression.class)) {
        variable = fieldCall1.getExpression().cast(InferenceReferenceExpression.class).getVariable();
        // expr1 == view field call
        if (variable instanceof TypeClassInferenceVariable && ((TypeClassInferenceVariable) variable).getClassifyingField() == fieldCall1.getDefinition()) {
          Expression stuck2 = expr2.getStuckExpression();
          if (stuck2 == null || !stuck2.isInstance(InferenceReferenceExpression.class) || stuck2.cast(InferenceReferenceExpression.class).getVariable() == null) {
            result = ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getClassViewInstancePool(), expr2);
          }
        }
      }

      // expr2 == field call
      FieldCallExpression fieldCall2 = expr2.checkedCast(FieldCallExpression.class);
      if (variable == null && fieldCall2 != null && fieldCall2.getExpression().isInstance(InferenceReferenceExpression.class)) {
        variable = fieldCall2.getExpression().cast(InferenceReferenceExpression.class).getVariable();
        // expr2 == view field call
        if (variable instanceof TypeClassInferenceVariable && ((TypeClassInferenceVariable) variable).getClassifyingField() == fieldCall2.getDefinition()) {
          Expression stuck1 = expr1.getStuckExpression();
          if (stuck1 == null || !stuck1.isInstance(InferenceReferenceExpression.class) || stuck1.cast(InferenceReferenceExpression.class).getVariable() == null) {
            result = ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getClassViewInstancePool(), expr1);
          }
        }
      }

      if (result != null) {
        solve(variable, result);
        return;
      }
    }

    // expr1 == ?x && expr2 /= ?y || expr1 /= ?x && expr2 == ?y
    if (inf1 != null && inf2 == null || inf2 != null && inf1 == null) {
      InferenceVariable cInf = inf1 != null ? inf1 : inf2;
      Expression cType = inf1 != null ? expr2 : expr1;
      cType = cType.normalize(NormalizeVisitor.Mode.WHNF);

      // cType /= Pi, cType /= Type, cType /= Class, cType /= stuck on ?X
      if (!cType.isInstance(PiExpression.class) && !cType.isInstance(UniverseExpression.class) && !cType.isInstance(ClassCallExpression.class)) {
        Expression stuck = cType.getStuckExpression();
        if (stuck == null || !stuck.isInstance(InferenceReferenceExpression.class)) {
          cmp = CMP.EQ;
        }
      }

      if (inf1 != null) {
        cmp = cmp.not();
      }

      if (cType.isInstance(UniverseExpression.class) && cType.cast(UniverseExpression.class).getSort().isProp()) {
        if (cmp == CMP.LE) {
          myProps.add(cInf);
          return;
        } else {
          cmp = CMP.EQ;
        }
      }

      // ?x == _
      if (cmp == CMP.EQ) {
        solve(cInf, cType);
        return;
      }

      // ?x <> Pi
      if (cType.isInstance(PiExpression.class)) {
        PiExpression pi = cType.cast(PiExpression.class);
        Sort domSort = pi.getParameters().getType().getSortOfType();
        Sort codSort = Sort.generateInferVars(this, sourceNode);
        Sort piSort = PiExpression.generateUpperBound(domSort, codSort, this, sourceNode);

        InferenceVariable infVar = new DerivedInferenceVariable(cInf.getName() + "-cod", cInf, new UniverseExpression(codSort));
        Expression newRef = new InferenceReferenceExpression(infVar, this);
        solve(cInf, new PiExpression(piSort, pi.getParameters(), newRef));
        addEquation(pi.getCodomain(), newRef, cmp, sourceNode, infVar);
        return;
      }

      // ?x <> Type
      Sort sort = cType.toSort();
      if (sort != null) {
        Sort genSort = Sort.generateInferVars(this, cInf.getSourceNode());
        solve(cInf, new UniverseExpression(genSort));
        if (cmp == CMP.LE) {
          Sort.compare(sort, genSort, CMP.LE, this, sourceNode);
        } else {
          if (!sort.getPLevel().isInfinity()) {
            addLevelEquation(genSort.getPLevel().getVar(), sort.getPLevel().getVar(), sort.getPLevel().getConstant(), sort.getPLevel().getMaxAddedConstant(), sourceNode);
          }
          if (!sort.getHLevel().isInfinity()) {
            addLevelEquation(genSort.getHLevel().getVar(), sort.getHLevel().getVar(), sort.getHLevel().getConstant(), sort.getHLevel().getMaxAddedConstant(), sourceNode);
          }
        }
        return;
      }
    }

    Equation equation;
    if (!expr2.isInstance(InferenceReferenceExpression.class) && expr1.isInstance(InferenceReferenceExpression.class)) {
      equation = new Equation(expr2, expr1, cmp.not(), sourceNode);
    } else {
      equation = new Equation(expr1, expr2, cmp, sourceNode);
    }

    myEquations.add(equation);
    if (expr1.isInstance(InferenceReferenceExpression.class) && expr2.isInstance(InferenceReferenceExpression.class)) {
      expr1.cast(InferenceReferenceExpression.class).getVariable().addListener(equation);
      expr2.cast(InferenceReferenceExpression.class).getVariable().addListener(equation);
    } else {
      stuckVar.addListener(equation);
    }
  }

  @Override
  public void bindVariables(InferenceLevelVariable pVar, InferenceLevelVariable hVar) {
    assert pVar.getType() == LevelVariable.LvlType.PLVL;
    assert hVar.getType() == LevelVariable.LvlType.HLVL;
    myBoundVariables.add(new Pair<>(pVar, hVar));
  }

  private void addEquation(LevelEquation<InferenceLevelVariable> equation, boolean based) {
    InferenceLevelVariable var1 = equation.isInfinity() ? equation.getVariable() : equation.getVariable1();
    InferenceLevelVariable var2 = equation.isInfinity() ? equation.getVariable() : equation.getVariable2();
    assert var1 == null || var2 == null || var1.getType() == var2.getType();

    if (var1 != null && var1.getType() == LevelVariable.LvlType.PLVL || var2 != null && var2.getType() == LevelVariable.LvlType.PLVL) {
      if (based) {
        myBasedPLevelEquations.addEquation(equation);
      } else {
        myPLevelEquations.addEquation(equation);
      }
    } else
    if (var1 != null && var1.getType() == LevelVariable.LvlType.HLVL || var2 != null && var2.getType() == LevelVariable.LvlType.HLVL) {
      if (based) {
        myBasedHLevelEquations.addEquation(equation);
      } else {
        myHLevelEquations.addEquation(equation);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  private void addLevelEquation(LevelVariable var1, LevelVariable var2, int constant, int maxConstant, Abstract.SourceNode sourceNode) {
    // _ <= max(-c, -d), _ <= max(l - c, -d) // 6
    if (constant < 0 && maxConstant < 0 && !(var2 instanceof InferenceLevelVariable)) {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant, maxConstant)), sourceNode));
      return;
    }

    // 0 <= max(_ +-c, +-d) // 10
    if (var1 == null) {
      // 0 <= max(?y - c, -d) // 1
      if (constant < 0 && maxConstant < 0) {
        addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, constant, maxConstant), false);
      }
      return;
    }

    if (var2 instanceof InferenceLevelVariable && var1 != var2) {
      myDependencyGraph.computeIfAbsent(var1, k -> new HashSet<>()).add((InferenceLevelVariable) var2);
    }

    // ?x <= max(_ +- c, +-d) // 10
    if (var1 instanceof InferenceLevelVariable) {
      if (var2 instanceof InferenceLevelVariable) {
        // ?x <= max(?y +- c, +-d) // 4
        LevelEquation<InferenceLevelVariable> equation = new LevelEquation<>((InferenceLevelVariable) var1, (InferenceLevelVariable) var2, constant, maxConstant < 0 ? null : maxConstant);
        addEquation(equation, false);
        addEquation(equation, true);
      } else {
        // ?x <= max(+-c, +-d), ?x <= max(l +- c, +-d) // 6
        addEquation(new LevelEquation<>((InferenceLevelVariable) var1, null, Math.max(constant, maxConstant)), false);
        if (var2 != null) {
          addEquation(new LevelEquation<>((InferenceLevelVariable) var1, null, constant), true);
        }
      }
      return;
    }

    // l <= max(_ +- c, +-d) // 10
    {
      // l <= max(l + c, +-d) // 2
      if (var1 == var2 && constant >= 0) {
        return;
      }

      // l <= max(?y +- c, +-d) // 4
      if (var2 instanceof InferenceLevelVariable) {
        if (constant < 0) {
          addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, constant), true);
        }
        return;
      }

      // l <= max(l - c, +d), l <= max(+-c, +-d) // 4
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant, maxConstant)), sourceNode));
    }
  }

  private void addLevelEquation(LevelVariable var, Abstract.SourceNode sourceNode) {
    if (var instanceof InferenceLevelVariable) {
      addEquation(new LevelEquation<>((InferenceLevelVariable) var), false);
    } else {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var)), sourceNode));
    }
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    addEquation(expr1, expr2, cmp, sourceNode, stuckVar);
    return true;
  }

  @Override
  public boolean solve(Expression type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode) {
    if (!CompareVisitor.compare(this, cmp, type, expr, sourceNode)) {
      myVisitor.getErrorReporter().report(new SolveEquationError(type, expr, sourceNode));
      return false;
    } else {
      return true;
    }
  }

  @Override
  public boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode) {
    if (level1.isInfinity() && level2.isInfinity() || level1.isInfinity() && cmp == CMP.GE || level2.isInfinity() && cmp == CMP.LE) {
      return true;
    }
    if (level1.isInfinity()) {
      addLevelEquation(level2.getVar(), sourceNode);
      return true;
    }
    if (level2.isInfinity()) {
      addLevelEquation(level1.getVar(), sourceNode);
      return true;
    }

    if (cmp == CMP.LE || cmp == CMP.EQ) {
      addLevelEquation(level1.getVar(), level2.getVar(), level2.getConstant() - level1.getConstant(), level2.getMaxAddedConstant() - level1.getConstant(), sourceNode);
      addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxAddedConstant(), level2.getMaxAddedConstant() - level1.getMaxAddedConstant(), sourceNode);
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), level1.getMaxAddedConstant() - level2.getConstant(), sourceNode);
      addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxAddedConstant(), level1.getMaxAddedConstant() - level2.getMaxAddedConstant(), sourceNode);
    }
    return true;
  }

  @Override
  public boolean addVariable(InferenceLevelVariable var) {
    if (var.getType() == LevelVariable.LvlType.PLVL) {
      myPLevelEquations.addVariable(var);
      myBasedPLevelEquations.addVariable(var);
    } else {
      myHLevelEquations.addVariable(var);
      myBasedHLevelEquations.addVariable(var);
    }
    return true;
  }

  @Override
  public void remove(Equation equation) {
    myEquations.remove(equation);
  }

  private void reportCycle(List<LevelEquation<InferenceLevelVariable>> cycle, Set<InferenceLevelVariable> based) {
    List<LevelEquation<LevelVariable>> basedCycle = new ArrayList<>();
    for (LevelEquation<InferenceLevelVariable> equation : cycle) {
      if (equation.isInfinity() || equation.getVariable1() != null) {
        basedCycle.add(new LevelEquation<>(equation));
      } else {
        basedCycle.add(new LevelEquation<>(equation.getVariable2() != null && based.contains(equation.getVariable2()) ? equation.getVariable2().getStd() : null, equation.getVariable2(), equation.getConstant()));
      }
    }
    LevelEquation<InferenceLevelVariable> lastEquation = cycle.get(cycle.size() - 1);
    InferenceLevelVariable var = lastEquation.getVariable1() != null ? lastEquation.getVariable1() : lastEquation.getVariable2();
    myVisitor.getErrorReporter().report(new SolveLevelEquationsError(new ArrayList<LevelEquation<? extends LevelVariable>>(basedCycle), var.getSourceNode()));
  }

  @Override
  public LevelSubstitution solve(Abstract.SourceNode sourceNode) {
    solveClassCalls();

    for (InferenceVariable var : myProps) {
      if (!var.isSolved()) {
        var.solve(this, new UniverseExpression(Sort.PROP));
      }
    }

    Set<InferenceLevelVariable> based = new HashSet<>();
    {
      Stack<LevelVariable> stack = new Stack<>();
      stack.push(LevelVariable.PVAR);
      stack.push(LevelVariable.HVAR);

      while (!stack.isEmpty()) {
        Set<InferenceLevelVariable> dependencies = myDependencyGraph.get(stack.pop());
        if (dependencies != null) {
          for (InferenceLevelVariable dependency : dependencies) {
            if (based.add(dependency)) {
              stack.push(dependency);
            }
          }
        }
      }
    }

    Map<InferenceLevelVariable, Integer> solution = new HashMap<>();
    Map<InferenceLevelVariable, Integer> basedSolution = new HashMap<>();
    List<LevelEquation<InferenceLevelVariable>> cycle = myHLevelEquations.solve(solution);
    boolean ok = cycle == null;
    if (!ok) {
      reportCycle(cycle, based);
    }
    cycle = myBasedHLevelEquations.solve(basedSolution);
    if (ok && cycle != null) {
      reportCycle(cycle, based);
    }

    for (Pair<InferenceLevelVariable, InferenceLevelVariable> vars : myBoundVariables) {
      Integer sol = solution.get(vars.proj2);
      if (sol != null && sol == 0) {
        myPLevelEquations.getEquations().removeIf(equation -> !equation.isInfinity() && (equation.getVariable1() == vars.proj1 || equation.getVariable2() == vars.proj1));
        myBasedPLevelEquations.getEquations().removeIf(equation -> !equation.isInfinity() && (equation.getVariable1() == vars.proj1 || equation.getVariable2() == vars.proj1));
      }
    }

    cycle = myPLevelEquations.solve(solution);
    ok = cycle == null;
    if (!ok) {
      reportCycle(cycle, based);
    }
    cycle = myBasedPLevelEquations.solve(basedSolution);
    if (ok && cycle != null) {
      reportCycle(cycle, based);
    }

    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    for (Map.Entry<InferenceLevelVariable, Integer> entry : solution.entrySet()) {
      Integer constant = entry.getValue();
      assert constant != null || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
      Integer basedConstant = basedSolution.get(entry.getKey());
      assert basedConstant != null || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
      result.add(entry.getKey(), constant == null || basedConstant == null ? Level.INFINITY : new Level(based.contains(entry.getKey()) ? entry.getKey().getStd() : null, -basedConstant, -constant));
    }

    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      Expression stuckExpr = equation.expr.getStuckExpression();
      if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isInstance(ErrorExpression.class))) {
        iterator.remove();
      } else {
        stuckExpr = equation.type.getStuckExpression();
        if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isInstance(ErrorExpression.class))) {
          iterator.remove();
        }
      }
    }
    if (!myEquations.isEmpty()) {
      myVisitor.getErrorReporter().report(new SolveEquationsError(new ArrayList<>(myEquations), sourceNode));
    }

    myEquations.clear();
    myPLevelEquations.clear();
    myHLevelEquations.clear();
    myBasedPLevelEquations.clear();
    myBasedHLevelEquations.clear();
    myBoundVariables.clear();
    myDependencyGraph.clear();
    myProps.clear();
    return result;
  }

  private void solveClassCalls() {
    List<Equation> lowerBounds = new ArrayList<>(myEquations.size());
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.expr.isInstance(InferenceReferenceExpression.class) && equation.expr.cast(InferenceReferenceExpression.class).getSubstExpression() == null) {
        Expression type = equation.type;
        if (type.isInstance(InferenceReferenceExpression.class) && type.cast(InferenceReferenceExpression.class).getSubstExpression() == null || type.isInstance(ClassCallExpression.class) && equation.cmp != CMP.GE) {
          if (equation.cmp == CMP.LE) {
            lowerBounds.add(equation);
          } else if (equation.cmp == CMP.GE) {
            lowerBounds.add(new Equation(equation.expr, type, CMP.LE, equation.sourceNode));
          } else {
            lowerBounds.add(new Equation(equation.type, equation.expr, CMP.LE, equation.sourceNode));
            lowerBounds.add(new Equation(equation.expr, type, CMP.LE, equation.sourceNode));
          }
          iterator.remove();
        }
      }
    }

    solveClassCallLowerBounds(lowerBounds);

    Map<InferenceVariable, Expression> result = new HashMap<>();
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.expr.isInstance(InferenceReferenceExpression.class) && equation.expr.cast(InferenceReferenceExpression.class).getSubstExpression() == null) {
        Expression newResult = equation.type;
        if (newResult.isInstance(InferenceReferenceExpression.class) && newResult.cast(InferenceReferenceExpression.class).getSubstExpression() == null || newResult.isInstance(ClassCallExpression.class) && equation.cmp == CMP.GE) {
          InferenceVariable var = equation.expr.cast(InferenceReferenceExpression.class).getVariable();
          Expression oldResult = result.get(var);
          if (oldResult == null || newResult.isLessOrEquals(oldResult, DummyEquations.getInstance(), var.getSourceNode())) {
            result.put(var, newResult);
          } else
          if (!oldResult.isLessOrEquals(newResult, DummyEquations.getInstance(), var.getSourceNode())) {
            List<Equation> eqs = new ArrayList<>(2);
            eqs.add(new Equation(equation.expr, oldResult, CMP.LE, var.getSourceNode()));
            eqs.add(new Equation(equation.expr, newResult, CMP.LE, var.getSourceNode()));
            myVisitor.getErrorReporter().report(new SolveEquationsError(eqs, var.getSourceNode()));
          }
          iterator.remove();
        }
      }
    }

    for (Map.Entry<InferenceVariable, Expression> entry : result.entrySet()) {
      solve(entry.getKey(), entry.getValue());
    }
  }

  private void solveClassCallLowerBounds(List<Equation> lowerBounds) {
    Map<InferenceVariable, Expression> solutions = new HashMap<>();
    while (true) {
      boolean updated = false;
      for (Equation equation : lowerBounds) {
        Expression newSolution = equation.type;
        if (newSolution.isInstance(InferenceReferenceExpression.class) && newSolution.cast(InferenceReferenceExpression.class).getSubstExpression() == null) {
          newSolution = solutions.get(newSolution.cast(InferenceReferenceExpression.class).getVariable());
        }
        if (newSolution != null) {
          InferenceVariable var = equation.expr.cast(InferenceReferenceExpression.class).getVariable();
          Expression oldSolution = solutions.get(var);
          if (oldSolution == null) {
            solutions.put(var, newSolution);
            updated = true;
          } else {
            if (!newSolution.isLessOrEquals(oldSolution, DummyEquations.getInstance(), var.getSourceNode())) {
              if (oldSolution.isLessOrEquals(newSolution, DummyEquations.getInstance(), var.getSourceNode())) {
                solutions.put(var, newSolution);
                updated = true;
              } else {
                List<Equation> eqs = new ArrayList<>(2);
                eqs.add(new Equation(oldSolution, equation.expr, CMP.LE, var.getSourceNode()));
                eqs.add(new Equation(newSolution, equation.expr, CMP.LE, var.getSourceNode()));
                myVisitor.getErrorReporter().report(new SolveEquationsError(eqs, var.getSourceNode()));
              }
            }
          }
        }
      }
      if (!updated) {
        break;
      }
    }

    for (Map.Entry<InferenceVariable, Expression> entry : solutions.entrySet()) {
      solve(entry.getKey(), entry.getValue());
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private boolean solve(InferenceVariable var, Expression expr) {
    expr = expr.normalize(NormalizeVisitor.Mode.WHNF);
    if (expr.isInstance(InferenceReferenceExpression.class) && expr.cast(InferenceReferenceExpression.class).getVariable() == var) {
      return true;
    }
    if (expr.findBinding(var)) {
      LocalTypeCheckingError error = var.getErrorInfer(expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(null, error));
      return false;
    }

    Expression expectedType = var.getType();
    Expression actualType = expr.getType();
    if (actualType.isLessOrEquals(expectedType, this, var.getSourceNode())) {
      var.solve(this, OfTypeExpression.make(expr, actualType, expectedType));
      return true;
    } else {
      LocalTypeCheckingError error = var.getErrorMismatch(expectedType, actualType, expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(expr, error));
      return false;
    }
  }
}
