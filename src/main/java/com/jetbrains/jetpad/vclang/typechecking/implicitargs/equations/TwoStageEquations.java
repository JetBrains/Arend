package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.DerivedInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.LevelInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationError;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.error.SolveLevelEquationsError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.*;

public class TwoStageEquations implements Equations {
  private List<Equation> myEquations;
  private final Map<LevelInferenceVariable, Variable> myBases;
  private final LevelEquations<LevelInferenceVariable> myLevelEquations;
  private final CheckTypeVisitor myVisitor;

  public TwoStageEquations(CheckTypeVisitor visitor) {
    myEquations = new ArrayList<>();
    myBases = new HashMap<>();
    myLevelEquations = new LevelEquations<>();
    myVisitor = visitor;
  }

  private void addEquation(Type type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    InferenceVariable inf1 = type instanceof Expression && ((Expression) type).toInferenceReference() != null ? ((Expression) type).toInferenceReference().getVariable() : null;
    InferenceVariable inf2 = expr.toInferenceReference() != null ? expr.toInferenceReference().getVariable() : null;

    if (inf1 == inf2 && inf1 != null) {
      return;
    }

    if (inf1 == null && inf2 == null) {
      Expression expr1 = type.toExpression();
      // TODO: correctly check for stuck expressions
      if (expr1 != null && (expr1.getFunction().toInferenceReference() == null || expr1.getFunction().toInferenceReference().getVariable() == null) && (expr.getFunction().toInferenceReference() == null || expr.getFunction().toInferenceReference().getVariable() == null)) {
        InferenceVariable variable = null;
        Expression result = null;
        if (expr1.toFieldCall() != null && expr1.toFieldCall().getExpression().toInferenceReference() != null) {
          variable = expr1.toFieldCall().getExpression().toInferenceReference().getVariable();
          if (variable instanceof TypeClassInferenceVariable) {
            result = myVisitor.getClassViewInstancePool().getInstance(expr);
          }
        }
        if (variable == null && expr.toFieldCall() != null && expr.toFieldCall().getExpression().toInferenceReference() != null) {
          variable = expr.toFieldCall().getExpression().toInferenceReference().getVariable();
          if (variable instanceof TypeClassInferenceVariable) {
            result = myVisitor.getClassViewInstancePool().getInstance(expr1);
          }
        }
        if (result != null) {
          solve(variable, result);
          return;
        }
      }
    }

    if (inf1 != null && inf2 == null || inf2 != null && inf1 == null) {
      InferenceVariable cInf = inf1 != null ? inf1 : inf2;
      Type cType = inf1 != null ? expr : type;

      if (cType instanceof Expression) {
        Expression cExpr = (Expression) cType;
        // TODO: set cmp to CMP.EQ only if cExpr is not stuck on a meta-variable
        if (cExpr.toPi() == null && cExpr.toUniverse() == null && cExpr.toClassCall() == null) {
          cmp = CMP.EQ;
        }
      }

      if (cmp == CMP.EQ) {
        assert cType instanceof Expression;
        solve(cInf, (Expression) cType);
        return;
      }

      if (inf1 != null) {
        cmp = cmp.not();
      }

      DependentLink piParams = cType.getPiParameters();
      if (piParams.hasNext()) {
        InferenceVariable infVar = new DerivedInferenceVariable(cInf.getName() + "-cod", cInf);
        Expression newRef = new InferenceReferenceExpression(infVar);
        solve(cInf, new PiExpression(piParams, newRef));
        addEquation(cType.getPiCodomain(), newRef, cmp, sourceNode, infVar);
        return;
      }

      SortMax sorts = cType.toSorts();
      if (sorts != null) {
        LevelInferenceVariable lpInf = new LevelInferenceVariable(cInf.getName() + "-lp", new DataCallExpression(Prelude.LVL), cInf.getSourceNode());
        LevelInferenceVariable lhInf = new LevelInferenceVariable(cInf.getName() + "-lh", new DataCallExpression(Prelude.CNAT), cInf.getSourceNode());
        myLevelEquations.addVariable(lpInf);
        myLevelEquations.addVariable(lhInf);
        Level lp = new Level(lpInf);
        Level lh = new Level(lhInf);
        solve(cInf, new UniverseExpression(new Sort(lp, lh)));
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

    Equation equation;
    if (expr.toInferenceReference() == null && type instanceof Expression && ((Expression) type).toInferenceReference() != null) {
      equation = new Equation(expr, (Expression) type, cmp.not(), sourceNode);
    } else {
      equation = new Equation(type, expr, cmp, sourceNode);
    }

    myEquations.add(equation);
    stuckVar.addListener(equation);
  }

  private void addBase(LevelInferenceVariable var, Variable base, Abstract.SourceNode sourceNode) {
    Variable base1 = myBases.get(var);
    if (base1 == null) {
      myBases.put(var, base);
    } else {
      if (base != base1) {
        List<LevelEquation<Variable>> equations = new ArrayList<>(2);
        equations.add(new LevelEquation<>(base, var, 0));
        equations.add(new LevelEquation<>(base1, var, 0));
        myVisitor.getErrorReporter().report(new SolveLevelEquationsError(equations, sourceNode));
      }
    }
  }

  private void addLevelEquation(Variable var1, Variable var2, int constant, Abstract.SourceNode sourceNode) {
    if (!(var1 instanceof LevelInferenceVariable) && !(var2 instanceof LevelInferenceVariable)) {
      if (var1 != var2 || constant < 0) {
        myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant)), sourceNode));
      }
      return;
    }

    if (var1 != null && var2 instanceof LevelInferenceVariable) {
      Variable base = var1 instanceof LevelInferenceVariable ? myBases.get(var1) : var1;
      if (base != null) {
        addBase((LevelInferenceVariable) var2, base, sourceNode);
      }
    }

    myLevelEquations.addEquation(new LevelEquation<>(var1 instanceof LevelInferenceVariable ? (LevelInferenceVariable) var1 : null, var2 instanceof LevelInferenceVariable ? (LevelInferenceVariable) var2 : null, constant));
  }

  private void addLevelEquation(Variable var, Abstract.SourceNode sourceNode) {
    if (var instanceof LevelInferenceVariable) {
      myLevelEquations.addEquation(new LevelEquation<>((LevelInferenceVariable) var));
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
  public boolean solve(Type type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode) {
    boolean ok;
    if (type instanceof Expression) {
      ok = CompareVisitor.compare(this, cmp, ((Expression) type).normalize(NormalizeVisitor.Mode.NF), expr.normalize(NormalizeVisitor.Mode.NF), sourceNode);
    } else
    if (cmp == CMP.LE) {
      ok = type.normalize(NormalizeVisitor.Mode.NF).isLessOrEquals(expr.normalize(NormalizeVisitor.Mode.NF), this, sourceNode);
    } else {
      throw new IllegalStateException();
    }

    if (!ok) {
      myVisitor.getErrorReporter().report(new SolveEquationError<>(type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr.normalize(NormalizeVisitor.Mode.HUMAN_NF), null, sourceNode));
    }
    return ok;
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
      addLevelEquation(level1.getVar(), level2.getVar(), level2.getConstant() - level1.getConstant(), sourceNode);
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), sourceNode);
    }
    return true;
  }

  @Override
  public boolean add(Type type, Expression expr, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    addEquation(type, expr, CMP.LE, sourceNode, stuckVar);
    return true;
  }

  @Override
  public boolean addVariable(LevelInferenceVariable var) {
    myLevelEquations.addVariable(var);
    return true;
  }

  @Override
  public void remove(Equation equation) {
    myEquations.remove(equation);
  }

  @Override
  public LevelSubstitution solve(Abstract.SourceNode sourceNode) {
    solveClassCalls();

    Map<LevelInferenceVariable, Integer> solution = new HashMap<>();
    List<LevelEquation<LevelInferenceVariable>> circle = myLevelEquations.solve(solution);
    if (circle != null) {
      LevelEquation<LevelInferenceVariable> lastEquation = circle.get(circle.size() - 1);
      LevelInferenceVariable var = lastEquation.getVariable1() != null ? lastEquation.getVariable1() : lastEquation.getVariable2();
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(new ArrayList<LevelEquation<? extends Variable>>(circle), var.getSourceNode()));
    }

    LevelSubstitution result = new LevelSubstitution();
    for (Map.Entry<LevelInferenceVariable, Integer> entry : solution.entrySet()) {
      Integer constant = entry.getValue();
      result.add(entry.getKey(), constant == null ? Level.INFINITY : new Level(myBases.get(entry.getKey()), -constant));
    }

    if (!myEquations.isEmpty()) {
      myVisitor.getErrorReporter().report(new SolveEquationsError(new ArrayList<>(myEquations), sourceNode));
    }
    myEquations.clear();
    myBases.clear();
    myLevelEquations.clear();
    return result;
  }

  private void solveClassCalls() {
    List<Equation> lowerBounds = new ArrayList<>(myEquations.size());
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.expr.toInferenceReference() != null && equation.expr.toInferenceReference().getSubstExpression() == null && equation.type instanceof Expression) {
        Expression expr = (Expression) equation.type;
        if (expr.toInferenceReference() != null && expr.toInferenceReference().getSubstExpression() == null || expr.toClassCall() != null && !(equation.cmp == CMP.GE && expr.toClassCall() != null)) {
          if (equation.cmp == CMP.LE) {
            lowerBounds.add(equation);
          } else if (equation.cmp == CMP.GE) {
            lowerBounds.add(new Equation(equation.expr, expr, CMP.LE, equation.sourceNode));
          } else {
            lowerBounds.add(new Equation(equation.type, equation.expr, CMP.LE, equation.sourceNode));
            lowerBounds.add(new Equation(equation.expr, expr, CMP.LE, equation.sourceNode));
          }
          iterator.remove();
        }
      }
    }

    solveClassCallLowerBounds(lowerBounds);

    Map<InferenceVariable, Expression> result = new HashMap<>();
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.expr.toInferenceReference() != null && equation.expr.toInferenceReference().getSubstExpression() == null && equation.type instanceof Expression) {
        Expression newResult = (Expression) equation.type;
        if (newResult.toInferenceReference() != null && newResult.toInferenceReference().getSubstExpression() == null || newResult.toClassCall() != null && equation.cmp == CMP.GE && newResult.toClassCall() != null) {
          InferenceVariable var = equation.expr.toInferenceReference().getVariable();
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
        Expression newSolution = (Expression) equation.type;
        if (newSolution.toInferenceReference() != null && newSolution.toInferenceReference().getSubstExpression() == null) {
          newSolution = solutions.get(newSolution.toInferenceReference().getVariable());
        }
        if (newSolution != null) {
          InferenceVariable var = equation.expr.toInferenceReference().getVariable();
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

  private boolean solve(InferenceVariable var, Expression expr) {
    if (expr.findBinding(var)) {
      TypeCheckingError error = var.getErrorInfer(expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(expr, error));
      return false;
    }

    Expression expectedType = var.getType();
    Type actualType = expr.getType();
    if (!actualType.isLessOrEquals(expectedType.normalize(NormalizeVisitor.Mode.NF), this, var.getSourceNode())) {
      actualType = actualType.normalize(NormalizeVisitor.Mode.HUMAN_NF);
      TypeCheckingError error = var.getErrorMismatch(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), actualType, expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(expr, error));
      return false;
    } else {
      var.solve(this, new OfTypeExpression(expr, expectedType));
      return true;
    }
  }
}
