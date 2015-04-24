package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.PrettyPrintVisitor;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static String renameVar(List<String> names, String var) {
    while (names.contains(var)) {
      var += "'";
    }
    return var;
  }

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

  public static int numberOfVariables(Abstract.Argument argument) {
    if (argument instanceof Abstract.TelescopeArgument) {
      return ((Abstract.TelescopeArgument) argument).getNames().size();
    } else {
      return 1;
    }
  }

  public static int numberOfVariables(List<? extends Abstract.Argument> arguments) {
    int result = 0;
    for (Abstract.Argument argument : arguments) {
      result += numberOfVariables(argument);
    }
    return result;
  }

  public static void prettyPrintArgument(Abstract.Argument argument, StringBuilder builder, List<String> names, byte prec) {
    if (argument instanceof Abstract.NameArgument) {
      String name = renameVar(names, ((Abstract.NameArgument) argument).getName());
      builder.append(argument.getExplicit() ? name : '{' + name + '}');
      names.add(name);
    } else
    if (argument instanceof TelescopeArgument) {
      builder.append(argument.getExplicit() ? '(' : '{');
      List<String> newNames = new ArrayList<>(((TelescopeArgument) argument).getNames().size());
      for (String name : ((TelescopeArgument) argument).getNames()) {
        String newName = renameVar(names, name);
        builder.append(newName).append(' ');
        newNames.add(newName);
      }
      builder.append(": ");
      ((TypeArgument) argument).getType().prettyPrint(builder, names, Abstract.Expression.PREC);
      builder.append(argument.getExplicit() ? ')' : '}');
      for (String name : newNames) {
        names.add(name);
      }
    } else
    if (argument instanceof TypeArgument) {
      Abstract.Expression type = ((TypeArgument) argument).getType();
      if (argument.getExplicit()) {
        type.accept(new PrettyPrintVisitor(builder, names), prec);
      } else {
        builder.append('{');
        type.accept(new PrettyPrintVisitor(builder, names), Abstract.Expression.PREC);
        builder.append('}');
      }
      names.add(null);
    }
  }
}
