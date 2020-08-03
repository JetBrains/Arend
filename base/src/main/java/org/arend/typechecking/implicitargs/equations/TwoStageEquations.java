package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.Utils;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.DerivedInferenceVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.context.binding.inference.TypeClassInferenceVariable;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.ElimBindingVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SimpleLevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.LocalError;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ConstantSolveLevelEquationError;
import org.arend.typechecking.error.local.SolveEquationError;
import org.arend.typechecking.error.local.SolveEquationsError;
import org.arend.typechecking.error.local.SolveLevelEquationsError;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.SearchVisitor;
import org.arend.util.Pair;

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
      ClassDefinition classDef = ((TypeClassInferenceVariable) variable).getClassDefinition();
      if (classDef == null) {
        return null;
      }
      if (classDef.getClassifyingField() == fieldCall.getDefinition() && expr.getStuckInferenceVariable() == null) {
        return ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getInstancePool(), expr, null, variable.getSourceNode());
      }
    }
    return null;
  }

  @Override
  public boolean addEquation(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode, InferenceVariable stuckVar1, InferenceVariable stuckVar2) {
    InferenceVariable inf1 = expr1.getInferenceVariable();
    InferenceVariable inf2 = expr2.getInferenceVariable();

    // expr1 == expr2 == ?x
    if (inf1 == inf2 && inf1 != null) {
      return true;
    }

    if (inf1 == null && inf2 == null) {
      Expression result = null;

      // expr1 == field call
      FieldCallExpression fieldCall1 = expr1.getFunction().cast(FieldCallExpression.class);
      InferenceVariable variable = fieldCall1 == null ? null : fieldCall1.getArgument().getInferenceVariable();
      if (variable != null) {
        // expr1 == class field call
        result = getInstance(variable, fieldCall1, expr2);
      }

      // expr2 == field call
      if (variable == null) {
        FieldCallExpression fieldCall2 = expr2.getFunction().cast(FieldCallExpression.class);
        variable = fieldCall2 == null ? null : fieldCall2.getArgument().getInferenceVariable();
        if (variable != null) {
          // expr2 == class field call
          result = getInstance(variable, fieldCall2, expr1);
        }
      }

      if (result != null) {
        SolveResult solveResult = solve(variable, result.normalize(NormalizationMode.WHNF), false);
        return solveResult != SolveResult.SOLVED || CompareVisitor.compare(this, cmp, expr1, expr2, type, sourceNode);
      }
    }

    CMP origCmp = cmp;

    // expr1 == ?x && expr2 /= ?y || expr1 /= ?x && expr2 == ?y
    if (inf1 != null && inf2 == null || inf2 != null && inf1 == null) {
      InferenceVariable cInf = inf1 != null ? inf1 : inf2;
      Expression cType = (inf1 != null ? expr2 : expr1).normalize(NormalizationMode.WHNF);
      Expression cTypeExpr = cType.getUnderlyingExpression();

      // cType /= Pi, cType /= Type, cType /= Class, cType /= stuck on ?X
      if (!(cTypeExpr instanceof PiExpression) && !(cTypeExpr instanceof UniverseExpression) && !(cTypeExpr instanceof ClassCallExpression) && cTypeExpr.getStuckInferenceVariable() == null) {
        cmp = CMP.EQ;
      }

      if (inf1 != null) {
        cmp = cmp.not();
      }

      if (cTypeExpr instanceof UniverseExpression && ((UniverseExpression) cTypeExpr).getSort().isProp()) {
        if (cmp == CMP.LE) {
          myProps.push(cInf);
          return true;
        } else {
          cmp = CMP.EQ;
        }
      }

      // If cType is not pi, classCall, universe, or a stuck expression, then solve immediately.
      if (cmp != CMP.EQ) {
        Expression cod = cTypeExpr;
        while (cod instanceof PiExpression) {
          cod = ((PiExpression) cod).getCodomain().getUnderlyingExpression();
        }
        if (!(cod instanceof ClassCallExpression) && !(cod instanceof UniverseExpression) && cod.getStuckInferenceVariable() == null) {
          cmp = CMP.EQ;
        }
      }

      // ?x == _
      if (cmp == CMP.EQ) {
        InferenceReferenceExpression infRef = cTypeExpr instanceof FieldCallExpression ? ((FieldCallExpression) cTypeExpr).getArgument().cast(InferenceReferenceExpression.class) : null;
        if (infRef == null || !(infRef.getVariable() instanceof TypeClassInferenceVariable)) {
          if (solve(cInf, cType, false, cInf instanceof TypeClassInferenceVariable) != SolveResult.NOT_SOLVED) {
            return true;
          }
        }
      }

      // ?x <> Pi
      if (cTypeExpr instanceof PiExpression) {
        PiExpression pi = (PiExpression) cTypeExpr;
        Sort domSort = pi.getParameters().getType().getSortOfType();
        Sort codSort = Sort.generateInferVars(this, false, sourceNode);
        Sort piSort = PiExpression.generateUpperBound(domSort, codSort, this, sourceNode);

        try (var ignore = new Utils.SetContextSaver<>(myVisitor.getContext())) {
          for (SingleDependentLink link = pi.getParameters(); link.hasNext(); link = link.getNext()) {
            myVisitor.addBinding(null, link);
          }
          InferenceVariable infVar = new DerivedInferenceVariable(cInf.getName() + "-cod", cInf, new UniverseExpression(codSort), myVisitor.getAllBindings());
          Expression newRef = new InferenceReferenceExpression(infVar, this);
          solve(cInf, new PiExpression(piSort, pi.getParameters(), newRef), false);
          return addEquation(pi.getCodomain().normalize(NormalizationMode.WHNF), newRef, Type.OMEGA, cmp, sourceNode, pi.getCodomain().getStuckInferenceVariable(), infVar);
        }
      }

      // ?x <> Type
      if (cTypeExpr instanceof UniverseExpression) {
        Sort genSort = Sort.generateInferVars(this, true, cInf.getSourceNode());
        solve(cInf, new UniverseExpression(genSort), false);
        Sort sort = ((UniverseExpression) cTypeExpr).getSort();
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
        return true;
      }
    }

    Equation equation = new Equation(expr1, expr2, type, origCmp, sourceNode);
    myEquations.add(equation);
    if (inf1 != null && inf2 != null) {
      inf1.addListener(equation);
      inf2.addListener(equation);
    } else {
      if (stuckVar1 != null) {
        stuckVar1.addListener(equation);
      }
      if (stuckVar2 != null) {
        stuckVar2.addListener(equation);
      }
    }

    return true;
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
    if (!(var2 instanceof InferenceLevelVariable) && maxConstant < 0 && (constant < 0 || constant == 0 && var2 == LevelVariable.HVAR && var1 == null) && !(var2 == null && var1 instanceof InferenceLevelVariable && var1.getType() == LevelVariable.LvlType.HLVL && constant >= -1 && maxConstant >= -1)) {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant)), sourceNode));
      return;
    }

    // 0 <= max(_ +-c, +-d) // 10
    if (var1 == null) {
      // 0 <= max(?y - c, -d) // 1
      if (maxConstant < 0 && (constant < 0 || constant == 0 && var2 instanceof InferenceLevelVariable && var2.getType() == LevelVariable.LvlType.HLVL)) {
        addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, var2.getType() == LevelVariable.LvlType.PLVL ? constant : constant - 1), false);
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
          myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(var2, constant, maxConstant >= constant ? maxConstant - constant : maxConstant - constant == -1 && var2 != null && var2.getType() == LevelVariable.LvlType.HLVL ? -1 : 0));
        } else {
          if (var2 == null && oldLevel.getVar() != null || var2 != null && oldLevel.getVar() == null) {
            int otherConstant = var2 == null ? Math.max(constant, maxConstant) : Math.max(oldLevel.getConstant(), oldLevel.getMaxConstant());
            int thisConst = var2 == null ? oldLevel.getConstant() : constant;
            int thisMaxConst = var2 == null ? oldLevel.getMaxAddedConstant() : maxConstant;
            myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(Math.max(Math.min(thisMaxConst, otherConstant), Math.min(thisConst, otherConstant))));
          } else {
            if (var2 == null) {
              int newConst = Math.max(constant, maxConstant);
              if (newConst < oldLevel.getConstant()) {
                myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(newConst));
              }
            } else {
              if (constant < 0) {
                myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(Math.min(maxConstant, oldLevel.getMaxAddedConstant())));
              } else {
                int newConst = Math.min(constant, oldLevel.getConstant());
                int newMaxConst = Math.min(maxConstant, oldLevel.getMaxAddedConstant());
                myConstantUpperBounds.put((InferenceLevelVariable) var1, new Level(var2, newConst, newMaxConst >= newConst ? newMaxConst - newConst : newMaxConst - newConst == -1 && var2.getType() == LevelVariable.LvlType.HLVL ? -1 : 0));
              }
            }
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
      addEquation(new LevelEquation<>((InferenceLevelVariable) var), false);
    } else {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var)), sourceNode));
    }
  }

  @Override
  public boolean solve(Expression expr1, Expression expr2, Expression type, CMP cmp, Concrete.SourceNode sourceNode) {
    if (!CompareVisitor.compare(this, cmp, expr1, expr2, type, sourceNode)) {
      myVisitor.getErrorReporter().report(new SolveEquationError(expr1, expr2, sourceNode));
      return false;
    } else {
      return true;
    }
  }

  @Override
  public boolean addEquation(Level level1, Level level2, CMP cmp, Concrete.SourceNode sourceNode) {
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
      if (level1.withMaxConstant() && level1.getMaxAddedConstant() > level2.getMaxAddedConstant()) {
        addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxAddedConstant(), -1, sourceNode);
      }
      /*
      if (level1.getVar() != null || level1.getConstant() != 0) {
        addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxAddedConstant(), level2.getMaxAddedConstant() - level1.getMaxAddedConstant(), sourceNode);
      }
      */
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), level1.getMaxAddedConstant() - level2.getConstant(), sourceNode);
      if (level2.withMaxConstant() && level2.getMaxAddedConstant() > level1.getMaxAddedConstant()) {
        addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxAddedConstant(), -1, sourceNode);
      }
      /*
      if (level2.getVar() != null || level2.getConstant() != 0) {
        addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxAddedConstant(), level1.getMaxAddedConstant() - level2.getMaxAddedConstant(), sourceNode);
      }
      */
    }
    return true;
  }

  @Override
  public boolean addPropEquationIfPossible(Level level) {
    if (!(level.getVar() instanceof InferenceLevelVariable && level.isVarOnly())) {
      return false;
    }
    InferenceLevelVariable var = (InferenceLevelVariable) level.getVar();
    Level oldLevel = myConstantUpperBounds.get(var);
    if (oldLevel != null && oldLevel.isProp()) {
      return true;
    }

    if (trySolveProp(var)) {
      myConstantUpperBounds.put(var, new Level(-1));
      return true;
    } else {
      return false;
    }
  }

  // needed for lemmas and properties
  private boolean trySolveProp(InferenceLevelVariable var) {
    Set<InferenceLevelVariable> visited = new HashSet<>();
    Deque<InferenceLevelVariable> toVisit = new ArrayDeque<>();
    toVisit.add(var);
    while (!toVisit.isEmpty()) {
      InferenceLevelVariable vVar = toVisit.removeLast();
      if (!visited.add(vVar)) {
        continue;
      }

      Set<LevelVariable> lowerBounds = myLowerBounds.get(vVar);
      if (lowerBounds == null) {
        continue;
      }

      for (LevelVariable lowerBound : lowerBounds) {
        if (!(lowerBound instanceof InferenceLevelVariable)) {
          return false;
        }
        toVisit.add((InferenceLevelVariable) lowerBound);
      }
    }

    if (myHLevelEquations.isEmpty()) {
      return true;
    }

    Map<InferenceLevelVariable, Integer> solution = new HashMap<>();
    return myHLevelEquations.solve(solution) == null && solution.get(var) == 0;
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
  public boolean remove(Equation equation) {
    return myEquations.remove(equation);
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
    Map<InferenceLevelVariable,Boolean> unBasedMap = new HashMap<>();
    for (InferenceLevelVariable var : basedEquations.getVariables()) {
      Level ub = myConstantUpperBounds.get(var);
      if (ub != null) {
        if (ub.getVar() == null) {
          unBasedMap.put(var, true);
        } else {
          int sol = basedSolution.get(var);
          if (sol == LevelEquations.INFINITY || ub.getConstant() < sol) {
            unBasedMap.put(var, true);
          }
        }
      }
    }

    if (!unBasedMap.isEmpty()) {
      Stack<InferenceLevelVariable> stack = new Stack<>();
      for (InferenceLevelVariable var : unBasedMap.keySet()) {
        stack.push(var);
      }

      boolean ok = true;
      while (!stack.isEmpty()) {
        InferenceLevelVariable var = stack.pop();
        Set<LevelVariable> lowerBounds = myLowerBounds.get(var);
        if (lowerBounds != null) {
          for (LevelVariable lowerBound : lowerBounds) {
            if (lowerBound instanceof InferenceLevelVariable) {
              if (unBasedMap.put((InferenceLevelVariable) lowerBound, true) == null) {
                stack.push((InferenceLevelVariable) lowerBound);
              }
            } else if (ok) {
              myVisitor.getErrorReporter().report(new ConstantSolveLevelEquationError(lowerBound, var.getSourceNode()));
              ok = false;
            }
          }
        }
      }
    }

    for (InferenceLevelVariable variable : basedEquations.getVariables()) {
      calculateUnBasedMap(variable, unBasedMap);
    }

    for (Map.Entry<InferenceLevelVariable, Boolean> entry : unBasedMap.entrySet()) {
      if (entry.getValue()) {
        unBased.add(entry.getKey());
      }
    }
  }

  private boolean calculateUnBasedMap(InferenceLevelVariable variable, Map<InferenceLevelVariable,Boolean> unBasedMap) {
    if (variable.isUniverseLike()) {
      Boolean prev = unBasedMap.putIfAbsent(variable, false);
      return prev == null ? false : prev;
    }
    Boolean val = unBasedMap.get(variable);
    if (val != null) {
      return val;
    }

    unBasedMap.put(variable, true);
    Set<LevelVariable> lowerBounds = myLowerBounds.get(variable);
    if (lowerBounds != null) {
      for (LevelVariable lowerBound : lowerBounds) {
        boolean lowerBoundIsUnBased = lowerBound instanceof InferenceLevelVariable && calculateUnBasedMap((InferenceLevelVariable) lowerBound, unBasedMap);
        if (!lowerBoundIsUnBased) {
          unBasedMap.put(variable, false);
          return false;
        }
      }
    }

    return true;
  }

  private void solveLevelEquations(SimpleLevelSubstitution result) {
    if (myPLevelEquations.isEmpty() &&
        myHLevelEquations.isEmpty() &&
        myBasedPLevelEquations.isEmpty() &&
        myBasedHLevelEquations.isEmpty() &&
        myBoundVariables.isEmpty() &&
        myLowerBounds.isEmpty()) {
      return;
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
    for (Map.Entry<InferenceLevelVariable, Integer> entry : solution.entrySet()) {
      if (entry.getValue() != LevelEquations.INFINITY) {
        entry.setValue(entry.getValue() + 1);
      }
    }
    if (ok && cycle != null) {
      reportCycle(cycle, unBased);
    }

    if (!unBased.isEmpty()) {
      for (Pair<InferenceLevelVariable, InferenceLevelVariable> vars : myBoundVariables) {
        if (unBased.contains(vars.proj2)) {
          if (solution.get(vars.proj2) == 1) {
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

    for (InferenceLevelVariable var : unBased) {
      int sol = solution.get(var);
      assert sol != LevelEquations.INFINITY || var.getType() == LevelVariable.LvlType.HLVL;
      result.add(var, sol == LevelEquations.INFINITY ? Level.INFINITY : new Level(-sol));
    }
    for (Map.Entry<InferenceLevelVariable, Integer> entry : basedSolution.entrySet()) {
      assert entry.getValue() != LevelEquations.INFINITY || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
      if (!unBased.contains(entry.getKey())) {
        int sol = solution.get(entry.getKey());
        assert sol != LevelEquations.INFINITY || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
        result.add(entry.getKey(), sol == LevelEquations.INFINITY || entry.getValue() == LevelEquations.INFINITY ? Level.INFINITY : new Level(entry.getKey().getStd(), -entry.getValue(), -sol >= -entry.getValue() ? -sol - (-entry.getValue()) : entry.getKey().getType() == LevelVariable.LvlType.HLVL ? -1 : 0));
      }
    }

    for (Equation equation : myEquations) {
      equation.expr1 = equation.expr1.subst(result);
      equation.expr2 = equation.expr2.subst(result);
      if (equation.type != null) {
        equation.type = equation.type.subst(result);
      }
    }

    myPLevelEquations.clear();
    myHLevelEquations.clear();
    myBasedPLevelEquations.clear();
    myBasedHLevelEquations.clear();
    myBoundVariables.clear();
    myLowerBounds.clear();
  }

  @Override
  public void solveEquations() {
    while (!myProps.isEmpty()) {
      InferenceVariable var = myProps.pop();
      if (!var.isSolved()) {
        solve(var, new UniverseExpression(Sort.PROP), false, false);
      }
    }

    for (Equation equation : myEquations) {
      equation.expr1 = equation.expr1.normalize(NormalizationMode.WHNF);
      equation.expr2 = equation.expr2.normalize(NormalizationMode.WHNF);
    }

    while (!myEquations.isEmpty()) {
      if (!solveClassCallsEq()) {
        break;
      }
    }

    while (!myEquations.isEmpty()) {
      if (!solveClassCalls(CMP.LE) && !solveClassCalls(CMP.GE)) {
        break;
      }
    }
  }

  @Override
  public LevelSubstitution solveLevels(Concrete.SourceNode sourceNode) {
    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    solveLevelEquations(result);

    for (Map.Entry<InferenceLevelVariable, Level> entry : myConstantUpperBounds.entrySet()) {
      Level level = result.get(entry.getKey());
      if (!Level.compare(level, entry.getValue(), CMP.LE, DummyEquations.getInstance(), null)) {
        int maxConstant = entry.getValue().getMaxAddedConstant();
        List<LevelEquation<LevelVariable>> equations = new ArrayList<>(2);
        if (!Level.compare(level.withMaxConstant() ? new Level(level.getVar(), level.getConstant()) : level, entry.getValue(), CMP.LE, DummyEquations.getInstance(), null)) {
          equations.add(level.isInfinity() ? new LevelEquation<>(entry.getKey()) : new LevelEquation<>(level.getVar(), entry.getKey(), -level.getConstant()));
        }
        if (level.withMaxConstant() && !Level.compare(new Level(level.getMaxAddedConstant()), entry.getValue(), CMP.LE, DummyEquations.getInstance(), null)) {
          equations.add(new LevelEquation<>(null, entry.getKey(), -level.getMaxAddedConstant()));
        }
        equations.add(new LevelEquation<>(entry.getKey(), entry.getValue().getVar(), entry.getValue().getConstant(), maxConstant));
        myVisitor.getErrorReporter().report(new SolveLevelEquationsError(equations, entry.getKey().getSourceNode()));
      }
    }

    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      Expression stuckExpr = equation.expr2.getStuckExpression();
      if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isError())) {
        iterator.remove();
      } else {
        stuckExpr = equation.expr1.getStuckExpression();
        if (stuckExpr != null && (stuckExpr.isInstance(InferenceReferenceExpression.class) || stuckExpr.isError())) {
          iterator.remove();
        }
      }
    }
    if (!myEquations.isEmpty()) {
      myVisitor.getErrorReporter().report(new SolveEquationsError(new ArrayList<>(myEquations), sourceNode));
    }

    myEquations.clear();
    myConstantUpperBounds.clear();
    myProps.clear();
    return result;
  }

  @Override
  public boolean supportsLevels() {
    return true;
  }

  @Override
  public boolean supportsExpressions() {
    return true;
  }

  private boolean solveClassCallsEq() {
    List<Pair<InferenceVariable, Expression>> solved = null;
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.cmp == CMP.EQ) {
        InferenceVariable var1 = equation.expr1.getInferenceVariable();
        InferenceVariable var2 = equation.expr2.getInferenceVariable();
        if (var1 != null && var2 == null || var2 != null && var1 == null) {
          iterator.remove();
          if (solved == null) {
            solved = new ArrayList<>();
          }
          solved.add(new Pair<>(var1 != null ? var1 : var2, var1 != null ? equation.expr2 : equation.expr1));
        }
      }
    }

    if (solved != null) {
      for (Pair<InferenceVariable, Expression> pair : solved) {
        solve(pair.proj1, pair.proj2, false);
      }
      return true;
    } else {
      return false;
    }
  }

  // If cmp == LE, then solve lower bounds; if cmp == GE, solve upper bounds.
  private boolean solveClassCalls(CMP cmp) {
    boolean solved = false;
    boolean allOK = true;

    boolean hasBound = false;
    Map<InferenceVariable,Set<Wrapper>> bounds = new HashMap<>();
    List<Equation> classCallEquations = new ArrayList<>();
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      Expression lower = equation.getLowerBound();
      Expression upper = equation.getUpperBound();
      ClassCallExpression lowerClassCall = lower.cast(ClassCallExpression.class);
      ClassCallExpression upperClassCall = upper.cast(ClassCallExpression.class);
      if (lowerClassCall != null && upperClassCall != null) {
        classCallEquations.add(new Equation(lowerClassCall, upperClassCall, Type.OMEGA, equation.cmp == CMP.EQ ? CMP.EQ : CMP.LE, equation.sourceNode));
        iterator.remove();
        solved = true;
        continue;
      }

      if (equation.cmp == CMP.EQ) {
        InferenceVariable var1 = equation.expr1.getInferenceVariable();
        InferenceVariable var2 = equation.expr2.getInferenceVariable();
        if (var1 != null && var2 != null) {
          bounds.computeIfAbsent(var1, k -> new LinkedHashSet<>()).add(new Wrapper(equation.expr2));
          bounds.computeIfAbsent(var2, k -> new LinkedHashSet<>()).add(new Wrapper(equation.expr1));
        }
        continue;
      }

      InferenceVariable var = (cmp == CMP.LE ? upper : lower).getInferenceVariable();
      InferenceVariable otherVar = (cmp == CMP.LE ? lower : upper).getInferenceVariable();
      if (var != null) {
        boolean isClassCall = (cmp == CMP.LE ? lowerClassCall : upperClassCall) != null;
        if (isClassCall || otherVar != null) {
          bounds.computeIfAbsent(var, k -> new LinkedHashSet<>()).add(new Wrapper(cmp == CMP.LE ? lower : upper));
          if (isClassCall) {
            hasBound = true;
            iterator.remove();
          }
        }
      }
    }

    for (Equation equation : classCallEquations) {
      if (!CompareVisitor.compare(this, equation.cmp, equation.expr1, equation.expr2, equation.type, equation.sourceNode)) {
        allOK = false;
        myVisitor.getErrorReporter().report(new SolveEquationsError(Collections.singletonList(equation), equation.sourceNode));
      }
    }

    if (!hasBound) {
      return allOK && solved;
    }

    // @bounds consists of entries (@v,@list) such that every expression @e in @list is either a classCall or an inference variable and @e `cmp` @v.
    // The result of @calculateClosure is the transitive closure of @bounds.
    loop:
    for (Pair<InferenceVariable, List<ClassCallExpression>> pair : calculateClosure(bounds)) {
      // Solve pair.proj1 as the intersection of their bounds

      if (pair.proj2.size() == 1) {
        solve(pair.proj1, pair.proj2.get(0), true);
        solved = true;
        continue;
      }

      ClassDefinition classDef = checkClasses(pair.proj1, pair.proj2, cmp);
      if (classDef == null) {
        allOK = false;
        continue;
      }

      UniverseKind universeKind = classDef.getUniverseKind();
      if (universeKind != UniverseKind.NO_UNIVERSES) {
        universeKind = UniverseKind.NO_UNIVERSES;
        for (ClassField field : classDef.getFields()) {
          if (field.getUniverseKind() == UniverseKind.NO_UNIVERSES || classDef.isImplemented(field)) {
            continue;
          }
          boolean implemented = false;
          for (ClassCallExpression classCall : pair.proj2) {
            if (classCall.isImplementedHere(field)) {
              implemented = true;
              break;
            }
          }
          if (!implemented) {
            universeKind = universeKind.max(field.getUniverseKind());
            if (universeKind == UniverseKind.WITH_UNIVERSES) {
              break;
            }
          }
        }
      }

      ClassCallExpression solution;
      if (cmp == CMP.LE) {
        Equations wrapper = new LevelEquationsWrapper(this);
        Sort sortArg = Sort.generateInferVars(this, universeKind, pair.proj1.getSourceNode());
        Map<ClassField, Expression> implementations = new HashMap<>();
        solution = new ClassCallExpression(classDef, sortArg, implementations, classDef.getSort(), universeKind);
        ReferenceExpression thisExpr = new ReferenceExpression(solution.getThisBinding());
        boolean first = true;
        for (ClassCallExpression bound : pair.proj2) {
          if (first) {
            for (Map.Entry<ClassField, Expression> entry : bound.getImplementedHere().entrySet()) {
              implementations.put(entry.getKey(), entry.getValue().subst(bound.getThisBinding(), thisExpr));
            }
            first = false;
            continue;
          }

          for (Iterator<Map.Entry<ClassField, Expression>> iterator = implementations.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ClassField, Expression> entry = iterator.next();
            Expression other = bound.getAbsImplementationHere(entry.getKey());
            if (other == null || !CompareVisitor.compare(wrapper, CMP.EQ, entry.getValue(), other, entry.getKey().getType(solution.getSortArgument()).applyExpression(thisExpr), pair.proj1.getSourceNode())) {
              iterator.remove();
            }
          }
        }

        solution.setSort(classDef.computeSort(solution.getSortArgument(), implementations, solution.getThisBinding()));
        solution.updateHasUniverses();

        if (!Sort.compare(pair.proj2.get(0).getSortArgument(), sortArg, CMP.LE, this, pair.proj1.getSourceNode())) {
          reportBoundsError(pair.proj1, pair.proj2, CMP.GE);
          allOK = false;
          continue;
        }
        for (ClassCallExpression lowerBound : pair.proj2) {
          if (!new CompareVisitor(this, CMP.LE, pair.proj1.getSourceNode()).compareClassCallSortArguments(lowerBound, solution)) {
            reportBoundsError(pair.proj1, pair.proj2, CMP.GE);
            allOK = false;
            continue loop;
          }
        }
      } else {
        solution = pair.proj2.get(0);
        Map<ClassField, Expression> map = solution.getImplementedHere();
        Expression thisExpr = new ReferenceExpression(solution.getThisBinding());
        for (int i = 1; i < pair.proj2.size(); i++) {
          Map<ClassField, Expression> otherMap = pair.proj2.get(i).getImplementedHere();
          if (map.size() != otherMap.size()) {
            reportBoundsError(pair.proj1, pair.proj2, CMP.LE);
            allOK = false;
            continue loop;
          }

          for (Map.Entry<ClassField, Expression> entry : map.entrySet()) {
            Expression other = otherMap.get(entry.getKey());
            if (other == null || !CompareVisitor.compare(this, CMP.EQ, entry.getValue(), other, entry.getKey().getType(solution.getSortArgument()).applyExpression(thisExpr), pair.proj1.getSourceNode())) {
              reportBoundsError(pair.proj1, pair.proj2, CMP.LE);
              allOK = false;
              continue loop;
            }
          }
        }
      }

      solve(pair.proj1, solution, true);
      solved = true;
    }

    return allOK && solved;
  }

  private ClassDefinition checkClasses(InferenceVariable var, List<ClassCallExpression> bounds, CMP cmp) {
    ClassDefinition classDef = bounds.get(0).getDefinition();
    for (ClassCallExpression classCall : bounds) {
      if (classCall.getDefinition() != classDef) {
        reportBoundsError(var, bounds, cmp);
        return null;
      }
    }

    return classDef;
  }

  private void reportBoundsError(InferenceVariable var, List<ClassCallExpression> bounds, CMP cmp) {
    List<Equation> equations = new ArrayList<>();
    Expression infRefExpr = new InferenceReferenceExpression(var, (Expression) null);
    for (ClassCallExpression bound : bounds) {
      equations.add(cmp == CMP.GE ? new Equation(bound, infRefExpr, Type.OMEGA, CMP.LE, var.getSourceNode()) : new Equation(infRefExpr, bound, Type.OMEGA, CMP.LE, var.getSourceNode()));
    }
    myVisitor.getErrorReporter().report(new SolveEquationsError(equations, var.getSourceNode()));
  }

  private static class Wrapper {
    Expression expression;

    Wrapper(Expression expression) {
      this.expression = expression;
    }
  }

  private List<Pair<InferenceVariable,List<ClassCallExpression>>> calculateClosure(Map<InferenceVariable,Set<Wrapper>> bounds) {
    List<Pair<InferenceVariable,List<ClassCallExpression>>> result = new ArrayList<>(bounds.size());
    for (Map.Entry<InferenceVariable, Set<Wrapper>> entry : bounds.entrySet()) {
      Set<Wrapper> varResult = new HashSet<>();
      calculateBoundsOfVariable(entry.getKey(), varResult, bounds, new HashSet<>());
      if (!varResult.isEmpty()) {
        List<ClassCallExpression> list = new ArrayList<>(varResult.size());
        for (Wrapper wrapper : varResult) {
          list.add((ClassCallExpression) wrapper.expression);
        }
        result.add(new Pair<>(entry.getKey(), list));
      }
    }
    return result;
  }

  private void calculateBoundsOfVariable(InferenceVariable variable, Set<Wrapper> result, Map<InferenceVariable,Set<Wrapper>> bounds, Set<InferenceVariable> visited) {
    if (!visited.add(variable)) {
      return;
    }

    Set<Wrapper> varBounds = bounds.get(variable);
    if (varBounds == null) {
      return;
    }

    for (Wrapper wrapper : varBounds) {
      ClassCallExpression classCall = wrapper.expression.cast(ClassCallExpression.class);
      if (classCall != null) {
        wrapper.expression = classCall;
        result.add(wrapper);
      } else {
        InferenceVariable var = wrapper.expression.getInferenceVariable();
        if (var != null) {
          calculateBoundsOfVariable(var, result, bounds, visited);
        }
      }
    }
  }

  private ClassCallExpression removeDependencies(ClassCallExpression solution, int originalSize) {
    ClassDefinition classDef = solution.getDefinition();
    Map<ClassField, Expression> implementations = solution.getImplementedHere();
    Sort sortArgument = solution.getSortArgument();

    for (ClassField field : classDef.getFields()) {
      if (!implementations.containsKey(field)) {
        continue;
      }
      field.getType(sortArgument).getCodomain().accept(new SearchVisitor<Void>() {
        @Override
        protected boolean processDefCall(DefCallExpression expression, Void param) {
          if (expression instanceof FieldCallExpression && classDef.getFields().contains(((FieldCallExpression) expression).getDefinition()) && !solution.isImplemented((ClassField) expression.getDefinition())) {
            implementations.remove(field);
            return true;
          }
          return false;
        }
      }, null);
    }

    ClassCallExpression sol = solution;
    if (originalSize != implementations.size()) {
      Sort newSort = classDef.computeSort(sortArgument, implementations, solution.getThisBinding());
      if (!newSort.equals(sol.getSort())) {
        sol = new ClassCallExpression(classDef, sortArgument, implementations, newSort, classDef.getUniverseKind());
        for (Map.Entry<ClassField, Expression> entry : implementations.entrySet()) {
          entry.setValue(entry.getValue().subst(solution.getThisBinding(), new ReferenceExpression(sol.getThisBinding())));
        }
      }
    }
    sol.updateHasUniverses();
    return sol;
  }

  private enum SolveResult { SOLVED, NOT_SOLVED, ERROR }

  private SolveResult solve(InferenceVariable var, Expression expr, boolean isLowerBound) {
    return solve(var, expr, isLowerBound, false);
  }

  private SolveResult solve(InferenceVariable var, Expression expr, boolean isLowerBound, boolean trySolve) {
    if (expr.getInferenceVariable() == var) {
      return SolveResult.SOLVED;
    }
    if (myProps.contains(var) && !expr.isInstance(UniverseExpression.class)) {
      LocalError error = var.getErrorInfer(new UniverseExpression(Sort.PROP), expr);
      myVisitor.getErrorReporter().report(error);
      return SolveResult.ERROR;
    }

    if (expr.findBinding(var)) {
      return inferenceError(var, expr);
    }

    Expression expectedType = var.getType();
    Expression result = ElimBindingVisitor.keepBindings(expr, var.getBounds(), isLowerBound);
    if (isLowerBound && result != null) {
      ClassCallExpression classCall = result.cast(ClassCallExpression.class);
      if (classCall != null) {
        result = removeDependencies(classCall, classCall.getImplementedHere().size());
      }
    }

    Expression actualType = result == null ? null : result.getType();
    if (actualType == null) {
      return inferenceError(var, expr);
    }

    if (actualType.isLessOrEquals(expectedType, this, var.getSourceNode())) {
      var.solve(this, OfTypeExpression.make(result, actualType, expectedType));
      return SolveResult.SOLVED;
    } else {
      if (trySolve) {
        return SolveResult.NOT_SOLVED;
      } else {
        LocalError error = var.getErrorMismatch(expectedType, actualType, expr);
        myVisitor.getErrorReporter().report(error);
        var.solve(this, new ErrorExpression(result, error));
        return SolveResult.ERROR;
      }
    }
  }

  private SolveResult inferenceError(InferenceVariable var, Expression expr) {
    LocalError error = var.getErrorInfer(expr);
    myVisitor.getErrorReporter().report(error);
    var.solve(this, new ErrorExpression(error));
    return SolveResult.ERROR;
  }
}
