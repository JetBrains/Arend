package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.DerivedInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ElimBindingVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SimpleLevelSubstitution;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class TwoStageEquations implements Equations {
  private final List<Equation> myEquations;
  private final LevelEquations<InferenceLevelVariable> myPLevelEquations;      // equations of the forms      c <= ?y and ?x <= max(?y + c', d)
  private final LevelEquations<InferenceLevelVariable> myBasedPLevelEquations; // equations of the forms lp + c <= ?y and ?x <= max(?y + c', d)
  private final LevelEquations<InferenceLevelVariable> myHLevelEquations;
  private final LevelEquations<InferenceLevelVariable> myBasedHLevelEquations;
  private final CheckTypeVisitor myVisitor;
  private final Stack<InferenceVariable> myProps;
  private final List<Pair<InferenceLevelVariable, InferenceLevelVariable>> myBoundVariables;
  private final Map<InferenceLevelVariable, Set<LevelVariable>> myLowerBounds;
  private final Map<InferenceLevelVariable, Level> myConstantUpperBounds;

  public TwoStageEquations(CheckTypeVisitor visitor) {
    myEquations = new ArrayList<>();
    myPLevelEquations = new LevelEquations<>();
    myBasedPLevelEquations = new LevelEquations<>();
    myHLevelEquations = new LevelEquations<>();
    myBasedHLevelEquations = new LevelEquations<>();
    myBoundVariables = new ArrayList<>();
    myLowerBounds = new HashMap<>();
    myConstantUpperBounds = new HashMap<>();
    myProps = new Stack<>();
    myVisitor = visitor;
  }

  private Expression getInstance(InferenceVariable variable, FieldCallExpression fieldCall, Expression expr) {
    if (variable instanceof TypeClassInferenceVariable) {
      ClassDefinition classDef = (ClassDefinition) myVisitor.getTypecheckingState().getTypechecked(((TypeClassInferenceVariable) variable).getClassReferable());
      if (classDef.getClassifyingField() == fieldCall.getDefinition()) {
        Expression stuck2 = expr.getStuckExpression();
        if (stuck2 == null || !stuck2.isInstance(InferenceReferenceExpression.class) || stuck2.cast(InferenceReferenceExpression.class).getVariable() == null) {
          return ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getInstancePool(), expr);
        }
      }
    }
    return null;
  }

  private void addEquation(Expression expr1, Expression expr2, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar) {
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
      if (fieldCall1 != null && fieldCall1.getArgument().isInstance(InferenceReferenceExpression.class)) {
        variable = fieldCall1.getArgument().cast(InferenceReferenceExpression.class).getVariable();
        // expr1 == view field call
        result = getInstance(variable, fieldCall1, expr2);
      }

      // expr2 == field call
      FieldCallExpression fieldCall2 = expr2.checkedCast(FieldCallExpression.class);
      if (variable == null && fieldCall2 != null && fieldCall2.getArgument().isInstance(InferenceReferenceExpression.class)) {
        variable = fieldCall2.getArgument().cast(InferenceReferenceExpression.class).getVariable();
        // expr2 == view field call
        result = getInstance(variable, fieldCall2, expr1);
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
          myProps.push(cInf);
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

        try (Utils.SetContextSaver ignore = new Utils.SetContextSaver<>(myVisitor.getFreeBindings())) {
          for (SingleDependentLink link = pi.getParameters(); link.hasNext(); link = link.getNext()) {
            myVisitor.getFreeBindings().add(link);
          }
          InferenceVariable infVar = new DerivedInferenceVariable(cInf.getName() + "-cod", cInf, new UniverseExpression(codSort), myVisitor.getAllBindings());
          Expression newRef = new InferenceReferenceExpression(infVar, this);
          solve(cInf, new PiExpression(piSort, pi.getParameters(), newRef));
          addEquation(pi.getCodomain(), newRef, cmp, sourceNode, infVar);
          return;
        }
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

    Equation equation = new Equation(expr1, expr2, cmp, sourceNode);
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

  private void addLevelEquation(final LevelVariable var1, LevelVariable var2, int constant, int maxConstant, Concrete.SourceNode sourceNode) {
    // _ <= max(-c, -d), _ <= max(l - c, -d) // 6
    if (constant < 0 && maxConstant < 0 && !(var2 instanceof InferenceLevelVariable)) {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant, maxConstant)), sourceNode));
      return;
    }

    // 0 <= max(_ +-c, +-d) // 10
    if (var1 == null) {
      // 0 <= max(?y - c, -d) // 1
      if (constant < 0 && maxConstant < 0) {
        addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, constant), false);
      }
      return;
    }

    if (var2 instanceof InferenceLevelVariable && var1 != var2) {
      myLowerBounds.computeIfAbsent((InferenceLevelVariable) var2, k -> new HashSet<>()).add(var1);
    }

    // ?x <= max(_ +- c, +-d) // 10
    if (var1 instanceof InferenceLevelVariable) {
      // ?x <= max(?y +- c, +-d) // 4
      if (var2 instanceof InferenceLevelVariable) {
        LevelEquation<InferenceLevelVariable> equation = new LevelEquation<>((InferenceLevelVariable) var1, (InferenceLevelVariable) var2, constant, maxConstant < 0 ? null : maxConstant);
        addEquation(equation, false);
        addEquation(equation, true);
      } else {
        // ?x <= max(+-c, +-d), ?x <= max(l +- c, +-d) // 6
        Level oldLevel = myConstantUpperBounds.get(var1);
        if (oldLevel == null) {
          myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(var2, constant, maxConstant >= constant ? maxConstant - constant : 0));
        } else {
          if (var2 == null && oldLevel.getVar() != null) {
            myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(null, Math.max(constant, maxConstant)));
          } else
          if (var2 != null && oldLevel.getVar() == null) {
            myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(null, Math.max(oldLevel.getConstant(), oldLevel.getMaxConstant())));
          } else {
            int newConst = Math.min(constant, oldLevel.getConstant());
            int newMaxConst = Math.min(maxConstant, oldLevel.getMaxConstant());
            myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(var2, newConst, newMaxConst >= newConst ? newMaxConst - newConst : 0));
          }
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

  private void addLevelEquation(LevelVariable var, Concrete.SourceNode sourceNode) {
    if (var instanceof InferenceLevelVariable) {
      //noinspection unchecked
      addEquation(new LevelEquation<>((InferenceLevelVariable) var), false);
    } else {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var)), sourceNode));
    }
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar) {
    addEquation(expr1, expr2, cmp, sourceNode, stuckVar);
    return true;
  }

  @Override
  public boolean solve(Expression type, Expression expr, CMP cmp, Concrete.SourceNode sourceNode) {
    if (!CompareVisitor.compare(this, cmp, type, expr, sourceNode)) {
      myVisitor.getErrorReporter().report(new SolveEquationError(type, expr, sourceNode));
      return false;
    } else {
      return true;
    }
  }

  @Override
  public boolean add(Level level1, Level level2, CMP cmp, Concrete.SourceNode sourceNode) {
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
      /*
      if (level1.getVar() != null || level1.getConstant() != 0) {
        addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxAddedConstant(), level2.getMaxAddedConstant() - level1.getMaxAddedConstant(), sourceNode);
      }
      */
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), level1.getMaxAddedConstant() - level2.getConstant(), sourceNode);
      /*
      if (level2.getVar() != null || level2.getConstant() != 0) {
        addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxAddedConstant(), level1.getMaxAddedConstant() - level2.getMaxAddedConstant(), sourceNode);
      }
      */
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

  private void reportCycle(List<LevelEquation<InferenceLevelVariable>> cycle, Set<InferenceLevelVariable> unBased) {
    Set<LevelEquation<? extends LevelVariable>> basedCycle = new LinkedHashSet<>();
    for (LevelEquation<InferenceLevelVariable> equation : cycle) {
      if (equation.isInfinity() || equation.getVariable1() != null) {
        basedCycle.add(new LevelEquation<>(equation));
      } else {
        basedCycle.add(new LevelEquation<>(equation.getVariable2() == null || unBased.contains(equation.getVariable2()) ? null : equation.getVariable2().getStd(), equation.getVariable2(), equation.getConstant()));
      }
    }
    LevelEquation<InferenceLevelVariable> lastEquation = cycle.get(cycle.size() - 1);
    InferenceLevelVariable var = lastEquation.getVariable1() != null ? lastEquation.getVariable1() : lastEquation.getVariable2();
    myVisitor.getErrorReporter().report(new SolveLevelEquationsError(basedCycle, var.getSourceNode()));
  }

  private void calculateUnBased(LevelEquations<InferenceLevelVariable> basedEquations, Set<InferenceLevelVariable> unBased, Map<InferenceLevelVariable, Integer> basedSolution) {
    if (myConstantUpperBounds.isEmpty()) {
      return;
    }

    LevelVariable errorLowerBound = null;
    InferenceLevelVariable errorVar = null;

    for (InferenceLevelVariable var : basedEquations.getVariables()) {
      Level ub = myConstantUpperBounds.get(var);
      if (ub != null && (ub.getVar() == null || ub.getConstant() < basedSolution.get(var))) {
        unBased.add(var);
      }
    }

    if (!unBased.isEmpty()) {
      Stack<InferenceLevelVariable> stack = new Stack<>();
      for (InferenceLevelVariable var : unBased) {
        stack.push(var);
      }

      while (!stack.isEmpty()) {
        InferenceLevelVariable var = stack.pop();
        Set<LevelVariable> lowerBounds = myLowerBounds.get(var);
        if (lowerBounds != null) {
          for (LevelVariable lowerBound : lowerBounds) {
            if (lowerBound instanceof InferenceLevelVariable) {
              if (unBased.add((InferenceLevelVariable) lowerBound)) {
                stack.push((InferenceLevelVariable) lowerBound);
              }
            } else if (errorLowerBound == null || errorVar == null) {
              errorLowerBound = lowerBound;
              errorVar = var;
            }
          }
        }
      }
    }

    if (errorLowerBound != null && errorVar != null) {
      myVisitor.getErrorReporter().report(new ConstantSolveLevelEquationError(errorLowerBound, errorVar.getSourceNode()));
    }
  }

  @Override
  public LevelSubstitution solve(Concrete.SourceNode sourceNode) {
    solveClassCalls();

    while (!myProps.isEmpty()) {
      InferenceVariable var = myProps.pop();
      if (!var.isSolved()) {
        var.solve(this, new UniverseExpression(Sort.PROP));
      }
    }

    Map<InferenceLevelVariable, Integer> basedSolution = new HashMap<>();
    List<LevelEquation<InferenceLevelVariable>> cycle = myBasedHLevelEquations.solve(basedSolution);

    Set<InferenceLevelVariable> unBased = new HashSet<>();
    calculateUnBased(myBasedHLevelEquations, unBased, basedSolution);

    boolean ok = cycle == null;
    if (!ok) {
      reportCycle(cycle, unBased);
    }

    Map<InferenceLevelVariable, Integer> solution = new HashMap<>();
    cycle = myHLevelEquations.solve(solution);
    if (ok && cycle != null) {
      reportCycle(cycle, unBased);
    }

    if (!unBased.isEmpty()) {
      for (Pair<InferenceLevelVariable, InferenceLevelVariable> vars : myBoundVariables) {
        if (unBased.contains(vars.proj2)) {
          Integer sol = solution.get(vars.proj2);
          if (sol != null && sol == 0) {
            myPLevelEquations.getEquations().removeIf(equation -> !equation.isInfinity() && (equation.getVariable1() == vars.proj1 || equation.getVariable2() == vars.proj1));
            myBasedPLevelEquations.getEquations().removeIf(equation -> !equation.isInfinity() && (equation.getVariable1() == vars.proj1 || equation.getVariable2() == vars.proj1));
            myConstantUpperBounds.remove(vars.proj1);
          }
        }
      }
    }

    cycle = myBasedPLevelEquations.solve(basedSolution);
    Set<InferenceLevelVariable> pUnBased = new HashSet<>();
    calculateUnBased(myBasedPLevelEquations, pUnBased, basedSolution);
    ok = cycle == null;
    if (!ok) {
      reportCycle(cycle, pUnBased);
    }
    cycle = myPLevelEquations.solve(solution);
    if (ok && cycle != null) {
      reportCycle(cycle, pUnBased);
    }
    unBased.addAll(pUnBased);

    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    for (InferenceLevelVariable var : unBased) {
      Integer sol = solution.get(var);
      assert sol != null || var.getType() == LevelVariable.LvlType.HLVL;
      result.add(var, sol == null ? Level.INFINITY : new Level(-sol));
    }
    for (Map.Entry<InferenceLevelVariable, Integer> entry : basedSolution.entrySet()) {
      assert entry.getValue() != null || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
      if (!unBased.contains(entry.getKey())) {
        Integer sol = solution.get(entry.getKey());
        assert sol != null || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
        result.add(entry.getKey(), sol == null || entry.getValue() == null ? Level.INFINITY : new Level(entry.getKey().getStd(), -entry.getValue(), -sol <= -entry.getValue() ? 0 : -sol - (-entry.getValue())));
      }
    }

    for (Map.Entry<InferenceLevelVariable, Level> entry : myConstantUpperBounds.entrySet()) {
      int constant = entry.getValue().getConstant();
      Level level = result.get(entry.getKey());
      if (!Level.compare(level, new Level(entry.getValue().getVar(), constant, entry.getValue().getMaxConstant()), CMP.LE, DummyEquations.getInstance(), null)) {
        int maxConstant = entry.getValue().getMaxAddedConstant();
        List<LevelEquation<LevelVariable>> equations = new ArrayList<>(2);
        equations.add(level.isInfinity() ? new LevelEquation<>(entry.getKey()) : level.getMaxAddedConstant() <= constant || level.getMaxAddedConstant() <= maxConstant ? new LevelEquation<>(level.getVar(), entry.getKey(), -level.getConstant()) : new LevelEquation<>(null, entry.getKey(), -level.getMaxAddedConstant()));
        equations.add(new LevelEquation<>(entry.getKey(), entry.getValue().getVar(), constant, maxConstant));
        myVisitor.getErrorReporter().report(new SolveLevelEquationsError(equations, entry.getKey().getSourceNode()));
      }
    }

    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      Expression stuckExpr = equation.expr.getStuckExpression();
      if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isError())) {
        iterator.remove();
      } else {
        stuckExpr = equation.type.getStuckExpression();
        if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isError())) {
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
    myLowerBounds.clear();
    myConstantUpperBounds.clear();
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
    if (myProps.contains(var) && !expr.isInstance(UniverseExpression.class)) {
      LocalError error = var.getErrorInfer(new UniverseExpression(Sort.PROP), expr);
      myVisitor.getErrorReporter().report(error);
      return false;
    }

    if (expr.findBinding(var)) {
      LocalError error = var.getErrorInfer(expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(null, error));
      return false;
    }

    Expression expectedType = var.getType();
    Expression actualType = expr.getType();
    if (actualType == null || actualType.isLessOrEquals(expectedType, this, var.getSourceNode())) {
      Expression result = actualType == null ? null : ElimBindingVisitor.findBindings(expr, var.getBounds());
      if (result != null) {
        var.solve(this, OfTypeExpression.make(result, actualType, expectedType));
        return true;
      } else {
        LocalError error = var.getErrorInfer(expr);
        myVisitor.getErrorReporter().report(error);
        var.solve(this, new ErrorExpression(null, error));
        return false;
      }
    } else {
      LocalError error = var.getErrorMismatch(expectedType, actualType, expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(actualType, error));
      return false;
    }
  }
}
