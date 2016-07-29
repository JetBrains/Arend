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

    for (int i = myVariables.size() - 1; i >= 0; i--) {
      boolean updated = false;
      for (LevelEquation<Var> equation : myEquations) {
        if (equation.constant == null) {
          solution.put(equation.var2, null);
        } else {
          Integer a = solution.get(equation.var1);
          Integer b = solution.get(equation.var2);
          if (b != null && (a == null || b > a + equation.constant)) {
            if (i == 0) {
              solution.remove(null);
              return equation.var1 != null ? equation.var1 : equation.var2;
            }

            solution.put(equation.var2, a == null ? null : a + equation.constant);
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
