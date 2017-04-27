package com.jetbrains.jetpad.vclang.core.context;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static void removeFromList(List<?> list, Abstract.Argument argument) {
    if (argument instanceof Abstract.TelescopeArgument) {
      for (Object ignored : ((Abstract.TelescopeArgument) argument).getReferableList()) {
        list.remove(list.size() - 1);
      }
    } else {
      list.remove(list.size() - 1);
    }
  }

  public static void removeFromList(List<?> list, List<? extends Abstract.Argument> arguments) {
    for (Abstract.Argument argument : arguments) {
      removeFromList(list, argument);
    }
  }

  public static void trimToSize(List<?> list, int size) {
    if (size < list.size()) {
      list.subList(size, list.size()).clear();
    }
  }

  public static class ContextSaver implements AutoCloseable {
    private final List myContext;
    private final int myOldContextSize;

    public ContextSaver(List context) {
      myContext = context;
      myOldContextSize = context != null ? context.size() : 0;
    }

    public int getOriginalSize() {
      return myOldContextSize;
    }

    @Override
    public void close() {
      if (myContext != null) {
        trimToSize(myContext, myOldContextSize);
      }
    }
  }

  public static class CompleteContextSaver<T> implements AutoCloseable {
    private final List<T> myContext;
    private final List<T> myOldContext;

    public CompleteContextSaver(List<T> context) {
      myContext = context;
      myOldContext = new ArrayList<>(context);
    }

    public List<T> getCurrentContext() {
      return myContext;
    }

    public List<T> getOldContext() {
      return myOldContext;
    }

    @Override
    public void close() {
      myContext.clear();
      myContext.addAll(myOldContext);
    }
  }

  public static ExprSubstitution matchParameters(DependentLink link, List<Expression> parameters) {
    ExprSubstitution substs = new ExprSubstitution();
    for (Expression parameter : parameters) {
      if (!link.hasNext()) {
        return null;
      }
      substs.add(link, parameter);
      link = link.getNext();
    }
    return substs;
  }
}
