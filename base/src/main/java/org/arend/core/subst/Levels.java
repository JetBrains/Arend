package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.Definition;
import org.arend.core.sort.Level;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.level.CoreLevels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public interface Levels extends CoreLevels {
  LevelSubstitution makeSubstitution(Definition definition);
  Levels subst(LevelSubstitution substitution);
  boolean compare(Levels other, CMP cmp, Equations equations, Concrete.SourceNode sourceNode);
  boolean isClosed();
  List<? extends Level> toList();
  int size();

  @Override
  default LevelSubstitution makeSubstitution(@NotNull CoreDefinition definition) {
    return makeSubstitution((Definition) definition);
  }

  default LevelPair toLevelPair() {
    LevelSubstitution levelSubst = makeSubstitution(Prelude.DEP_ARRAY);
    return new LevelPair((Level) levelSubst.get(LevelVariable.PVAR), (Level) levelSubst.get(LevelVariable.HVAR));
  }

  Levels EMPTY = new Levels() {
    @Override
    public LevelSubstitution makeSubstitution(Definition definition) {
      return LevelSubstitution.EMPTY;
    }

    @Override
    public Levels subst(LevelSubstitution substitution) {
      return this;
    }

    @Override
    public boolean compare(Levels other, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
      return other.size() == 0;
    }

    @Override
    public boolean isClosed() {
      return true;
    }

    @Override
    public List<? extends Level> toList() {
      return Collections.emptyList();
    }

    @Override
    public int size() {
      return 0;
    }
  };
}
