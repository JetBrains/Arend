package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import java.util.ArrayList;
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

  public Var solve(Map<Var, Integer> solution) {
    solution.put(null, 0);
    for (Var var : myVariables) {
      solution.put(var, 0);
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
            if (i == 0 || equation.getVariable2() == null && a != null) {
              solution.remove(null);
              return equation.getVariable1() != null ? equation.getVariable1() : equation.getVariable2();
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
