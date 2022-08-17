package org.arend.typechecking.visitor;

import org.arend.core.definition.Definition;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ParameterReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ReplaceDataVisitor;
import org.arend.term.prettyprint.FreeVariableCollectorConcrete;

import java.util.*;

public class WhereVarsFixVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final Concrete.Definition myDefinition;
  private final Set<TCDefReferable> myDefinitions;
  private final List<Concrete.Argument> mySelfArgs;

  private WhereVarsFixVisitor(Concrete.Definition definition, Set<TCDefReferable> definitions, List<Concrete.Argument> selfArgs) {
    myDefinition = definition;
    myDefinitions = definitions;
    mySelfArgs = selfArgs;
  }

  private static class WhereVarData {
    final ParameterReferable parameterRef;
    final Concrete.Parameter parameter;
    final Referable referable;
    final TCDefReferable definitionRef;
    final int parameterIndex;

    WhereVarData(ParameterReferable parameterRef, Concrete.Parameter parameter, Referable referable, TCDefReferable definitionRef, int parameterIndex) {
      this.parameterRef = parameterRef;
      this.parameter = parameter;
      this.referable = referable;
      this.definitionRef = definitionRef;
      this.parameterIndex = parameterIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      WhereVarData that = (WhereVarData) o;
      return parameterIndex == that.parameterIndex && definitionRef.equals(that.definitionRef);
    }

    @Override
    public int hashCode() {
      return Objects.hash(definitionRef, parameterIndex);
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

  public static void fixDefinition(Collection<? extends Concrete.Definition> definitions) {
    var whereVars = WhereVarsCollector.findWhereVars(definitions);
    Set<TCDefReferable> definitionSet = new HashSet<>();
    for (Concrete.Definition definition : definitions) {
      definitionSet.add(definition.getData());
    }

    for (Concrete.Definition definition : definitions) {
      List<Concrete.Parameter> newParams = Collections.emptyList();
      List<Pair<TCDefReferable, Integer>> parametersOriginalDefinitions = Collections.emptyList();
      List<Concrete.Argument> selfArgs = Collections.emptyList();
      if (!whereVars.proj1.isEmpty() || !whereVars.proj2.isEmpty()) {
        Set<Pair<TCDefReferable, Integer>> wherePairs = new HashSet<>();
        Set<WhereVarData> dataSet = new HashSet<>();
        for (ParameterReferable whereRef : whereVars.proj1) {
          TCDefReferable defRef = (TCDefReferable) whereRef.getDefinition().getData();
          Referable origRef = whereRef.getReferable();
          int index = 0;
          loop:
          for (Concrete.Parameter parameter : whereRef.getDefinition().getParameters()) {
            for (Referable referable : parameter.getRefList()) {
              if (referable == origRef) {
                dataSet.add(new WhereVarData(whereRef, parameter, referable, defRef, index));
                wherePairs.add(new Pair<>(defRef, index));
                break loop;
              }
              index++;
            }
          }
        }

        Map<Concrete.GeneralDefinition, Set<Referable>> refMap = new HashMap<>();
        for (WhereVarData varData : dataSet) {
          refMap.computeIfAbsent(varData.parameterRef.getDefinition(), k -> new HashSet<>()).add(varData.referable);
        }

        for (Map.Entry<Concrete.GeneralDefinition, Set<Referable>> entry : refMap.entrySet()) {
          Map<Referable, Pair<Concrete.Parameter,Integer>> parameterMap = new HashMap<>();
          int index = 0;
          for (Concrete.Parameter parameter : entry.getKey().getParameters()) {
            for (Referable referable : parameter.getReferableList()) {
              if (referable != null) parameterMap.put(referable, new Pair<>(parameter, index));
              index++;
            }
          }

          Set<Referable> found = entry.getValue();
          while (!found.isEmpty()) {
            Set<Referable> foundRefs = new HashSet<>();
            for (Referable referable : found) {
              Concrete.Expression type = parameterMap.get(referable).proj1.getType();
              if (type != null) {
                type.accept(new FreeVariableCollectorConcrete(foundRefs), null);
              }
            }

            found.clear();
            for (Referable foundRef : foundRefs) {
              var pair = parameterMap.get(foundRef);
              if (pair != null) {
                dataSet.add(new WhereVarData(null, pair.proj1, foundRef, (TCDefReferable) entry.getKey().getData(), pair.proj2));
                if (wherePairs.add(new Pair<>((TCDefReferable) entry.getKey().getData(), pair.proj2))) {
                  found.add(foundRef);
                }
              }
            }
          }
        }

        int myLevel = getReferableLevel(definition.getData());
        for (Definition def : whereVars.proj2) {
          for (Pair<TCDefReferable, Integer> pair : def.getParametersOriginalDefinitions()) {
            if (pair.proj1 != definition.getData() && !wherePairs.contains(pair) && getReferableLevel(pair.proj1) < myLevel) {
              dataSet.add(new WhereVarData(null, null, null, pair.proj1, pair.proj2));
            }
          }
        }

        List<WhereVarData> dataList = new ArrayList<>(dataSet);
        dataList.sort(Comparator.comparingInt((WhereVarData data) -> getReferableLevel(data.definitionRef)).thenComparingInt(data -> data.parameterIndex));
        newParams = new ArrayList<>();
        selfArgs = new ArrayList<>();
        parametersOriginalDefinitions = new ArrayList<>();
        for (WhereVarData data : dataList) {
          if (data.parameter != null) {
            newParams.add(new Concrete.TelescopeParameter(definition.getData(), data.parameter.isExplicit(), Collections.singletonList(data.referable), data.parameter.getType() == null ? null : data.parameter.getType()));
            selfArgs.add(new Concrete.Argument(new Concrete.ReferenceExpression(null, data.referable), data.parameter.isExplicit()));
          } else {
            List<? extends Concrete.Parameter> params = definition.getExternalParameters().get(data.definitionRef);
            if (params != null) {
              Pair<Concrete.Parameter, Referable> param = Concrete.getParameter(params, data.parameterIndex);
              if (param != null) {
                newParams.add(new Concrete.TelescopeParameter(definition.getData(), param.proj1.isExplicit(), Collections.singletonList(param.proj2), param.proj1.getType() == null ? null : param.proj1.getType()));
                selfArgs.add(new Concrete.Argument(new Concrete.ReferenceExpression(null, param.proj2), param.proj1.isExplicit()));
              }
            }
          }
          parametersOriginalDefinitions.add(new Pair<>(data.definitionRef, data.parameterIndex));
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
            newNewParams.add(new Concrete.TelescopeParameter(definition.getData(), param.isExplicit(), referables, param.getType() == null ? null : param.getType().accept(new ReplaceDataVisitor(definition.getData()), null)));
          } else {
            newNewParams.add(new Concrete.TelescopeParameter(definition.getData(), param.isExplicit(), param.getReferableList(), param.getType() == null ? null : param.getType().accept(new ReplaceDataVisitor(definition.getData()), null)));
          }
        }
        newParams = newNewParams;

        if (!parametersOriginalDefinitions.isEmpty()) {
          if (definition instanceof Concrete.BaseFunctionDefinition && !definition.getParameters().isEmpty()) {
            Concrete.FunctionBody body = ((Concrete.BaseFunctionDefinition) definition).getBody();
            if (body instanceof Concrete.ElimFunctionBody && body.getEliminatedReferences().isEmpty()) {
              for (Concrete.Parameter parameter : definition.getParameters()) {
                for (Referable referable : parameter.getReferableList()) {
                  ((Concrete.ElimFunctionBody) body).getEliminatedReferences().add(new Concrete.ReferenceExpression(definition.getData(), referable));
                }
              }
            }
          }
        }
      }
      definition.accept(new WhereVarsFixVisitor(definition, definitionSet, selfArgs), null);
      if (!parametersOriginalDefinitions.isEmpty()) {
        definition.addParameters(newParams, parametersOriginalDefinitions);
      }
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
          List<? extends Concrete.Parameter> parameters = myDefinition.getData() == pair.proj1 ? myDefinition.getParameters() : myDefinition.getExternalParameters().get(pair.proj1);
          if (parameters != null) {
            Pair<Concrete.Parameter, Referable> paramRef = Concrete.getParameter(parameters, pair.proj2);
            if (paramRef != null) {
              args.add(new Concrete.Argument(new Concrete.ReferenceExpression(expr.getData(), paramRef.proj2), paramRef.proj1.isExplicit()));
            }
          }
        }
        return Concrete.AppExpression.make(expr.getData(), expr, args);
      } else if (myDefinitions.contains(ref) && !mySelfArgs.isEmpty()) {
        List<Concrete.Argument> args = new ArrayList<>(mySelfArgs.size());
        for (Concrete.Argument arg : mySelfArgs) {
          args.add(new Concrete.Argument(new Concrete.ReferenceExpression(expr.getData(), ((Concrete.ReferenceExpression) arg.expression).getReferent()), arg.isExplicit()));
        }
        return Concrete.AppExpression.make(expr.getData(), expr, args);
      }
    }
    return expr;
  }
}
