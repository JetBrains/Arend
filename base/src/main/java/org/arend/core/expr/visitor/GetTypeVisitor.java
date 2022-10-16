package org.arend.core.expr.visitor;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.core.subst.ListLevels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.util.SingletonList;

import java.util.*;

import static org.arend.core.expr.ExpressionFactory.*;

public class GetTypeVisitor implements ExpressionVisitor<Void, Expression> {
  public final static GetTypeVisitor INSTANCE = new GetTypeVisitor(true, false);
  public final static GetTypeVisitor NN_INSTANCE = new GetTypeVisitor(false, false);
  public final static GetTypeVisitor MIN_INSTANCE = new GetTypeVisitor(true, true);

  private final boolean myNormalizing;
  private final boolean myMinimal;

  private GetTypeVisitor(boolean normalizing, boolean minimal) {
    myNormalizing = normalizing;
    myMinimal = minimal;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression result = expr.getFunction().accept(this, null).applyExpression(expr.getArgument(), myNormalizing);
    return result == null ? new ErrorExpression() : result;
  }

  private Level getMaxLevel(Level level1, Level level2) {
    return level2 == null ? level1 : level2.max(level1);
  }

  private boolean matchLevels(Levels paramLevels, Levels argLevels, Map<LevelVariable, Level> levelMap) {
    List<? extends Level> paramList = paramLevels.toList();
    List<? extends Level> argList = argLevels.toList();
    if (paramList.size() != argList.size()) {
      return false;
    }
    for (int i = 0; i < paramList.size(); i++) {
      if (paramList.get(i).getVar() != null) {
        Level level = getMaxLevel(argList.get(i), levelMap.get(paramList.get(i).getVar()));
        if (level == null) {
          return false;
        }
        levelMap.put(paramList.get(i).getVar(), level);
      }
    }
    return true;
  }

  private boolean matchArguments(Expression paramType, Expression argType, Map<LevelVariable, Level> levelMap) {
    int skip = 0;
    while (paramType instanceof PiExpression) {
      skip += DependentLink.Helper.size(((PiExpression) paramType).getParameters());
      paramType = ((PiExpression) paramType).getCodomain();
    }

    if (paramType instanceof UniverseExpression) {
      argType = argType.dropPiParameter(skip);
      argType = argType == null ? null : argType.normalize(NormalizationMode.WHNF);
      if (!(argType instanceof UniverseExpression)) {
        return false;
      }
      Sort paramSort = ((UniverseExpression) paramType).getSort();
      Sort argSort = ((UniverseExpression) argType).getSort();
      return matchLevels(new LevelPair(paramSort.getPLevel(), paramSort.getHLevel()), new LevelPair(argSort.getPLevel(), argSort.getHLevel()), levelMap);
    } else if (paramType instanceof SigmaExpression) {
      argType = argType.dropPiParameter(skip);
      argType = argType == null ? null : argType.normalize(NormalizationMode.WHNF);
      if (!(argType instanceof SigmaExpression)) {
        return false;
      }
      DependentLink paramParam = ((SigmaExpression) paramType).getParameters();
      DependentLink argParam = ((SigmaExpression) argType).getParameters();
      while (paramParam.hasNext() && argParam.hasNext()) {
        if (!matchArguments(paramParam.getTypeExpr(), argParam.getTypeExpr(), levelMap)) {
          return false;
        }
        paramParam = paramParam.getNext();
        argParam = argParam.getNext();
      }
      return !(paramParam.hasNext() || argParam.hasNext());
    } else if (paramType instanceof ClassCallExpression) {
      argType = argType.dropPiParameter(skip);
      argType = argType == null ? null : argType.normalize(NormalizationMode.WHNF);
      if (!(argType instanceof ClassCallExpression)) {
        return false;
      }
      ClassCallExpression paramClassCall = (ClassCallExpression) paramType;
      ClassCallExpression argClassCall = (ClassCallExpression) argType;
      if (paramClassCall.getUniverseKind() != UniverseKind.NO_UNIVERSES && !matchLevels(paramClassCall.getLevels(), minimizeLevelsToSuperClass(argClassCall, paramClassCall.getDefinition()), levelMap)) {
        return false;
      }
      for (Map.Entry<ClassField, Expression> entry : paramClassCall.getImplementedHere().entrySet()) {
        Expression argImpl = argClassCall.getAbsImplementationHere(entry.getKey());
        if (argImpl == null || !matchArguments(entry.getValue(), argImpl, levelMap)) {
          return false;
        }
      }
      return true;
    } else {
      return true;
    }
  }

