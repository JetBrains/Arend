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
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingElim;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.typeOfFunctionArg;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

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
    Definition parent = namespace.getParent().getMember(namespace.getName().name).definition;
    return parent instanceof ClassDefinition ? (ClassDefinition) parent : null;
  }

  private ClassDefinition getParentClass(Abstract.Definition definition, Namespace namespace, Abstract.DefineStatement dynamicStatement) {
    if (definition instanceof Abstract.ClassDefinition) {
      ClassDefinition classDefinition = getNamespaceClass(namespace);
      if (classDefinition == null) {
        myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), "Internal error: definition '" + definition.getName() + "' is not a class", definition, new ArrayList<String>()));
      }
      return classDefinition;
    }

    Abstract.DefineStatement statement = definition == null ? null : definition.getParentStatement();
    Abstract.Definition parentDefinition = statement == null ? null : statement.getParentDefinition();
    Namespace parentNamespace = namespace.getParent();
    if (statement == null || parentDefinition == null || parentNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), "Non-static definitions are allowed only inside a class definition", dynamicStatement, new ArrayList<String>()));
      return null;
    }

    return getParentClass(parentDefinition, parentNamespace, dynamicStatement);
  }

  private ClassDefinition getThisClass(Abstract.Definition definition, Namespace namespace) {
    Abstract.DefineStatement statement = definition.getParentStatement();
    if (statement == null) {
      return null;
    }

    if (statement.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
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
    Name name = def.getName();
    Abstract.Definition.Arrow arrow = def.getArrow();
    final FunctionDefinition typedDef = new FunctionDefinition(myNamespace, name, def.getPrecedence());
    /*
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + name + " in the parent class", def, getNames(myContext)));
      myErrorReporter.report(new TypeCheckingError("Overridden function " + name + " cannot be defined in a base class", def, getNames(myContext)));
      return null;
    }
    */

    List<? extends Abstract.Argument> arguments = def.getArguments();
    final List<Argument> typedArguments = new ArrayList<>(arguments.size());
    final List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).build();
    ClassDefinition thisClass = getThisClass(def, myNamespace);
    if (thisClass != null) {
      context.add(new TypedBinding("\\this", ClassCall(thisClass)));
      visitor.setThisClass(thisClass);
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

            context.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
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
          context.add(new TypedBinding((Name) null, result.expression));
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
      if (term instanceof Abstract.ElimExpression) {
        typedDef.setElimTree(visitor.getTypeCheckingElim().typeCheckElim((Abstract.ElimExpression) term, arrow == Abstract.Definition.Arrow.LEFT ? (thisClass == null ? 0 : 1) : null, expectedType));
      } else {
        CheckTypeVisitor.OKResult termResult = visitor.checkType(term, expectedType);
        if (termResult != null) {
          typedDef.setElimTree(new LeafElimTreeNode(def.getArrow(), termResult.expression));
          if (expectedType == null)
            typedDef.setResultType(termResult.type);
        }
      }

      if (typedDef.getElimTree() != null) {
        if (!typedDef.getElimTree().accept(new TerminationCheckVisitor(typedDef, numberOfVariables(typedDef.getArguments())), null)) {
          myErrorReporter.report(new TypeCheckingError("Termination check failed", term, getNames(context)));
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        TypeCheckingError error = TypeCheckingElim.checkCoverage(def, context, typedDef.getElimTree());
        if (error != null) {
          myErrorReporter.report(error);
          typedDef.setElimTree(null);
        }
      }

      if (typedDef.getElimTree() != null) {
        typedDef.hasErrors(false); // we need normalization here
        TypeCheckingError error = TypeCheckingElim.checkConditions(def, context, typedDef.getElimTree());
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
      type = type.getType(new ArrayList<Binding>(2));
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
    Name name = def.getName();
    List<? extends Abstract.Argument> arguments = def.getArguments();
    List<TypeArgument> typedArguments = new ArrayList<>(arguments.size());
    Expression typedResultType;
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("\\this", ClassCall(thisClass)));
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).thisClass(thisClass).build();
    Universe universe = new Universe.Type(0, Universe.Type.PROP);

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
            context.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
            ++index;
          }
        } else {
          typedArguments.add(TypeArg(argument.getExplicit(), result.expression));
          context.add(new TypedBinding((Name) null, result.expression));
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

    ClassField typedDef = new ClassField(myNamespace, name, def.getPrecedence(), Pi(typedArguments, typedResultType), thisClass);
    typedDef.setUniverse(universe);
    typedDef.setThisClass(thisClass);
    myNamespaceMember.definition = typedDef;
    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Void params) {
    List<? extends Abstract.TypeArgument> parameters = def.getParameters();
    List<TypeArgument> typedParameters = new ArrayList<>(parameters.size());
    Universe universe = def.getUniverse();
    Universe typedUniverse = new Universe.Type(0, Universe.Type.PROP);

    List<Binding> context = new ArrayList<>();
    CheckTypeVisitor visitor = new CheckTypeVisitor.Builder(context, myErrorReporter).build();
    ClassDefinition thisClass = getThisClass(def, myNamespace);
    if (thisClass != null) {
      context.add(new TypedBinding("\\this", ClassCall(thisClass)));
      visitor.setThisClass(thisClass);
    }

    for (Abstract.TypeArgument parameter : parameters) {
      CheckTypeVisitor.OKResult result = visitor.checkType(parameter.getType(), Universe());
      if (result == null) return null;
      if (parameter instanceof Abstract.TelescopeArgument) {
        typedParameters.add(Tele(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression));
        List<String> names = ((Abstract.TelescopeArgument) parameter).getNames();
        for (int i = 0; i < names.size(); ++i) {
          context.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
        }
      } else {
        typedParameters.add(TypeArg(parameter.getExplicit(), result.expression));
        context.add(new TypedBinding((Name) null, result.expression));
      }
    }

    Name name = def.getName();
    DataDefinition dataDefinition = new DataDefinition(myNamespace, name, def.getPrecedence(), universe != null ? universe : new Universe.Type(0, Universe.Type.PROP), typedParameters);
    dataDefinition.setThisClass(thisClass);
    myNamespaceMember.definition = dataDefinition;

    myNamespace = myNamespace.getChild(name);
    for (Abstract.Constructor constructor : def.getConstructors()) {
      Constructor typedConstructor = visitConstructor(constructor, dataDefinition, context, visitor);
      if (typedConstructor == null) {
        continue;
      }

      NamespaceMember member = myNamespace.getMember(constructor.getName().name);
      if (member == null) {
        continue;
      }
      member.definition = typedConstructor;

      Universe maxUniverse = typedUniverse.max(typedConstructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + typedConstructor.getUniverse() + " of constructor '" + constructor.getName() + "' is not compatible with universe " + typedUniverse + " of previous constructors";
        myErrorReporter.report(new TypeCheckingError(msg, null, null));
      } else {
        typedUniverse = maxUniverse;
      }
    }
    myNamespace = myNamespace.getParent();

    if (universe != null) {
      if (typedUniverse.lessOrEquals(universe)) {
        dataDefinition.setUniverse(universe);
      } else {
        myErrorReporter.report(new TypeMismatchError(new UniverseExpression(universe), new UniverseExpression(typedUniverse), null, new ArrayList<String>()));
        dataDefinition.setUniverse(typedUniverse);
      }
    } else {
      dataDefinition.setUniverse(typedUniverse);
    }

    context.clear();
    if (def.getConditions() != null) {
      List<Constructor> cycle = typeCheckConditions(visitor, dataDefinition, def.getConditions());
      if (cycle != null) {
        StringBuilder cycleConditionsError = new StringBuilder();
        cycleConditionsError.append("Conditions form a cycle: ");
        for (Constructor constructor : cycle) {
          cycleConditionsError.append(constructor.getName()).append(" - ");
        }
        cycleConditionsError.append(cycle.get(0).getName());
        TypeCheckingError error = new TypeCheckingError(cycleConditionsError.toString(), def, getNames(visitor.getLocalContext()));
        myErrorReporter.report(error);
      }
    }
    if (dataDefinition.getConditions() != null) {
      List<Condition> failedConditions = new ArrayList<>();
      for (Condition condition : dataDefinition.getConditions()) {
        try (Utils.CompleteContextSaver<Binding> ignore = new Utils.CompleteContextSaver<>(visitor.getLocalContext())) {
          expandConstructorContext(condition.getConstructor(), visitor.getLocalContext());
          TypeCheckingError error = TypeCheckingElim.checkConditions(condition.getConstructor().getName(), def, condition.getConstructor().getArguments(), visitor.getLocalContext(), condition.getElimTree());
          if (error != null) {
            myErrorReporter.report(error);
            failedConditions.add(condition);
          }
        }
      }
      dataDefinition.getConditions().removeAll(failedConditions);
    }

    return dataDefinition;
  }

  private List<Constructor> typeCheckConditions(CheckTypeVisitor visitor, DataDefinition dataDefinition, Collection<? extends Abstract.Condition> conds) {
    Map<Constructor, List<Abstract.Condition>> condMap = new HashMap<>();
    for (Abstract.Condition cond : conds) {
      Constructor constructor = dataDefinition.getConstructor(cond.getConstructorName().name);
      if (constructor == null) {
        myErrorReporter.report(new NotInScopeError(cond, cond.getConstructorName()));
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
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(visitor.getLocalContext())) {
        final List<List<Pattern>> patterns = new ArrayList<>();
        final List<Expression> expressions = new ArrayList<>();
        final List<Abstract.Definition.Arrow> arrows = new ArrayList<>();
        expandConstructorContext(constructor, visitor.getLocalContext());

        for (Abstract.Condition cond : condMap.get(constructor)) {
          try (Utils.CompleteContextSaver<Binding> saver = new Utils.CompleteContextSaver<>(visitor.getLocalContext())) {
            List<Expression> resultType = new ArrayList<>(Collections.singletonList(splitArguments(constructor.getBaseType(), new ArrayList<TypeArgument>(), visitor.getLocalContext())));
            List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(cond, constructor.getArguments(), visitor.getLocalContext(), cond.getPatterns());
            if (processedPatterns == null)
              continue;

            List<PatternArgument> typedPatterns = visitor.visitPatternArgs(processedPatterns, resultType, CheckTypeVisitor.PatternExpansionMode.CONDITION);

            CheckTypeVisitor.OKResult result = visitor.checkType(cond.getTerm(), resultType.get(0));
            if (result == null)
              continue;

            patterns.add(toPatterns(typedPatterns));
            expressions.add(result.expression);
            arrows.add(Abstract.Definition.Arrow.RIGHT);
          }
        }

        TypeCheckingElim.TypeCheckElimTreeOKResult elimTreeResult = (TypeCheckingElim.TypeCheckElimTreeOKResult) visitor.getTypeCheckingElim().typeCheckElimTree(numberOfVariables(constructor.getArguments()), patterns, expressions, arrows);

        if (!elimTreeResult.elimTree.accept(new TerminationCheckVisitor(constructor, numberOfVariables(constructor.getArguments()) + constructor.getNumberOfAllParameters()), null)) {
          myErrorReporter.report(new TypeCheckingError("Termination check failed", dataDefinition, getNames(visitor.getLocalContext())));
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

  private void expandConstructorContext(Constructor constructor, List<Binding> context) {
    if (constructor.getPatterns() != null) {
      for (TypeArgument arg : expandConstructorParameters(constructor, context)) {
        Utils.pushArgument(context, arg);
      }
    } else {
      for (TypeArgument arg : constructor.getDataType().getParameters()) {
        Utils.pushArgument(context, arg);
      }
    }
    for (TypeArgument arg : constructor.getArguments()) {
      Utils.pushArgument(context, arg);
    }
  }

  @Override
  public Definition visitConstructor(Abstract.Constructor def, Void params) {
    throw new IllegalStateException();
  }

  public Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition, List<Binding> context, CheckTypeVisitor visitor) {
    try (Utils.CompleteContextSaver ignored = new Utils.CompleteContextSaver<>(context)) {
      List<? extends Abstract.TypeArgument> arguments = def.getArguments();
      List<TypeArgument> typeArguments = new ArrayList<>(arguments.size());
      Universe universe = new Universe.Type(0, Universe.Type.PROP);
      int index = 1;
      boolean ok = true;

      List<? extends Abstract.PatternArgument> patterns = def.getPatterns();
      List<PatternArgument> typedPatterns = null;
      if (patterns != null) {

        List<Abstract.PatternArgument> processedPatterns = processImplicitPatterns(def, dataDefinition.getParameters(), context, patterns);
        if (processedPatterns == null)
          return null;

        typedPatterns = visitor.visitPatternArgs(processedPatterns, Collections.<Expression>emptyList(), CheckTypeVisitor.PatternExpansionMode.DATATYPE);
        if (typedPatterns == null)
          return null;
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
            context.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
          }
          index += ((Abstract.TelescopeArgument) argument).getNames().size();
        } else {
          typeArguments.add(TypeArg(argument.getExplicit(), result.expression));
          context.add(new TypedBinding((Name) null, result.expression));
          ++index;
        }
      }

      if (!ok) {
        return null;
      }

      Name name = def.getName();

      for (int j = 0; j < typeArguments.size(); ++j) {
        Expression type = typeArguments.get(j).getType().normalize(NormalizeVisitor.Mode.WHNF, context);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(dataDefinition), null)) {
              String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
              myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), msg, arguments.get(j).getType(), getNames(context)));
              return null;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF, context);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(dataDefinition), null)) {
            String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + name;
            myErrorReporter.report(new TypeCheckingError(dataDefinition.getParentNamespace().getResolvedName(), msg, arguments.get(j).getType(), getNames(context)));
            return null;
          }
        }
      }

      Constructor constructor = new Constructor(dataDefinition.getParentNamespace().getChild(dataDefinition.getName()), name, def.getPrecedence(), universe, typeArguments, dataDefinition, typedPatterns);
      constructor.setThisClass(dataDefinition.getThisClass());
      dataDefinition.addConstructor(constructor);
      dataDefinition.getParentNamespace().addDefinition(constructor);
      return constructor;
    }
  }

  private List<Abstract.PatternArgument> processImplicitPatterns(Abstract.SourceNode expression, List<TypeArgument> arguments, List<Binding> context, List<? extends Abstract.PatternArgument> patterns) {
    List<Abstract.PatternArgument> processedPatterns = null;
    ProcessImplicitResult processImplicitResult = processImplicit(patterns, arguments);
    if (processImplicitResult.patterns == null) {
      if (processImplicitResult.numExcessive != 0) {
        myErrorReporter.report(new TypeCheckingError("Too many arguments: " + processImplicitResult.numExcessive + " excessive", expression, getNames(context)));
      } else if (processImplicitResult.wrongImplicitPosition < patterns.size()) {
        myErrorReporter.report(new TypeCheckingError("Unexpected implicit argument", patterns.get(processImplicitResult.wrongImplicitPosition), getNames(context)));
      } else {
        myErrorReporter.report(new TypeCheckingError("Too few explicit arguments, expected: " + processImplicitResult.numExplicit, expression, getNames(context)));
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
          NamespaceMember member = namespace.getMember(definition.getName().name);
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
                myErrorReporter.report(new TypeCheckingError(myNamespace.getResolvedName(), error, definition, new ArrayList<String>()));
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
    Name name = def.getName();
    ClassDefinition typedDef = new ClassDefinition(myNamespace, name);
    ClassDefinition thisClass = getThisClass(def, myNamespace);
    if (thisClass != null) {
      typedDef.addParentField(thisClass);
    }
    typeCheckStatements(typedDef, def.getStatements(), myNamespace.getChild(name));
    return typedDef;
  }
}
