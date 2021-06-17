package org.arend.core.context.binding;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.variable.Variable;

public interface LevelVariable extends Variable {
  enum LvlType {
    PLVL,
    HLVL { @Override public int getMinValue() { return -1; } };

    public int getMinValue() {
      return 0;
    }
  }

  LvlType getType();
  LevelVariable max(LevelVariable other);
  boolean compare(LevelVariable other, CMP cmp);

  default int getMinValue() {
    return getType().getMinValue();
  }

  @Override
  default String getName() {
    return toString();
  }

  default LevelVariable getStd() {
    return getType() == LvlType.PLVL ? PVAR : HVAR;
  }

  LevelVariable PVAR = new LevelVariable() {
    @Override
    public LvlType getType() {
      return LvlType.PLVL;
    }

    @Override
    public LevelVariable max(LevelVariable other) {
      return other instanceof InferenceLevelVariable || getType() != other.getType() ? null : other;
    }

    @Override
    public boolean compare(LevelVariable other, CMP cmp) {
      return this == other || other instanceof ParamLevelVariable && other.getType() == LvlType.PLVL && (cmp == CMP.LE || ((ParamLevelVariable) other).getSize() == 0);
    }

    @Override
    public String toString() {
      return "\\lp";
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof LevelVariable && compare((LevelVariable) o, CMP.EQ);
    }
  };

  LevelVariable HVAR = new LevelVariable() {
    @Override
    public LvlType getType() {
      return LvlType.HLVL;
    }

    @Override
    public LevelVariable max(LevelVariable other) {
      return other instanceof InferenceLevelVariable || getType() != other.getType() ? null : other;
    }

    @Override
    public boolean compare(LevelVariable other, CMP cmp) {
      return this == other || other instanceof ParamLevelVariable && other.getType() == LvlType.HLVL && (cmp == CMP.LE || ((ParamLevelVariable) other).getSize() == 0);
    }

    @Override
    public String toString() {
      return "\\lh";
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof LevelVariable && compare((LevelVariable) o, CMP.EQ);
    }
  };
}
