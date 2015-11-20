package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;

import java.util.List;

public interface ElimTreeVisitor<T> {
  T visitElimOK(boolean isExplicit, Constructor constructor, List<T> children);
  T visitElimIncomplete(boolean isExplicit, Constructor constructor, List<T> children);
  T visitElimFailed(boolean isExplicit, Constructor constructor, List<T> children);

  T visitName(boolean isExplicit);
  T visitFailed(boolean isExplicit);
  T visitIncomplete(boolean isExplicit, Constructor constructor);
}
