package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

import java.util.HashSet;
import java.util.Set;

public class Substitution {
  public ExprSubstitution ExprSubst = new ExprSubstitution();
  public LevelSubstitution LevelSubst = new LevelSubstitution();

  public Substitution() {
  }

  public Substitution(ExprSubstitution subst) {
    ExprSubst = subst;
  }

  public Substitution(LevelSubstitution subst) {
    LevelSubst = subst;
  }

  public Substitution(ExprSubstitution subst, LevelSubstitution levelSubst) {
    ExprSubst = subst;
    LevelSubst = levelSubst;
  }

  public Set<Binding> getDomain() {
    Set<Binding> result = new HashSet<>(ExprSubst.getDomain());
    result.addAll(LevelSubst.getDomain());
    return result;
  }
}
