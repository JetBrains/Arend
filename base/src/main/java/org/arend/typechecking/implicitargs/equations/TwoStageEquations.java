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
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.LocalError;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.local.SolveEquationError;
import org.arend.typechecking.error.local.SolveEquationsError;
import org.arend.typechecking.error.local.SolveLevelEquationsError;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.SearchVisitor;
import org.arend.util.Pair;

import java.util.*;

public class TwoStageEquations implements Equations {
  private List<Equation> myEquations = new ArrayList<>();
  private final List<LevelEquation<LevelVariable>> myLevelEquations = new ArrayList<>();
  private final List<InferenceLevelVariable> myLevelVariables = new ArrayList<>();
  private final CheckTypeVisitor myVisitor;
  private final List<InferenceVariable> myProps = new ArrayList<>();
  private final List<Pair<InferenceLevelVariable, InferenceLevelVariable>> myBoundVariables = new ArrayList<>();
  private final Map<InferenceVariable, Expression> myNotSolvableFromEquationsVars = new HashMap<>();

  public TwoStageEquations(CheckTypeVisitor visitor) {
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
    if (inf1 == null && inf2 != null && inf2.isSolvableFromEquations() || inf2 == null && inf1 != null && inf1.isSolvableFromEquations()) {
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
          myProps.add(cInf);
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
          if (solve(cInf, cType, false, cInf instanceof TypeClassInferenceVariable, true) != SolveResult.NOT_SOLVED) {
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
            addLevelEquation(genSort.getPLevel().getVar(), sort.getPLevel().getVar(), sort.getPLevel().getConstant(), sort.getPLevel().getMaxConstant(), sourceNode);
          }
          if (!sort.getHLevel().isInfinity()) {
            addLevelEquation(genSort.getHLevel().getVar(), sort.getHLevel().getVar(), sort.getHLevel().getConstant(), sort.getHLevel().getMaxConstant(), sourceNode);
          }
        }
        return true;
      }
    }

    if (cmp == CMP.EQ && (inf1 != null && inf2 == null || inf2 != null && inf1 == null)) {
      Expression prev = myNotSolvableFromEquationsVars.putIfAbsent(inf1 != null ? inf1 : inf2, inf1 != null ? expr2 : expr1);
      if (prev != null) {
        return CompareVisitor.compare(this, CMP.EQ, prev, inf1 != null ? expr2 : expr1, type, sourceNode);
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

  private void addLevelEquation(final LevelVariable var1, LevelVariable var2, int constant, int maxConstant, Concrete.SourceNode sourceNode) {
    // _ <= max(-c, -d), _ <= max(l - c, -d) // 6
    if (!(var2 instanceof InferenceLevelVariable) && maxConstant < 0 && (constant < 0 || constant == 0 && var2 == LevelVariable.HVAR && var1 == null) && !(var2 == null && var1 instanceof InferenceLevelVariable && var1.getType() == LevelVariable.LvlType.HLVL && constant >= -1 && maxConstant >= -1)) {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant)), sourceNode));
      return;
    }

    // l <= max(l - c, +d), l <= max(+-c, +-d) // 4
    if ((var1 == LevelVariable.PVAR || var1 == LevelVariable.HVAR) && !(var2 instanceof InferenceLevelVariable) && (var2 == null || constant < 0)) {
      myVisitor.getErrorReporter().report(new SolveLevelEquationsError(Collections.singletonList(new LevelEquation<>(var1, var2, constant, maxConstant)), sourceNode));
      return;
    }

    myLevelEquations.add(new LevelEquation<>(var1, var2, constant, maxConstant));
  }

  private void addLevelEquation(LevelVariable var, Concrete.SourceNode sourceNode) {
    if (var instanceof InferenceLevelVariable) {
      myLevelEquations.add(new LevelEquation<>(var));
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
      addLevelEquation(level1.getVar(), level2.getVar(), level2.getConstant() - level1.getConstant(), level2.getMaxConstant() - level1.getConstant(), sourceNode);
      if (level1.withMaxConstant() && level1.getMaxConstant() > level2.getMaxConstant()) {
        addLevelEquation(null, level2.getVar(), level2.getConstant() - level1.getMaxConstant(), -1, sourceNode);
      }
    }
    if (cmp == CMP.GE || cmp == CMP.EQ) {
      addLevelEquation(level2.getVar(), level1.getVar(), level1.getConstant() - level2.getConstant(), level1.getMaxConstant() - level2.getConstant(), sourceNode);
      if (level2.withMaxConstant() && level2.getMaxConstant() > level1.getMaxConstant()) {
        addLevelEquation(null, level1.getVar(), level1.getConstant() - level2.getMaxConstant(), -1, sourceNode);
      }
    }
    return true;
  }

  @Override
  public LevelEquationsSolver makeLevelEquationsSolver() {
    return new LevelEquationsSolver(myLevelEquations, myLevelVariables, myBoundVariables, myVisitor.getErrorReporter());
  }

  @Override
  public void finalizeEquations(LevelSubstitution levelSubstitution, Concrete.SourceNode sourceNode) {
    for (Equation equation : myEquations) {
      equation.expr1 = equation.expr1.subst(levelSubstitution);
      equation.expr2 = equation.expr2.subst(levelSubstitution);
      if (equation.type != null) {
        equation.type = equation.type.subst(levelSubstitution);
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
    myProps.clear();
    myNotSolvableFromEquationsVars.clear();
  }

  @Override
  public boolean addVariable(InferenceLevelVariable var) {
    myLevelVariables.add(var);
    return true;
  }

  @Override
  public boolean remove(Equation equation) {
    return myEquations.remove(equation);
  }

  @Override
  public void solveEquations() {
    while (!myProps.isEmpty()) {
      InferenceVariable var = myProps.remove(myProps.size() - 1);
      if (!var.isSolved()) {
        solve(var, new UniverseExpression(Sort.PROP), false, false, true);
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
  public boolean supportsLevels() {
    return true;
  }

  @Override
  public boolean supportsExpressions() {
    return true;
  }

  @Override
  public void saveState(TypecheckerState state) {
    state.equations = new ArrayList<>(myEquations);
    state.numberOfLevelVariables = myLevelVariables.size();
    state.numberOfLevelEquations = myLevelEquations.size();
    state.numberOfProps = myProps.size();
    state.numberOfBoundVars = myBoundVariables.size();
    state.notSolvableFromEquationsVars = new HashSet<>(myNotSolvableFromEquationsVars.keySet());
  }

  @Override
  public void loadState(TypecheckerState state) {
    myEquations = new ArrayList<>(state.equations);
    if (myLevelVariables.size() > state.numberOfLevelVariables) {
      myLevelVariables.subList(state.numberOfLevelVariables, myLevelVariables.size()).clear();
    }
    if (myLevelEquations.size() > state.numberOfLevelEquations) {
      myLevelEquations.subList(state.numberOfLevelEquations, myLevelEquations.size()).clear();
    }
    if (myProps.size() > state.numberOfProps) {
      myProps.subList(state.numberOfProps, myProps.size()).clear();
    }
    if (myBoundVariables.size() > state.numberOfBoundVars) {
      myBoundVariables.subList(state.numberOfBoundVars, myBoundVariables.size()).clear();
    }
    myNotSolvableFromEquationsVars.keySet().retainAll(state.notSolvableFromEquationsVars);
  }

  private boolean solveClassCallsEq() {
    List<Pair<InferenceVariable, Expression>> solved = null;
    for (Iterator<Equation> iterator = myEquations.iterator(); iterator.hasNext(); ) {
      Equation equation = iterator.next();
      if (equation.cmp == CMP.EQ) {
        InferenceVariable var1 = equation.expr1.getInferenceVariable();
        InferenceVariable var2 = equation.expr2.getInferenceVariable();
        if (var1 == null && var2 != null && var2.isSolvableFromEquations() || var2 == null && var1 != null && var1.isSolvableFromEquations()) {
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
        if (var1 != null && var2 != null && var1.isSolvableFromEquations() && var2.isSolvableFromEquations()) {
          bounds.computeIfAbsent(var1, k -> new LinkedHashSet<>()).add(new Wrapper(equation.expr2));
          bounds.computeIfAbsent(var2, k -> new LinkedHashSet<>()).add(new Wrapper(equation.expr1));
        }
        continue;
      }

      InferenceVariable var = (cmp == CMP.LE ? upper : lower).getInferenceVariable();
      InferenceVariable otherVar = (cmp == CMP.LE ? lower : upper).getInferenceVariable();
      if (var != null && var.isSolvableFromEquations()) {
        boolean isClassCall = (cmp == CMP.LE ? lowerClassCall : upperClassCall) != null;
        if (isClassCall || otherVar != null && otherVar.isSolvableFromEquations()) {
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
            if (other == null || !CompareVisitor.compare(wrapper, CMP.EQ, entry.getValue(), other, solution.getDefinition().getFieldType(entry.getKey(), solution.getSortArgument(), thisExpr), pair.proj1.getSourceNode())) {
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
            if (other == null || !CompareVisitor.compare(this, CMP.EQ, entry.getValue(), other, solution.getDefinition().getFieldType(entry.getKey(), solution.getSortArgument(), thisExpr), pair.proj1.getSourceNode())) {
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
        if (var != null && var.isSolvableFromEquations()) {
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

  @Override
  public boolean solve(InferenceVariable var, Expression expr) {
    return solve(var, expr, false, false, false) == SolveResult.SOLVED;
  }

  private enum SolveResult { SOLVED, NOT_SOLVED, ERROR }

  private SolveResult solve(InferenceVariable var, Expression expr, boolean isLowerBound) {
    return solve(var, expr, isLowerBound, false, true);
  }

  private SolveResult solve(InferenceVariable var, Expression expr, boolean isLowerBound, boolean trySolve, boolean fromEquations) {
    assert !fromEquations || var.isSolvableFromEquations();
    if (var.isSolved()) {
      return SolveResult.NOT_SOLVED;
    }

    if (expr.getInferenceVariable() == var) {
      return SolveResult.SOLVED;
    }
    if (myProps.contains(var) && !expr.isInstance(UniverseExpression.class)) {
      LocalError error = var.getErrorInfer(new UniverseExpression(Sort.PROP), expr);
      myVisitor.getErrorReporter().report(error);
      return SolveResult.ERROR;
    }

    if (fromEquations && expr.findBinding(var)) {
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
      var.solve(myVisitor, OfTypeExpression.make(result, actualType, expectedType));
      return SolveResult.SOLVED;
    } else {
      if (trySolve) {
        return SolveResult.NOT_SOLVED;
      } else {
        LocalError error = var.getErrorMismatch(expectedType, actualType, expr);
        myVisitor.getErrorReporter().report(error);
        var.solve(myVisitor, new ErrorExpression(result, error));
        return SolveResult.ERROR;
      }
    }
  }

  private SolveResult inferenceError(InferenceVariable var, Expression expr) {
    LocalError error = var.getErrorInfer(expr);
    myVisitor.getErrorReporter().report(error);
    var.solve(myVisitor, new ErrorExpression(error));
    return SolveResult.ERROR;
  }
}
