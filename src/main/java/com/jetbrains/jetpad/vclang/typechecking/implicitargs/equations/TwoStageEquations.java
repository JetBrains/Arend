package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.DerivedInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.TypeClassInferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
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

import java.util.*;

public class TwoStageEquations implements Equations {
  private List<Equation> myEquations;
  private final Map<InferenceLevelVariable, LevelVariable> myBases;
  private final LevelEquations<InferenceLevelVariable> myLevelEquations;
  private final CheckTypeVisitor myVisitor;

  public TwoStageEquations(CheckTypeVisitor visitor) {
    myEquations = new ArrayList<>();
    myBases = new HashMap<>();
    myLevelEquations = new LevelEquations<>();
    myVisitor = visitor;
  }

  private void addEquation(Expression type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    InferenceVariable inf1 = type.toInferenceReference() != null ? type.toInferenceReference().getVariable() : null;
    InferenceVariable inf2 = expr.toInferenceReference() != null ? expr.toInferenceReference().getVariable() : null;

    // expr1 == expr2 == ?x
    if (inf1 == inf2 && inf1 != null) {
      return;
    }

    if (inf1 == null && inf2 == null) {
      Expression expr1 = type.toExpression();
      // TODO: correctly check for stuck expressions
      // expr1 /= stuck, expr2 /= stuck
      if (expr1 != null && (expr1.getFunction().toInferenceReference() == null || expr1.getFunction().toInferenceReference().getVariable() == null) && (expr.getFunction().toInferenceReference() == null || expr.getFunction().toInferenceReference().getVariable() == null)) {
        InferenceVariable variable = null;
        Expression result = null;

        // expr1 == field call
        if (expr1.toFieldCall() != null && expr1.toFieldCall().getExpression().toInferenceReference() != null) {
          variable = expr1.toFieldCall().getExpression().toInferenceReference().getVariable();
          // expr1 == view field call
          if (variable instanceof TypeClassInferenceVariable && ((TypeClassInferenceVariable) variable).getClassifyingField() == expr1.toFieldCall().getDefinition()) {
            result = ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getClassViewInstancePool(), expr);
          }
        }

        // expr2 == field call
        if (variable == null && expr.toFieldCall() != null && expr.toFieldCall().getExpression().toInferenceReference() != null) {
          variable = expr.toFieldCall().getExpression().toInferenceReference().getVariable();
          // expr2 == view field call
          if (variable instanceof TypeClassInferenceVariable && ((TypeClassInferenceVariable) variable).getClassifyingField() == expr.toFieldCall().getDefinition()) {
            result = ((TypeClassInferenceVariable) variable).getInstance(myVisitor.getClassViewInstancePool(), expr1);
          }
        }

        if (result != null) {
          solve(variable, result);
          return;
        }
      }
    }

