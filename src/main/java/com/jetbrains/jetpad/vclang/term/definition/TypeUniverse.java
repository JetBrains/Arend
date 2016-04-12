package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.exprorder.CNatOrder;
import com.jetbrains.jetpad.vclang.typechecking.exprorder.ExpressionOrder;
import com.jetbrains.jetpad.vclang.typechecking.exprorder.LevelOrder;
import com.jetbrains.jetpad.vclang.typechecking.exprorder.StandardOrder;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class TypeUniverse extends BaseUniverse<TypeUniverse, TypeUniverse.TypeLevel> {
  public static final TypeUniverse PROP = new TypeUniverse(new TypeLevel(HomotopyLevel.PROP, true));
  public static final TypeUniverse SET = new TypeUniverse(new TypeLevel(HomotopyLevel.SET, true));

  public static TypeUniverse SetOfLevel(int level) { return new TypeUniverse(new TypeLevel(level, 0)); }

  public static class PredicativeLevel implements Universe.Level<PredicativeLevel> {
    private Expression myLevel;

    public PredicativeLevel() {
      myLevel = ZeroLvl();
    }

    public PredicativeLevel(Expression level) {
      myLevel = level;
    }

    public PredicativeLevel(int level) {
      myLevel = ZeroLvl();
      for (int i = 0; i < level; ++i) {
        myLevel = SucLvl(myLevel);
      }
    }

    public Expression getValue() {
      return myLevel;
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof PredicativeLevel) && compare((PredicativeLevel) other) == Cmp.EQUALS;
    }

    @Override
    public Cmp compare(PredicativeLevel other) {
      Expression otherLevel = other.myLevel;
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      otherLevel = otherLevel.normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, otherLevel)) return Cmp.EQUALS;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.GE)) return Cmp.GREATER;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.LE)) return Cmp.LESS;
      return Cmp.NOT_COMPARABLE;
    }

    /*
    @Override
    public boolean compare(PredicativeLevel other, CompareVisitor visitor, Equations.CMP expectedCMP) {
      Expression otherLevel = other.myLevel;
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      otherLevel = otherLevel.normalize(NormalizeVisitor.Mode.NF);
      return visitor.compare(myLevel, otherLevel);
    } /**/

    @Override
    public PredicativeLevel max(PredicativeLevel other) {
      return new PredicativeLevel(MaxLvl(myLevel, other.myLevel));
    }

    @Override
    public PredicativeLevel succ() {
      return new PredicativeLevel(SucLvl(myLevel));
    }
  }

  public static class HomotopyLevel implements Universe.Level<HomotopyLevel> {
    private Expression myLevel;

    public static final HomotopyLevel NOT_TRUNCATED = new HomotopyLevel(Inf());
    public static final HomotopyLevel PROP = new HomotopyLevel(Fin(Zero()));
    public static final HomotopyLevel SET = new HomotopyLevel(Fin(Suc(Zero())));

    public HomotopyLevel() {
      myLevel = PROP.myLevel;
    }

    public HomotopyLevel(Expression level) {
      myLevel = level;
    }

    public HomotopyLevel(int level) {
      myLevel = Zero();
      for (int i = -1; i < level; ++i) {
        myLevel = Suc(myLevel);
      }
      myLevel = Fin(myLevel);
    }

    public Expression getValue() {
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      return myLevel;
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof HomotopyLevel) && compare((HomotopyLevel) other) == Cmp.EQUALS;
    }

    @Override
    public Cmp compare(HomotopyLevel other) {
      Expression otherLevel = other.myLevel;
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      otherLevel = otherLevel.normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, otherLevel)) return Cmp.EQUALS;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.GE)) return Cmp.GREATER;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.LE)) return Cmp.LESS;
      return Cmp.NOT_COMPARABLE;
    }

    /*
    @Override
    public Boolean compare(HomotopyLevel other, CompareVisitor visitor, Equations.CMP expectedCMP) {
      /*myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      other.myLevel = other.myLevel.normalize(NormalizeVisitor.Mode.NF);
      if (checkLevelExprsAreEqual(myLevel, other.myLevel, visitor)) return Cmp.EQUALS;

      if (checkLevelExprsAreEqual(myLevel, PROP.getValue(), visitor)) return Cmp.LESS;
      if (checkLevelExprsAreEqual(other.myLevel, PROP.getValue(), visitor)) return Cmp.GREATER;

      if (checkLevelExprsAreEqual(myLevel, NOT_TRUNCATED.myLevel, visitor)) return Cmp.GREATER;
      if (checkLevelExprsAreEqual(other.myLevel, NOT_TRUNCATED.myLevel, visitor)) return Cmp.LESS;

      Expression maxHlevel = MaxCNat(myLevel, other.myLevel).normalize(NormalizeVisitor.Mode.NF);
      if (checkLevelExprsAreEqual(myLevel, maxHlevel, visitor)) return Cmp.GREATER;
      if (checkLevelExprsAreEqual(other.myLevel, maxHlevel, visitor)) return Cmp.LESS;
      return Cmp.UNKNOWN; /*
      Expression otherLevel = other.myLevel;
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      otherLevel = otherLevel.normalize(NormalizeVisitor.Mode.NF);
      return visitor.compare(myLevel, otherLevel);
    } /**/

    @Override
    public HomotopyLevel max(HomotopyLevel other) {
      return new HomotopyLevel(CNatOrder.maxCNat(myLevel, other.myLevel));
    }

    @Override
    public HomotopyLevel succ() {
      return new HomotopyLevel(SucCNat(myLevel));
    }
  }

  public static class TypeLevel implements Universe.Level<TypeLevel> {
    private PredicativeLevel myPLevel;
    private HomotopyLevel myHLevel;
    private boolean myIgnorePLevel = false;

    private Expression myLevel = null;

    public TypeLevel() {
      myPLevel = new PredicativeLevel();
      myHLevel = new HomotopyLevel();
      myIgnorePLevel = true;
    }

    public TypeLevel(HomotopyLevel hlevel, boolean ignorePLevel) {
      myPLevel = new PredicativeLevel();
      myHLevel = hlevel;
      myIgnorePLevel = ignorePLevel;
    }

    public TypeLevel(Expression plevel, Expression hlevel) {
      myPLevel = new PredicativeLevel(plevel);
      myHLevel = new HomotopyLevel(hlevel);
      myIgnorePLevel = myHLevel.equals(HomotopyLevel.PROP);
    }

    public TypeLevel(Expression level) {
      myLevel = level;
      myIgnorePLevel = getHLevel().equals(HomotopyLevel.PROP);
    }

    public TypeLevel(int plevel, int hlevel) {
      myPLevel = new PredicativeLevel(plevel);
      myHLevel = new HomotopyLevel(hlevel);
      myIgnorePLevel = getHLevel().equals(HomotopyLevel.PROP);
    }

    public TypeLevel(PredicativeLevel plevel, HomotopyLevel hlevel) {
      myPLevel = plevel;
      myHLevel = hlevel;
      myIgnorePLevel = getHLevel().equals(HomotopyLevel.PROP);
    }

    public PredicativeLevel getPLevel() {
      if (myPLevel != null) {
        return myPLevel;
      }
      myPLevel = new PredicativeLevel(PLevel().applyThis(myLevel).normalize(NormalizeVisitor.Mode.NF));
      return myPLevel;
    }

    public HomotopyLevel getHLevel() {
      if (myHLevel != null) {
        return myHLevel;
      }
      myHLevel = new HomotopyLevel(HLevel().applyThis(myLevel).normalize(NormalizeVisitor.Mode.NF));
      return myHLevel;
    }

    public Expression getValue() {
      if (myLevel != null) {
        myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
        return myLevel;
      }
      return Level(myPLevel.getValue(), myHLevel.getValue());
    }

    //public boolean getIgnorePLevel() { return myIgnorePLevel || getHLevel().equals(HomotopyLevel.PROP); }

    @Override
    public Cmp compare(TypeLevel other) {
      if (myIgnorePLevel || other.myIgnorePLevel) {
        return getHLevel().compare(other.getHLevel());
      }

      Expression otherLevel = other.getValue();
      myLevel = getValue();
      if (Expression.compare(myLevel, otherLevel)) return Cmp.EQUALS;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.GE)) return Cmp.GREATER;
      if (Expression.compare(myLevel, otherLevel, Equations.CMP.LE)) return Cmp.LESS;
      return Cmp.NOT_COMPARABLE;

      /*

      Cmp r1 = myIgnorePLevel || other.myIgnorePLevel ? Cmp.EQUALS : getPLevel().compare(other.getPLevel(), visitor);
      Cmp r2 = getHLevel().compare(other.getHLevel(), visitor);
      if (r1 == Cmp.UNKNOWN || r2 == Cmp.UNKNOWN) {
        return Cmp.UNKNOWN;
      }
      if (r1 == Cmp.LESS) {
        return r2 == Cmp.LESS || r2 == Cmp.EQUALS ? Cmp.LESS : Cmp.NOT_COMPARABLE;
      }
      if (r1 == Cmp.GREATER) {
        return r2 == Cmp.GREATER || r2 == Cmp.EQUALS ? Cmp.GREATER : Cmp.NOT_COMPARABLE;
      }
      return r2; /**/
    }

    /*
    @Override
    public boolean compare(TypeLevel other, CompareVisitor visitor, Equations.CMP expectedCMP) {
      if (myIgnorePLevel) {
        myPLevel = other.myPLevel;
      } else if (other.myIgnorePLevel) {
        other.myPLevel = myPLevel;
      }

      Expression otherLevel = other.getValue().normalize(NormalizeVisitor.Mode.NF);
      Expression thisLevel = getValue().normalize(NormalizeVisitor.Mode.NF);
      return visitor.compare(thisLevel, otherLevel);
    } /**/

    @Override
    public TypeLevel max(TypeLevel other) {
      if (myIgnorePLevel) {
        return new TypeLevel(other.getPLevel(), getHLevel().max(other.getHLevel()));
      }
      if (other.myIgnorePLevel) {
        return new TypeLevel(getPLevel(), getHLevel().max(other.getHLevel()));
      }
      return new TypeLevel(LevelOrder.maxLevel(getValue(), other.getValue()));
    }

    @Override
    public TypeLevel succ() {
      if (myIgnorePLevel) {
        if (getHLevel().equals(HomotopyLevel.PROP)) {
          return new TypeLevel(0, 0);
        }
        return new TypeLevel(getPLevel(), getHLevel().succ());
      }
      return new TypeLevel(getPLevel().succ(), getHLevel().succ());
    }

    /*
    @Override
    public boolean equals(Object other) {
      return this == other || (other instanceof TypeLevel && ((TypeLevel) other).compare(this, new CompareVisitor(DummyEquations.getInstance(), Equations.CMP.EQ, null)) == Cmp.EQUALS);
    }
    /**/

    @Override
    public String toString() {
      return getValue().toString();
    }
  }

  public TypeUniverse() { }

  public TypeUniverse(TypeLevel typeLevel) {
    super(typeLevel);
  }

  @Override
  public String toString() {
    if (getLevel() == null) return "\\Type";
    return "\\Type " + getLevel();
  }

  @Override
  public TypeUniverse createUniverse(TypeLevel level) {
    return new TypeUniverse(level);
  }
}
