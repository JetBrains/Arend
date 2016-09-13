package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleDynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleStaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.FindMatchOnIntervalVisitor;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.NotInScopeError;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;
import static com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError.typeOfFunctionArg;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<ClassDefinition, Definition> {
  private final TypecheckerState myState;
  private final LocalErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(TypecheckerState state, LocalErrorReporter errorReporter) {
    myState = state;
    myErrorReporter = errorReporter;
  }

  public static void typeCheck(TypecheckerState state, ClassDefinition enclosingClass, Abstract.Definition definition, LocalErrorReporter errorReporter, boolean isPrelude) {
    if (state.getTypechecked(definition) == null) {
      definition.accept(new DefinitionCheckTypeVisitor(state, errorReporter), enclosingClass);
      if (isPrelude) {
        Prelude.update(definition, state.getTypechecked(definition));
      }
    }
  }

  public static void typeCheck(TypecheckerState state, ClassDefinition enclosingClass, Abstract.Definition definition, LocalErrorReporter errorReporter) {
    typeCheck(state, enclosingClass, definition, errorReporter, false);
  }

  private DependentLink createThisParam(ClassDefinition enclosingClass) {
    assert enclosingClass != null;
    return param("\\this", ClassCall(enclosingClass));
  }

  private static boolean isPolyParam(Abstract.TypeArgument arg) {
    if (arg.getType() instanceof Abstract.DefCallExpression) {
      String typeName = ((Abstract.DefCallExpression) arg.getType()).getName();
      return typeName.equals(Prelude.LVL.getName()) || typeName.equals(Prelude.CNAT.getName());
    }
    return false;
  }

  private static DefCallExpression levelTypeByName(String typeName) {
    if (typeName.equals("Lvl")) {
      return Lvl();
    } else if (typeName.equals("CNat")) {
      return CNat();
    }
    return null;
  }

  private Binding visitPolyParam(Abstract.TypeArgument typeArgument, Map<String, Binding> polyParams, Abstract.SourceNode node) {
    assert (typeArgument.getType() instanceof Abstract.DefCallExpression);
    String typeName = ((Abstract.DefCallExpression) typeArgument.getType()).getName();
    if (!(typeArgument instanceof Abstract.TelescopeArgument)) {
      myErrorReporter.report(new LocalTypeCheckingError("Parameter of type " + typeName + " must have name", node));
      return null;
    }
    Abstract.TelescopeArgument teleArgument = (Abstract.TelescopeArgument)typeArgument;
    if (teleArgument.getNames().size() > 1 || polyParams.containsKey(typeName)) {
      myErrorReporter.report(new LocalTypeCheckingError("Function definition must have at most one polymorphic variable of type " + typeName, node));
      return null;
    }
    if (teleArgument.getExplicit()) {
      myErrorReporter.report(new LocalTypeCheckingError("Polymorphic variables must be implicit", node));
      return null;
    }
    Binding levelParam = new TypedBinding(((Abstract.TelescopeArgument) typeArgument).getNames().get(0), levelTypeByName(typeName));
    polyParams.put(typeName, levelParam);
    return levelParam;
  }

  @Override
  public FunctionDefinition visitFunction(final Abstract.FunctionDefinition def, ClassDefinition enclosingClass) {
    Abstract.Definition.Arrow arrow = def.getArrow();
    final FunctionDefinition typedDef = new FunctionDefinition(def, SimpleStaticNamespaceProvider.INSTANCE.forDefinition(def));
    myState.record(def, typedDef);
    // TODO[scopes] Fill namespace

    List<? extends Abstract.Argument> arguments = def.getArguments();
    final List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).build(def);
    LinkList list = new LinkList();
    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
      typedDef.setThisClass(enclosingClass);
    }

    List<Binding> polyParamsList = new ArrayList<>();
    Map<String, Binding> polyParamsMap = new HashMap<>();
    // int numberOfArgs = index;
    int index = 0;
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument)argument;

        if (isPolyParam(typeArgument)) {
          Binding levelParam = visitPolyParam(typeArgument, polyParamsMap, def);
          if (levelParam == null) {
            return typedDef;
          }
          context.add(levelParam);
          polyParamsList.add(levelParam);
          //polyParams.put(((Abstract.DefCallExpression)typeArgument.getType()).getName(), levelParam);
          ++index;
          continue;
        }

        CheckTypeVisitor.Result result = visitor.checkType(typeArgument.getType(), Universe());
        if (result == null) return typedDef;

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
      } else {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
        return typedDef;
      }
    }

    Expression expectedType = null;
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      CheckTypeVisitor.Result typeResult = visitor.checkType(resultType, Universe());
      if (typeResult != null) {
        expectedType = typeResult.expression;
      }
    }

    typedDef.setPolyParams(polyParamsList);
    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.typeHasErrors(typedDef.getResultType() == null);

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        context.subList(context.size() - size(list.getFirst()), context.size()).clear();
        ElimTreeNode elimTree = visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, def.getArrow() == Abstract.Definition.Arrow.LEFT ? list.getFirst() : null, expectedType, false);
        if (elimTree != null) {
          typedDef.setElimTree(elimTree);
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, expectedType);
        if (termResult != null) {
          typedDef.setElimTree(top(list.getFirst(), leaf(def.getArrow(), termResult.expression)));
          if (expectedType == null) {
            typedDef.setResultType(termResult.type);
          }
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, typedDef.getParameters()), null)) {
          myErrorReporter.report(new LocalTypeCheckingError("Termination check failed", term));
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        LocalTypeCheckingError error = TypeCheckingElim.checkCoverage(def, list.getFirst(), typedDef.getElimTree(), expectedType);
        if (error != null) {
          myErrorReporter.report(error);
        }
      }

      if (typedDef.getElimTree() != null) {
        typedDef.hasErrors(false); // we need normalization here
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(def, list.getFirst(), typedDef.getElimTree());
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

    return typedDef;
  }

  @Override
  public ClassField visitAbstract(Abstract.AbstractDefinition def, ClassDefinition enclosingClass) {
    throw new IllegalStateException();
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, ClassDefinition enclosingClass) {
    List<? extends Abstract.TypeArgument> parameters = def.getParameters();

    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).build(def);
    List<Binding> polyParamsList = new ArrayList<>();
    Map<String, Binding> polyParamsMap = new HashMap<>();
    LinkList list = new LinkList();
    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    SortMax inferredSorts = def.getConstructors().size() > 1 ? new SortMax(new Sort(new Level(0), Sort.SET.getHLevel())) : new SortMax();
    SortMax userSorts = null;
    DataDefinition dataDefinition = new DataDefinition(def, inferredSorts, null);
    dataDefinition.setThisClass(enclosingClass);
    dataDefinition.hasErrors(true);
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      for (Abstract.TypeArgument parameter : parameters) {
        if (isPolyParam(parameter)) {
          Binding levelParam = visitPolyParam(parameter, polyParamsMap, def);
          if (levelParam == null) {
            return dataDefinition;
          }
          context.add(levelParam);
          polyParamsList.add(levelParam);
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

        if (def.getUniverse() instanceof Abstract.PolyUniverseExpression) {
          userSorts = visitor.visitDataUniverse((Abstract.PolyUniverseExpression)def.getUniverse());
        } else if (def.getUniverse() instanceof Abstract.UniverseExpression) {
          CheckTypeVisitor.Result result = visitor.checkType(def.getUniverse(), Universe());
          if (result != null) {
            userSorts = new SortMax(result.expression.toUniverse().getSort());
          }
        } else {
          String msg = "Specified type " + PrettyPrintVisitor.prettyPrint(def.getUniverse(), 0) + " of '" + def.getName() + "' is not a universe";
          myErrorReporter.report(new LocalTypeCheckingError(msg, def.getUniverse()));
        }
      }
    }

    dataDefinition.setPolyParams(polyParamsList);
    dataDefinition.setParameters(list.getFirst());
    if (userSorts != null) dataDefinition.setSorts(userSorts);
    dataDefinition.hasErrors(false);
    myState.record(def, dataDefinition);

    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor typedConstructor = visitConstructor(constructor, def, dataDefinition, enclosingClass, visitor, inferredSorts);
      if (typedConstructor == null) {
        continue;
      }

      myState.record(constructor, typedConstructor);
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
        LocalTypeCheckingError error = new LocalTypeCheckingError(cycleConditionsError.toString(), def);
        myErrorReporter.report(error);
      }
    }

    if (!dataDefinition.getConditions().isEmpty()) {
      List<Condition> failedConditions = new ArrayList<>();
      for (Condition condition : dataDefinition.getConditions()) {
        LocalTypeCheckingError error = TypeCheckingElim.checkConditions(condition.getConstructor().getName(), def, condition.getConstructor().getParameters(), condition.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          failedConditions.add(condition);
        }
      }
      dataDefinition.getConditions().removeAll(failedConditions);
      for (Condition condition : dataDefinition.getConditions()) {
        if (condition.getElimTree().accept(new FindMatchOnIntervalVisitor(), null)) {
          dataDefinition.setMatchesOnInterval();
          inferredSorts = new SortMax(inferredSorts.getPLevel(), LevelMax.INFINITY);
        }
      }
    }

    if (userSorts != null) {
      if (inferredSorts.isLessOrEquals(userSorts)) {
        dataDefinition.setSorts(userSorts);
      } else {
        String msg = "Actual universe " + inferredSorts + " is not compatible with expected universe " + userSorts;
        myErrorReporter.report(new LocalTypeCheckingError(msg, def.getUniverse()));
        dataDefinition.setSorts(inferredSorts);
      }
    } else {
      dataDefinition.setSorts(inferredSorts);
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
          myErrorReporter.report(new LocalTypeCheckingError("Termination check failed", null));
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
        Set<Abstract.Definition> dependencies = new HashSet<>();
        condition.getTerm().accept(new CollectDefCallsVisitor(dependencies), null);
        for (Abstract.Definition def : dependencies) {
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
  public Definition visitConstructor(Abstract.Constructor def, ClassDefinition enclosingClass) {
    throw new IllegalStateException();
  }

  private Constructor visitConstructor(Abstract.Constructor def, Abstract.DataDefinition abstractData, DataDefinition dataDefinition, ClassDefinition enclosingClass, CheckTypeVisitor visitor, SortMax sorts) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(visitor.getContext())) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      String name = def.getName();

      Constructor constructor = new Constructor(def, null, dataDefinition, null);
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

      LinkList list = new LinkList();
      for (Abstract.TypeArgument argument : arguments) {
        CheckTypeVisitor.Result result = visitor.checkType(argument.getType(), Universe());
        if (result == null) {
          return constructor;
        }

        //if (!constructor.containsInterval() && result.expression.accept(new FindIntervalVisitor(), null)) {
        //  constructor.setContainsInterval();
       // }

        sorts.add(result.type.toSorts());

        DependentLink param;
        if (argument instanceof Abstract.TelescopeArgument) {
          param = param(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), result.expression);
        } else {
          param = param(argument.getExplicit(), (String) null, result.expression);
        }
        list.append(param);
        visitor.getContext().addAll(toContext(param));
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
            if (typeDef == Prelude.PATH && exprs.size() >= 1) {
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
    myErrorReporter.report(new LocalTypeCheckingError(msg, argument == null ? constructor : argument));
    return false;
  }

  private List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.PatternArgument> patterns, Abstract.DataDefinition abstractData) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, parameters);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        myErrorReporter.report(new LocalTypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        myErrorReporter.report(new LocalTypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition)));
      } else {
        myErrorReporter.report(new LocalTypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression));
      }
    } else {
      processedPatterns = processImplicitResult.patterns;
    }
    return processedPatterns;
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, ClassDefinition enclosingClass) {
    boolean classOk = true;
    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).build(def);

    if (enclosingClass != null) {
      DependentLink thisParam = createThisParam(enclosingClass);
      context.add(thisParam);
      visitor.setThisClass(enclosingClass, Reference(thisParam));
    }

    FieldSet fieldSet = new FieldSet();
    Set<ClassDefinition> superClasses = new HashSet<>();
    try {
      Map<ClassField, Abstract.ReferableSourceNode> aliases = new HashMap<>();
      ClassDefinition typedDef = new ClassDefinition(def, fieldSet, superClasses, SimpleStaticNamespaceProvider.INSTANCE.forDefinition(def), SimpleDynamicNamespaceProvider.INSTANCE.forClass(def), aliases);
      typedDef.setThisClass(enclosingClass);
      ClassCallExpression thisClassCall = typedDef.getDefCall();

      for (Abstract.SuperClass aSuperClass : def.getSuperClasses()) {
        CheckTypeVisitor.Result result = aSuperClass.getSuperClass().accept(visitor, Universe());
        if (result == null) continue;

        ClassCallExpression typeCheckedSuperClass = result.expression.toClassCall();
        if (typeCheckedSuperClass == null) {
          myErrorReporter.report(new LocalTypeCheckingError("Parent must be a class", aSuperClass.getSuperClass()));
          continue;
        }

        fieldSet.addFieldsFrom(typeCheckedSuperClass.getFieldSet(), thisClassCall);
        superClasses.add(typeCheckedSuperClass.getDefinition());

        for (Map.Entry<ClassField, FieldSet.Implementation> entry : typeCheckedSuperClass.getFieldSet().getImplemented()) {
          if (!fieldSet.implementField(entry.getKey(), entry.getValue(), thisClassCall)) {
            classOk = false;
            myErrorReporter.report(new LocalTypeCheckingError("Implementations of '" + entry.getKey().getName() + "' differ", aSuperClass.getSuperClass()));  // FIXME[error] report proper, especially in case of \\parent
          }
        }

        Namespace ns = SimpleDynamicNamespaceProvider.INSTANCE.forClass(typeCheckedSuperClass.getDefinition().getAbstractDefinition());
        Set<ClassField> hidden = new HashSet<>();
        for (Abstract.Identifier identifier : aSuperClass.getHidings()) {
          Abstract.Definition aDef = ns.resolveName(identifier.getName());
          Definition definition = myState.getTypechecked(aDef);
          if (definition instanceof ClassField) {
            hidden.add((ClassField) definition);
          } else {
            if (definition == null) {
              myErrorReporter.report(new LocalTypeCheckingError("Not in scope: " + identifier.getName(), identifier));  // FIXME[error] report proper, especially in case of \\parent
            } else {
              myErrorReporter.report(new LocalTypeCheckingError("Expected a field", identifier));  // FIXME[error] report proper, especially in case of \\parent
            }
          }
        }

        Map<ClassField, Abstract.ReferableSourceNode> renamings = new HashMap<>();
        for (Abstract.IdPair pair : aSuperClass.getRenamings()) {
          Abstract.Definition aDef = ns.resolveName(pair.getFirstName());
          Definition definition = myState.getTypechecked(aDef);
          if (definition instanceof ClassField) {
            if (hidden.contains(definition)) {
              myErrorReporter.report(new LocalTypeCheckingError("Field '" + pair.getFirstName() + "' is hidden", pair));  // FIXME[error] report proper, especially in case of \\parent
            } else {
              renamings.put((ClassField) definition, pair);
            }
          } else {
            if (definition == null) {
              myErrorReporter.report(new LocalTypeCheckingError("Not in scope: " + pair.getFirstName(), pair));  // FIXME[error] report proper, especially in case of \\parent
            } else {
              myErrorReporter.report(new LocalTypeCheckingError("Expected a field", pair));  // FIXME[error] report proper, especially in case of \\parent
            }
          }
        }

        for (ClassField field : typeCheckedSuperClass.getDefinition().getFieldSet().getFields()) {
          Abstract.ReferableSourceNode alias = renamings.get(field);
          if (alias == null) {
            alias = typeCheckedSuperClass.getDefinition().getFieldAlias(field);
            if (alias == field.getAbstractDefinition()) {
              alias = null;
            }
          }

          if (alias != null) {
            Abstract.ReferableSourceNode oldAlias = aliases.get(field);
            if (oldAlias == null) {
              aliases.put(field, alias);
            } else {
              if (oldAlias != alias) {
                myErrorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already renamed to '" + oldAlias + "'", alias));  // FIXME[error] report proper
              }
            }
          }
        }
      }

      if (enclosingClass != null) {
        assert context.size() == 1;
        context.remove(0);
      } else {
        assert context.size() == 0;
      }

      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement) {
          Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
          if (definition instanceof Abstract.AbstractDefinition) {
            ClassField field = visitClassField((Abstract.AbstractDefinition) definition, typedDef);
            fieldSet.addField(field, thisClassCall);
          } else if (definition instanceof Abstract.ImplementDefinition) {
            Definition implementedDef = myState.getTypechecked(((Abstract.ImplementDefinition) definition).getImplemented());
            if (!(implementedDef instanceof ClassField)) {
              myErrorReporter.report(new LocalTypeCheckingError("'" + implementedDef.getName() + "' is not a field", definition));
              continue;
            }
            ClassField field = (ClassField) implementedDef;
            if (fieldSet.isImplemented(field)) {
              myErrorReporter.report(new LocalTypeCheckingError("Field '" + field.getName() + "' is already implemented", definition));
              continue;
            }

            DependentLink thisParameter = createThisParam(typedDef);
            visitor.setThisClass(typedDef, Reference(thisParameter));
            CheckTypeVisitor.Result result = fieldSet.implementField(field, ((Abstract.ImplementDefinition) definition).getExpression(), visitor, thisClassCall, thisParameter);
            if (result == null || result.expression.toError() != null) {
              classOk = false;
            }
          }
        }
      }

      myState.record(def, typedDef);
      typedDef.hasErrors(!classOk);
      return typedDef;
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }
    return null;
  }

  private ClassField visitClassField(Abstract.AbstractDefinition def, ClassDefinition enclosingClass) {
    if (enclosingClass == null) throw new IllegalStateException();

    List<? extends Abstract.Argument> arguments = def.getArguments();
    Expression typedResultType;
    DependentLink thisParameter = createThisParam(enclosingClass);
    List<Binding> context = new ArrayList<>();
    context.add(thisParameter);
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(myState, context, myErrorReporter).thisClass(enclosingClass, Reference(thisParameter)).build(def);
    LevelMax pLevel = new LevelMax();
    ClassField typedDef = new ClassField(def, Error(null, null), enclosingClass, thisParameter);
    myState.record(def, typedDef);

    Map<String, Binding> polyParams = new HashMap<>();
    int index = 0;
    LinkList list = new LinkList();
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) argument;

        if (isPolyParam(typeArgument)) {
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
        pLevel.add(result.type.toSorts().getPLevel());
      } else {
        myErrorReporter.report(new ArgInferenceError(typeOfFunctionArg(index + 1), argument, new Expression[0]));
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

    SortMax resultSort = typeResult.type.toSorts();
    pLevel.add(resultSort.getPLevel());

    typedDef.hasErrors(false);
    typedDef.setPolyParams(new ArrayList<>(polyParams.values()));
    typedDef.setBaseType(list.isEmpty() ? typedResultType : Pi(list.getFirst(), typedResultType));
    typedDef.setSorts(new SortMax(pLevel, resultSort.getHLevel()));
    typedDef.setThisClass(enclosingClass);
    return typedDef;
  }

  @Override
  public Definition visitImplement(Abstract.ImplementDefinition def, ClassDefinition params) {
    throw new IllegalStateException();
  }
}
