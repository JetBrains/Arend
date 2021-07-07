package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.Definition;
import org.arend.core.sort.Level;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ListLevels implements Levels {
  private final List<Level> myLevels;

  public ListLevels(List<Level> levels) {
    myLevels = levels;
  }

  @Override
  public LevelSubstitution makeSubstitution(@NotNull Definition definition) {
    List<? extends LevelVariable> vars = definition.getLevelParameters();
    if (vars.size() != myLevels.size()) {
      throw new IllegalStateException();
    }
    SimpleLevelSubstitution result = new SimpleLevelSubstitution();
    for (int i = 0; i < vars.size(); i++) {
      result.add(vars.get(i), myLevels.get(i));
    }
    return result;
  }

  @Override
  public Levels subst(LevelSubstitution substitution) {
    List<Level> result = new ArrayList<>(myLevels.size());
    for (Level level : myLevels) {
      result.add(level.subst(substitution));
    }
    return new ListLevels(result);
  }

  @Override
  public boolean compare(Levels other, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
    if (!(other instanceof ListLevels)) return false;
    ListLevels otherList = (ListLevels) other;
    if (myLevels.size() != otherList.myLevels.size()) return false;
    for (int i = 0; i < myLevels.size(); i++) {
      if (!Level.compare(myLevels.get(i), otherList.myLevels.get(i), cmp, equations, sourceNode)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isClosed() {
    for (Level level : myLevels) {
      if (!level.isClosed()) return false;
    }
    return true;
  }

  @Override
  public List<? extends Level> toList() {
    return myLevels;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Levels && compare((Levels) obj, CMP.EQ, DummyEquations.getInstance(), null);
  }
}
