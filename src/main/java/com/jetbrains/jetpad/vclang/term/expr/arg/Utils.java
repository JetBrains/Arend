package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.PrettyPrintVisitor;

import java.io.PrintStream;
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

  public static void prettyPrintArgument(Abstract.Argument argument, PrintStream stream, List<String> names, int prec) {
    if (argument instanceof Abstract.NameArgument) {
      String name = ((Abstract.NameArgument) argument).getName();
      stream.print(argument.getExplicit() ? name : "{" + name + "}");
    } else
    if (argument instanceof TelescopeArgument) {
      stream.print(argument.getExplicit() ? '(' : '{');
      for (String name : ((TelescopeArgument) argument).getNames()) {
        stream.print(name + " ");
      }
      stream.print(": ");
      ((TypeArgument) argument).getType().prettyPrint(stream, names, 0);
      stream.print(argument.getExplicit() ? ')' : '}');
    } else
    if (argument instanceof TypeArgument) {
      Abstract.Expression type = ((TypeArgument) argument).getType();
      if (argument.getExplicit()) {
        type.accept(new PrettyPrintVisitor(stream, names), prec);
      } else {
        stream.print('{');
        type.accept(new PrettyPrintVisitor(stream, names), 0);
        stream.print('}');
      }
    } else {
      throw new IllegalStateException();
    }
  }
}
