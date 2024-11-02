package org.arend.typechecking.visitor;

import org.arend.core.definition.Definition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.*;
import org.arend.term.concrete.*;
import org.arend.term.group.AccessModifier;
import org.arend.term.prettyprint.FreeVariableCollectorConcrete;

import java.util.*;

public class WhereVarsFixVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final Concrete.Definition myDefinition;
  private final Set<Referable> myAddedParams;
  private final Map<TCDefReferable, List<Referable>> mySelfArgs;
  private final Map<ParameterReferable, Referable> myReferableMap = new HashMap<>();

  private WhereVarsFixVisitor(Concrete.Definition definition, Map<TCDefReferable, List<Referable>> selfArgs) {
    myDefinition = definition;
    mySelfArgs = selfArgs;
    myAddedParams = new HashSet<>();
    List<Referable> refs = selfArgs.get(definition.getData());
    if (refs != null) {
      myAddedParams.addAll(refs);
    }
    for (Concrete.Parameter parameter : definition.getParameters()) {
      myAddedParams.addAll(parameter.getRefList());
    }
  }

  private static int getReferableLevel(LocatedReferable referable) {
    int level = 0;
    while (true) {
      LocatedReferable parent = referable.getLocatedReferableParent();
      if (parent == null) {
        break;
      }
      level++;
      referable = parent;
    }
    return level;
  }

  public static void fixDefinition(Collection<? extends Concrete.Definition> definitions, ErrorReporter errorReporter) {
    var whereVars = WhereVarsCollector.findWhereVars(definitions);
    Map<TCDefReferable, List<Referable>> selfArgsMap = new HashMap<>();
    Map<TCDefReferable, Pair<List<Concrete.Parameter>, List<Pair<TCDefReferable, Integer>>>> paramsMap = new HashMap<>();
    for (Concrete.Definition definition : definitions) {
      List<Concrete.Parameter> newParams = Collections.emptyList();
      List<Pair<TCDefReferable, Integer>> parametersOriginalDefinitions = Collections.emptyList();
      if (!whereVars.proj1.isEmpty() || !whereVars.proj2.isEmpty()) {
        Set<Pair<TCDefReferable, Integer>> wherePairs = new HashSet<>();
        Map<TCDefReferable, Set<Referable>> refMap = new HashMap<>();
        for (ParameterReferable whereRef : whereVars.proj1) {
          if (definition.getExternalParameters().containsKey(whereRef.getDefinition())) {
            wherePairs.add(new Pair<>(whereRef.getDefinition(), whereRef.getIndex()));
            refMap.computeIfAbsent(whereRef.getDefinition(), k -> new HashSet<>()).add(getReferable(definition, whereRef));
          }
        }

        record ParamData(TCDefReferable definition, Concrete.Parameter parameter, int index) {}

        loop:
        for (Map.Entry<TCDefReferable, Set<Referable>> entry : refMap.entrySet()) {
          Map<Referable, ParamData> parameterMap = new HashMap<>();
          for (Map.Entry<TCDefReferable, Concrete.ExternalParameters> entry1 : definition.getExternalParameters().entrySet()) {
            int index = 0;
            for (Concrete.Parameter parameter : entry1.getValue().parameters) {
              for (Referable referable : parameter.getReferableList()) {
                if (referable != null) {
                  ParamData data = new ParamData(entry1.getKey(), parameter, index);
                  parameterMap.put(referable, data);
                  parameterMap.put(referable.getUnderlyingReferable(), data);
                }
                index++;
              }
            }
          }

          Set<Referable> found = entry.getValue();
          for (Referable referable : found) {
            if (!parameterMap.containsKey(referable)) {
              continue loop;
            }
          }
          while (!found.isEmpty()) {
            Set<Referable> foundRefs = new HashSet<>();
            for (Referable referable : found) {
              Concrete.Expression type = parameterMap.get(referable).parameter.getType();
              if (type != null) {
                type.accept(new FreeVariableCollectorConcrete(foundRefs), null);
              }
            }

            found.clear();
            for (Referable foundRef : foundRefs) {
              while (foundRef instanceof ParameterReferable paramRef) {
                foundRef = paramRef.getUnderlyingReferable();
              }
              ParamData paramData = parameterMap.get(foundRef);
              if (paramData != null && wherePairs.add(new Pair<>(paramData.definition, paramData.index))) {
                found.add(foundRef);
              }
            }
          }
        }

        int myLevel = getReferableLevel(definition.getData());
        for (Definition def : whereVars.proj2) {
          for (Pair<TCDefReferable, Integer> pair : def.getParametersOriginalDefinitions()) {
            if (definition.getExternalParameters().containsKey(pair.proj1) && pair.proj1 != definition.getData() && !wherePairs.contains(pair) && getReferableLevel(pair.proj1) < myLevel && !(definition instanceof Concrete.CoClauseFunctionDefinition && definition.getUseParent() == pair.proj1)) {
              wherePairs.add(pair);
            }
          }
        }

        parametersOriginalDefinitions = new ArrayList<>(wherePairs);
        parametersOriginalDefinitions.sort(Comparator.comparingInt((Pair<TCDefReferable, Integer> data) -> getReferableLevel(data.proj1)).thenComparingInt(data -> data.proj2));
        newParams = new ArrayList<>();
        for (var data : parametersOriginalDefinitions) {
          Concrete.ExternalParameters params = definition.getExternalParameters().get(data.proj1);
          if (params != null) {
            Pair<Concrete.Parameter, Referable> param = Concrete.getParameter(params.parameters, data.proj2);
            if (param != null) {
              newParams.add(new Concrete.TelescopeParameter(definition.getData(), false, Collections.singletonList(param.proj2), param.proj1.getType() == null ? null : param.proj1.getType(), param.proj1.isProperty()));
            }
          }
        }

        List<Concrete.Parameter> newNewParams = new ArrayList<>();
        for (int i = 0; i < newParams.size(); i++) {
          Concrete.Parameter param = newParams.get(i);
          if (i + 1 < newParams.size() && param.getType() != null && param.getType() == newParams.get(i + 1).getType() && param.isExplicit() == newParams.get(i + 1).isExplicit()) {
            List<Referable> referables = new ArrayList<>(param.getReferableList());
            while (i + 1 < newParams.size() && param.getType() == newParams.get(i + 1).getType() && param.isExplicit() == newParams.get(i + 1).isExplicit()) {
              i++;
              referables.addAll(newParams.get(i).getReferableList());
            }
            newNewParams.add(new Concrete.TelescopeParameter(definition.getData(), param.isExplicit(), referables, param.getType() == null ? null : param.getType().accept(new ReplaceDataVisitor(definition.getData()), null), param.isProperty()));
          } else {
            newNewParams.add(new Concrete.TelescopeParameter(definition.getData(), param.isExplicit(), param.getReferableList(), param.getType() == null ? null : param.getType().accept(new ReplaceDataVisitor(definition.getData()), null), param.isProperty()));
          }
        }
        newParams = newNewParams;

        List<Referable> selfArgs = new ArrayList<>();
        for (Concrete.Parameter param : newParams) {
          selfArgs.addAll(param.getReferableList());
        }
        if (!selfArgs.isEmpty()) {
          selfArgsMap.put(definition.getData(), selfArgs);
        }

        Set<Referable> levelRefs = new HashSet<>();
        for (Concrete.Parameter param : newParams) {
          if (param.getType() != null) {
            param.getType().accept(new FindLevelVariablesVisitor(levelRefs), null);
          }
        }
        levelRefs.removeIf(ref -> ref instanceof TCLevelReferable);
        if (!levelRefs.isEmpty()) {
          Concrete.LevelParameters pLevels = null;
          Concrete.LevelParameters hLevels = null;
          Set<TCDefReferable> pLevelsDefs = new HashSet<>();
          Set<TCDefReferable> hLevelsDefs = new HashSet<>();
          for (var varData : parametersOriginalDefinitions) {
            Concrete.ExternalParameters params = definition.getExternalParameters().get(varData.proj1);
            if (params != null) {
              if (params.pLevelParameters != null) {
                pLevelsDefs.add(varData.proj1);
                if (pLevels == null) pLevels = params.pLevelParameters;
              }
              if (params.hLevelParameters != null) {
                hLevelsDefs.add(varData.proj1);
                if (hLevels == null) hLevels = params.hLevelParameters;
              }
            }
          }
          checkLevels(pLevelsDefs, definition.getPLevelParameters(), errorReporter, "p", definition);
          checkLevels(hLevelsDefs, definition.getHLevelParameters(), errorReporter, "h", definition);
          if (pLevels != null && definition.getPLevelParameters() == null) {
            definition.setPLevelParameters(pLevels);
          }
          if (hLevels != null && definition.getHLevelParameters() == null) {
            definition.setHLevelParameters(hLevels);
          }
        }
      }
      if (!parametersOriginalDefinitions.isEmpty()) {
        paramsMap.put(definition.getData(), new Pair<>(newParams, parametersOriginalDefinitions));
      }
    }

    for (Concrete.Definition definition : definitions) {
      WhereVarsFixVisitor varsFixVisitor = new WhereVarsFixVisitor(definition, selfArgsMap);
      definition.accept(varsFixVisitor, null);
      var pair = paramsMap.get(definition.getData());
      if (pair != null) {
        if (definition instanceof Concrete.ClassDefinition) {
          SubstConcreteVisitor visitor = new SubstConcreteVisitor(null);
          for (int i = 0; i < pair.proj1.size(); i++) {
            Concrete.Parameter param = pair.proj1.get(i);
            List<Referable> newRefs = new ArrayList<>(param.getRefList().size());
            pair.proj1.set(i, new Concrete.TelescopeParameter(param.getData(), param.isExplicit(), newRefs, param.getType() == null ? null : param.getType().accept(visitor, null), param.isProperty()));
            for (Referable referable : param.getRefList()) {
              FieldReferableImpl newRef = new FieldReferableImpl(AccessModifier.PUBLIC, Precedence.DEFAULT, referable.getRefName(), param.isExplicit(), true, true, definition.getData());
              newRefs.add(newRef);
              visitor.bind(referable, newRef);
            }
          }
          definition.accept(visitor, null);
        }
        varsFixVisitor.visitParameters(pair.proj1, null);
        definition.addParameters(pair.proj1, pair.proj2);

        if (definition instanceof Concrete.BaseFunctionDefinition) {
          Concrete.FunctionBody body = ((Concrete.BaseFunctionDefinition) definition).getBody();
          if (body instanceof Concrete.ElimFunctionBody && body.getEliminatedReferences().isEmpty()) {
            for (Concrete.FunctionClause clause : body.getClauses()) {
              addParametersToClause(pair.proj1, clause);
            }
          }
        }

        if (definition instanceof Concrete.DataDefinition dataDef && dataDef.getEliminatedReferences() != null && dataDef.getEliminatedReferences().isEmpty()) {
          for (Concrete.ConstructorClause clause : dataDef.getConstructorClauses()) {
            addParametersToClause(pair.proj1, clause);
          }
        }

        if (definition instanceof Concrete.CoClauseFunctionDefinition coClauseDef) {
          int n = coClauseDef.getNumberOfExternalParameters();
          for (Concrete.Parameter parameter : pair.proj1) {
            parameter.setExplicit(false);
            n += parameter.getNumberOfParameters();
          }
          coClauseDef.setNumberOfExternalParameters(n);
        }
      }
    }
  }

  private static void addParametersToClause(List<Concrete.Parameter> newParameters, Concrete.Clause clause) {
    List<Concrete.Pattern> newPatterns = new ArrayList<>();
    for (Concrete.Parameter parameter : newParameters) {
      for (Referable referable : parameter.getReferableList()) {
        newPatterns.add(new Concrete.NamePattern(clause.getData(), false, referable, null));
      }
    }
    clause.getPatterns().addAll(0, newPatterns);
  }

  private static void checkLevels(Set<TCDefReferable> defs, Concrete.LevelParameters parameters, ErrorReporter errorReporter, String kind, Concrete.SourceNode sourceNode) {
    if (defs.size() > 1 || !defs.isEmpty() && parameters != null) {
      errorReporter.report(new TypecheckingError("Definition refers to different " + kind + "-levels", parameters != null ? parameters : sourceNode));
    }
  }

  private static Referable getReferable(Concrete.Definition definition, ParameterReferable parameterRef) {
    Concrete.ExternalParameters extParams = definition.getExternalParameters().get(parameterRef.getDefinition());
    if (extParams != null) {
      var pair = Concrete.getParameter(extParams.parameters, parameterRef.getIndex());
      if (pair != null) {
        return pair.proj2;
      }
    }
    return parameterRef;
  }

  private Referable getReferable(ParameterReferable parameterRef) {
    return myReferableMap.computeIfAbsent(parameterRef, k -> getReferable(myDefinition, k));
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    if (expr.getFunction() instanceof Concrete.ReferenceExpression refExpr && refExpr.getReferent() instanceof MetaReferable metaRef) {
      MetaDefinition metaDef = metaRef.getDefinition();
      if (metaDef != null) {
        List<Concrete.Argument> args = expr.getArguments();
        int[] indices = metaDef.desugarArguments(args);
        if (indices != null) {
          for (int index : indices) {
            args.get(index).expression = args.get(index).expression.accept(this, null);
          }
          return expr;
        }
      }
    } else if (expr.getFunction() instanceof Concrete.ReferenceExpression refExpr && !expr.getArguments().get(0).isExplicit()) {
      if (refExpr.getReferent() instanceof ParameterReferable) {
        visitReference(refExpr, null);
      }
      for (Concrete.Argument argument : expr.getArguments()) {
        argument.expression = argument.expression.accept(this, params);
      }
      return expr;
    }

    return super.visitApp(expr, params);
  }

  @Override
  protected void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, Void params) {
    if (classFieldImpl instanceof Concrete.CoClauseFunctionReference) {
      visitClassElements(classFieldImpl.getSubCoclauseList(), params);
    } else {
      super.visitClassFieldImpl(classFieldImpl, params);
    }
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    Referable ref = expr.getReferent();
    if (ref instanceof TCDefReferable) {
      Definition def = ((TCDefReferable) ref).getTypechecked();
      if (def != null && !def.getParametersOriginalDefinitions().isEmpty()) {
        List<Concrete.Argument> args = new ArrayList<>();
        for (Pair<TCDefReferable, Integer> pair : def.getParametersOriginalDefinitions()) {
          List<? extends Concrete.Parameter> parameters;
          if (myDefinition.getData() == pair.proj1) {
            parameters = myDefinition.getParameters();
          } else {
            Concrete.ExternalParameters externalParameters = myDefinition.getExternalParameters().get(pair.proj1);
            parameters = externalParameters == null ? null : externalParameters.parameters;
          }
          if (parameters != null) {
            Pair<Concrete.Parameter, Referable> paramRef = Concrete.getParameter(parameters, pair.proj2);
            if (paramRef != null) {
              args.add(new Concrete.GeneratedArgument(new Concrete.ReferenceExpression(expr.getData(), paramRef.proj2)));
            }
          }
        }
        return Concrete.AppExpression.make(expr.getData(), expr, args);
      } else if (!myAddedParams.isEmpty()) {
        List<Referable> selfArgs = mySelfArgs.get(ref);
        if (selfArgs != null) {
          boolean found = false;
          List<Concrete.Argument> args = new ArrayList<>(selfArgs.size());
          for (Referable selfArg : selfArgs) {
            boolean myHas = myAddedParams.contains(selfArg);
            if (myHas) found = true;
            args.add(new Concrete.GeneratedArgument(myHas ? new Concrete.ReferenceExpression(expr.getData(), selfArg) : new Concrete.HoleExpression(expr.getData())));
          }
          if (found) {
            return Concrete.AppExpression.make(expr.getData(), expr, args);
          }
        }
      }
    } else if (ref instanceof ParameterReferable) {
      expr.setReferent(getReferable((ParameterReferable) ref));
    }
    return expr;
  }
}
