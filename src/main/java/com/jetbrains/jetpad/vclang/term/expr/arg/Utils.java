package com.jetbrains.jetpad.vclang.term.expr.arg;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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

  public static int numberOfVariables(Expression expr) {
    List<TypeArgument> args = new ArrayList<>();
    splitArguments(expr, args);
    return args.size();
  }

  private static void addArgs(TypeArgument argument, List<TypeArgument> result) {
    if (argument instanceof TelescopeArgument) {
      int i = 0;
      for (String name : ((TelescopeArgument) argument).getNames()) {
        result.add(Tele(argument.getExplicit(), vars(name), argument.getType().liftIndex(0, i++)));
      }
    } else {
      result.add(TypeArg(argument.getExplicit(), argument.getType()));
    }
  }

  public static void splitArguments(List<TypeArgument> arguments, List<TypeArgument> result) {
    for (TypeArgument argument : arguments) {
      addArgs(argument, result);
    }
  }

  public static Expression splitArguments(Expression type, List<TypeArgument> result) {
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    while (type instanceof PiExpression) {
      PiExpression pi = (PiExpression) type;
      splitArguments(pi.getArguments(), result);
      type = pi.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
    }
    return type;
  }

  public static void prettyPrintArgument(Abstract.Argument argument, StringBuilder builder, List<String> names, byte prec, int indent) {
    if (argument instanceof Abstract.NameArgument) {
      String name = ((Abstract.NameArgument) argument).getName();
      String newName = name == null ? null : renameVar(names, name);
      names.add(newName);
      if (newName == null) {
        newName = "_";
      }
      builder.append(argument.getExplicit() ? newName : '{' + newName + '}');
    } else
    if (argument instanceof Abstract.TelescopeArgument) {
      builder.append(argument.getExplicit() ? '(' : '{');
      List<String> newNames = new ArrayList<>(((Abstract.TelescopeArgument) argument).getNames().size());
      for (String name : ((Abstract.TelescopeArgument) argument).getNames()) {
        String newName = name == null ? null : renameVar(names, name);
        builder.append(newName == null ? "_" : newName).append(' ');
        names.add(newName);
        newNames.add(newName);
      }

      for (String ignored : newNames) {
        names.remove(names.size() - 1);
      }

      builder.append(": ");
      ((Abstract.TypeArgument) argument).getType().accept(new PrettyPrintVisitor(builder, names, indent), Abstract.Expression.PREC);
      builder.append(argument.getExplicit() ? ')' : '}');

      names.addAll(newNames);
    } else
    if (argument instanceof Abstract.TypeArgument) {
      Abstract.Expression type = ((Abstract.TypeArgument) argument).getType();
      if (argument.getExplicit()) {
        type.accept(new PrettyPrintVisitor(builder, names, indent), prec);
      } else {
        builder.append('{');
        type.accept(new PrettyPrintVisitor(builder, names, indent), Abstract.Expression.PREC);
        builder.append('}');
      }
      names.add(null);
    }
  }

  public static void prettyPrintClause(Abstract.ElimExpression expr, Abstract.Clause clause, StringBuilder builder, List<String> names, int indent) {
    if (clause == null) return;

    new PrettyPrintVisitor(builder, names, indent).printIndent();
    builder.append("| ").append(clause.getName());
    int startIndex = names.size();
    for (Abstract.Argument argument : clause.getArguments()){
      builder.append(' ');
      prettyPrintArgument(argument, builder, names, (byte) (Abstract.AppExpression.PREC + 1), indent);
    }

    List<String> newNames = names;
    if (expr.getExpression() instanceof Abstract.IndexExpression) {
      int varIndex = ((Abstract.IndexExpression) expr.getExpression()).getIndex();
      newNames = new ArrayList<>(names.subList(0, startIndex - varIndex - 1 > 0 ? startIndex - varIndex - 1 : 0));
      newNames.addAll(names.subList(startIndex, names.size()));
      if (startIndex >= varIndex) {
        newNames.addAll(names.subList(startIndex - varIndex, startIndex));
      } else {
        for (int i = 0; i < varIndex; ++i) {
          newNames.add(null);
        }
      }
    }

    builder.append(clause.getArrow() == Abstract.Definition.Arrow.LEFT ? " <= " : " => ");
    clause.getExpression().accept(new PrettyPrintVisitor(builder, newNames, indent), Abstract.Expression.PREC);
    builder.append('\n');
    removeFromList(names, clause.getArguments());
  }
}
