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
    final TCDefReferable definitionRef;
    final int parameterIndex;

    WhereVarData(ParameterReferable parameterRef, TCDefReferable definitionRef, int parameterIndex) {
      this.parameterRef = parameterRef;
      this.definitionRef = definitionRef;
      this.parameterIndex = parameterIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      WhereVarData that = (WhereVarData) o;
      return parameterIndex == that.parameterIndex && Objects.equals(parameterRef, that.parameterRef) && definitionRef.equals(that.definitionRef);
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameterRef, definitionRef, parameterIndex);
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
                break loop;
              }
              index++;
            }
          }
          dataSet.add(new WhereVarData(whereRef, defRef, index));
          wherePairs.add(new Pair<>(defRef, index));
        }

        int myLevel = getReferableLevel(definition.getData());
        for (Definition def : whereVars.proj2) {
          for (Pair<TCDefReferable, Integer> pair : def.getParametersOriginalDefinitions()) {
            if (pair.proj1 != definition.getData() && !wherePairs.contains(pair) && getReferableLevel(pair.proj1) < myLevel) {
              dataSet.add(new WhereVarData(null, pair.proj1, pair.proj2));
            }
          }
        }

        List<WhereVarData> dataList = new ArrayList<>(dataSet);
        dataList.sort(Comparator.comparingInt((WhereVarData data) -> getReferableLevel(data.definitionRef)).thenComparingInt(data -> data.parameterIndex));
        newParams = new ArrayList<>();
        selfArgs = new ArrayList<>();
        parametersOriginalDefinitions = new ArrayList<>();
        for (WhereVarData data : dataList) {
          if (data.parameterRef == null) {
            List<? extends Concrete.Parameter> params = definition.getExternalParameters().get(data.definitionRef);
            if (params != null) {
              Pair<Concrete.Parameter, Referable> param = Concrete.getParameter(params, data.parameterIndex);
              if (param != null) {
                newParams.add(new Concrete.TelescopeParameter(definition.getData(), param.proj1.isExplicit(), Collections.singletonList(param.proj2), param.proj1.getType() == null ? null : param.proj1.getType().accept(new ReplaceDataVisitor(definition.getData()), null)));
                selfArgs.add(new Concrete.Argument(new Concrete.ReferenceExpression(null, param.proj2), param.proj1.isExplicit()));
              }
            }
          } else {
            Referable origRef = data.parameterRef.getReferable();
            loop:
            for (Concrete.Parameter parameter : data.parameterRef.getDefinition().getParameters()) {
              for (Referable referable : parameter.getRefList()) {
                if (referable == origRef) {
                  newParams.add(new Concrete.TelescopeParameter(definition.getData(), parameter.isExplicit(), Collections.singletonList(referable), parameter.getType() == null ? null : parameter.getType().accept(new ReplaceDataVisitor(definition.getData()), null)));
                  selfArgs.add(new Concrete.Argument(new Concrete.ReferenceExpression(null, referable), parameter.isExplicit()));
                  break loop;
                }
              }
            }
          }
          parametersOriginalDefinitions.add(new Pair<>(data.definitionRef, data.parameterIndex));
        }
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
