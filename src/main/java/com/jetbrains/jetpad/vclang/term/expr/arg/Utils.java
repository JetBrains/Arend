package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.PrettyPrintVisitor;

import java.util.List;

public class Utils {
  private static String renameVar(List<String> names, String var) {
    while (names.contains(var)) {
      var += "'";
    }
    return var;
  }

  public static void addNames(List<String> names, Abstract.Argument argument) {
    if (argument instanceof Abstract.NameArgument) {
      names.add(renameVar(names, ((Abstract.NameArgument) argument).getName()));
    } else
    if (argument instanceof Abstract.TelescopeArgument) {
      for (String name : ((Abstract.TelescopeArgument) argument).getNames()) {
        names.add(renameVar(names, name));
      }
    }
  }

  public static void removeNames(List<String> names, Abstract.Argument argument) {
    if (argument instanceof Abstract.NameArgument) {
      names.remove(names.size() - 1);
    } else
    if (argument instanceof Abstract.TelescopeArgument) {
      for (String ignored : ((Abstract.TelescopeArgument) argument).getNames()) {
        names.remove(names.size() - 1);
      }
    }
  }

  public static void prettyPrintArgument(Abstract.Argument argument, StringBuilder builder, List<String> names, int prec) {
    if (argument instanceof Abstract.NameArgument) {
      String name = ((Abstract.NameArgument) argument).getName();
      builder.append(argument.getExplicit() ? name : "{" + name + "}");
    } else
    if (argument instanceof TelescopeArgument) {
      builder.append(argument.getExplicit() ? '(' : '{');
      for (String name : ((TelescopeArgument) argument).getNames()) {
        builder.append(name).append(" ");
      }
      builder.append(": ");
      ((TypeArgument) argument).getType().prettyPrint(builder, names, 0);
      builder.append(argument.getExplicit() ? ')' : '}');
    } else
    if (argument instanceof TypeArgument) {
      Abstract.Expression type = ((TypeArgument) argument).getType();
      if (argument.getExplicit()) {
        type.accept(new PrettyPrintVisitor(builder, names), prec);
      } else {
        builder.append('{');
        type.accept(new PrettyPrintVisitor(builder, names), 0);
        builder.append('}');
      }
    } else {
      throw new IllegalStateException();
    }
  }
}
