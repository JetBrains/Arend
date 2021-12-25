package org.arend.typechecking.visitor;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.core.subst.ListLevels;
import org.arend.ext.core.ops.CMP;
import org.arend.typechecking.implicitargs.equations.DummyEquations;

import java.util.*;

public class FixLevelParameters extends VoidExpressionVisitor<Void> {
  private final Set<? extends Definition> myDefinitions;
  private final boolean myRemovePLevels;
  private final boolean myRemoveHLevels;

  private FixLevelParameters(Set<? extends Definition> definitions, boolean removePLevels, boolean removeHLevels) {
    myDefinitions = definitions;
    myRemovePLevels = removePLevels;
    myRemoveHLevels = removeHLevels;
  }

  public static void fix(Set<? extends Definition> definitions) {
    for (Definition definition : definitions) {
      if (definition.hasNonTrivialPLevelParameters() && definition.hasNonTrivialHLevelParameters()) return;
    }

    Set<Definition> extendedDefs = new HashSet<>();
    for (Definition definition : definitions) {
      if (definition instanceof DataDefinition) {
        boolean found = false;
        for (Constructor constructor : ((DataDefinition) definition).getConstructors()) {
          if (constructor.getBody() != null) {
            found = true;
            break;
          }
        }
        if (found) {
          extendedDefs.addAll(((DataDefinition) definition).getConstructors());
        }
      }
    }
    extendedDefs.addAll(definitions);

    FindLevelParameters visitor = new FindLevelParameters(extendedDefs);
    for (Definition definition : definitions) {
      if (definition.hasNonTrivialPLevelParameters()) {
        visitor.hasPLevels = true;
      }
      if (definition.hasNonTrivialHLevelParameters()) {
        visitor.hasHLevels = true;
      }
    }
    if (visitor.hasPLevels && visitor.hasHLevels) return;
    for (Definition definition : definitions) {
      if (definition.accept(visitor, null)) break;
    }
    if (visitor.hasPLevels && visitor.hasHLevels) return;

    for (Definition definition : definitions) {
      if (!visitor.hasPLevels && !visitor.hasHLevels) {
        definition.setLevelParameters(Collections.emptyList());
      } else if (definition.getLevelParameters() == null) {
        definition.setLevelParameters(Collections.singletonList(visitor.hasPLevels ? LevelVariable.PVAR : LevelVariable.HVAR));
      } else {
        List<LevelVariable> params = new ArrayList<>(definition.getLevelParameters());
        if (!visitor.hasPLevels) {
          params.subList(0, definition.getNumberOfPLevelParameters()).clear();
        } else {
          params.subList(definition.getNumberOfPLevelParameters(), definition.getLevelParameters().size()).clear();
        }
        definition.setLevelParameters(params);
      }
    }

    FixLevelParameters fixer = new FixLevelParameters(extendedDefs, !visitor.hasPLevels, !visitor.hasHLevels);
    for (Definition definition : definitions) {
      definition.accept(fixer, null);
    }
  }

  public static void fix(Expression expr) {
    expr.accept(new FixLevelParameters(null, false, false), null);
  }

  private static void removeLevels(LeveledDefCallExpression defCall, boolean removePLevels, boolean removeHLevels) {
    Levels levels;
    if (removePLevels && removeHLevels) {
      levels = Levels.EMPTY;
    } else {
      List<Level> list;
      List<? extends Level> oldList = defCall.getLevels().toList();
      if (removePLevels) {
        list = new ArrayList<>(oldList.subList(oldList.size() - defCall.getDefinition().getLevelParameters().size(), oldList.size()));
      } else {
        list = new ArrayList<>(oldList.subList(0, defCall.getDefinition().getLevelParameters().size()));
      }
      levels = new ListLevels(list);
    }
    defCall.setLevels(levels);
  }

