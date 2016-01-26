package com.jetbrains.jetpad.vclang.term.context;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static void removeFromList(List<?> list, Abstract.Argument argument) {
    if (argument instanceof Abstract.TelescopeArgument) {
      for (String ignored : ((Abstract.TelescopeArgument) argument).getNames()) {
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
    while (list.size() > size) {
      list.remove(list.size() - 1);
    }
  }

  public static class ContextSaver implements AutoCloseable {
    private final List myContext;
    private final int myOldContextSize;

    public ContextSaver(List context) {
      myContext = context;
      myOldContextSize = context != null ? context.size() : 0;
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

  public static Substitution matchParameters(DependentLink link, List<Expression> parameters) {
    Substitution substs = new Substitution();
    for (Expression parameter : parameters) {
      if (link == null) {
        return null;
      }
      substs.addMapping(link, parameter);
      link = link.getNext();
    }
    return substs;
  }
}
