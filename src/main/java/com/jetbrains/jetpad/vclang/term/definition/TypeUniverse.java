package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

public class TypeUniverse extends BaseUniverse<TypeUniverse.TypeLevel> {

  public static class PredicativeLevel implements Universe.Level<PredicativeLevel> {
    private Expression myLevel;

    public PredicativeLevel() {
      myLevel = ZeroLvl();
    }

    public PredicativeLevel(Expression level) {
      myLevel = level;
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

    public static final Expression NOT_TRUNCATED = Inf();
    public static final Expression PROP = Fin(Zero());
    public static final Expression SET = Fin(Suc(Zero()));

    public HomotopyLevel() {
      myLevel = NOT_TRUNCATED;
    }

    public HomotopyLevel(Expression level) {
      myLevel = level;
    }

    @Override
    public Cmp compare(HomotopyLevel other) {
      myLevel = myLevel.normalize(NormalizeVisitor.Mode.NF);
      other.myLevel = other.myLevel.normalize(NormalizeVisitor.Mode.NF);
      if (Expression.compare(myLevel, other.myLevel)) return Cmp.EQUALS;
      if (Expression.compare(myLevel, NOT_TRUNCATED)) return Cmp.GREATER;
      if (Expression.compare(other.myLevel, NOT_TRUNCATED)) return Cmp.LESS;
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
    private PredicativeLevel myPLevel = new PredicativeLevel();
    private HomotopyLevel myHLevel = new HomotopyLevel();

    public TypeLevel() {}

    public TypeLevel(Expression PLevel, Expression HLevel) {
      myPLevel = new PredicativeLevel(PLevel);
      myHLevel = new HomotopyLevel(HLevel);
    }

    public TypeLevel(PredicativeLevel PLevel, HomotopyLevel HLevel) {
      myPLevel = PLevel;
      myHLevel = HLevel;
    }

    @Override
    public Cmp compare(TypeLevel other) {
      Cmp r1 = myPLevel.compare(other.myPLevel);
      Cmp r2 = myHLevel.compare(other.myHLevel);
      if (r1 == Cmp.UNKNOWN || r2 == Cmp.UNKNOWN) return Cmp.UNKNOWN;
      if (r1 == Cmp.LESS) {
        return r2 == Cmp.LESS || r2 == Cmp.EQUALS ? Cmp.LESS : Cmp.NOT_COMPARABLE;
      }
      if (r1 == Cmp.GREATER) {
        return r2 == Cmp.GREATER || r2 == Cmp.EQUALS ? Cmp.GREATER : Cmp.NOT_COMPARABLE;
      }
      return Cmp.EQUALS;
    }

    @Override
    public TypeLevel max(TypeLevel other) {
      return new TypeLevel(myPLevel.max(other.myPLevel), myHLevel.max(other.myHLevel));
    }

    @Override
    public TypeLevel succ() {
      return new TypeLevel(myPLevel.succ(), myHLevel.succ());
    }
  }

  public TypeUniverse() {
    super(new TypeLevel());
  }

  public TypeUniverse(TypeLevel typeLevel) {
    super(typeLevel);
  }
}
