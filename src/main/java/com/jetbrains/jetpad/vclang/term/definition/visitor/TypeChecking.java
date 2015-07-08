package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.typeOfFunctionArg;
import static com.jetbrains.jetpad.vclang.term.error.TypeCheckingError.getNames;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class TypeChecking {
  public static DataDefinition typeCheckDataBegin(ModuleLoader moduleLoader, ClassDefinition parent, Abstract.DataDefinition def, List<Binding> localContext) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    Set<Definition> abstractCalls = new HashSet<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor(parent, localContext, abstractCalls, moduleLoader, CheckTypeVisitor.Side.RHS);
    for (Abstract.TypeArgument parameter : def.getParameters()) {
      CheckTypeVisitor.OKResult result = visitor.checkType(parameter.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      if (parameter instanceof Abstract.TelescopeArgument) {
        parameters.add(Tele(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression));
        List<String> names = ((Abstract.TelescopeArgument) parameter).getNames();
        for (int i = 0; i < names.size(); ++i) {
          localContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
        }
      } else {
        parameters.add(TypeArg(parameter.getExplicit(), result.expression));
        localContext.add(new TypedBinding(null, result.expression));
      }
    }

    DataDefinition result = new DataDefinition(def.getName(), parent, def.getPrecedence(), def.getFixity(), def.getUniverse() != null ? def.getUniverse() : new Universe.Type(0, Universe.Type.PROP), parameters, new ArrayList<Constructor>());
    result.setDependencies(abstractCalls);
    if (!parent.add(result, moduleLoader.getErrors())) {
      return null;
    }
    return result;
  }

  public static void typeCheckDataEnd(ModuleLoader moduleLoader, Abstract.DataDefinition def, DataDefinition definition, List<Binding> localContext) {
    if (localContext != null) {
      for (TypeArgument parameter : definition.getParameters()) {
        if (parameter instanceof TelescopeArgument) {
          for (String ignored : ((TelescopeArgument) parameter).getNames()) {
            localContext.remove(localContext.size() - 1);
          }
        } else {
          localContext.remove(localContext.size() - 1);
        }
      }
    }

    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (Constructor constructor : definition.getConstructors()) {
      Universe maxUniverse = universe.max(constructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + constructor.getUniverse() + " of constructor " + constructor.getName() + " is not compatible with universe " + universe + " of previous constructors";
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(msg, null, null));
      } else {
        universe = maxUniverse;
      }
    }
    definition.setUniverse(universe);

    if (def.getUniverse() != null && !universe.lessOrEquals(def.getUniverse())) {
      moduleLoader.getTypeCheckingErrors().add(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
    }
  }

  public static Constructor typeCheckConstructor(ModuleLoader moduleLoader, DataDefinition dataDefinition, Abstract.Constructor con, List<Binding> localContext, int conIndex) {
    List<TypeArgument> arguments = new ArrayList<>(con.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    int index = 1;
    boolean ok = true;
    CheckTypeVisitor visitor = new CheckTypeVisitor(dataDefinition, localContext, dataDefinition.getDependencies(), moduleLoader, CheckTypeVisitor.Side.RHS);
    for (Abstract.TypeArgument argument : con.getArguments()) {
      CheckTypeVisitor.OKResult result = visitor.checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not compatible with universe " + universe + " of previous arguments";
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(error, con, new ArrayList<String>()));
        ok = false;
      } else {
        universe = maxUniverse;
      }

      if (argument instanceof Abstract.TelescopeArgument) {
        arguments.add(Tele(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), result.expression));
        List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
        for (int i = 0; i < names.size(); ++i) {
          localContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
        }
        index += ((Abstract.TelescopeArgument) argument).getNames().size();
      } else {
        arguments.add(TypeArg(argument.getExplicit(), result.expression));
        localContext.add(new TypedBinding(null, result.expression));
        ++index;
      }
    }

    trimToSize(localContext, origSize);
    if (!ok) {
      return null;
    }

    Constructor constructor = new Constructor(conIndex, con.getName(), dataDefinition, con.getPrecedence(), con.getFixity(), universe, arguments);
    for (int j = 0; j < constructor.getArguments().size(); ++j) {
      Expression type = constructor.getArguments().get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
      while (type instanceof PiExpression) {
        for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
          if (argument1.getType().accept(new FindDefCallVisitor(dataDefinition))) {
            String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + constructor.getName();
            moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(msg, con.getArguments().get(j).getType(), getNames(localContext)));
            return null;
          }
        }
        type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
      }

      List<Expression> exprs = new ArrayList<>();
      type.getFunction(exprs);
      for (Expression expr : exprs) {
        if (expr.accept(new FindDefCallVisitor(dataDefinition))) {
          String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + constructor.getName();
          moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(msg, con.getArguments().get(j).getType(), getNames(localContext)));
          return null;
        }
      }
    }

    dataDefinition.getConstructors().add(constructor);
    return constructor;
  }

  public static FunctionDefinition typeCheckFunctionBegin(ModuleLoader moduleLoader, ClassDefinition parent, Abstract.FunctionDefinition def, List<Binding> localContext, FunctionDefinition overriddenFunction) {
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + def.getName() + " in the parent class", def, getNames(localContext)));
      moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Overridden function " + def.getName() + " cannot be defined in a base class", def, getNames(localContext)));
      return null;
    }

    List<Argument> arguments = new ArrayList<>(def.getArguments().size());
    Set<Definition> abstractCalls = new HashSet<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor(parent, localContext, abstractCalls, moduleLoader, CheckTypeVisitor.Side.RHS);

    List<TypeArgument> splitArgs = null;
    Expression splitResult = null;
    if (overriddenFunction != null) {
      splitArgs = new ArrayList<>();
      splitResult = splitArguments(overriddenFunction.getType(), splitArgs);
    }

    int index = 0;
    if (splitArgs != null) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (index >= splitArgs.size()) {
          index = -1;
          break;
        }

        boolean ok = true;
        if (argument instanceof Abstract.TelescopeArgument) {
          for (String ignored : ((Abstract.TelescopeArgument) argument).getNames()) {
            if (splitArgs.get(index).getExplicit() != argument.getExplicit()) {
              ok = false;
              break;
            }
            ++index;
          }
        } else {
          if (splitArgs.get(index).getExplicit() != argument.getExplicit()) {
            ok = false;
          } else {
            ++index;
          }
        }

        if (!ok) {
          moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Type of the argument does not match the type in the overridden function", argument, null));
          return null;
        }
      }

      if (index == -1) {
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Function has more arguments than overridden function", def, null));
        return null;
      }
    }

    int numberOfArgs = index;
    index = 0;
    int origSize = localContext.size();
    for (Abstract.Argument argument : def.getArguments()) {
      if (argument instanceof Abstract.TypeArgument) {
        CheckTypeVisitor.OKResult result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
        if (result == null) {
          trimToSize(localContext, origSize);
          return null;
        }

        boolean ok = true;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          arguments.add(Tele(argument.getExplicit(), names, result.expression));
          for (int i = 0; i < names.size(); ++i) {
            if (splitArgs != null) {
              List<CompareVisitor.Equation> equations = new ArrayList<>(0);
              CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
              if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
                ok = false;
                break;
              }
            }

            localContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
            ++index;
          }
        } else {
          if (splitArgs != null) {
            List<CompareVisitor.Equation> equations = new ArrayList<>(0);
            CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
            if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
              ok = false;
            }
          }

          if (ok) {
            arguments.add(TypeArg(argument.getExplicit(), result.expression));
            localContext.add(new TypedBinding(null, result.expression));
            ++index;
          }
        }

        if (!ok) {
          moduleLoader.getTypeCheckingErrors().add(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          trimToSize(localContext, origSize);
          return null;
        }
      } else {
        if (splitArgs == null) {
          moduleLoader.getTypeCheckingErrors().add(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          trimToSize(localContext, origSize);
          return null;
        } else {
          List<String> names = new ArrayList<>(1);
          names.add(((Abstract.NameArgument) argument).getName());
          arguments.add(Tele(argument.getExplicit(), names, splitArgs.get(index).getType()));
          localContext.add(new TypedBinding(names.get(0), splitArgs.get(index).getType()));
        }
      }
    }

    Expression overriddenResultType = null;
    if (overriddenFunction != null) {
      if (numberOfArgs == splitArgs.size()) {
        overriddenResultType = splitResult;
      } else {
        List<TypeArgument> args = new ArrayList<>(splitArgs.size() - numberOfArgs);
        for (; numberOfArgs < splitArgs.size(); ++numberOfArgs) {
          args.add(splitArgs.get(numberOfArgs));
        }
        overriddenResultType = Pi(args, splitResult);
      }
    }

    Expression expectedType = null;
    if (def.getResultType() != null) {
      CheckTypeVisitor.OKResult typeResult = visitor.checkType(def.getResultType(), Universe());
      if (typeResult != null) {
        expectedType = typeResult.expression;
        if (overriddenResultType != null) {
          List<CompareVisitor.Equation> equations = new ArrayList<>(0);
          CompareVisitor.Result cmpResult = compare(expectedType, overriddenResultType, equations);
          if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
            moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Result type of the function does not match the result type in the overridden function", def.getResultType(), null));
            trimToSize(localContext, origSize);
            return null;
          }
        }
      }
    }

    if (expectedType == null) {
      expectedType = overriddenResultType;
    }

    FunctionDefinition result;
    if (def.isOverridden()) {
      OverriddenDefinition overriddenDefinition = new OverriddenDefinition(def.getName(), parent, def.getPrecedence(), def.getFixity(), arguments, expectedType, def.getArrow(), null, overriddenFunction);
      overriddenDefinition.setOverriddenFunction(overriddenFunction);
      result = overriddenDefinition;
    } else {
      result = new FunctionDefinition(def.getName(), parent, def.getPrecedence(), def.getFixity(), arguments, expectedType, def.getArrow(), null);
    }
    if (!def.isOverridden() && !parent.add(result, moduleLoader.getErrors())) {
      trimToSize(localContext, origSize);
      return null;
    }

    result.setDependencies(abstractCalls);
    return result;
  }

  public static void typeCheckFunctionEnd(ModuleLoader moduleLoader, ClassDefinition parent, Abstract.Expression term, FunctionDefinition definition, List<Binding> localContext, FunctionDefinition overriddenFunction) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(parent, localContext, definition.getDependencies(), moduleLoader, CheckTypeVisitor.Side.LHS);
    CheckTypeVisitor.OKResult termResult = visitor.checkType(term, definition.getResultType());

    if (termResult != null) {
      definition.setTerm(termResult.expression);
      definition.setResultType(termResult.type);

      if (!termResult.expression.accept(new TerminationCheckVisitor(overriddenFunction == null ? definition : overriddenFunction))) {
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Termination check failed", term, getNames(localContext)));
        termResult = null;
      }
    } else {
      if (definition.getResultType() == null) {
        definition.typeHasErrors(true);
      }
    }

    if (termResult == null) {
      definition.setTerm(null);
      if (!definition.isAbstract()) {
        definition.hasErrors(true);
      }
    }

    for (Argument argument : definition.getArguments()) {
      if (argument instanceof TelescopeArgument) {
        for (String ignored : ((TelescopeArgument) argument).getNames()) {
          localContext.remove(localContext.size() - 1);
        }
      } else {
        localContext.remove(localContext.size() - 1);
      }
    }
  }
}
