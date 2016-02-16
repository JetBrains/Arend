package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.TerminationCheckVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Patterns;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsToElimTreeConversion;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.size;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.ordinal;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.typeOfFunctionArg;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Void, Definition> {
  private NamespaceMember myNamespaceMember;
  private Namespace myNamespace;
  private final ErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(Namespace namespace, ErrorReporter errorReporter) {
    myNamespaceMember = null;
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  private DefinitionCheckTypeVisitor(NamespaceMember namespaceMember, Namespace namespace, ErrorReporter errorReporter) {
    myNamespaceMember = namespaceMember;
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  public void setNamespaceMember(NamespaceMember namespaceMember) {
    myNamespaceMember = namespaceMember;
  }

  public static void typeCheck(NamespaceMember namespaceMember, Namespace namespace, ErrorReporter errorReporter) {
    if (namespaceMember != null && !namespaceMember.isTypeChecked()) {
      DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(namespaceMember, namespace, errorReporter);
      namespaceMember.definition = namespaceMember.abstractDefinition.accept(visitor, null);
    }
  }

  private ClassDefinition getNamespaceClass(Namespace namespace) {
    Definition parent = namespace.getParent().getMember(namespace.getName()).definition;
    return parent instanceof ClassDefinition ? (ClassDefinition) parent : null;
  }

  private ClassDefinition getParentClass(Abstract.Definition definition, Namespace namespace, Abstract.DefineStatement dynamicStatement) {
    if (definition instanceof Abstract.ClassDefinition) {
      ClassDefinition classDefinition = getNamespaceClass(namespace);
      if (classDefinition == null) {
        myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), "Internal error: definition '" + definition.getName() + "' is not a class", definition));
      }
      return classDefinition;
    }

    Abstract.DefineStatement statement = definition == null ? null : definition.getParentStatement();
    Abstract.Definition parentDefinition = statement == null ? null : statement.getParentDefinition();
    Namespace parentNamespace = namespace.getParent();
    if (statement == null || parentDefinition == null || parentNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), "Non-static definitions are allowed only inside a class definition", dynamicStatement));
      return null;
    }

    return getParentClass(parentDefinition, parentNamespace, dynamicStatement);
  }

  private ClassDefinition getThisClass(Abstract.Definition definition, Namespace namespace) {
    Abstract.DefineStatement statement = definition.getParentStatement();
    if (statement == null) {
      return null;
    }

    if (!statement.isStatic()) {
      return getParentClass(statement.getParentDefinition(), namespace, statement);
    }

    Abstract.Definition parentDefinition = statement.getParentDefinition();
    Namespace parentNamespace = namespace.getParent();
    if (parentDefinition == null || parentNamespace == null) {
      return null;
    }

    return getThisClass(parentDefinition, parentNamespace);
  }

  @Override
  public FunctionDefinition visitFunction(final Abstract.FunctionDefinition def, Void params) {
    String name = def.getName();
    Abstract.Definition.Arrow arrow = def.getArrow();
    final FunctionDefinition typedDef = new FunctionDefinition(myNamespace, new Name(name), def.getPrecedence());
    /*
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + name + " in the parent class", def, getNames(myContext)));
      myErrorReporter.report(new TypeCheckingError("Overridden function " + name + " cannot be defined in a base class", def, getNames(myContext)));
      return null;
    }
    */

    List<? extends Abstract.Argument> arguments = def.getArguments();
    final List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).build();
    ClassDefinition thisClass = getThisClass(def, myNamespace);
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

    // int numberOfArgs = index;
    int index = 0;
    for (Abstract.Argument argument : arguments) {
      DependentLink param;
      if (argument instanceof Abstract.TypeArgument) {
        CheckTypeVisitor.Result result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
        if (result == null) return typedDef;

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
        myErrorReporter.report(new ArgInferenceError(typedDef.getParentNamespace().getResolvedName(), typeOfFunctionArg(index + 1), argument, null));
        return typedDef;
    /*
    } else {
      List<String> names = new ArrayList<>(1);
      names.add(((Abstract.NameArgument) argument).getName());
      typedParameters.add(Tele(argument.getExplicit(), names, splitArgs.get(index).getType()));
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

    typedDef.setParameters(list.getFirst());
    typedDef.setResultType(expectedType);
    typedDef.typeHasErrors(typedDef.getResultType() == null);

    myNamespaceMember.definition = typedDef;
    Abstract.Expression term = def.getTerm();

    if (term != null) {
      if (term instanceof Abstract.ElimExpression) {
        context.subList(context.size() - size(list.getFirst()), context.size()).clear();
        TypeCheckingElim.Result elimResult = visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, def.getArrow() == Abstract.Definition.Arrow.LEFT ? list.getFirst() : null, expectedType, false);
        if (elimResult != null) {
          elimResult.update();
          elimResult.reportErrors(myErrorReporter);
          typedDef.setElimTree(elimResult.elimTree);
        }
      } else {
        CheckTypeVisitor.Result termResult = visitor.checkType(term, expectedType);
        if (termResult != null) {
          typedDef.setElimTree(new LeafElimTreeNode(def.getArrow(), termResult.expression));
          if (expectedType == null)
            typedDef.setResultType(termResult.type);
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, typedDef.getParameters()), null)) {
          myErrorReporter.report(new TypeCheckingError("Termination check failed", term));
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        TypeCheckingError error = TypeCheckingElim.checkCoverage(def, list.getFirst(), typedDef.getElimTree());
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
      type = type.getType();
      if (type instanceof UniverseExpression) {
        typedDef.setUniverse(((UniverseExpression) type).getUniverse());
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
    String name = def.getName();
    List<? extends Abstract.Argument> arguments = def.getArguments();
    Expression typedResultType;
    DependentLink thisParameter = param("\\this", ClassCall(thisClass));
    List<Binding> context = new ArrayList<>();
    context.add(thisParameter);
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).thisClass(thisClass, Reference(thisParameter)).build();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);

    int index = 0;
    LinkList list = new LinkList();
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        CheckTypeVisitor.Result result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
        if (result == null) {
          return null;
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

        Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
        Universe maxUniverse = universe.max(argUniverse);
        if (maxUniverse == null) {
          String error = "Universe " + argUniverse + " of " + ordinal(index) + " argument is not compatible with universe " + universe + " of previous arguments";
          myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, def));
          return null;
        } else {
          universe = maxUniverse;
        }
      } else {
        myErrorReporter.report(new ArgInferenceError(myNamespace.getResolvedName(), typeOfFunctionArg(index + 1), argument, null));
        return null;
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType == null) {
      return null;
    }
    CheckTypeVisitor.Result typeResult = visitor.checkType(resultType, Universe());
    if (typeResult == null) {
      return null;
    }
    typedResultType = typeResult.expression;

    Universe resultTypeUniverse = ((UniverseExpression) typeResult.type).getUniverse();
    Universe maxUniverse = universe.max(resultTypeUniverse);
    if (maxUniverse == null) {
      String error = "Universe " + resultTypeUniverse + " of the result type is not compatible with universe " + universe + " of arguments";
      myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, def));
      return null;
    } else {
      universe = maxUniverse;
    }

    ClassField typedDef = new ClassField(myNamespace, new Name(name), def.getPrecedence(), list.isEmpty() ? typedResultType : Pi(list.getFirst(), typedResultType), thisClass, thisParameter);
    typedDef.setUniverse(universe);
    typedDef.setThisClass(thisClass);
    myNamespaceMember.definition = typedDef;
    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Void params) {
    List<? extends Abstract.TypeArgument> parameters = def.getParameters();
    Universe universe = def.getUniverse();
    Universe typedUniverse = new Universe.Type(0, Universe.Type.PROP);

    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).build();
    ClassDefinition thisClass = getThisClass(def, myNamespace);
    LinkList list = new LinkList();
    if (thisClass != null) {
      DependentLink thisParam = param("\\this", ClassCall(thisClass));
      context.add(thisParam);
      list.append(thisParam);
      visitor.setThisClass(thisClass, Reference(thisParam));
    }

    try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getContext())) {
      for (Abstract.TypeArgument parameter : parameters) {
        CheckTypeVisitor.Result result = visitor.checkType(parameter.getType(), Universe());
        if (result == null) return null;
        DependentLink param;
        if (parameter instanceof Abstract.TelescopeArgument) {
          param = param(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression);
        } else {
          param = param(parameter.getExplicit(), (String) null, result.expression);
        }
        list.append(param);
        context.addAll(toContext(param));
      }
    }

    String name = def.getName();
    DataDefinition dataDefinition = new DataDefinition(myNamespace, new Name(name), def.getPrecedence(), universe != null ? universe : new Universe.Type(0, Universe.Type.PROP), list.getFirst());
    dataDefinition.setThisClass(thisClass);
    myNamespaceMember.definition = dataDefinition;

    myNamespace = myNamespace.getChild(name);
    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor typedConstructor = visitConstructor(constructor, dataDefinition, visitor);
      if (typedConstructor == null) {
        continue;
      }

      NamespaceMember member = myNamespace.getMember(constructor.getName());
      if (member == null) {
        continue;
      }
      member.definition = typedConstructor;

      Universe maxUniverse = typedUniverse.max(typedConstructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + typedConstructor.getUniverse() + " of constructor '" + constructor.getName() + "' is not compatible with universe " + typedUniverse + " of previous constructors";
        myErrorReporter.report(new TypeCheckingError(msg, null));
      } else {
        typedUniverse = maxUniverse;
      }
    }
    myNamespace = myNamespace.getParent();

    if (universe != null) {
      if (typedUniverse.lessOrEquals(universe)) {
        dataDefinition.setUniverse(universe);
      } else {
        myErrorReporter.report(new TypeMismatchError(new UniverseExpression(universe), new UniverseExpression(typedUniverse), null));
        dataDefinition.setUniverse(typedUniverse);
      }
    } else {
      dataDefinition.setUniverse(typedUniverse);
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
        TypeCheckingError error = new TypeCheckingError(cycleConditionsError.toString(), def);
        myErrorReporter.report(error);
      }
    }

    if (dataDefinition.getConditions() != null) {
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
        myErrorReporter.report(new NotInScopeError(cond, cond.getConstructorName()));
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
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(constructor.getType().getPiParameters(null, false, false)));
            DependentLink params = constructor.getParameters();
            if (constructor.getThisClass() != null) {
              params = params.getNext();
            }
            List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(cond, params, cond.getPatterns());
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
          myErrorReporter.report(new TypeCheckingError("Termination check failed", def));
          continue;
        }

        dataDefinition.addCondition(new Condition(constructor, elimTreeResult.elimTree));
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
        for (ResolvedName rn : condition.getTerm().accept(new CollectDefCallsVisitor(), true)) {
          if (rn.toDefinition() != null && rn.toDefinition() != constructor && rn.toDefinition() instanceof Constructor && ((Constructor) rn.toDefinition()).getDataType().equals(constructor.getDataType())) {
            List<Constructor> cycle = searchConditionCycle(condMap, (Constructor) rn.toDefinition(), visited, visiting);
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

  public Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition, CheckTypeVisitor visitor) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(visitor.getContext())) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      Universe universe = new Universe.Type(0, Universe.Type.PROP);
      int index = 1;
      boolean ok = true;

      List<? extends Abstract.PatternArgument> patterns = def.getPatterns();
      Patterns typedPatterns = null;
      if (patterns != null) {
        List<Abstract.PatternArgument> processedPatterns = new ArrayList<>(patterns);
        if (dataDefinition.getThisClass() != null) {
          processedPatterns.add(0, new PatternArgument(new NamePattern(dataDefinition.getParameters()), true, true));
        }
        processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), processedPatterns);
        if (processedPatterns == null)
          return null;

        typedPatterns = visitor.getTypeCheckingElim().visitPatternArgs(processedPatterns, dataDefinition.getParameters(), Collections.<Expression>emptyList(), TypeCheckingElim.PatternExpansionMode.DATATYPE);
        if (typedPatterns == null)
          return null;
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
          return null;
        }

        Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
        Universe maxUniverse = universe.max(argUniverse);
        if (maxUniverse == null) {
          String error = "Universe " + argUniverse + " of " + ordinal(index) + " argument is not compatible with universe " + universe + " of previous arguments";
          myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), error, def));
          ok = false;
        } else {
          universe = maxUniverse;
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
        return null;
      }

      String name = def.getName();

      for (DependentLink link = list.getFirst(); link.hasNext(); link = link.getNext()) {
        Expression type = link.getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (DependentLink link1 = ((PiExpression) type).getParameters(); link1.hasNext(); link1 = link1.getNext()) {
            if (link1.getType().findBinding(dataDefinition)) {
              nonPositiveError(dataDefinition, name, list.getFirst(), link, arguments, def);
              return null;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.findBinding(dataDefinition)) {
            nonPositiveError(dataDefinition, name, list.getFirst(), link, arguments, def);
            return null;
          }
        }
      }

      Constructor constructor = new Constructor(dataDefinition.getParentNamespace().getChild(dataDefinition.getName()), new Name(name), def.getPrecedence(), universe, list.getFirst(), dataDefinition, typedPatterns);
      constructor.setThisClass(dataDefinition.getThisClass());
      dataDefinition.addConstructor(constructor);
      dataDefinition.getParentNamespace().addDefinition(constructor);
      return constructor;
    }
  }

  private void nonPositiveError(DataDefinition dataDefinition, String name, DependentLink params, DependentLink param, List<? extends Abstract.Argument> args, Abstract.Constructor constructor) {
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
    myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), msg, argument == null ? constructor : argument));
  }

  private List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, DependentLink parameters, List<? extends Abstract.PatternArgument> patterns) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, parameters);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        myErrorReporter.report(new TypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        myErrorReporter.report(new TypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition)));
      } else {
        myErrorReporter.report(new TypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression));
      }
    } else {
      processedPatterns = processImplicitResult.patterns;
    }
    return processedPatterns;
  }

  private void typeCheckStatements(ClassDefinition classDefinition, Collection<? extends Abstract.Statement> statements, Namespace namespace) {
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.Definition definition = ((Abstract.DefineStatement) statement).getDefinition();
        if (definition instanceof Abstract.AbstractDefinition) {
          NamespaceMember member = namespace.getMember(definition.getName());
          if (member != null) {
            if (!member.isTypeChecked()) {
              DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(member, namespace, myErrorReporter);
              member.definition = visitor.visitAbstract((Abstract.AbstractDefinition) definition, classDefinition);
            }

            if (member.definition instanceof ClassField) {
              ClassField field = (ClassField) member.definition;
              Universe oldUniverse = classDefinition.getUniverse();
              Universe newUniverse = field.getUniverse();
              Universe maxUniverse = oldUniverse.max(newUniverse);
              if (maxUniverse == null) {
                String error = "Universe " + newUniverse + " of abstract definition '" + field.getName() + "' is not compatible with universe " + oldUniverse + " of previous abstract definitions";
                myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, definition));
              } else {
                classDefinition.setUniverse(maxUniverse);
                classDefinition.addField(field);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, Void params) {
    String name = def.getName();
    ClassDefinition typedDef = new ClassDefinition(myNamespace, new Name(name));
    ClassDefinition thisClass = getThisClass(def, myNamespace);
    if (thisClass != null) {
      typedDef.addParentField(thisClass);
    }
    typeCheckStatements(typedDef, def.getStatements(), myNamespace.getChild(name));
    return typedDef;
  }
}
