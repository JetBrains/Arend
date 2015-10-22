package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.FindDefCallVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.TerminationCheckVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.typeOfFunctionArg;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Void, Definition> {
  private NamespaceMember myNamespaceMember;
  private final List<Binding> myContext;
  private Namespace myNamespace;
  private final ErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(Namespace namespace, ErrorReporter errorReporter) {
    myNamespaceMember = null;
    myContext = new ArrayList<>();
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  private DefinitionCheckTypeVisitor(NamespaceMember namespaceMember, List<Binding> context, Namespace namespace, ErrorReporter errorReporter) {
    myNamespaceMember = namespaceMember;
    myContext = context;
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  public void setNamespaceMember(NamespaceMember namespaceMember) {
    myNamespaceMember = namespaceMember;
  }

  public static void typeCheck(NamespaceMember namespaceMember, List<Binding> context, Namespace namespace, ErrorReporter errorReporter) {
    if (namespaceMember != null && !namespaceMember.isTypeChecked()) {
      DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(namespaceMember, context, namespace, errorReporter);
      namespaceMember.definition = namespaceMember.abstractDefinition.accept(visitor, null);
    }
  }

  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, Void params) {
    Name name = def.getName();
    FunctionDefinition typedDef = new FunctionDefinition(myNamespace, name, def.getPrecedence(), def.getArrow());
    typeCheckStatements(null, def.getStatements(), myNamespace.getChild(name));
    /*
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + name + " in the parent class", def, getNames(myContext)));
      myErrorReporter.report(new TypeCheckingError("Overridden function " + name + " cannot be defined in a base class", def, getNames(myContext)));
      return null;
    }
    */

    List<? extends Abstract.Argument> arguments = def.getArguments();
    List<Argument> typedArguments = new ArrayList<>(arguments.size());
    CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter);

    /*
    List<TypeArgument> splitArgs = null;
    Expression splitResult = null;
    if (overriddenFunction != null) {
      splitArgs = new ArrayList<>();
      splitResult = splitArguments(overriddenFunction.getType(), splitArgs);
    }

    int index = 0;
    if (splitArgs != null) {
      for (Abstract.Argument argument : arguments) {
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
          myErrorReporter.report(new TypeCheckingError("Type of the argument does not match the type in the overridden function", argument, null));
          return null;
        }
      }

      if (index == -1) {
        myErrorReporter.report(new TypeCheckingError("Function has more arguments than overridden function", def, null));
        return null;
      }
    }
    */

    // int numberOfArgs = index;
    int index = 0;
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : arguments) {
        if (argument instanceof Abstract.TypeArgument) {
          CheckTypeVisitor.OKResult result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
          if (result == null) return typedDef;

          // boolean ok = true;
          if (argument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
            typedArguments.add(Tele(argument.getExplicit(), names, result.expression));
            for (int i = 0; i < names.size(); ++i) {
            /*
            if (splitArgs != null) {
              List<CompareVisitor.Equation> equations = new ArrayList<>(0);
              CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
              if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
                ok = false;
                break;
              }
            }
            */

              myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
              ++index;
            }
          } else {
          /*
          if (splitArgs != null) {
            List<CompareVisitor.Equation> equations = new ArrayList<>(0);
            CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
            if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
              ok = false;
            }
          }
          */

            // if (ok) {
            typedArguments.add(TypeArg(argument.getExplicit(), result.expression));
            myContext.add(new TypedBinding((Name) null, result.expression));
            ++index;
            // }
          }

        /*
        if (!ok) {
          myErrorReporter.report(new ArgInferenceError(typedDef.getNamespace().getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(name)));
          return null;
        }
        */
        } else {
          // if (splitArgs == null) {
          myErrorReporter.report(new ArgInferenceError(typedDef.getParentNamespace().getResolvedName(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(name)));
          return typedDef;
        /*
        } else {
          List<String> names = new ArrayList<>(1);
          names.add(((Abstract.NameArgument) argument).getName());
          typedArguments.add(Tele(argument.getExplicit(), names, splitArgs.get(index).getType()));
          myContext.add(new TypedBinding(names.get(0), splitArgs.get(index).getType()));
        }
        */
        }
      }

    /*
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
    */

      Expression expectedType = null;
      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        CheckTypeVisitor.OKResult typeResult = visitor.checkType(resultType, Universe());
        if (typeResult != null) {
          expectedType = typeResult.expression;
        /*
        if (overriddenResultType != null) {
          List<CompareVisitor.Equation> equations = new ArrayList<>(0);
          CompareVisitor.Result cmpResult = compare(expectedType, overriddenResultType, equations);
          if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
            myErrorReporter.report(new TypeCheckingError("Result type of the function does not match the result type in the overridden function", resultType, null));
            return null;
          }
        }
        */
        }
      }

    /*
    if (expectedType == null) {
      expectedType = overriddenResultType;
    }
    */

      typedDef.setArguments(typedArguments);
      typedDef.setResultType(expectedType);
      typedDef.typeHasErrors(typedDef.getResultType() == null);

      myNamespaceMember.definition = typedDef;
      Abstract.Expression term = def.getTerm();
      if (term != null) {
        visitor.setArgsStartCtxIndex(0);
        CheckTypeVisitor.OKResult termResult = visitor.checkType(term, expectedType);

        if (termResult != null) {
          typedDef.setTerm(termResult.expression);
          if (expectedType == null) {
            typedDef.setResultType(termResult.type);
          }

          if (!termResult.expression.accept(new TerminationCheckVisitor(/* overriddenFunction == null ? */ typedDef /* : overriddenFunction */))) {
            myErrorReporter.report(new TypeCheckingError("Termination check failed", term, getNames(myContext)));
            termResult = null;
          }
        }

        if (termResult == null) {
          typedDef.setTerm(null);
        }
      }

      if (typedDef.getTerm() != null || typedDef.isAbstract()) {
        typedDef.hasErrors(false);
      }

      typedDef.typeHasErrors(typedDef.getResultType() == null);
      if (typedDef.typeHasErrors()) {
        typedDef.hasErrors(true);
      }
      Expression type = typedDef.getType();
      if (type != null) {
        type = type.getType(new ArrayList<Binding>(2));
        if (type instanceof UniverseExpression) {
          typedDef.setUniverse(((UniverseExpression) type).getUniverse());
        } else {
          throw new IllegalStateException();
        }
      }
    }
    /*
    if (typedDef instanceof OverriddenDefinition) {
      ((OverriddenDefinition) typedDef).setOverriddenFunction(overriddenFunction);
    }
    */

    return typedDef;
  }

  @Override
  public ClassField visitAbstract(Abstract.AbstractDefinition def, Void params) {
    Name name = def.getName();
    List<? extends Abstract.Argument> arguments = def.getArguments();
    List<TypeArgument> typedArguments = new ArrayList<>(arguments.size());
    Expression typedResultType;
    CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter);
    Universe universe = new Universe.Type(0, Universe.Type.PROP);

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      int index = 0;
      for (Abstract.Argument argument : arguments) {
        if (argument instanceof Abstract.TypeArgument) {
          CheckTypeVisitor.OKResult result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
          if (result == null) {
            return null;
          }

          if (argument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
            typedArguments.add(Tele(argument.getExplicit(), names, result.expression));
            for (int i = 0; i < names.size(); ++i) {
              myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
              ++index;
            }
          } else {
            typedArguments.add(TypeArg(argument.getExplicit(), result.expression));
            myContext.add(new TypedBinding((Name) null, result.expression));
            ++index;
          }

          Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
          Universe maxUniverse = universe.max(argUniverse);
          if (maxUniverse == null) {
            String error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not compatible with universe " + universe + " of previous arguments";
            myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, def, new ArrayList<String>()));
            return null;
          } else {
            universe = maxUniverse;
          }
        } else {
          myErrorReporter.report(new ArgInferenceError(myNamespace.getResolvedName(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(name)));
          return null;
        }
      }

      Abstract.Expression resultType = def.getResultType();
      if (resultType == null) {
        return null;
      }
      CheckTypeVisitor.OKResult typeResult = visitor.checkType(resultType, Universe());
      if (typeResult == null) {
        return null;
      }
      typedResultType = typeResult.expression;

      Universe resultTypeUniverse = ((UniverseExpression) typeResult.type).getUniverse();
      Universe maxUniverse = universe.max(resultTypeUniverse);
      if (maxUniverse == null) {
        String error = "Universe " + resultTypeUniverse + " of the result type is not compatible with universe " + universe + " of arguments";
        myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, def, new ArrayList<String>()));
        return null;
      } else {
        universe = maxUniverse;
      }
    }

    ClassField typedDef = new ClassField(myNamespace, name, def.getPrecedence(), Pi(typedArguments, typedResultType));
    Expression type = typedDef.getType().getType(myContext).normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof UniverseExpression) {
      typedDef.setUniverse(universe);
    }
    myNamespaceMember.definition = typedDef;
    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Void params) {
    List<? extends Abstract.TypeArgument> parameters = def.getParameters();
    List<TypeArgument> typedParameters = new ArrayList<>(parameters.size());
    DataDefinition dataDefinition;
    Universe universe = def.getUniverse();
    Universe typedUniverse = new Universe.Type(0, Universe.Type.PROP);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter);
      for (Abstract.TypeArgument parameter : parameters) {
        CheckTypeVisitor.OKResult result = visitor.checkType(parameter.getType(), Universe());
        if (result == null) return null;
        if (parameter instanceof Abstract.TelescopeArgument) {
          typedParameters.add(Tele(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression));
          List<String> names = ((Abstract.TelescopeArgument) parameter).getNames();
          for (int i = 0; i < names.size(); ++i) {
            myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
          }
        } else {
          typedParameters.add(TypeArg(parameter.getExplicit(), result.expression));
          myContext.add(new TypedBinding((Name) null, result.expression));
        }
      }

      Name name = def.getName();
      dataDefinition = new DataDefinition(myNamespace, name, def.getPrecedence(), universe != null ? universe : new Universe.Type(0, Universe.Type.PROP), typedParameters);
      myNamespaceMember.definition = dataDefinition;

      myNamespace = myNamespace.getChild(name);
      for (Abstract.Constructor constructor : def.getConstructors()) {
        Constructor typedConstructor = visitConstructor(constructor, dataDefinition);
        if (typedConstructor == null) {
          continue;
        }

        NamespaceMember member = myNamespace.getMember(constructor.getName().name);
        if (member == null) {
          continue;
        }
        member.definition = typedConstructor;

        Universe maxUniverse = typedUniverse.max(member.definition.getUniverse());
        if (maxUniverse == null) {
          String msg = "Universe " + member.definition.getUniverse() + " of constructor " + constructor.getName() + " is not compatible with universe " + typedUniverse + " of previous constructors";
          myErrorReporter.report(new TypeCheckingError(msg, null, null));
        } else {
          typedUniverse = maxUniverse;
        }
      }
      myNamespace = myNamespace.getParent();
    }

    if (universe != null) {
      if (typedUniverse.lessOrEquals(universe)) {
        dataDefinition.setUniverse(universe);
      } else {
        myErrorReporter.report(new TypeMismatchError(new UniverseExpression(universe), new UniverseExpression(typedUniverse), null, new ArrayList<String>()));
        dataDefinition.setUniverse(typedUniverse);
      }
    }

    return dataDefinition;
  }

  @Override
  public Definition visitConstructor(Abstract.Constructor def, Void params) {
    throw new IllegalStateException();
  }

  public Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition) {
    try (Utils.CompleteContextSaver ignored = new Utils.CompleteContextSaver<>(myContext)) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      List<TypeArgument> typeArguments = new ArrayList<>(arguments.size());
      Universe universe = new Universe.Type(0, Universe.Type.PROP);
      int index = 1;
      boolean ok = true;

      CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter);
      List<? extends Abstract.Pattern> patterns = def.getPatterns();
      List<Pattern> typedPatterns = null;
      if (patterns != null) {
        typedPatterns = new ArrayList<>();

        ProcessImplicitResult processImplicitResult = processImplicit(patterns, dataDefinition.getParameters());
        if (processImplicitResult.patterns == null) {
          if (processImplicitResult.numExcessive != 0) {
            myErrorReporter.report(new TypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", def, getNames(myContext)));
          } else if (processImplicitResult.wrongImplicitPosition < typedPatterns.size()) {
            myErrorReporter.report(new TypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition), getNames(myContext)));
          } else {
            myErrorReporter.report(new TypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, def, getNames(myContext)));
          }
          return null;
        }
        List<Abstract.Pattern> processedPatterns = processImplicitResult.patterns;
        for (int i = 0; i < processImplicitResult.patterns.size(); i++) {
          CheckTypeVisitor.ExpandPatternResult result = visitor.expandPatternOn(processedPatterns.get(i), processedPatterns.size() - 1 - i);
          if (result == null || result instanceof CheckTypeVisitor.ExpandPatternErrorResult)
            return null;
          typedPatterns.add(((CheckTypeVisitor.ExpandPatternOKResult)result).pattern);
        }
      }

      for (Abstract.TypeArgument argument : arguments) {
        CheckTypeVisitor.OKResult result = visitor.checkType(argument.getType(), Universe());
        if (result == null) {
          return null;
        }

        Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
        Universe maxUniverse = universe.max(argUniverse);
        if (maxUniverse == null) {
          String error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not compatible with universe " + universe + " of previous arguments";
          myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), error, def, new ArrayList<String>()));
          ok = false;
        } else {
          universe = maxUniverse;
        }

        if (argument instanceof Abstract.TelescopeArgument) {
          typeArguments.add(Tele(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), result.expression));
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          for (int i = 0; i < names.size(); ++i) {
            myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
          }
          index += ((Abstract.TelescopeArgument) argument).getNames().size();
        } else {
          typeArguments.add(TypeArg(argument.getExplicit(), result.expression));
          myContext.add(new TypedBinding((Name) null, result.expression));
          ++index;
        }
      }

      if (!ok) {
        return null;
      }

      Name name = def.getName();

      for (int j = 0; j < typeArguments.size(); ++j) {
        Expression type = typeArguments.get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(dataDefinition))) {
              String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
              myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), msg, arguments.get(j).getType(), getNames(myContext)));
              return null;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(dataDefinition))) {
            String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
            myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), msg, arguments.get(j).getType(), getNames(myContext)));
            return null;
          }
        }
      }

      Constructor constructor = new Constructor(dataDefinition.getParentNamespace().getChild(dataDefinition.getName()), name, def.getPrecedence(), universe, typeArguments, dataDefinition, typedPatterns);
      dataDefinition.addConstructor(constructor);
      dataDefinition.getParentNamespace().addDefinition(constructor);
      return constructor;
    }
  }

  private void typeCheckStatements(ClassDefinition classDefinition, Collection<? extends Abstract.Statement> statements, Namespace namespace) {
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
        NamespaceMember member = namespace.getMember(definition.getName().name);
        if (member != null) {
          typeCheck(member, myContext, namespace, myErrorReporter);
          if (classDefinition != null && member.definition instanceof ClassField) {
            classDefinition.addField((ClassField) member.definition);
          }
        }
      }
    }
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, Void params) {
    Name name = def.getName();
    ClassDefinition typedDef = new ClassDefinition(myNamespace, name);
    typeCheckStatements(typedDef, def.getStatements(), myNamespace.getChild(name));
    return typedDef;
  }
}
