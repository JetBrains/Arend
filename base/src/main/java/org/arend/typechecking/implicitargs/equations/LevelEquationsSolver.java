package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SimpleLevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.ErrorReporter;
import org.arend.typechecking.error.local.ConstantSolveLevelEquationError;
import org.arend.typechecking.error.local.SolveLevelEquationsError;
import org.arend.util.Pair;

import java.util.*;

public class LevelEquationsSolver {
  private final LevelEquations<InferenceLevelVariable> myPLevelEquations = new LevelEquations<>();      // equations of the forms      c <= ?y and ?x <= max(?y + c', d)
  private final LevelEquations<InferenceLevelVariable> myBasedPLevelEquations = new LevelEquations<>(); // equations of the forms lp + c <= ?y and ?x <= max(?y + c', d)
  private final LevelEquations<InferenceLevelVariable> myHLevelEquations = new LevelEquations<>();
  private final LevelEquations<InferenceLevelVariable> myBasedHLevelEquations = new LevelEquations<>();
  private final List<Pair<InferenceLevelVariable, InferenceLevelVariable>> myBoundVariables;
  private final Map<InferenceLevelVariable, Level> myConstantUpperBounds = new HashMap<>();
  private final Map<InferenceLevelVariable, Set<LevelVariable>> myLowerBounds = new HashMap<>();
  private final ErrorReporter myErrorReporter;

  public LevelEquationsSolver(List<LevelEquation<LevelVariable>> levelEquations, List<InferenceLevelVariable> variables, List<Pair<InferenceLevelVariable, InferenceLevelVariable>> boundVariables, ErrorReporter errorReporter) {
    for (InferenceLevelVariable var : variables) {
      if (var.getType() == LevelVariable.LvlType.PLVL) {
        myPLevelEquations.addVariable(var);
        myBasedPLevelEquations.addVariable(var);
      } else {
        myHLevelEquations.addVariable(var);
        myBasedHLevelEquations.addVariable(var);
      }
    }
    variables.clear();

    for (LevelEquation<LevelVariable> levelEquation : levelEquations) {
      if (levelEquation.isInfinity()) {
        //noinspection unchecked
        addEquation((LevelEquation<InferenceLevelVariable>) (LevelEquation<?>) levelEquation, false);
      } else {
        addLevelEquation(levelEquation.getVariable1(), levelEquation.getVariable2(), levelEquation.getConstant(), levelEquation.getMaxConstant());
      }
    }
    levelEquations.clear();

    myBoundVariables = boundVariables;
    myErrorReporter = errorReporter;
  }

  private void addLevelEquation(final LevelVariable var1, LevelVariable var2, int constant, int maxConstant) {
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

    // l <= max(_ +- c, +-d) // 6
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
      }
    }
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
    } else if (var1 != null && var1.getType() == LevelVariable.LvlType.HLVL || var2 != null && var2.getType() == LevelVariable.LvlType.HLVL) {
      if (based) {
        myBasedHLevelEquations.addEquation(equation);
      } else {
        myHLevelEquations.addEquation(equation);
      }
    } else {
      throw new IllegalStateException();
    }
  }

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

  public LevelSubstitution solveLevels() {
    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
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
        myErrorReporter.report(new SolveLevelEquationsError(equations, entry.getKey().getSourceNode()));
      }
    }

    return result;
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
              myErrorReporter.report(new ConstantSolveLevelEquationError(lowerBound, var.getSourceNode()));
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
      return prev != null && prev;
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
    myErrorReporter.report(new SolveLevelEquationsError(basedCycle, var.getSourceNode()));
  }
}
