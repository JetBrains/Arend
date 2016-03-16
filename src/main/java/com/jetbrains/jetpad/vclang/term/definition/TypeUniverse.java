package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

import com.jetbrains.jetpad.vclang.term.expr.NewExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.HashMap;

public class TypeUniverse extends BaseUniverse<TypeUniverse, TypeUniverse.TypeLevel> {
  public static final TypeUniverse PROP = new TypeUniverse(new TypeLevel(HomotopyLevel.PROP, true));
  public static final TypeUniverse SET = new TypeUniverse(new TypeLevel(HomotopyLevel.SET, true));

  public static TypeUniverse SetOfLevel(int level) { return new TypeUniverse(new TypeLevel(level, 1)); }

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
    public Cmp compare(PredicativeLevel other) {
      Expression otherLevel = other.myLevel;
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      otherLevel = otherLevel.normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, otherLevel)) return Cmp.EQUALS;
      Expression maxUlevel = MaxLvl(myLevel, otherLevel).normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, maxUlevel)) return Cmp.GREATER;
      if (Expression.compare(otherLevel, maxUlevel)) return Cmp.LESS;
      return Cmp.UNKNOWN;
    }

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
      myLevel = NOT_TRUNCATED.myLevel;
    }

    public HomotopyLevel(Expression level) {
      myLevel = level;
    }

    public HomotopyLevel(int level) {
      myLevel = Zero();
      for (int i = 0; i < level; ++i) {
        myLevel = Suc(myLevel);
      }
      myLevel = Fin(myLevel);
    }

    public Expression getValue() {
      return myLevel;
    }

    @Override
    public Cmp compare(HomotopyLevel other) {
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      other.myLevel = other.myLevel.normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, other.myLevel)) return Cmp.EQUALS;
      if (Expression.compare(myLevel, NOT_TRUNCATED.myLevel)) return Cmp.GREATER;
      if (Expression.compare(other.myLevel, NOT_TRUNCATED.myLevel)) return Cmp.LESS;
      Expression maxHlevel = MaxCNat(myLevel, other.myLevel).normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, maxHlevel)) return Cmp.GREATER;
      if (Expression.compare(other.myLevel, maxHlevel)) return Cmp.LESS;
      return Cmp.UNKNOWN;
    }

    @Override
    public HomotopyLevel max(HomotopyLevel other) {
      return new HomotopyLevel(MaxCNat(myLevel, other.myLevel));
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

    public TypeLevel() {
      myPLevel = new PredicativeLevel();
      myHLevel = new HomotopyLevel();
    }

    public TypeLevel(PredicativeLevel plevel) {
      myPLevel = plevel;
      myHLevel = new HomotopyLevel();
    }

    public TypeLevel(HomotopyLevel hlevel, boolean ignorePLevel) {
      myPLevel = new PredicativeLevel();
      myHLevel = hlevel;
      myIgnorePLevel = ignorePLevel;
    }

    public TypeLevel(Expression plevel, Expression hlevel) {
      myPLevel = new PredicativeLevel(plevel);
      myHLevel = new HomotopyLevel(hlevel);
    }

    public TypeLevel(Expression level) {
      this(PLevel().applyThis(level), HLevel().applyThis(level));
    }

    public TypeLevel(int plevel, int hlevel) {
      myPLevel = new PredicativeLevel(plevel);
      myHLevel = new HomotopyLevel(hlevel);
    }

    public TypeLevel(PredicativeLevel plevel, HomotopyLevel hlevel) {
      myPLevel = plevel;
      myHLevel = hlevel;
    }

    public Expression getValue() {
      HashMap<ClassField, ClassCallExpression.ImplementStatement> map = new HashMap<>();
      map.put(Prelude.PLEVEL, new ClassCallExpression.ImplementStatement(Lvl(), myPLevel.getValue()));
      map.put(Prelude.HLEVEL, new ClassCallExpression.ImplementStatement(CNat(), myHLevel.getValue()));
      return new NewExpression(new ClassCallExpression(Prelude.LEVEL, map));
    }

    public void setIgnorePLevel() { myIgnorePLevel = true; }

    @Override
    public Cmp compare(TypeLevel other) {
      Cmp r1 = myIgnorePLevel || other.myIgnorePLevel ? Cmp.EQUALS : myPLevel.compare(other.myPLevel);
      Cmp r2 = myHLevel.compare(other.myHLevel);
      if (r1 == Cmp.UNKNOWN || r2 == Cmp.UNKNOWN) return Cmp.UNKNOWN;
      if (r1 == Cmp.LESS) {
        return r2 == Cmp.LESS || r2 == Cmp.EQUALS ? Cmp.LESS : Cmp.NOT_COMPARABLE;
      }
      if (r1 == Cmp.GREATER) {
        return r2 == Cmp.GREATER || r2 == Cmp.EQUALS ? Cmp.GREATER : Cmp.NOT_COMPARABLE;
      }
      return r2;
    }

    @Override
    public TypeLevel max(TypeLevel other) {
      return new TypeLevel(myPLevel.max(other.myPLevel), myHLevel.max(other.myHLevel));
    }

    @Override
    public TypeLevel succ() {
      return new TypeLevel(myPLevel.succ(), myHLevel.succ());
    }

    @Override
    public boolean equals(Object other) {
      return this == other || (other instanceof TypeLevel && ((TypeLevel) other).compare(this) == Cmp.EQUALS);
    }

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
