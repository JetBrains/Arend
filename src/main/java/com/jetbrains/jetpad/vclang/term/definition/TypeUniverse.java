package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.term.expr.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class TypeUniverse {
  private LevelExpression myPLevel;
  private LevelExpression myHLevel;

  public static final int ANY_LEVEL = -10;
  public static final int NOT_TRUNCATED = -10;
  public static final TypeUniverse PROP = new TypeUniverse(0, -1);
  public static final TypeUniverse SET = new TypeUniverse(ANY_LEVEL, 0);

  public static TypeUniverse SetOfLevel(int level) {
    return new TypeUniverse(level, 0);
  }
  public static TypeUniverse SetOfLevel(LevelExpression level) {
    return new TypeUniverse(level, SET.getHLevel());
  }

  public TypeUniverse(int plevel, int hlevel) {
    if (hlevel == -1) {
      plevel = 0;
    }
    if (plevel != ANY_LEVEL) {
      myPLevel = new LevelExpression(plevel);
    } else {
      myPLevel = new LevelExpression();
    }
    if (hlevel != NOT_TRUNCATED)
      myHLevel = new LevelExpression(hlevel + 1);
    else {
      myHLevel = new LevelExpression();
    }
  }

  public TypeUniverse(LevelExpression plevel, LevelExpression hlevel) {
    myPLevel = hlevel.isZero() ? new LevelExpression(0) : plevel;
    myHLevel = hlevel;
  }

  public TypeUniverse(TypeUniverse universe) {
    myPLevel = new LevelExpression(universe.getPLevel());
    myHLevel = new LevelExpression(universe.getHLevel());
  }

  public LevelExpression getPLevel() {
    return myPLevel;
  }

  public LevelExpression getHLevel() {
    return myHLevel;
  }

  public TypeUniverse max(TypeUniverse other) {
    return new TypeUniverse(myPLevel.max(other.getPLevel()), myHLevel.max(other.getHLevel()));
  }

  public static LevelExpression intToPLevel(int plevel) {
    return new LevelExpression(plevel);
  }

  public static LevelExpression intToHLevel(int hlevel) {
    if (hlevel == NOT_TRUNCATED) return new LevelExpression();
    return new LevelExpression(hlevel + 1);
  }

  public TypeUniverse succ() {
    return isProp() ? SetOfLevel(0) : new TypeUniverse(getPLevel().succ(), getHLevel().succ());
  }

  public boolean isProp() {
    return myHLevel.equals(PROP.getHLevel());
  }

  public boolean isLessOrEquals(TypeUniverse other) {
    if (equals(other)) return true;
    UniverseExpression uni1 = new UniverseExpression(this);
    UniverseExpression uni2 = new UniverseExpression(other);
    return Expression.compare(uni1, uni2, Equations.CMP.LE);
  }

  public TypeUniverse subst(LevelSubstitution subst) {
    LevelExpression plevel = myPLevel;
    LevelExpression hlevel = myHLevel;
    for (Binding var : subst.getDomain()) {
      if (var.getType().toDefCall().getDefinition() == Preprelude.LVL) {
        plevel = plevel.subst(var, subst.get(var));
      } else if (var.getType().toDefCall().getDefinition() == Preprelude.CNAT) {
        hlevel = hlevel.subst(var, subst.get(var));
      }
    }
    return new TypeUniverse(plevel, hlevel);
  }

  public static boolean compare(TypeUniverse uni1, TypeUniverse uni2, Equations.CMP cmp, Equations equations) {
    if (uni1.getHLevel().isZero() || uni2.getHLevel().isZero()) {
      LevelExpression.compare(uni1.getPLevel(), new LevelExpression(0), Equations.CMP.GE, equations);
      LevelExpression.compare(uni2.getPLevel(), new LevelExpression(0), Equations.CMP.GE, equations);
      return LevelExpression.compare(uni1.getHLevel(), uni2.getHLevel(), cmp, equations);
    }
    return LevelExpression.compare(uni1.getPLevel(), uni2.getPLevel(), cmp, equations) && LevelExpression.compare(uni1.getHLevel(), uni2.getHLevel(), cmp, equations);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TypeUniverse)) return false;
    if (isProp() || ((TypeUniverse) other).isProp()) return myHLevel.equals(((TypeUniverse) other).getHLevel());
    if (myPLevel.isInfinity() || ((TypeUniverse) other).getPLevel().isInfinity()) return myHLevel.equals(((TypeUniverse) other).getHLevel());
    return myPLevel.equals(((TypeUniverse) other).getPLevel()) && myHLevel.equals(((TypeUniverse) other).getHLevel());
  }

  @Override
  public String toString() {
    return "\\Type (" + myPLevel + "," + myHLevel + ")";
  }
}