    // expr1 == ?x && expr2 /= ?y || expr1 /= ?x && expr2 == ?y
    if (inf1 != null && inf2 == null || inf2 != null && inf1 == null) {
      InferenceVariable cInf = inf1 != null ? inf1 : inf2;
      Expression cType = inf1 != null ? expr : type;

      // TODO: set cmp to CMP.EQ only if cExpr is not stuck on a meta-variable
      // cExpr /= Pi, cExpr /= Type, cExpr /= Class, cExpr /= stuck
      if (cType.toPi() == null && cType.toUniverse() == null && cType.toClassCall() == null) {
        cmp = CMP.EQ;
      }

      // ?x == _
      if (cmp == CMP.EQ) {
        solve(cInf, cType);
        return;
      }

      if (inf1 != null) {
        cmp = cmp.not();
      }

      // ?x <> Pi
      DependentLink piParams = cType.getPiParameters();
      if (piParams.hasNext()) {
        InferenceVariable infVar = new DerivedInferenceVariable(cInf.getName() + "-cod", cInf);
        Expression newRef = new InferenceReferenceExpression(infVar, this);
        solve(cInf, new PiExpression(piParams, newRef));
        addEquation(cType.getPiCodomain(), newRef, cmp, sourceNode, infVar);
        return;
      }

      // ?x <> Type
      Sort sort = cType.toSort();
      if (sort != null) {
        InferenceLevelVariable lpInf = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, cInf.getSourceNode());
        InferenceLevelVariable lhInf = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, cInf.getSourceNode());
        myLevelEquations.addVariable(lpInf);
        myLevelEquations.addVariable(lhInf);
        Level lp = new Level(lpInf);
        Level lh = new Level(lhInf);
        solve(cInf, new UniverseExpression(new Sort(lp, lh)));
        if (cmp == CMP.LE) {
          Level.compare(sort.getPLevel(), lp, CMP.LE, this, sourceNode);
          Level.compare(sort.getHLevel(), lh, CMP.LE, this, sourceNode);
        } else {
          if (!sort.getPLevel().isInfinity()) {
            addLevelEquation(lpInf, sort.getPLevel().getVar(), sort.getPLevel().getConstant(), sort.getPLevel().getMaxConstant(), sourceNode);
          }
          if (!sort.getHLevel().isInfinity()) {
            addLevelEquation(lhInf, sort.getHLevel().getVar(), sort.getHLevel().getConstant(), sort.getHLevel().getMaxConstant(), sourceNode);
          }
        }
        return;
      }
    }

    Equation equation;
    if (expr.toInferenceReference() == null && type.toInferenceReference() != null) {
      equation = new Equation(expr, type, cmp.not(), sourceNode);
    } else {
      equation = new Equation(type, expr, cmp, sourceNode);
    }

    myEquations.add(equation);
    stuckVar.addListener(equation);
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
        myLevelEquations.addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, constant, maxConstant));
      }
      return;
    }

    // ?x <= max(_ +- c, +-d) // 10
    if (var1 instanceof InferenceLevelVariable) {
      if (var2 instanceof InferenceLevelVariable) {
        // ?x <= max(?y +- c, +-d) // 4
        myLevelEquations.addEquation(new LevelEquation<>((InferenceLevelVariable) var1, (InferenceLevelVariable) var2, constant, maxConstant < 0 ? null : maxConstant));
      } else {
        // ?x <= max(+-c, +-d), ?x <= max(l +- c, +-d) // 6
        myLevelEquations.addEquation(new LevelEquation<>((InferenceLevelVariable) var1, null, Math.max(constant, maxConstant)));
      }
      return;
    }

    // l <= max(_ +- c, +-d) // 10
    {
      // l <= max(l + c, +-d) // 2
      if (var1 == var2) {
        return;
      }

      // l <= max(?y +- c, +-d) // 4
      if (var2 instanceof InferenceLevelVariable) {
        myBases.put((InferenceLevelVariable) var2, var1);
        if (constant < 0) {
          myLevelEquations.addEquation(new LevelEquation<>(null, (InferenceLevelVariable) var2, constant));
        }
        return;
      }

      // l <= max(l - c, +-d), l <= max(+-c, +-d) // 4
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant, maxConstant)), sourceNode));
    }
  }

  private void addLevelEquation(LevelVariable var, Abstract.SourceNode sourceNode) {
    if (var instanceof InferenceLevelVariable) {
      myLevelEquations.addEquation(new LevelEquation<>((InferenceLevelVariable) var));
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
  public boolean solve(TypeMax type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode) {
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
      myVisitor.getErrorReporter().report(new SolveEquationError<>(type.normalize(NormalizeVisitor.Mode.HUMAN_NF), expr.normalize(NormalizeVisitor.Mode.HUMAN_NF), sourceNode));
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
      addLevelEquation(level1.getVar(), level2.getVar(), level2.getConstant() - level1.getConstant(), level2.getMaxConstant() - level1.getConstant(), sourceNode);
      addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxConstant(), level2.getMaxConstant() - level1.getMaxConstant(), sourceNode);
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), level1.getMaxConstant() - level2.getConstant(), sourceNode);
      addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxConstant(), level1.getMaxConstant() - level2.getMaxConstant(), sourceNode);
    }
    return true;
  }

  @Override
  public boolean add(Expression type, Expression expr, Abstract.SourceNode sourceNode, InferenceVariable stuckVar) {
    addEquation(type, expr, CMP.LE, sourceNode, stuckVar);
    return true;
  }

  @Override
  public boolean addVariable(InferenceLevelVariable var) {
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

    Map<InferenceLevelVariable, Integer> solution = new HashMap<>();
    List<LevelEquation<InferenceLevelVariable>> cycle = myLevelEquations.solve(solution);
    if (cycle != null) {
      List<LevelEquation<LevelVariable>> basedCycle = new ArrayList<>();
      for (LevelEquation<InferenceLevelVariable> equation : cycle) {
        if (equation.isInfinity() || equation.getVariable1() != null) {
          basedCycle.add(new LevelEquation<LevelVariable>(equation));
        } else {
          basedCycle.add(new LevelEquation<>(myBases.get(equation.getVariable2()), equation.getVariable2(), equation.getConstant()));
        }
      }
      LevelEquation<InferenceLevelVariable> lastEquation = cycle.get(cycle.size() - 1);
      InferenceLevelVariable var = lastEquation.getVariable1() != null ? lastEquation.getVariable1() : lastEquation.getVariable2();
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(new ArrayList<LevelEquation<? extends LevelVariable>>(basedCycle), var.getSourceNode()));
    }

    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    for (Map.Entry<InferenceLevelVariable, Integer> entry : solution.entrySet()) {
      Integer constant = entry.getValue();
      assert constant != null || entry.getKey().getType() == LevelVariable.LvlType.HLVL;
      result.add(entry.getKey(), constant == null ? Level.INFINITY : new Level(myBases.get(entry.getKey()), -constant));
    }

    // TODO: Do not add equations with expressions that stuck on errors, i.e. check this in CompareVisitor
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      Expression stuckExpr = equation.expr.getStuckExpression();
      if (stuckExpr instanceof InferenceReferenceExpression || stuckExpr instanceof ErrorExpression) {
        iterator.remove();
      } else
      if (equation.type instanceof Expression) {
        stuckExpr = ((Expression) equation.type).getStuckExpression();
        if (stuckExpr instanceof InferenceReferenceExpression || stuckExpr instanceof ErrorExpression) {
          iterator.remove();
        }
      }
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
      LocalTypeCheckingError error = var.getErrorInfer(expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(expr, error));
      return false;
    }

    Expression expectedType = var.getType();
    Expression actualType = expr.getType();
    if (actualType == null || actualType.isLessOrEquals(expectedType.normalize(NormalizeVisitor.Mode.NF), this, var.getSourceNode())) {
      // TODO: if actualType == null then add equation type_of(var) == type_of(expr)
      var.solve(this, new OfTypeExpression(expr, expectedType));
      return true;
    } else {
      actualType = actualType.normalize(NormalizeVisitor.Mode.HUMAN_NF);
      LocalTypeCheckingError error = var.getErrorMismatch(expectedType.normalize(NormalizeVisitor.Mode.HUMAN_NF), actualType, expr);
      myVisitor.getErrorReporter().report(error);
      var.solve(this, new ErrorExpression(expr, error));
      return false;
    }
  }
}
