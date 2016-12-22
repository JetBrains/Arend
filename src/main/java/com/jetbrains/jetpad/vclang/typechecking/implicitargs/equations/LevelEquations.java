package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelEquations<Var> {
  private final List<Var> myVariables = new ArrayList<>();
  private final List<LevelEquation<Var>> myEquations = new ArrayList<>();

  public List<LevelEquation<Var>> getEquations() {
    return myEquations;
  }

  public void addVariable(Var var) {
    myVariables.add(var);
  }

  public void add(LevelEquations<Var> equations) {
    myVariables.addAll(equations.myVariables);
    myEquations.addAll(equations.myEquations);
  }

  public void addEquation(LevelEquation<Var> equation) {
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
    paths.put(null, new ArrayList<LevelEquation<Var>>());
    for (Var var : myVariables) {
      solution.put(var, 0);
      paths.put(var, new ArrayList<LevelEquation<Var>>());
    }

    for (int i = myVariables.size(); i >= 0; i--) {
      boolean updated = false;
      for (LevelEquation<Var> equation : myEquations) {
        if (equation.isInfinity()) {
          solution.put(equation.getVariable(), null);
        } else {
          Integer a = solution.get(equation.getVariable1());
          Integer b = solution.get(equation.getVariable2());
          if (b != null && (a == null || b > a + equation.getConstant())) {
            if (a != null) {
              List<LevelEquation<Var>> newPath = new ArrayList<>(paths.get(equation.getVariable1()));
              newPath.add(equation);
              paths.put(equation.getVariable2(), newPath);
            }
            if (i == 0 || equation.getVariable2() == null && a != null) {
              solution.remove(null);
             // Var var = equation.getVariable1() != null ? equation.getVariable1() : equation.getVariable2();
              return paths.get(equation.getVariable2());
            }

            solution.put(equation.getVariable2(), a == null ? null : a + equation.getConstant());
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
