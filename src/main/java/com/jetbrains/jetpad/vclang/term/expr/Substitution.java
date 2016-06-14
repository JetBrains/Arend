package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

import java.util.HashSet;
import java.util.Set;

public class Substitution {
  public ExprSubstitution ExprSubst;
  public LevelSubstitution LevelSubst;

  public Substitution() {
    ExprSubst = new ExprSubstitution();
    LevelSubst = new LevelSubstitution();
  }

  public Substitution(ExprSubstitution subst, LevelSubstitution levelSubst) {
    ExprSubst = subst;
    LevelSubst = levelSubst;
  }

  public Set<Binding> getDomain() {
    Set<Binding> result = new HashSet<>(ExprSubst.getDomain());
    return (result).addAll(LevelSubst.getDomain());
  }
}
