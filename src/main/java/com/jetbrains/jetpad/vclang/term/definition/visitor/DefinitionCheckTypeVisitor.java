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
import com.jetbrains.jetpad.vclang.term.expr.visitor.TerminationCheckVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.term.error.TypeCheckingError.getNames;
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
      for (int i = 0; i < argument.getNames().size(); ++i) {
        localContext.add(new TypedBinding(argument.getName(i), result.expression.liftIndex(0, i)));
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

    FunctionDefinition result = new FunctionDefinition(def.getName(), def.getPrecedence(), def.getFixity(), arguments, expectedType, def.getArrow(), null);
    myGlobalContext.put(def.getName(), result);
    CheckTypeVisitor.OKResult termResult = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.LHS).checkType(def.getTerm(), expectedType);

    if (termResult != null && !termResult.expression.accept(new TerminationCheckVisitor(result))) {
      myErrors.add(new TypeCheckingError("Termination check failed", def.getTerm(), getNames(localContext)));
      termResult = null;
    }

    if (termResult != null) {
      result.setTerm(termResult.expression);
      if (expectedType == null) {
        result.setResultType(termResult.type);
      }
    }
    trimToSize(localContext, origSize);

    if (termResult == null && expectedType == null) {
      myGlobalContext.remove(def.getName());
      return null;
    }

    return result;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, List<Binding> localContext) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (Abstract.TypeArgument parameter : def.getParameters()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(myGlobalContext, localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(parameter.getType(), Universe());
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

    List<Constructor> constructors = new ArrayList<>(def.getConstructors().size());
    DataDefinition result = new DataDefinition(def.getName(), def.getPrecedence(), def.getFixity(), def.getUniverse() != null ? def.getUniverse() : universe, parameters, constructors);

    myGlobalContext.put(def.getName(), result);
    constructors_loop:
    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor newConstructor = visitConstructor(constructor, localContext);
      if (newConstructor == null) {
        continue;
      }

      for (int i = 0; i < newConstructor.getArguments().size(); ++i) {
        Expression type = newConstructor.getArgument(i).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(result))) {
              String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + newConstructor.getName();
              myErrors.add(new TypeCheckingError(msg, constructor.getArgument(i).getType(), getNames(localContext)));
              continue constructors_loop;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(result))) {
            String msg = "Non-positive recursive occurrence of data type " + result.getName() + " in constructor " + newConstructor.getName();
            myErrors.add(new TypeCheckingError(msg, constructor.getArgument(i).getType(), getNames(localContext)));
            continue constructors_loop;
          }
        }
      }

      Universe maxUniverse = universe.max(newConstructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + newConstructor.getUniverse() + " of constructor " + newConstructor.getName() + " is not comparable to universe " + universe + " of previous constructors";
        myErrors.add(new TypeCheckingError(msg, null, null));
        continue;
      }
      universe = maxUniverse;

      constructors.add(newConstructor);
      newConstructor.setDataType(result);
    }

    result.setUniverse(universe);
    trimToSize(localContext, origSize);
    if (def.getUniverse() != null && !universe.lessOrEquals(def.getUniverse())) {
      myErrors.add(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
      return null;
    }

    return result;
  }

  @Override
  public Constructor visitConstructor(Abstract.Constructor def, List<Binding> localContext) {
    List<TypeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
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
    Constructor newConstructor = new Constructor(def.getDataType().getConstructors().indexOf(def), def.getName(), def.getPrecedence(), def.getFixity(), universe, arguments, null);
    if (error != null) {
      myErrors.add(new TypeCheckingError(error, DefCall(newConstructor), new ArrayList<String>()));
      return null;
    } else {
      myGlobalContext.put(newConstructor.getName(), newConstructor);
      return newConstructor;
    }
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, List<Binding> localContext) {
    List<Definition> fields = new ArrayList<>(def.getFields().size());
    Universe universe = new Universe.Type(0, Universe.Type.PROP);

    for (Abstract.Definition field : def.getFields()) {
      Definition newField = field.accept(this, localContext);
      if (newField == null) continue;

      if (newField instanceof FunctionDefinition && ((FunctionDefinition) newField).getArrow() == null) {
        Universe maxUniverse = universe.max(newField.getUniverse());
        if (maxUniverse == null) {
          String msg = "Universe " + newField.getUniverse() + " of field " + newField.getName() + " is not comparable to universe " + universe + " of previous fields";
          myErrors.add(new TypeCheckingError(msg, null, null));
          continue;
        }
        universe = maxUniverse;
      }

      fields.add(newField);
    }

    return new ClassDefinition(def.getName(), universe, fields);
  }
}
