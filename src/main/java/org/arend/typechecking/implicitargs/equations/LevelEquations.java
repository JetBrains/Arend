package org.arend.typechecking.implicitargs.equations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelEquations<Var> {
  private final List<Var> myVariables = new ArrayList<>();
  private final List<LevelEquation<Var>> myEquations = new ArrayList<>();
  static final int INFINITY = Integer.MAX_VALUE;

  public List<LevelEquation<Var>> getEquations() {
    return myEquations;
  }

  public List<Var> getVariables() {
    return myVariables;
  }

  void addVariable(Var var) {
    myVariables.add(var);
  }

  public void add(LevelEquations<Var> equations) {
    myVariables.addAll(equations.myVariables);
    myEquations.addAll(equations.myEquations);
  }

  void addEquation(LevelEquation<Var> equation) {
    myEquations.add(equation);
  }

  public void clear() {
    myVariables.clear();
    myEquations.clear();
  }

  public boolean isEmpty() {
    return myVariables.isEmpty() && myEquations.isEmpty();
  }

  public List<LevelEquation<Var>> solve(Map<Var, Integer> solution) {
    Map<Var, List<LevelEquation<Var>>> paths = new HashMap<>();

    solution.put(null, 0);
    paths.put(null, new ArrayList<>());
    for (Var var : myVariables) {
      solution.put(var, 0);
      paths.put(var, new ArrayList<>());
    }

    for (int i = myVariables.size(); i >= 0; i--) {
      boolean updated = false;
      for (LevelEquation<Var> equation : myEquations) {
        if (equation.isInfinity()) {
          Integer prev = solution.put(equation.getVariable(), INFINITY);
          if (prev == null || prev != INFINITY) {
            updated = true;
          }
        } else {
          int a = solution.get(equation.getVariable1());
          int b = solution.get(equation.getVariable2());
          Integer m = equation.getMaxConstant();
          if (b != INFINITY && (a == INFINITY || (m == null || a + m < 0) && b > a + equation.getConstant())) {
            if (a != INFINITY) {
              List<LevelEquation<Var>> newPath = new ArrayList<>(paths.get(equation.getVariable1()));
              newPath.add(equation);
              paths.put(equation.getVariable2(), newPath);
            }
            if (i == 0 || equation.getVariable2() == null && a != INFINITY) {
              solution.remove(null);
              return paths.get(equation.getVariable2());
            }

            solution.put(equation.getVariable2(), a == INFINITY ? INFINITY : a + equation.getConstant());
            updated = true;
          }
        }
      }
      if (!updated) {
        break;
      }
    }

    solution.remove(null);
    return null;
  }
}
