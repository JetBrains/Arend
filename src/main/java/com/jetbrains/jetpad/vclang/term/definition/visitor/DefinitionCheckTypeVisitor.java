package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
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

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<List<Binding>, Definition> {
  private final Definition myParent;
  private final List<TypeCheckingError> myErrors;

  public DefinitionCheckTypeVisitor(Definition parent, List<TypeCheckingError> errors) {
    myParent = parent;
    myErrors = errors;
  }

  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, List<Binding> localContext) {
    // TODO
    FunctionDefinition overriddenFunction = null; // result instanceof OverriddenDefinition ? ((OverriddenDefinition) result).getOverriddenFunction() : null;
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myErrors.add(new TypeCheckingError("Cannot find function " + def.getName() + " in the parent class", def, getNames(localContext)));
      myErrors.add(new TypeCheckingError("Overridden function " + def.getName() + " cannot be defined in a base class", def, getNames(localContext)));
      return null;
    }

    List<Argument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    Set<Definition> abstractCalls = new HashSet<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor(myParent, localContext, abstractCalls, myErrors, CheckTypeVisitor.Side.RHS);

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
          myErrors.add(new TypeCheckingError("Type of the argument does not match the type in the overridden function", argument, null));
          trimToSize(localContext, origSize);
          return null;
        }
      }

      if (index == -1) {
        myErrors.add(new TypeCheckingError("Function has more arguments than overridden function", def, null));
        trimToSize(localContext, origSize);
        return null;
      }
    }

    int numberOfArgs = index;
    index = 0;
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
              List <CompareVisitor.Equation> equations = new ArrayList<>(0);
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
            List <CompareVisitor.Equation> equations = new ArrayList<>(0);
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
          myErrors.add(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          trimToSize(localContext, origSize);
          return null;
        }
      } else {
        if (splitArgs == null) {
          myErrors.add(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
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
            myErrors.add(new TypeCheckingError("Result type of the function does not match the result type in the overridden function", def.getResultType(), null));
            trimToSize(localContext, origSize);
            return null;
          }
        }
      }
    }

    if (expectedType == null) {
      expectedType = overriddenResultType;
    }

    FunctionDefinition result = def.isOverridden() ? new OverriddenDefinition(def.getName(), myParent, def.getPrecedence(), def.getFixity(), arguments, expectedType, def.getArrow(), null, overriddenFunction) : new FunctionDefinition(def.getName(), myParent, def.getPrecedence(), def.getFixity(), arguments, expectedType, def.getArrow(), null);
    result.typeHasErrors(false);
    result.hasErrors(false);
    visitor.setSide(CheckTypeVisitor.Side.LHS);
    CheckTypeVisitor.OKResult termResult = visitor.checkType(def.getTerm(), expectedType);

    if (termResult != null) {
      result.setTerm(termResult.expression);
      result.setResultType(termResult.type);

      if (!termResult.expression.accept(new TerminationCheckVisitor(overriddenFunction == null ? result : overriddenFunction))) {
        myErrors.add(new TypeCheckingError("Termination check failed", def.getTerm(), getNames(localContext)));
        termResult = null;
      }
    } else {
      if (result.getResultType() == null) {
        result.typeHasErrors(true);
      }
    }

    if (termResult == null) {
      result.setTerm(null);
      if (!result.isAbstract()) {
        result.hasErrors(true);
      }
    }
    result.setDependencies(abstractCalls);
    trimToSize(localContext, origSize);

    return result;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, List<Binding> localContext) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    Set<Definition> abstractCalls = new HashSet<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor(myParent, localContext, abstractCalls, myErrors, CheckTypeVisitor.Side.RHS);
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

    DataDefinition result = new DataDefinition(def.getName(), myParent, def.getPrecedence(), def.getFixity(), def.getUniverse() != null ? def.getUniverse() : universe, parameters, new ArrayList<Constructor>(def.getConstructors().size()));
    result.hasErrors(false);
    result.setDependencies(abstractCalls);

    constructors_loop:
    for (int i = 0; i < def.getConstructors().size(); ++i) {
      visitConstructor(def.getConstructors().get(i), result.getConstructors().get(i), localContext, abstractCalls);
      if (result.getConstructors().get(i).hasErrors()) {
        continue;
      }

      for (int j = 0; j < result.getConstructors().get(i).getArguments().size(); ++j) {
        Expression type = result.getConstructors().get(i).getArguments().get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(result))) {
              String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + result.getConstructors().get(i).getName();
              myErrors.add(new TypeCheckingError(msg, def.getConstructors().get(i).getArguments().get(j).getType(), getNames(localContext)));
              continue constructors_loop;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(result))) {
            String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + result.getConstructors().get(i).getName();
            myErrors.add(new TypeCheckingError(msg, def.getConstructors().get(i).getArguments().get(j).getType(), getNames(localContext)));
            continue constructors_loop;
          }
        }
      }

      Universe maxUniverse = universe.max(result.getConstructors().get(i).getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + result.getConstructors().get(i).getUniverse() + " of constructor " + result.getConstructors().get(i).getName() + " is not compatible with universe " + universe + " of previous constructors";
        myErrors.add(new TypeCheckingError(msg, null, null));
        continue;
      }
      universe = maxUniverse;
    }

    result.setUniverse(universe);
    trimToSize(localContext, origSize);
    if (def.getUniverse() != null && !universe.lessOrEquals(def.getUniverse())) {
      myErrors.add(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
    }

    return result;
  }

  private void visitConstructor(Abstract.Constructor def, Constructor constructor, List<Binding> localContext, Set<Definition> abstractCalls) {
    List<TypeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    int index = 1;
    String error = null;
    CheckTypeVisitor visitor = new CheckTypeVisitor(constructor.getParent(), localContext, abstractCalls, myErrors, CheckTypeVisitor.Side.RHS);
    for (Abstract.TypeArgument argument : def.getArguments()) {
      CheckTypeVisitor.OKResult result = visitor.checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return;
      }

      Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not compatible with universe " + universe + " of previous arguments";
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
    constructor.setUniverse(universe);
    constructor.setArguments(arguments);
    constructor.hasErrors(false);
    if (error != null) {
      myErrors.add(new TypeCheckingError(error, DefCall(constructor), new ArrayList<String>()));
    }
  }

  @Override
  public Definition visitConstructor(Abstract.Constructor def, List<Binding> localContext) {
    throw new IllegalStateException();
  }

  @Override
  public Definition visitClass(Abstract.ClassDefinition def, List<Binding> localContext) {
    throw new IllegalStateException();
    /*
    TypeCheckingError error = null;
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    Set<Definition> abstractCalls = new HashSet<>();
    List<Definition> fields = ((ClassDefinition) myResult).getFields();
    for (Definition field : fields) {
      if (field.isAbstract()) {
        Universe maxUniverse = universe.max(field.getUniverse());
        if (maxUniverse == null) {
          String msg = "Universe " + field.getUniverse() + " of field " + field.getName() + " is not compatible with universe " + universe + " of previous fields";
          error = new TypeCheckingError(msg, null, null);
        } else {
          universe = maxUniverse;
        }
      } else {
        if (field.getDependencies() != null) {
          abstractCalls.addAll(field.getDependencies());
        }
      }
    }
    for (Definition field : fields) {
      if (field.isAbstract()) {
        abstractCalls.remove(field);
      }
    }

    if (error != null) {
      myErrors.add(error);
    }
    myResult.setUniverse(universe);
    myResult.hasErrors(false);
    myResult.setDependencies(abstractCalls);
    return null;
    */
  }
}
