package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class TypeUniverseNew {
  private LevelExpression myPLevel;
  private LevelExpression myHLevel;

  public static final int ANY_LEVEL = -10;
  public static final int NOT_TRUNCATED = -10;
  public static final TypeUniverseNew PROP = new TypeUniverseNew(0, -1);
  public static final TypeUniverseNew SET = new TypeUniverseNew(ANY_LEVEL, -1);

  public static TypeUniverseNew SetOfLevel(int level) {
    return new TypeUniverseNew(level, -1);
  }

  private static class LvlConverter implements LevelExpression.Converter {
    @Override
    public Expression getType() {
      return ExpressionFactory.Lvl();
    }

    @Override
    public Expression convert(Binding var, int sucs) {
      return Preprelude.applyNumberOfSuc(ExpressionFactory.Reference(var), Preprelude.SUC_LVL, sucs);
    }

    @Override
    public Expression convert(int sucs) {
      return Preprelude.applyNumberOfSuc(ExpressionFactory.ZeroLvl(), Preprelude.SUC_LVL, sucs);
    }

    @Override
    public Expression convert() {
      return null;
    }

    @Override
    public LevelExpression convert(Expression expr) {
      return TypeUniverseNew.exprToPLevel(expr);
    }

    @Override
    public Expression max(Expression expr1, Expression expr2) {
      return ExpressionFactory.MaxLvl(expr1, expr2);
    }
  }

  private static class CNatConverter implements LevelExpression.Converter {
    @Override
    public Expression getType() {
      return ExpressionFactory.CNat();
    }

    @Override
    public Expression convert(Binding var, int sucs) {
      return Preprelude.applyNumberOfSuc(ExpressionFactory.Reference(var), Preprelude.SUC_CNAT, sucs);
    }

    @Override
    public Expression convert(int sucs) {
      return Preprelude.applyNumberOfSuc(ExpressionFactory.Fin(ExpressionFactory.Zero()), Preprelude.SUC_CNAT, sucs);
    }

    @Override
    public Expression convert() {
      return ExpressionFactory.Inf();
    }

    @Override
    public LevelExpression convert(Expression expr) {
      return TypeUniverseNew.exprToHLevel(expr);
    }

    @Override
    public Expression max(Expression expr1, Expression expr2) {
      return ExpressionFactory.MaxCNat(expr1, expr2);
    }
  }

  public TypeUniverseNew(int plevel, int hlevel) {
    if (plevel != ANY_LEVEL) {
      myPLevel = new LevelExpression(plevel, new LvlConverter());
    } else {
      myPLevel = new LevelExpression(new LvlConverter());
    }
    if (hlevel != NOT_TRUNCATED)
      myHLevel = new LevelExpression(hlevel + 1, new CNatConverter());
    else {
      myHLevel = new LevelExpression(new CNatConverter());
    }
  }

  public TypeUniverseNew(LevelExpression plevel, LevelExpression hlevel) {
    myPLevel = plevel;
    myHLevel = hlevel;
  }

  public TypeUniverseNew(Expression plevel, Expression hlevel) {
    myPLevel = exprToPLevel(plevel);
    myHLevel = exprToHLevel(hlevel);
  }

  public LevelExpression getPLevel() {
    return myPLevel;
  }

  public LevelExpression getHLevel() {
    return myHLevel;
  }

  /*public Expression getLevel() {
    return ExpressionFactory.Level(myPLevel, myHLevel);
  } /**/

  public TypeUniverseNew max(TypeUniverseNew other) {
    return new TypeUniverseNew(myPLevel.max(other.getPLevel()), myHLevel.max(other.getHLevel()));
  }

  public static LevelExpression intToPLevel(int plevel) {
    return new LevelExpression(plevel, new LvlConverter());
  }

  public static LevelExpression intToHLevel(int hlevel) {
    return new LevelExpression(hlevel, new CNatConverter());
  }

  public static LevelExpression exprToPLevel(Expression plevel) {
    LevelExpression alreadyLevel = plevel.toLevel();
    if (alreadyLevel != null) return alreadyLevel;
    Preprelude.SucExtrResult extrResult = Preprelude.extractSuc(plevel, Preprelude.SUC_LVL);
    ConCallExpression conCall = extrResult.Arg.toConCall();
    if (conCall != null && conCall.getDefinition() == Preprelude.ZERO_LVL) {
      return new LevelExpression(extrResult.NumSuc, new LvlConverter());
    }
    ReferenceExpression ref = extrResult.Arg.toReference();
    if (ref == null) {
      return null;
    }
    return new LevelExpression(ref.getBinding(), extrResult.NumSuc, new LvlConverter());
  }

  public static LevelExpression exprToHLevel(Expression hlevel) {
    LevelExpression alreadyLevel = hlevel.toLevel();
    if (alreadyLevel != null) return alreadyLevel;
    Preprelude.SucExtrResult extrCNatSuc = Preprelude.extractSuc(hlevel, Preprelude.SUC_CNAT);
    ConCallExpression conCall = extrCNatSuc.Arg.getFunction().toConCall();
    if (conCall != null && conCall.getDefinition() == Preprelude.INF) {
      return new LevelExpression(new CNatConverter());
    }
    if (conCall != null && conCall.getDefinition() == Preprelude.FIN) {
      if (extrCNatSuc.Arg.getArguments().size() != 1) {
        return null;
      }
      Preprelude.SucExtrResult extrNatSuc = Preprelude.extractSuc(extrCNatSuc.Arg.getArguments().get(0), Preprelude.SUC);
      ConCallExpression mustBeZero = extrNatSuc.Arg.toConCall();
      if (mustBeZero == null || mustBeZero.getDefinition() != Preprelude.ZERO) {
        return null;
      }
      return new LevelExpression(extrCNatSuc.NumSuc + extrNatSuc.NumSuc, new CNatConverter());
    }
    ReferenceExpression ref = extrCNatSuc.Arg.toReference();
    if (ref == null) {
      return null;
    }
    return new LevelExpression(ref.getBinding(), extrCNatSuc.NumSuc, new CNatConverter());
  }

  public TypeUniverseNew succ() {
    return new TypeUniverseNew(getPLevel().succ(), getHLevel().succ());
  }

  public boolean isProp() {
    return myHLevel.equals(PROP.getHLevel());
  }

  public boolean isLessOrEquals(TypeUniverseNew other) {
    if (equals(other)) return true;
    UniverseExpression uni1 = new UniverseExpression(this);
    UniverseExpression uni2 = new UniverseExpression(other);
    return Expression.compare(uni1, uni2, Equations.CMP.LE);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TypeUniverseNew)) return false;
    if (isProp() || ((TypeUniverseNew) other).isProp()) return myHLevel.equals(((TypeUniverseNew) other).getHLevel());
    if (myPLevel.isInfinity() || ((TypeUniverseNew) other).getPLevel().isInfinity()) return myHLevel.equals(((TypeUniverseNew) other).getHLevel());
    return myPLevel.equals(((TypeUniverseNew) other).getPLevel()) && myHLevel.equals(((TypeUniverseNew) other).getHLevel());
  }
}