  public Levels minimizeLevels(LeveledDefCallExpression defCall) {
    if (!(defCall.getUniverseKind() == UniverseKind.NO_UNIVERSES && defCall.getDefinition() != Prelude.DIV_MOD && defCall.getDefinition() != Prelude.MOD && !(defCall instanceof ConCallExpression))) {
      return defCall.getLevels();
    }
    boolean ok = true;
    Map<LevelVariable, Level> levelMap = new HashMap<>();
    if (defCall instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) defCall;
      Levels idLevels = classCall.getDefinition().makeIdLevels();
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        ClassField field = entry.getKey();
        if (classCall.getDefinition().isOmegaField(field)) {
          Levels superLevels = classCall.getDefinition().getSuperLevels().get(field.getParentClass());
          if (superLevels == null) superLevels = idLevels;
          ok = matchArguments(field.getResultType().subst(superLevels.makeSubstitution(field)), entry.getValue().accept(this, null), levelMap);
          if (!ok) break;
        }
      }
    } else {
      DependentLink param = defCall.getDefinition().getParameters();
      List<? extends Expression> defCallArguments = defCall.getDefCallArguments();
      for (int i = 0; i < defCallArguments.size(); i++) {
        ok = !defCall.getDefinition().isOmegaParameter(i) || matchArguments(param.getTypeExpr(), defCallArguments.get(i).accept(this, null), levelMap);
        if (!ok) break;
        param = param.getNext();
      }
    }

    Levels levels = defCall.getLevels();
    if (ok) {
      if (defCall.getDefinition().getLevelParameters() == null) {
        Level pLevel = levelMap.get(LevelVariable.PVAR);
        Level hLevel = levelMap.get(LevelVariable.HVAR);
        levels = new LevelPair(pLevel == null ? new Level(0) : pLevel, hLevel == null ? new Level(-1) : hLevel);
      } else {
        List<Level> list = new ArrayList<>();
        List<? extends LevelVariable> vars = defCall.getDefinition().getLevelParameters();
        for (LevelVariable var : vars) {
          Level level = levelMap.get(var);
          list.add(level == null ? new Level(var.getMinValue()) : level);
        }
        for (int i = 0; i < list.size() - 1; i++) {
          Level maxLevel = list.get(i).max(list.get(i + 1));
          if (maxLevel == null) {
            ok = false;
            break;
          }
          if (vars.get(i).compare(vars.get(i + 1), CMP.LE)) {
            list.set(i + 1, maxLevel);
          } else {
            list.set(i, maxLevel);
          }
        }
        if (ok) {
          levels = new ListLevels(list);
        }
      }
    }
    return levels;
  }

  @Override
  public Expression visitFunCall(FunCallExpression expr, Void params) {
    FunctionDefinition definition = expr.getDefinition();
    List<? extends Expression> arguments = expr.getDefCallArguments();
    if (definition == Prelude.DIV_MOD || definition == Prelude.MOD) {
      Expression arg2 = arguments.get(1);
      IntegerExpression integer = arg2.cast(IntegerExpression.class);
      ConCallExpression conCall = arg2.cast(ConCallExpression.class);
      if (integer != null && !integer.isZero() || conCall != null && conCall.getDefinition() == Prelude.SUC) {
        return definition == Prelude.MOD ? Fin(arg2) : finDivModType(arg2);
      } else {
        return definition == Prelude.MOD ? Nat() : Prelude.DIV_MOD_TYPE;
      }
    }

    List<DependentLink> defParams = new ArrayList<>();
    Expression type = definition.getTypeWithParams(defParams, myMinimal ? minimizeLevels(expr) : expr.getLevels());
    assert arguments.size() == defParams.size();
    return type.subst(DependentLink.Helper.toSubstitution(defParams, arguments));
  }

  @Override
  public UniverseExpression visitDataCall(DataCallExpression expr, Void params) {
    return new UniverseExpression(expr.getDefinition().getSort().subst((myMinimal ? minimizeLevels(expr) : expr.getLevels()).makeSubstitution(expr.getDefinition())));
  }

  private Levels minimizeLevelsToSuperClass(ClassCallExpression classCall, ClassDefinition superClass) {
    Levels argLevels = classCall.getLevels(superClass);
    if (argLevels != classCall.getLevels() && classCall.getUniverseKind() == UniverseKind.NO_UNIVERSES) {
      Map<ClassField, Expression> impls = new LinkedHashMap<>();
      ClassCallExpression newClassCall = new ClassCallExpression(superClass, argLevels, impls, classCall.getSort(), UniverseKind.NO_UNIVERSES);
      for (Map.Entry<ClassField, AbsExpression> entry : classCall.getDefinition().getImplemented()) {
        if (entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && superClass.isSubClassOf(entry.getKey().getParentClass())) {
          impls.put(entry.getKey(), entry.getValue().apply(new ReferenceExpression(classCall.getThisBinding()), LevelSubstitution.EMPTY).accept(new FieldCallSubstVisitor(classCall, new ReferenceExpression(newClassCall.getThisBinding())), null));
        }
      }
      for (Map.Entry<ClassField, Expression> entry : classCall.getImplementedHere().entrySet()) {
        if (entry.getKey().getUniverseKind() != UniverseKind.NO_UNIVERSES && superClass.isSubClassOf(entry.getKey().getParentClass())) {
          impls.put(entry.getKey(), entry.getValue().subst(classCall.getThisBinding(), new ReferenceExpression(newClassCall.getThisBinding())));
        }
      }
      return minimizeLevels(newClassCall);
    } else {
      return classCall.getDefinition().castLevels(superClass, minimizeLevels(classCall));
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression type = expr.getArgument().accept(this, null);
    if (myNormalizing) {
      type = type.normalize(NormalizationMode.WHNF);
    }
    if (type instanceof ClassCallExpression) {
      ClassCallExpression classCall = (ClassCallExpression) type;
      if (classCall.getDefinition().getOverriddenType(expr.getDefinition()) != null) {
        return classCall.getDefinition().getOverriddenType(expr.getDefinition(), myMinimal ? minimizeLevels(classCall) : classCall.getLevels()).applyExpression(expr.getArgument());
      }
      if (myMinimal) {
        return expr.getDefinition().getType(minimizeLevelsToSuperClass(classCall, expr.getDefinition().getParentClass())).applyExpression(expr.getArgument());
      } else {
        return expr.getDefinition().getType(classCall.getDefinition().castLevels(expr.getDefinition().getParentClass(), classCall.getLevels())).applyExpression(expr.getArgument());
      }
    }
    return new ErrorExpression();
  }

  @Override
  public DataCallExpression visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDefinition() == Prelude.SUC) {
      int sucs = 1;
      Expression expression = expr.getDefCallArguments().get(0);
      while (expression instanceof ConCallExpression && ((ConCallExpression) expression).getDefinition() == Prelude.SUC) {
        sucs++;
        expression = ((ConCallExpression) expression).getDefCallArguments().get(0);
      }
      Expression argType = expression.accept(this, null);
      if (myNormalizing) argType = argType.normalize(NormalizationMode.WHNF);
      DataCallExpression dataCall = argType.cast(DataCallExpression.class);
      if (dataCall != null && dataCall.getDefinition() == Prelude.FIN) {
        Expression arg = dataCall.getDefCallArguments().get(0);
        for (int i = 0; i < sucs; i++) {
          arg = Suc(arg);
        }
        return DataCallExpression.make(dataCall.getDefinition(), dataCall.getLevels(), new SingletonList<>(arg));
      }
      return Nat();
    }
    return expr.getDefinition().getDataTypeExpression(expr.getLevels(), expr.getDataTypeArguments());
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().subst((myMinimal ? minimizeLevels(expr) : expr.getLevels()).makeSubstitution(expr.getDefinition())));
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getTypeExpr();
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    return expr.getSubstExpression() != null ? expr.getSubstExpression().accept(this, null) : expr.getVariable().getType();
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    return expr.getExpression().accept(this, null).subst(expr.getSubstitution(), expr.getLevelSubstitution());
  }

  @Override
  public Expression visitLam(LamExpression expr, Void ignored) {
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    Sort sort1 = expr.getParameters().getTypeExpr().accept(this, null).toSort();
    Sort sort2 = sort1 == null ? null : expr.getCodomain().accept(this, null).toSort();
    Level maxPLevel = sort2 == null ? null : sort1.getPLevel().max(sort2.getPLevel());
    return new UniverseExpression(maxPLevel == null ? expr.getResultSort() : new Sort(maxPLevel, sort2.getHLevel()));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    Sort maxSort = Sort.PROP;
    for (DependentLink param = expr.getParameters(); param.hasNext(); param = param.getNext()) {
      param = param.getNextTyped(null);
      Sort sort = param.getTypeExpr().accept(this, null).toSort();
      maxSort = sort == null ? null : maxSort.max(sort);
      if (maxSort == null) {
        break;
      }
    }
    return new UniverseExpression(maxSort == null ? expr.getSort() : maxSort);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpression() == null ? expr : expr.replaceExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getSigmaType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Expression type = expr.getExpression().accept(this, null);
    if (myNormalizing) {
      type = type.normalize(NormalizationMode.WHNF);
    } else {
      type = type.getUnderlyingExpression();
    }
    if (!(type instanceof SigmaExpression)) {
      return type instanceof ErrorExpression ? type : new ErrorExpression();
    }

    DependentLink params = ((SigmaExpression) type).getParameters();
    if (expr.getField() == 0) {
      return params.getTypeExpr();
    }

    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < expr.getField(); i++) {
      subst.add(params, ProjExpression.make(expr.getExpression(), i));
      params = params.getNext();
    }
    return params.getTypeExpr().subst(subst);
  }

  @Override
  public ClassCallExpression visitNew(NewExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitLet(LetExpression expr, Void params) {
    List<HaveClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (HaveClause clause : expr.getClauses()) {
      if (!(clause instanceof LetClause)) {
        clauses.add(clause);
      }
    }
    Expression result = expr.getExpression().accept(this, null);
    return clauses.isEmpty() ? result : new LetExpression(expr.isStrict(), clauses, result);
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    return expr.getResultType().subst(DependentLink.Helper.toSubstitution(expr.getParameters(), expr.getArguments()));
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getTypeOf();
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return Fin(expr.suc());
  }

  @Override
  public Expression visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitTypeDestructor(TypeDestructorExpression expr, Void params) {
    Expression type = expr.getArgument().accept(this, null);
    if (myNormalizing) {
      type = type.normalize(NormalizationMode.WHNF);
    } else {
      type = type.getUnderlyingExpression();
    }
    if (!(type instanceof FunCallExpression && ((FunCallExpression) type).getDefinition() == expr.getDefinition())) {
      return type instanceof ErrorExpression ? type : new ErrorExpression();
    }

    FunCallExpression funCall = (FunCallExpression) type;
    return NormalizeVisitor.INSTANCE.visitBody(funCall.getDefinition().getActualBody(), funCall.getDefCallArguments(), funCall, NormalizationMode.WHNF);
  }

  @Override
  public Expression visitArray(ArrayExpression expr, Void params) {
    Map<ClassField, Expression> implementations = new LinkedHashMap<>();
    if (expr.getTail() == null) {
      implementations.put(Prelude.ARRAY_LENGTH, new SmallIntegerExpression(expr.getElements().size()));
    } else {
      Expression tailType = expr.getTail().accept(this, null).getUnderlyingExpression();
      Expression length = null;
      if (tailType instanceof ClassCallExpression && ((ClassCallExpression) tailType).getDefinition() == Prelude.DEP_ARRAY) {
        length = ((ClassCallExpression) tailType).getImplementationHere(Prelude.ARRAY_LENGTH, expr.getTail());
      }
      if (length == null) {
        length = FieldCallExpression.make(Prelude.ARRAY_LENGTH, expr.getTail());
      }
      length = length.getUnderlyingExpression();
      if (length instanceof IntegerExpression) {
        length = ((IntegerExpression) length).plus(expr.getElements().size());
      } else {
        for (Expression ignored : expr.getElements()) {
          length = Suc(length);
        }
      }
      implementations.put(Prelude.ARRAY_LENGTH, length);
    }
    implementations.put(Prelude.ARRAY_ELEMENTS_TYPE, expr.getElementsType());
    return new ClassCallExpression(Prelude.DEP_ARRAY, expr.getLevels(), implementations, Sort.STD, UniverseKind.NO_UNIVERSES);
  }

  @Override
  public Expression visitPath(PathExpression expr, Void params) {
    Expression left = AppExpression.make(expr.getArgument(), ExpressionFactory.Left(), true);
    Expression right = AppExpression.make(expr.getArgument(), ExpressionFactory.Right(), true);
    return DataCallExpression.make(Prelude.PATH, expr.getLevels(), Arrays.asList(expr.getArgumentType(), left, right));
  }

  @Override
  public Expression visitAt(AtExpression expr, Void params) {
    Expression type = expr.getPathArgument().accept(this, null);
    type = myNormalizing ? type.normalize(NormalizationMode.WHNF) : type.getUnderlyingExpression();
    if (!(type instanceof DataCallExpression && ((DataCallExpression) type).getDefinition() == Prelude.PATH)) {
      return type instanceof ErrorExpression ? type : new ErrorExpression();
    }
    return AppExpression.make(((DataCallExpression) type).getDefCallArguments().get(0), expr.getIntervalArgument(), true);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    Expression normExpr = expr.eval();
    if (normExpr == null) {
      return new ErrorExpression();
    }

    Expression type = expr.getExpression().accept(this, null);
    Sort sort = type.getSortOfType();
    if (sort == null) {
      return new ErrorExpression();
    }

    List<Expression> args = new ArrayList<>(3);
    args.add(type);
    args.add(expr.getExpression());
    args.add(normExpr);
    return FunCallExpression.make(Prelude.PATH_INFIX, new LevelPair(sort.getPLevel(), sort.getHLevel()), args);
  }

  @Override
  public Expression visitBox(BoxExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
