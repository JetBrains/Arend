package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.typeOfFunctionArg;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Void, Definition> {
  private final TypecheckerState myState;
  private final ErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(TypecheckerState state, ErrorReporter errorReporter) {
    myState = state;
    myErrorReporter = errorReporter;
  }

  public static void typeCheck(TypecheckerState state, Abstract.Definition definition, ErrorReporter errorReporter) {
    if (state.getTypechecked(definition) == null) {
      definition.accept(new DefinitionCheckTypeVisitor(state, errorReporter), null);
    }
  }

  private ClassDefinition getParentClass(Abstract.Definition definition) {
    Definition typechecked = myState.getTypechecked(definition);
    return typechecked instanceof ClassDefinition ? (ClassDefinition) typechecked : null;
  }

  private ClassDefinition getParentClass(Abstract.Definition definition, Abstract.DefineStatement dynamicStatement) {
    if (definition instanceof Abstract.ClassDefinition) {
      Definition classDefinition = myState.getTypechecked(definition);
      if (classDefinition instanceof ClassDefinition) {
        return (ClassDefinition) classDefinition;
      } else {
        myErrorReporter.report(new TypeCheckingError(definition, "Internal error: definition '" + definition.getName() + "' is not a class", null));
        return null;
      }
    }

    Abstract.DefineStatement statement = definition == null ? null : definition.getParentStatement();
    Abstract.Definition parentDefinition = statement == null ? null : statement.getParentDefinition();
    if (statement == null || parentDefinition == null) {
      // FIXME[errorformat] report dynamicStatement
      myErrorReporter.report(new TypeCheckingError(definition, "Non-static definitions are allowed only inside a class definition", null));
      return null;
    }

    return getParentClass(parentDefinition, dynamicStatement);
  }

  private ClassDefinition getThisClass(Abstract.Definition definition) {
    Abstract.DefineStatement statement = definition.getParentStatement();
    if (statement == null) {
      return null;
    }
    Abstract.Definition parentDefinition = statement.getParentDefinition();

    if (statement.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
      return getParentClass(parentDefinition, statement);
    } else {
      return parentDefinition != null ? getThisClass(parentDefinition) : null;
    }
  }

  private Binding visitPolyParam(Abstract.TypeArgument typeArgument, Map<String, Binding> polyParams, Abstract.SourceNode node) {
    assert (typeArgument.getType() instanceof Abstract.DefCallExpression);
    String typeName = ((Abstract.DefCallExpression) typeArgument.getType()).getName();
    if (!(typeArgument instanceof Abstract.TelescopeArgument)) {
      myErrorReporter.report(new TypeCheckingError("Parameter of type " + typeName + " must have name", node));
      return null;
    }
    Abstract.TelescopeArgument teleArgument = (Abstract.TelescopeArgument)typeArgument;
    if (teleArgument.getNames().size() > 1 || polyParams.containsKey(typeName)) {
      myErrorReporter.report(new TypeCheckingError("Function definition must have at most one polymorphic variable of type " + typeName, node));
      return null;
    }
    if (teleArgument.getExplicit()) {
      myErrorReporter.report(new TypeCheckingError("Polymorphic variables must be implicit", node));
      return null;
    }
    Binding levelParam = new TypedBinding(((Abstract.TelescopeArgument) typeArgument).getNames().get(0), Preprelude.levelTypeByName(typeName));
    polyParams.put(typeName, levelParam);
    return levelParam;
  }

  @Override
  public FunctionDefinition visitFunction(final Abstract.FunctionDefinition def, Void params) {
    String name = def.getName();
    Abstract.Definition.Arrow arrow = def.getArrow();
    final FunctionDefinition typedDef = new FunctionDefinition(def.getName(), def.getPrecedence(), SimpleStaticNamespaceProvider.INSTANCE.forDefinition(def));
    myState.record(def, typedDef);
    // TODO[scopes] Fill namespace

    /*
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + name + " in the parent class", def, getNames(context)));
      myErrorReporter.report(new TypeCheckingError("Overridden function " + name + " cannot be defined in a base class", def, getNames(context)));
      return null;
    }
    */

    List<? extends Abstract.Argument> arguments = def.getArguments();
    final List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).build(def);
    ClassDefinition thisClass = getThisClass(def);
    LinkList list = new LinkList();
    if (thisClass != null) {
      DependentLink thisParam = param("\\this", ClassCall(thisClass));
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(thisClass, Reference(thisParam));
      typedDef.setThisClass(thisClass);
    }

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
            index++;
          }
        } else {
          if (splitArgs.get(index).getExplicit() != argument.getExplicit()) {
            ok = false;
          } else {
            index++;
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

    Map<String, Binding> polyParams = new HashMap<>();
    // int numberOfArgs = index;
    int index = 0;
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument)argument;

        if (Preprelude.isPolyParam(typeArgument)) {
          Binding levelParam = visitPolyParam(typeArgument, polyParams, def);
          if (levelParam == null) {
            return typedDef;
          }
          context.add(levelParam);
          //polyParams.put(((Abstract.DefCallExpression)typeArgument.getType()).getName(), levelParam);
          ++index;
          continue;
        }

        CheckTypeVisitor.Result result = visitor.checkType(typeArgument.getType(), Universe());
        if (result == null) return typedDef;

        DependentLink param;
        // boolean ok = true;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          param = param(argument.getExplicit(), names, result.expression);
          index += names.size();
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
          param = param(argument.getExplicit(), (String) null, result.expression);
          index++;
          // }
        }
        list.append(param);
        context.addAll(toContext(param));


    /*
    if (!ok) {
      myErrorReporter.report(new ArgInferenceError(typedDef.getNamespace().getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(name)));
      return null;
    }
    */
      } else {
        // if (splitArgs == null) {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0], new LevelExpression[0]));
        return typedDef;
    /*
    } else {
      List<String> names = new ArrayList<>(1);
      names.add(((Abstract.NameArgument) argument).getName());
      typedParameters.add(Tele(argument.getExplicit(), names, splitArgs.get(index).getType()));
      context.add(new TypedBinding(names.get(0), splitArgs.get(index).getType()));
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
      for (; numberOfArgs < splitArgs.size(); numberOfArgs++) {
        args.add(splitArgs.get(numberOfArgs));
      }
      overriddenResultType = Pi(args, splitResult);
    }
  }
  */

    Expression expectedType = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      CheckTypeVisitor.Result typeResult = visitor.checkType(resultType, Universe());
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

    typedDef.setPolyParams(new ArrayList<>(polyParams.values()));
    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.typeHasErrors(typedDef.getResultType() == null);

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        context.subList(context.size() - size(list.getFirst()), context.size()).clear();
        TypeCheckingElim.Result elimResult = visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, def.getArrow() == Abstract.Definition.Arrow.LEFT ? list.getFirst() : null, expectedType, false);
        if (elimResult != null) {
          elimResult.update(false);
          elimResult.reportErrors(myErrorReporter);
          typedDef.setElimTree(elimResult.elimTree);
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, expectedType);
        if (termResult != null) {
          typedDef.setElimTree(top(list.getFirst(), leaf(def.getArrow(), termResult.expression)));
          if (expectedType == null)
            typedDef.setResultType(termResult.type);
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, typedDef.getParameters()), null)) {
          // FIXME[errorformat]
          myErrorReporter.report(new TypeCheckingError(def, "Termination check failed", term));
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        TypeCheckingError error = TypeCheckingElim.checkCoverage(def, list.getFirst(), typedDef.getElimTree(), expectedType);
        if (error != null) {
          myErrorReporter.report(error);
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        typedDef.hasErrors(false); // we need normalization here
        TypeCheckingError error = TypeCheckingElim.checkConditions(def, list.getFirst(), typedDef.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          typedDef.setElimTree(null);
        }
      }
    }

    if (typedDef.getElimTree() == null && arrow != null) {
      typedDef.hasErrors(true);
    }

    typedDef.typeHasErrors(typedDef.getResultType() == null);
    if (typedDef.typeHasErrors()) {
      typedDef.hasErrors(true);
    }
    Expression type = typedDef.getType();
    if (type != null) {
      UniverseExpression universeType = type.getType().normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      if (universeType != null) {
        typedDef.setUniverse(universeType.getUniverse());
      } else {
        throw new IllegalStateException();
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
    throw new IllegalStateException();
  }

  public ClassField visitAbstract(Abstract.AbstractDefinition def, ClassDefinition thisClass) {
    List<? extends Abstract.Argument> arguments = def.getArguments();
    Expression typedResultType;
    DependentLink thisParameter = param("\\this", ClassCall(thisClass));
    List<Binding> context = new ArrayList<>();
    context.add(thisParameter);
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).thisClass(thisClass, Reference(thisParameter)).build(def);
    LevelExpression plevel = null;
    ClassField typedDef = new ClassField(def.getName(), def.getPrecedence(), Error(null, null), thisClass, thisParameter);
    myState.record(def, typedDef);

    Map<String, Binding> polyParams = new HashMap<>();
    int index = 0;
    LinkList list = new LinkList();
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) argument;

        if (Preprelude.isPolyParam(typeArgument)) {
          Binding levelParam = visitPolyParam(typeArgument, polyParams, def);
          if (levelParam == null) {
            return typedDef;
          }
          context.add(levelParam);
          ++index;
          continue;
        }

        CheckTypeVisitor.Result result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
        if (result == null) {
          return typedDef;
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          param = param(argument.getExplicit(), names, result.expression);
          index += names.size();
        } else {
          param = param(argument.getExplicit(), (String) null, result.expression);
          index++;
        }
        list.append(param);
        context.addAll(toContext(param));

        LevelExpression argPLevel = result.type.toUniverse().getUniverse().getPLevel();
        if (plevel == null) {
          plevel = argPLevel;
        } else {
          plevel = plevel.max(argPLevel);
        }
      } else {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0], new LevelExpression[0]));
        return typedDef;
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType == null) {
      return typedDef;
    }
    CheckTypeVisitor.Result typeResult = visitor.checkType(resultType, Universe());
    if (typeResult == null) {
      return typedDef;
    }
    typedResultType = typeResult.expression;

    TypeUniverse resultTypeUniverse = typeResult.type.toUniverse().getUniverse();
    if (plevel == null) {
      plevel = resultTypeUniverse.getPLevel();
    } else {
      plevel = plevel.max(resultTypeUniverse.getPLevel());
    }

    typedDef.hasErrors(false);
    typedDef.setPolyParams(new ArrayList<>(polyParams.values()));
    typedDef.setBaseType(list.isEmpty() ? typedResultType : Pi(list.getFirst(), typedResultType));
    typedDef.setUniverse(new TypeUniverse(plevel, resultTypeUniverse.getHLevel()));
    typedDef.setThisClass(thisClass);
    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Void params) {
    List<? extends Abstract.TypeArgument> parameters = def.getParameters();

    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).build(def);
    ClassDefinition thisClass = getThisClass(def);
    Map<String, Binding> polyParams = new HashMap<>();
    LinkList list = new LinkList();
    if (thisClass != null) {
      DependentLink thisParam = param("\\this", ClassCall(thisClass));
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(thisClass, Reference(thisParam));
    }

    LevelExpression inferredHLevel = (def.getConditions() != null && def.getConditions().size() > 0) ? new LevelExpression() :
            def.getConstructors().size() > 1 ? TypeUniverse.SET.getHLevel() : TypeUniverse.PROP.getHLevel();
    LevelExpression inferredPLevel = TypeUniverse.intToPLevel(0);
    TypeUniverse inferredUniverse = new TypeUniverse(inferredPLevel, inferredHLevel);
    TypeUniverse userUniverse = null;
    DataDefinition dataDefinition = new DataDefinition(def.getName(), def.getPrecedence(), inferredUniverse, null);
    dataDefinition.hasErrors(true);
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      for (Abstract.TypeArgument parameter : parameters) {
        if (Preprelude.isPolyParam(parameter)) {
          Binding levelParam = visitPolyParam(parameter, polyParams, def);
          if (levelParam == null) {
            return dataDefinition;
          }
          context.add(levelParam);
          continue;
        }

        CheckTypeVisitor.Result result = visitor.checkType(parameter.getType(), Universe());
        if (result == null) {
          return dataDefinition;
        }

        DependentLink param;
        if (parameter instanceof Abstract.TelescopeArgument) {
          param = param(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression);
        } else {
          param = param(parameter.getExplicit(), (String) null, result.expression);
        }
        list.append(param);
        context.addAll(toContext(param));
      }

      if (def.getUniverse() != null) {
        CheckTypeVisitor.Result result = visitor.checkType(def.getUniverse(), Universe());

        if (result == null || result.expression.toUniverse() == null) {
          String msg = "Specified type " + def.getUniverse().accept(new PrettyPrintVisitor(new StringBuilder(), 0), Abstract.Expression.PREC) + " of '" + def.getName() + "' is not a universe";
          // FIXME[errorformat]
          myErrorReporter.report(new TypeCheckingError(def, msg, def.getUniverse()));
        } else {
          userUniverse = result.expression.toUniverse().getUniverse();
        }
      }
    }

    dataDefinition.setPolyParams(new ArrayList<>(polyParams.values()));
    dataDefinition.setParameters(list.getFirst());
    if (userUniverse != null) dataDefinition.setUniverse(userUniverse);
    dataDefinition.hasErrors(false);
    dataDefinition.setThisClass(thisClass);
    myState.record(def, dataDefinition);

    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor typedConstructor = visitConstructor(constructor, def, dataDefinition, visitor);
      if (typedConstructor == null) {
        continue;
      }

      myState.record(constructor, typedConstructor);

      inferredUniverse = inferredUniverse.max(typedConstructor.getUniverse());
    }

    if (userUniverse != null) {
      LevelExpression.CMP cmpP = inferredUniverse.getPLevel().compare(userUniverse.getPLevel());
      LevelExpression.CMP cmpH = inferredUniverse.getHLevel().compare(userUniverse.getHLevel());
      if (cmpP == LevelExpression.CMP.NOT_COMPARABLE || cmpP == LevelExpression.CMP.GREATER ||
              cmpH == LevelExpression.CMP.NOT_COMPARABLE || cmpH == LevelExpression.CMP.GREATER) {
        String msg = "Actual universe " + new UniverseExpression(inferredUniverse) + " is not compatible with expected universe " + new UniverseExpression(userUniverse);
        // FIXME[errorformat]
        myErrorReporter.report(new TypeCheckingError(def, msg, def.getUniverse()));
        dataDefinition.setUniverse(inferredUniverse);
      } else {
        dataDefinition.setUniverse(userUniverse);
      }
    } else {
    //  if (def.getConditions() != null && !def.getConditions().isEmpty()) {
     //   dataDefinition.setUniverse(new TypeUniverse(inferredUniverse.getPLevel(), TypeUniverse.intToHLevel(TypeUniverse.NOT_TRUNCATED)));
     // } else {
        dataDefinition.setUniverse(inferredUniverse);
     // }
    }

    context.clear();
    if (def.getConditions() != null) {
      List<Constructor> cycle = typeCheckConditions(visitor, dataDefinition, def);
      if (cycle != null) {
        StringBuilder cycleConditionsError = new StringBuilder();
        cycleConditionsError.append("Conditions form a cycle: ");
        for (Constructor constructor : cycle) {
          cycleConditionsError.append(constructor.getName()).append(" - ");
        }
        cycleConditionsError.append(cycle.get(0).getName());
        // FIXME[errorformat]
        TypeCheckingError error = new TypeCheckingError(def, cycleConditionsError.toString(), def);
        myErrorReporter.report(error);
      }
    }

    if (!dataDefinition.getConditions().isEmpty()) {
      List<Condition> failedConditions = new ArrayList<>();
      for (Condition condition : dataDefinition.getConditions()) {
        TypeCheckingError error = TypeCheckingElim.checkConditions(condition.getConstructor().getName(), def, condition.getConstructor().getParameters(), condition.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          failedConditions.add(condition);
        }
      }
      dataDefinition.getConditions().removeAll(failedConditions);
    }

    return dataDefinition;
  }

  private List<Constructor> typeCheckConditions(CheckTypeVisitor visitor, DataDefinition dataDefinition, Abstract.DataDefinition def) {
    Map<Constructor, List<Abstract.Condition>> condMap = new HashMap<>();
    for (Abstract.Condition cond : def.getConditions()) {
      Constructor constructor = dataDefinition.getConstructor(cond.getConstructorName());
      if (constructor == null) {
        myErrorReporter.report(new NotInScopeError(def, cond.getConstructorName()));  // TODO: refer by reference
        continue;
      }
      if (constructor.hasErrors()) {
        continue;
      }
      if (!condMap.containsKey(constructor)) {
        condMap.put(constructor, new ArrayList<Abstract.Condition>());
      }
      condMap.get(constructor).add(cond);
    }
    List<Constructor> cycle = searchConditionCycle(condMap);
    if (cycle != null) {
      return cycle;
    }
    for (Constructor constructor : condMap.keySet()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
        final List<List<Pattern>> patterns = new ArrayList<>();
        final List<Expression> expressions = new ArrayList<>();
        final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
        visitor.getContext().addAll(toContext(constructor.getDataTypeParameters()));

        for (Abstract.Condition cond : condMap.get(constructor)) {
          try (Utils.ContextSaver saver = new Utils.ContextSaver(visitor.getContext())) {
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getDataTypeExpression()));
            DependentLink params = constructor.getParameters();
            List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(cond, params, cond.getPatterns(), def);
            if (processedPatterns == null)
              continue;

            Patterns typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, constructor.getParameters(), resultType, TypeCheckingElim.PatternExpansionMode.CONDITION);
            if (typedPatterns == null) {
              continue;
            }

            CheckTypeVisitor.Result result = visitor.checkType(cond.getTerm(), resultType.get(0));
            if (result == null)
              continue;

            patterns.add(toPatterns(typedPatterns.getPatterns()));
            expressions.add(result.expression.normalize(NormalizeVisitor.Mode.NF));
            arrows.add(Abstract.Definition.Arrow.RIGHT);
          }
        }

        PatternsToElimTreeConversion.OKResult elimTreeResult = (PatternsToElimTreeConversion.OKResult) PatternsToElimTreeConversion.convert(constructor.getParameters(), patterns, expressions, arrows);

        if (!elimTreeResult.elimTree.accept(new TerminationCheckVisitor(constructor, constructor.getDataTypeParameters(), constructor.getParameters()), null)) {
          // FIXME[errorformat]
          myErrorReporter.report(new TypeCheckingError(def, "Termination check failed", null));
          continue;
        }

        Condition typedCond = new Condition(constructor, elimTreeResult.elimTree);
        dataDefinition.addCondition(typedCond);
        for (Abstract.Condition cond : condMap.get(constructor)) {
          cond.setWellTyped(typedCond);
        }
      }
    }
    return null;
  }

  private List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap) {
    Set<Constructor> visited = new HashSet<>();
    List<Constructor> visiting = new ArrayList<>();
    for (Constructor constructor : condMap.keySet()) {
      List<Constructor> cycle = searchConditionCycle(condMap, constructor, visited, visiting);
      if (cycle != null)
        return cycle;
    }
    return null;
  }

  private List<Constructor> searchConditionCycle(Map<Constructor, List<Abstract.Condition>> condMap, Constructor constructor, Set<Constructor> visited, List<Constructor> visiting) {
    if (visited.contains(constructor))
      return null;
    if (visiting.contains(constructor)) {
      return visiting.subList(visiting.lastIndexOf(constructor), visiting.size());
    }
    visiting.add(constructor);
    if (condMap.containsKey(constructor)) {
      for (Abstract.Condition condition : condMap.get(constructor)) {
        for (Referable def : condition.getTerm().accept(new CollectDefCallsVisitor(), null)) {
          final Definition typeCheckedDef = myState.getTypechecked(def);
          if (typeCheckedDef != null && typeCheckedDef != constructor && typeCheckedDef instanceof Constructor && ((Constructor) typeCheckedDef).getDataType().equals(constructor.getDataType())) {
            List<Constructor> cycle = searchConditionCycle(condMap, (Constructor) typeCheckedDef, visited, visiting);
            if (cycle != null)
              return cycle;
          }
        }
      }
    }
    visiting.remove(visiting.size() - 1);
    visited.add(constructor);
    return null;
  }

  @Override
  public Definition visitConstructor(Abstract.Constructor def, Void params) {
    throw new IllegalStateException();
  }

  public Constructor visitConstructor(Abstract.Constructor def, Abstract.DataDefinition abstractData, DataDefinition dataDefinition, CheckTypeVisitor visitor) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(visitor.getContext())) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      String name = def.getName();
      int index = 1;
      boolean ok = true;

      Constructor constructor = new Constructor(name, def.getPrecedence(), TypeUniverse.PROP, null, dataDefinition, null);
      constructor.hasErrors(true);
      List<? extends Abstract.PatternArgument> patterns = def.getPatterns();
      Patterns typedPatterns = null;
      if (patterns != null) {
        List<Abstract.PatternArgument> processedPatterns = new ArrayList<>(patterns);
        if (dataDefinition.getThisClass() != null) {
          processedPatterns.add(0, new PatternArgument(new NamePattern(dataDefinition.getParameters()), true, true));
        }
        processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), processedPatterns, abstractData);
        if (processedPatterns == null) {
          return constructor;
        }

        typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, dataDefinition.getParameters(), Collections.<Expression>emptyList(), TypeCheckingElim.PatternExpansionMode.DATATYPE);
        if (typedPatterns == null) {
          return constructor;
        }
      } else {
        visitor.getContext().addAll(toContext(dataDefinition.getParameters()));
      }

      if (dataDefinition.getThisClass() != null && typedPatterns != null) {
        visitor.setThisClass(dataDefinition.getThisClass(), Reference(typedPatterns.getParameters()));
      }

      LevelExpression plevel = null;
      LevelExpression hlevel = null;
      LinkList list = new LinkList();
      for (Abstract.TypeArgument argument : arguments) {
        CheckTypeVisitor.Result result = visitor.checkType(argument.getType(), Universe());
        if (result == null) {
          return constructor;
        }

        TypeUniverse argUniverse = result.type.toUniverse().getUniverse();
        if (plevel == null) {
          plevel = argUniverse.getPLevel();
          hlevel = argUniverse.getHLevel();
        } else {
          /*Universe.CompareResult cmp = universe.compare(argUniverse);
          if (cmp == null) {
            String error = "Universe " + argUniverse + " of " + ordinal(index) + " argument is not compatible with universe " + universe + " of previous arguments";
            myErrorReporter.report(new TypeCheckingError(error, def));
            ok = false;
          } else {
            universe = cmp.MaxUniverse;
          }/**/
          plevel = plevel.max(argUniverse.getPLevel());
          hlevel = hlevel.max(argUniverse.getHLevel());
        }

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          param = param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), result.expression);
          index += ((Abstract.TelescopeArgument) argument).getNames().size();
        } else {
          param = param(argument.getExplicit(), (String) null, result.expression);
          index++;
        }
        list.append(param);
        visitor.getContext().addAll(toContext(param));
      }

      if (!ok) {
        return constructor;
      }

      for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext()) {
        Expression type = link.getType().normalize(NormalizeVisitor.Mode.WHNF);
        PiExpression pi = type.toPi();
        while (pi != null) {
          for (DependentLink link1 = pi.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
            link1 = link1.getNextTyped(null);
            if (!checkNonPositiveError(link1.getType(), abstractData, dataDefinition, name, list.getFirst(), link, arguments, def)) {
              return constructor;
            }
          }
          type = pi.getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
          pi = type.toPi();
        }

        boolean check = true;
        while (check) {
          check = false;
          List<? extends Expression> exprs = type.getArguments();
          type = type.getFunction();
          DataCallExpression dataCall = type.toDataCall();
          if (dataCall != null) {
            DataDefinition typeDef = dataCall.getDefinition();
            if (Prelude.isPath(typeDef) && exprs.size() >= 1) {
              LamExpression lam = exprs.get(0).normalize(NormalizeVisitor.Mode.WHNF).toLam();
              if (lam != null) {
                check = true;
                type = lam.getBody().normalize(NormalizeVisitor.Mode.WHNF);
                exprs = exprs.subList(1, exprs.size());
              }
            }
          } else {
            if (!checkNonPositiveError(type, abstractData, dataDefinition, name, list.getFirst(), link, arguments, def)) {
              return constructor;
            }
          }

          for (Expression expr : exprs) {
            if (!checkNonPositiveError(expr, abstractData, dataDefinition, name, list.getFirst(), link, arguments, def)) {
              return constructor;
            }
          }
        }
      }

      constructor.setParameters(list.getFirst());
      constructor.setPatterns(typedPatterns);
      if (plevel != null) constructor.setUniverse(new TypeUniverse(plevel, hlevel));
      constructor.hasErrors(false);
      constructor.setThisClass(dataDefinition.getThisClass());
      dataDefinition.addConstructor(constructor);

      myState.record(def, constructor);
      return constructor;
    }
  }

  private boolean checkNonPositiveError(Expression expr, Abstract.DataDefinition abstractData, DataDefinition dataDefinition, String name, DependentLink params, DependentLink param, List<? extends Abstract.Argument> args, Abstract.Constructor constructor) {
    if (!expr.findBinding(dataDefinition)) {
      return true;
    }

    int index = DependentLink.Helper.getIndex(params, param);
    int i = 0;
    Abstract.Argument argument = null;
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.TelescopeArgument) {
        i += ((Abstract.TelescopeArgument) arg).getNames().size();
      } else {
        i++;
      }
      if (i > index) {
        argument = arg;
        break;
      }
    }

    String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
    // FIXME[errorformat]
    myErrorReporter.report(new TypeCheckingError(abstractData, msg, argument == null ? constructor : argument));
    return false;
  }

  private List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.PatternArgument> patterns, Abstract.DataDefinition abstractData) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, parameters);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        // FIXME[errorformat]
        myErrorReporter.report(new TypeCheckingError(abstractData, "Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        // FIXME[errorformat]
        myErrorReporter.report(new TypeCheckingError(abstractData, "Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition)));
      } else {
        // FIXME[errorformat]
        myErrorReporter.report(new TypeCheckingError(abstractData, "Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression));
      }
    } else {
      processedPatterns = processImplicitResult.patterns;
    }
    return processedPatterns;
  }

  private void typeCheckStatements(ClassDefinition classDefinition, Abstract.ClassDefinition classDef) {
    for (Abstract.Statement statement : classDef.getStatements()) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
        if (definition instanceof Abstract.AbstractDefinition) {
          Definition memberDefinition = myState.getTypechecked(definition);

          if (memberDefinition == null) {
            memberDefinition = visitAbstract((Abstract.AbstractDefinition) definition, classDefinition);
          }

          if (memberDefinition instanceof ClassField) {
            ClassField field = (ClassField) memberDefinition;
            // TypeUniverse oldUniverse = classDefinition.getUniverse();
            // TypeUniverse newUniverse = field.getUniverse();
            //Universe.CompareResult cmp = oldUniverse.compare(newUniverse);
           // if (cmp == null) {
           //   String error = "UniverseOld " + newUniverse + " of abstract definition '" + field.getName() + "' is not compatible with universe " + oldUniverse + " of previous abstract definitions";
           //   myErrorReporter.report(new TypeCheckingError(myNamespaceMember.getResolvedName(), error, definition));
           // } else {
              // classDefinition.setUniverse(new TypeUniverse(oldUniverse.getPLevel().max(newUniverse.getPLevel()), oldUniverse.getHLevel().max(newUniverse.getHLevel())));
              classDefinition.addField(field);
           // }
          }
        } else
        if (definition instanceof Abstract.ImplementDefinition) {
          ClassField field = (ClassField) myState.getTypechecked(((Abstract.ImplementDefinition) definition).getImplemented());
          if (classDefinition.getFieldImpl(field).isImplemented()) {
            myErrorReporter.report(new TypeCheckingError("Field '" + field.getName() + "' is already implemented", definition));
          }

          myState.record(definition, field);
          DependentLink thisParameter = param("\\this", ClassCall(classDefinition));
          List<Binding> context = new ArrayList<>();
          context.add(thisParameter);
          CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).thisClass(classDefinition, Reference(thisParameter)).build(classDef);
          CheckTypeVisitor.Result result = visitor.checkType(((Abstract.ImplementDefinition) definition).getExpression(), field.getBaseType().subst(field.getThisParameter(), Reference(thisParameter)));
          if (result != null) {
            classDefinition.setFieldImpl(field, thisParameter, result.expression);
          }
        }
      }
    }
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, Void params) {
    try {
      ClassDefinition typedDef = new ClassDefinition(def.getName(), SimpleStaticNamespaceProvider.INSTANCE.forDefinition(def), SimpleDynamicNamespaceProvider.INSTANCE.forClass(def));

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        if (aSuperClass.getReferent() == null) {
          myErrorReporter.report(new TypeCheckingError("Could not find class '" + aSuperClass.getName() + "'", def));
          continue;
        }

        ClassDefinition superClass = null;
        if (aSuperClass.getReferent() instanceof ClassDefinition) {
          superClass = (ClassDefinition) aSuperClass.getReferent();
        } else {
          Definition typeCheckedSuper = myState.getTypechecked(aSuperClass.getReferent());
          if (typeCheckedSuper instanceof ClassDefinition) {
            superClass = (ClassDefinition) typeCheckedSuper;
          }
        }

        if (superClass != null) {
          boolean ok = true;
          for (Map.Entry<ClassField, ClassDefinition.FieldImplementation> entry : superClass.getFieldsMap()) {
            String name = entry.getValue().name;
            for (Abstract.IdPair idPair : aSuperClass.getRenamings()) {
              if (name.equals(idPair.getFirstName())) {
                name = idPair.getSecondName();
                break;
              }
            }

            Map.Entry<ClassField, ClassDefinition.FieldImplementation> oldFieldEntry = typedDef.getFieldEntry(name);
            if (oldFieldEntry == null) {
              typedDef.addExistingField(entry.getKey(), new ClassDefinition.FieldImplementation(name, entry.getValue().thisParameter, entry.getValue().implementation));
            } else
            if (oldFieldEntry.getKey() != entry.getKey()) {
              myErrorReporter.report(new TypeCheckingError("Duplicate field '" + name + "'", null));  // FIXME[error] report proper
              ok = false;
            } else {
              ClassDefinition.FieldImplementation impl = oldFieldEntry.getValue();
              if (!impl.isImplemented() || !entry.getValue().isImplemented() || impl.implementation.equals(entry.getValue().implementation.subst(entry.getValue().thisParameter, Reference(impl.thisParameter)))) {
                typedDef.addExistingField(entry.getKey(), new ClassDefinition.FieldImplementation(name, impl.isImplemented() ? impl.thisParameter : entry.getValue().thisParameter , impl.isImplemented() ? impl.implementation : entry.getValue().implementation ));
              } else {
                myErrorReporter.report(new TypeCheckingError("Implementations of '" + name + "' differ", aSuperClass.getReferent()));
              }
            }
          }
          if (ok) {
            typedDef.addSuperClass(superClass);
          }
        } else {
          myErrorReporter.report(new TypeCheckingError("Expected a class", aSuperClass.getReferent()));
        }
      }

      ClassDefinition thisClass = getThisClass(def);
      if (thisClass != null) {
        typedDef.addParentField(thisClass);
      }
      typeCheckStatements(typedDef, def);
      typedDef.updateUniverse();
      myState.record(def, typedDef);
      return typedDef;
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }
    return null;
  }

  @Override
  public Definition visitImplement(Abstract.ImplementDefinition def, Void params) {
    throw new IllegalStateException();
  }
}
