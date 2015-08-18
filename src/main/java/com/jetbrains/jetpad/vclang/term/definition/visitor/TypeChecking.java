package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
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
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.typeOfFunctionArg;
import static com.jetbrains.jetpad.vclang.term.error.TypeCheckingError.getNames;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class TypeChecking {
  private static boolean addMember(Namespace namespace, Definition member, ModuleLoader moduleLoader) {
    if (namespace.addMember(member) != null) {
      moduleLoader.getErrors().add(new ModuleError(new Module(namespace, member.getName().name), "Name is already defined"));
      return false;
    } else {
      return true;
    }
  }

  public static DataDefinition typeCheckDataBegin(ModuleLoader moduleLoader, Namespace namespace, Namespace localNamespace, Abstract.DataDefinition def, List<Binding> localContext) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    CheckTypeVisitor visitor = new CheckTypeVisitor(namespace, localNamespace, localContext, moduleLoader, CheckTypeVisitor.Side.RHS);
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
        localContext.add(new TypedBinding((Utils.Name) null, result.expression));
      }
    }

    Namespace parentNamespace = localNamespace != null ? localNamespace : namespace;
    DataDefinition result = new DataDefinition(parentNamespace.getChild(def.getName()), def.getPrecedence(), def.getUniverse() != null ? def.getUniverse() : new Universe.Type(0, Universe.Type.PROP), parameters);
    return addMember(parentNamespace, result, moduleLoader) ? result : null;
  }

  public static void typeCheckDataEnd(ModuleLoader moduleLoader, Namespace namespace, Abstract.DataDefinition def, DataDefinition definition, List<Binding> localContext) {
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
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(namespace, msg, null, null));
      } else {
        universe = maxUniverse;
      }
    }

    if (def.getUniverse() != null) {
      if (universe.lessOrEquals(def.getUniverse())) {
        definition.setUniverse(def.getUniverse());
      } else {
        moduleLoader.getTypeCheckingErrors().add(new TypeMismatchError(namespace, new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
        definition.setUniverse(universe);
      }
    }
  }

  public static Constructor typeCheckConstructor(ModuleLoader moduleLoader, Namespace namespace, DataDefinition dataDefinition, Abstract.Constructor con, List<Binding> localContext, int conIndex) {
    List<TypeArgument> arguments = new ArrayList<>(con.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    int index = 1;
    boolean ok = true;
    CheckTypeVisitor visitor = new CheckTypeVisitor(namespace, namespace == dataDefinition.getParent() ? null : dataDefinition.getParent(), localContext, moduleLoader, CheckTypeVisitor.Side.RHS);

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
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(dataDefinition.getParent(), error, con, new ArrayList<String>()));
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
        localContext.add(new TypedBinding((Utils.Name) null, result.expression));
        ++index;
      }
    }

    trimToSize(localContext, origSize);
    if (!ok) {
      return null;
    }

    Constructor constructor = new Constructor(conIndex, dataDefinition.getNamespace().getChild(con.getName()), con.getPrecedence(), universe, arguments, dataDefinition);
    for (int j = 0; j < constructor.getArguments().size(); ++j) {
      Expression type = constructor.getArguments().get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
      while (type instanceof PiExpression) {
        for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
          if (argument1.getType().accept(new FindDefCallVisitor(dataDefinition))) {
            String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + constructor.getName();
            moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(dataDefinition.getParent(), msg, con.getArguments().get(j).getType(), getNames(localContext)));
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
          moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(dataDefinition.getParent(), msg, con.getArguments().get(j).getType(), getNames(localContext)));
          return null;
        }
      }
    }

    dataDefinition.addConstructor(constructor);
    dataDefinition.getParent().addMember(constructor);
    return constructor;
  }

  public static FunctionDefinition typeCheckFunctionBegin(ModuleLoader moduleLoader, Namespace namespace, Namespace localNamespace, Abstract.FunctionDefinition def, List<Binding> localContext, FunctionDefinition overriddenFunction) {
    Namespace parentNamespace = localNamespace != null ? localNamespace : namespace;
    FunctionDefinition typedDef;
    if (def.isOverridden()) {
      if (localNamespace == null) {
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(namespace, "Overridden function cannot be static", def, getNames(localContext)));
        return null;
      }
      typedDef = new OverriddenDefinition(parentNamespace.getChild(def.getName()), def.getPrecedence(), null, null, def.getArrow(), null, overriddenFunction);
    } else {
      typedDef = new FunctionDefinition(parentNamespace.getChild(def.getName()), def.getPrecedence(), null, null, def.getArrow(), null);
    }
    if (!typeCheckFunctionBegin(moduleLoader, namespace, def, localContext, overriddenFunction, typedDef)) {
      return null;
    }
    return typedDef;
  }

  public static boolean typeCheckFunctionBegin(ModuleLoader moduleLoader, Namespace namespace, Abstract.FunctionDefinition def, List<Binding> localContext, FunctionDefinition overriddenFunction, FunctionDefinition typedDef) {
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError(parent, "Cannot find function " + def.getName() + " in the parent class", def, getNames(localContext)));
      moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(typedDef.getParent(), "Overridden function " + def.getName() + " cannot be defined in a base class", def, getNames(localContext)));
      return false;
    }

    List<Argument> arguments = new ArrayList<>(def.getArguments().size());
    CheckTypeVisitor visitor = new CheckTypeVisitor(namespace, typedDef.getParent() == namespace ? namespace : typedDef.getParent(), localContext, moduleLoader, CheckTypeVisitor.Side.RHS);

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
          moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(typedDef.getParent(), "Type of the argument does not match the type in the overridden function", argument, null));
          return false;
        }
      }

      if (index == -1) {
        moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(typedDef.getParent(), "Function has more arguments than overridden function", def, null));
        return false;
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
          return false;
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
            localContext.add(new TypedBinding((Utils.Name) null, result.expression));
            ++index;
          }
        }

        if (!ok) {
          moduleLoader.getTypeCheckingErrors().add(new ArgInferenceError(typedDef.getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          trimToSize(localContext, origSize);
          return false;
        }
      } else {
        if (splitArgs == null) {
          moduleLoader.getTypeCheckingErrors().add(new ArgInferenceError(typedDef.getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          trimToSize(localContext, origSize);
          return false;
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
            moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(typedDef.getParent(), "Result type of the function does not match the result type in the overridden function", def.getResultType(), null));
            trimToSize(localContext, origSize);
            return false;
          }
        }
      }
    }

    if (expectedType == null) {
      expectedType = overriddenResultType;
    }

    typedDef.setArguments(arguments);
    typedDef.setResultType(expectedType);
    if (typedDef instanceof OverriddenDefinition) {
      ((OverriddenDefinition) typedDef).setOverriddenFunction(overriddenFunction);
    }

    typedDef.getParent().addMember(typedDef);
    if (expectedType == null) {
      typedDef.typeHasErrors(true);
    }

    return true;
  }

  public static boolean typeCheckFunctionEnd(ModuleLoader moduleLoader, Namespace namespace, Abstract.Expression term, FunctionDefinition definition, List<Binding> localContext, FunctionDefinition overriddenFunction) {
    if (term != null) {
      CheckTypeVisitor visitor = new CheckTypeVisitor(namespace, definition.getParent() == namespace ? null : definition.getParent(), localContext, moduleLoader, CheckTypeVisitor.Side.LHS);
      CheckTypeVisitor.OKResult termResult = visitor.checkType(term, definition.getResultType());

      if (termResult != null) {
        definition.setTerm(termResult.expression);
        definition.setResultType(termResult.type);

        if (!termResult.expression.accept(new TerminationCheckVisitor(overriddenFunction == null ? definition : overriddenFunction))) {
          moduleLoader.getTypeCheckingErrors().add(new TypeCheckingError(definition.getParent(), "Termination check failed", term, getNames(localContext)));
          termResult = null;
        }
      }

      if (termResult == null) {
        definition.setTerm(null);
        if (!definition.isAbstract()) {
          definition.hasErrors(true);
        }
      }
    } /* TODO
      else {
      if (definition.getParent() == namespace) {
        moduleLoader.getErrors().add(new ModuleError(new Module(namespace, definition.getName().name), "Non-static abstract definition"));
        return false;
      }
    } */

    definition.typeHasErrors(definition.getResultType() == null);
    Expression type = definition.getType();
    if (type != null) {
      type = type.getType(new ArrayList<Binding>(2));
      if (type instanceof UniverseExpression) {
        definition.setUniverse(((UniverseExpression) type).getUniverse());
      } else {
        throw new IllegalStateException();
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

    // TODO
    return definition.isOverridden() || definition.getParent().addMember(definition) == null;
  }
}