  private void processDefCall(LeveledDefCallExpression defCall) {
    if (myDefinitions == null) {
      List<? extends LevelVariable> params = defCall.getDefinition().getLevelParameters();
      if (params != null && (defCall.getLevels() instanceof LevelPair || defCall.getLevels().toList().size() != params.size())) {
        removeLevels(defCall, params.isEmpty() || params.get(0).getType() == LevelVariable.LvlType.HLVL, params.isEmpty() || params.get(0).getType() == LevelVariable.LvlType.PLVL);
      }
    } else if (myDefinitions.contains(defCall.getDefinition())) {
      removeLevels(defCall, myRemovePLevels, myRemoveHLevels);
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    if (expr instanceof LeveledDefCallExpression) {
      processDefCall((LeveledDefCallExpression) expr);
    }
    return super.visitDefCall(expr, params);
  }

  @Override
  protected void processConCall(ConCallExpression expr, Void params) {
    processDefCall(expr);
  }

  private Level removeVars(Level level) {
    return level.isClosed() ? level : new Level(Math.max(level.getConstant(), level.getMaxConstant()));
  }

  private Sort removeVars(Sort sort) {
    return new Sort(myRemovePLevels ? removeVars(sort.getPLevel()) : sort.getPLevel(), myRemoveHLevels ? removeVars(sort.getHLevel()) : sort.getHLevel());
  }

  private LevelPair removeVars(LevelPair levels) {
    return new LevelPair(myRemovePLevels ? removeVars(levels.getPLevel()) : levels.getPLevel(), myRemoveHLevels ? removeVars(levels.getHLevel()) : levels.getHLevel());
  }

  @Override
  public Void visitLam(LamExpression expr, Void param) {
    expr.setResultSort(removeVars(expr.getResultSort()));
    return super.visitLam(expr, param);
  }

  @Override
  public Void visitPi(PiExpression expr, Void param) {
    expr.setResultSort(removeVars(expr.getResultSort()));
    return super.visitPi(expr, param);
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void param) {
    expr.setSort(removeVars(expr.getSort()));
    return super.visitSigma(expr, param);
  }

  @Override
  public Void visitArray(ArrayExpression expr, Void params) {
    expr.setLevels(removeVars(expr.getLevels()));
    return super.visitArray(expr, params);
  }

  @Override
  public Void visitPath(PathExpression expr, Void params) {
    expr.setLevels(removeVars(expr.getLevels()));
    return super.visitPath(expr, params);
  }

  @Override
  public Void visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    if (expr.getLevels() instanceof LevelPair) {
      expr.setLevels(removeVars((LevelPair) expr.getLevels()));
    } else {
      List<Level> list = new ArrayList<>();
      List<LevelVariable> levelParameters = expr.getDefinition().getLevelParameters();
      List<? extends Level> oldList = expr.getLevels().toList();
      for (int i = 0; i < levelParameters.size(); i++) {
        if (myRemovePLevels && levelParameters.get(i).getType() == LevelVariable.LvlType.PLVL || myRemoveHLevels && levelParameters.get(i).getType() == LevelVariable.LvlType.HLVL) {
          list.add(removeVars(oldList.get(i)));
        } else {
          list.add(oldList.get(i));
        }
      }
      expr.setLevels(new ListLevels(list));
    }
    return super.visitTypeConstructor(expr, params);
  }

  @Override
  public Void visitData(DataDefinition def, Void params) {
    def.setSort(removeVars(def.getSort()));
    return super.visitData(def, params);
  }

  @Override
  public Void visitClass(ClassDefinition def, Void params) {
    Levels idLevels = def.makeIdLevels();
    def.getSuperLevels().entrySet().removeIf(entry -> entry.getValue().compare(idLevels, CMP.EQ, DummyEquations.getInstance(), null));
    for (Map.Entry<ClassField, AbsExpression> entry : def.getImplemented()) {
      if (entry.getValue().getBinding() != null) {
        entry.getValue().getBinding().getTypeExpr().accept(this, null);
      }
    }
    for (Map.Entry<ClassField, AbsExpression> entry : def.getDefaults()) {
      if (entry.getValue().getBinding() != null) {
        entry.getValue().getBinding().getTypeExpr().accept(this, null);
      }
    }
    return super.visitClass(def, params);
  }
}
