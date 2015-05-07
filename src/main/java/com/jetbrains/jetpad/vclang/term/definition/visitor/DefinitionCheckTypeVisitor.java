package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.FindDefCallVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<List<Binding>, Definition> {
  private final Map<String, Definition> myGlobalContext;
  private final List<TypeCheckingError> myErrors;

  public DefinitionCheckTypeVisitor(Map<String, Definition> globalContext, List<TypeCheckingError> errors) {
    myGlobalContext = globalContext;
    myErrors = errors;
  }

  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, List<Binding> localContext) {
    List<TelescopeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    for (Abstract.TelescopeArgument argument : def.getArguments()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      arguments.add(Tele(argument.getExplicit(), argument.getNames(), result.expression));
      for (String name : argument.getNames()) {
        localContext.add(new TypedBinding(name, result.expression));
      }
    }

    Expression expectedType;
    if (def.getResultType() != null) {
      CheckTypeVisitor.OKResult typeResult = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(def.getResultType(), Universe());
      if (typeResult == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      expectedType = typeResult.expression;
    } else {
      expectedType = null;
    }

    CheckTypeVisitor.OKResult termResult = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.LHS).checkType(def.getTerm(), expectedType);
    trimToSize(localContext, origSize);
    return termResult == null ? null : new FunctionDefinition(def.getName(), def.getPrecedence(), def.getFixity(), arguments, termResult.type, def.getArrow(), termResult.expression);
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, List<Binding> localContext) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (Abstract.TypeArgument parameter : def.getParameters()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(parameter.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      if (parameter instanceof TelescopeArgument) {
        parameters.add(Tele(parameter.getExplicit(), ((TelescopeArgument) parameter).getNames(), result.expression));
        for (String name : ((TelescopeArgument) parameter).getNames()) {
          localContext.add(new TypedBinding(name, result.expression));
        }
      } else {
        parameters.add(TypeArg(parameter.getExplicit(), result.expression));
        localContext.add(new TypedBinding(null, result.expression));
      }
    }

    List<Constructor> constructors = new ArrayList<>(def.getConstructors().size());
    DataDefinition result = new DataDefinition(def.getName(), def.getPrecedence(), def.getFixity(), def.getUniverse() != null ? def.getUniverse() : universe, parameters, constructors);

    myGlobalContext.put(def.getName(), result);
    constructors_loop:
    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor newConstructor = visitConstructor(constructor, localContext);
      if (newConstructor == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      for (int i = 0; i < newConstructor.getArguments().size(); ++i) {
        Expression type = newConstructor.getArgument(i).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            List<List<Expression>> arguments = new ArrayList<>();
            argument1.getType().accept(new FindDefCallVisitor(result, arguments));
            if (!arguments.isEmpty()) {
              String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + newConstructor.getName();
              myErrors.add(new TypeCheckingError(msg, constructor.getArgument(i).getType()));
              continue constructors_loop;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          List<List<Expression>> arguments = new ArrayList<>();
          expr.accept(new FindDefCallVisitor(result, arguments));
          if (!arguments.isEmpty()) {
            String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + newConstructor.getName();
            myErrors.add(new TypeCheckingError(msg, constructor.getArgument(i).getType()));
            continue constructors_loop;
          }
        }
      }

      Universe maxUniverse = universe.max(newConstructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + newConstructor.getUniverse() + " of constructor " + newConstructor.getName() + " is not comparable to universe " + universe + " of previous constructors";
        myErrors.add(new TypeCheckingError(msg, null));
        continue;
      }
      universe = maxUniverse;

      constructors.add(newConstructor);
      newConstructor.setDataType(result);
    }
    myGlobalContext.remove(def.getName());

    result.setUniverse(universe);
    trimToSize(localContext, origSize);
    if (def.getUniverse() != null && !universe.lessOrEquals(def.getUniverse())) {
      myErrors.add(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null));
      return null;
    }

    return result;
  }

  @Override
  public Constructor visitConstructor(Abstract.Constructor def, List<Binding> localContext) {
    List<TypeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    int index = 1;
    String error = null;
    for (Abstract.TypeArgument argument : def.getArguments()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not comparable to universe " + universe + " of previous arguments";
      } else {
        universe = maxUniverse;
      }

      if (argument instanceof TelescopeArgument) {
        arguments.add(Tele(argument.getExplicit(), ((TelescopeArgument) argument).getNames(), result.expression));
        for (String name : ((TelescopeArgument) argument).getNames()) {
          localContext.add(new TypedBinding(name, result.expression));
        }
        index += ((TelescopeArgument) argument).getNames().size();
      } else {
        arguments.add(TypeArg(argument.getExplicit(), result.expression));
        localContext.add(new TypedBinding(null, result.expression));
        ++index;
      }
    }

    trimToSize(localContext, origSize);
    Constructor newConstructor = new Constructor(def.getDataType().getConstructors().indexOf(def), def.getName(), def.getPrecedence(), def.getFixity(), universe, arguments, null);
    if (error != null) {
      myErrors.add(new TypeCheckingError(error, DefCall(newConstructor)));
      return null;
    } else {
      return newConstructor;
    }
  }
}
