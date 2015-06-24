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

import static com.jetbrains.jetpad.vclang.term.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.term.error.TypeCheckingError.getNames;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<List<Binding>, Void> {
  private final Definition myResult;
  private final List<TypeCheckingError> myErrors;

  public DefinitionCheckTypeVisitor(Definition result, List<TypeCheckingError> errors) {
    myResult = result;
    myErrors = errors;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, List<Binding> localContext) {
    FunctionDefinition functionResult = (FunctionDefinition) myResult;
    List<TelescopeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    for (Abstract.TelescopeArgument argument : def.getArguments()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      arguments.add(Tele(argument.getExplicit(), argument.getNames(), result.expression));
      for (int i = 0; i < argument.getNames().size(); ++i) {
        localContext.add(new TypedBinding(argument.getNames().get(i), result.expression.liftIndex(0, i)));
      }
    }

    Expression expectedType = null;
    if (def.getResultType() != null) {
      CheckTypeVisitor.OKResult typeResult = new CheckTypeVisitor(localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(def.getResultType(), Universe());
      if (typeResult != null) {
        expectedType = typeResult.expression;
      }
    }

    functionResult.setArguments(arguments);
    functionResult.setResultType(expectedType);
    functionResult.typeHasErrors(false);
    functionResult.hasErrors(false);
    CheckTypeVisitor.OKResult termResult = new CheckTypeVisitor(localContext, myErrors, CheckTypeVisitor.Side.LHS).checkType(def.getTerm(), expectedType);

    if (termResult != null) {
      functionResult.setTerm(termResult.expression);
      functionResult.setResultType(termResult.type);
      functionResult.typeHasErrors(false);
      functionResult.hasErrors(false);

      if (!termResult.expression.accept(new TerminationCheckVisitor(functionResult))) {
        myErrors.add(new TypeCheckingError("Termination check failed", def.getTerm(), getNames(localContext)));
        termResult = null;
      }
    } else {
      if (functionResult.getResultType() == null) {
        functionResult.typeHasErrors(true);
      }
    }

    if (termResult == null) {
      functionResult.setTerm(null);
      functionResult.hasErrors(true);
    }
    trimToSize(localContext, origSize);

    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, List<Binding> localContext) {
    DataDefinition dataResult = (DataDefinition) myResult;
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (Abstract.TypeArgument parameter : def.getParameters()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(parameter.getType(), Universe());
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

    dataResult.setUniverse(def.getUniverse() != null ? def.getUniverse() : universe);
    dataResult.setParameters(parameters);
    dataResult.hasErrors(false);

    constructors_loop:
    for (int i = 0; i < def.getConstructors().size(); ++i) {
      new DefinitionCheckTypeVisitor(dataResult.getConstructors().get(i), myErrors).visitConstructor(def.getConstructors().get(i), localContext);
      if (dataResult.getConstructors().get(i).hasErrors()) {
        continue;
      }

      for (int j = 0; j < dataResult.getConstructors().get(i).getArguments().size(); ++j) {
        Expression type = dataResult.getConstructors().get(i).getArguments().get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(dataResult))) {
              String msg = "Non-positive recursive occurrence of data type " + dataResult.getName() + " in constructor " + dataResult.getConstructors().get(i).getName();
              myErrors.add(new TypeCheckingError(msg, def.getConstructors().get(i).getArguments().get(j).getType(), getNames(localContext)));
              continue constructors_loop;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(dataResult))) {
            String msg = "Non-positive recursive occurrence of data type " + dataResult.getName() + " in constructor " + dataResult.getConstructors().get(i).getName();
            myErrors.add(new TypeCheckingError(msg, def.getConstructors().get(i).getArguments().get(j).getType(), getNames(localContext)));
            continue constructors_loop;
          }
        }
      }

      Universe maxUniverse = universe.max(dataResult.getConstructors().get(i).getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + dataResult.getConstructors().get(i).getUniverse() + " of constructor " + dataResult.getConstructors().get(i).getName() + " is not compatible with universe " + universe + " of previous constructors";
        myErrors.add(new TypeCheckingError(msg, null, null));
        continue;
      }
      universe = maxUniverse;
    }

    dataResult.setUniverse(universe);
    trimToSize(localContext, origSize);
    if (def.getUniverse() != null && !universe.lessOrEquals(def.getUniverse())) {
      myErrors.add(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, List<Binding> localContext) {
    List<TypeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    int index = 1;
    String error = null;
    for (Abstract.TypeArgument argument : def.getArguments()) {
      CheckTypeVisitor.OKResult result = new CheckTypeVisitor(localContext, myErrors, CheckTypeVisitor.Side.RHS).checkType(argument.getType(), Universe());
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
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
    myResult.setUniverse(universe);
    ((Constructor) myResult).setArguments(arguments);
    myResult.hasErrors(false);
    if (error != null) {
      myErrors.add(new TypeCheckingError(error, DefCall(myResult), new ArrayList<String>()));
    }
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, List<Binding> localContext) {
    TypeCheckingError error = null;
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (Definition field : ((ClassDefinition) myResult).getFields()) {
      if (field.isAbstract()) {
        Universe maxUniverse = universe.max(field.getUniverse());
        if (maxUniverse == null) {
          String msg = "Universe " + field.getUniverse() + " of field " + field.getName() + " is not compatible with universe " + universe + " of previous fields";
          error = new TypeCheckingError(msg, null, null);
        } else {
          universe = maxUniverse;
        }
      }
    }

    if (error != null) {
      myErrors.add(error);
    }
    myResult.setUniverse(universe);
    myResult.hasErrors(false);
    return null;
  }
}
