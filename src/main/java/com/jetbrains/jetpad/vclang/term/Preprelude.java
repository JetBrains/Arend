package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Preprelude extends SimpleNamespace {
  public static ModuleID moduleID = new ModuleID() {
    @Override
    public ModulePath getModulePath() {
      return new ModulePath("Preprelude");
    }
  };

  public static ClassDefinition PRE_PRELUDE_CLASS;

  public static SimpleNamespace PRE_PRELUDE = new Preprelude();

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition LVL;
  public static Constructor ZERO_LVL;
  public static Constructor SUC_LVL;
  public static FunctionDefinition MAX_LVL;

  public static DataDefinition CNAT;
  public static Constructor FIN, INF;

  public static FunctionDefinition MAX_NAT;
  public static FunctionDefinition PRED_NAT;
  public static FunctionDefinition MAX_CNAT;
  public static FunctionDefinition SUC_CNAT;
  public static FunctionDefinition PRED_CNAT;

  static {
    PRE_PRELUDE_CLASS = new ClassDefinition("Preprelude", PRE_PRELUDE, EmptyNamespace.INSTANCE);

    /* Nat, zero, suc */
    DefinitionBuilder.Data nat = new DefinitionBuilder.Data(PRE_PRELUDE, "Nat", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    NAT = nat.definition();
    ZERO = nat.addConstructor("zero", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    SUC = nat.addConstructor("suc", Abstract.Binding.DEFAULT_PRECEDENCE, null, param(DataCall(NAT)));

    /* Lvl, zeroLvl, sucLvl */
    DefinitionBuilder.Data lvl = new DefinitionBuilder.Data(PRE_PRELUDE, "Lvl", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    LVL = lvl.definition();
    DependentLink sucLvlParameter = param("l", DataCall(LVL));
    ZERO_LVL = lvl.addConstructor("zeroLvl", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    SUC_LVL = lvl.addConstructor("sucLvl", Abstract.Binding.DEFAULT_PRECEDENCE, null, sucLvlParameter);

    /* maxLvl */
    DependentLink maxLvlParameter1 = param(DataCall(LVL));
    DependentLink maxLvlParameter2 = param(DataCall(LVL));
    maxLvlParameter1.setNext(maxLvlParameter2);
    DependentLink sucLvlParameterPrime = param("l'", DataCall(LVL));
    MAX_LVL = new DefinitionBuilder.Function(PRE_PRELUDE, "maxLvl", Abstract.Binding.DEFAULT_PRECEDENCE, maxLvlParameter1, DataCall(LVL), null).definition();
    ElimTreeNode maxLvlElimTree = top(maxLvlParameter1, branch(maxLvlParameter1, tail(maxLvlParameter2),
            clause(ZERO_LVL, EmptyDependentLink.getInstance(), branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), ConCall(ZERO_LVL)),
                    clause(SUC_LVL, sucLvlParameter, Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))))),
            clause(SUC_LVL, sucLvlParameter, branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))),
                    clause(SUC_LVL, sucLvlParameterPrime, Apps(ConCall(SUC_LVL), Apps(FunCall(MAX_LVL), Reference(sucLvlParameter), Reference(sucLvlParameterPrime))))))));
    MAX_LVL.setElimTree(maxLvlElimTree);

    /* CNat, inf, fin */
    DefinitionBuilder.Data cnat = new DefinitionBuilder.Data(PRE_PRELUDE, "CNat", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    CNAT = cnat.definition();
    INF = cnat.addConstructor("inf", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    FIN = cnat.addConstructor("fin", Abstract.Binding.DEFAULT_PRECEDENCE, null, param("n", DataCall(NAT)));

    /* maxNat */
    DependentLink maxNatParameter1 = param(DataCall(NAT));
    DependentLink maxNatParameter2 = param(DataCall(NAT));
    maxNatParameter1.setNext(maxNatParameter2);
    DependentLink sucNatParameter = param("n", DataCall(NAT));
    DependentLink sucNatParameterPrime = param("n'", DataCall(NAT));
    MAX_NAT = new DefinitionBuilder.Function(PRE_PRELUDE, "maxNat", Abstract.Binding.DEFAULT_PRECEDENCE, maxNatParameter1, DataCall(NAT), null).definition();
    ElimTreeNode maxNatElimTree = top(maxNatParameter1, branch(maxNatParameter1, tail(maxNatParameter2),
            clause(ZERO, EmptyDependentLink.getInstance(), branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), ConCall(ZERO)),
                    clause(SUC, sucNatParameter, Apps(ConCall(SUC), Reference(sucNatParameter))))),
            clause(SUC, sucNatParameter, branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), Apps(ConCall(SUC), Reference(sucNatParameter))),
                    clause(SUC, sucNatParameterPrime, Apps(ConCall(SUC), Apps(FunCall(MAX_NAT), Reference(sucNatParameter), Reference(sucNatParameterPrime))))))));
    MAX_NAT.setElimTree(maxNatElimTree);

    /* predNat */
    DependentLink predNatParameter = param("n", DataCall(NAT));
    PRED_NAT = new DefinitionBuilder.Function(PRE_PRELUDE, "predNat", Abstract.Binding.DEFAULT_PRECEDENCE, predNatParameter, DataCall(NAT), null).definition();
    ElimTreeNode predNatElimTree = top(predNatParameter, branch(predNatParameter, tail(),
            clause(ZERO, EmptyDependentLink.getInstance(), Zero()),
            clause(SUC, sucNatParameter, Reference(sucNatParameter))));
    PRED_NAT.setElimTree(predNatElimTree);

    /* maxCNat */
    DependentLink maxCNatParameter1 = param(DataCall(CNAT));
    DependentLink maxCNatParameter2 = param(DataCall(CNAT));
    maxCNatParameter1.setNext(maxCNatParameter2);
    DependentLink finCNatParameter = param("n", DataCall(NAT));
    DependentLink finCNatParameterPrime = param("n'", DataCall(NAT));
    ElimTreeNode maxCNatElimTree = top(maxCNatParameter1, branch(maxCNatParameter1, tail(maxCNatParameter2),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, branch(maxCNatParameter2, tail(),
                    clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
                   // clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime)))),
                    clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime))))))));
    MAX_CNAT = new DefinitionBuilder.Function(PRE_PRELUDE, "maxCNat", Abstract.Binding.DEFAULT_PRECEDENCE, maxCNatParameter1, DataCall(CNAT), maxCNatElimTree).definition();

    /* sucCNat */
    DependentLink sucCNatParameter = param(DataCall(CNAT));
    ElimTreeNode sucCNatElimTree = top(sucCNatParameter, branch(sucCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(ConCall(SUC), Reference(finCNatParameter))))));
    SUC_CNAT = new DefinitionBuilder.Function(PRE_PRELUDE, "sucCNat", Abstract.Binding.DEFAULT_PRECEDENCE, sucCNatParameter, DataCall(CNAT), sucCNatElimTree).definition();

    /* predCNat */
    DependentLink predCNatParameter = param("n", DataCall(CNAT));
    PRED_CNAT = new DefinitionBuilder.Function(PRE_PRELUDE, "predCNat", Abstract.Binding.DEFAULT_PRECEDENCE, predNatParameter, DataCall(CNAT), null).definition();
    ElimTreeNode predCNatElimTree = top(predCNatParameter, branch(predCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(FunCall(PRED_NAT), Reference(finCNatParameter))))));
    PRED_CNAT.setElimTree(predCNatElimTree);

    /* I, left, right */
    DefinitionBuilder.Data interval = new DefinitionBuilder.Data(PRE_PRELUDE, "I", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    INTERVAL = interval.definition();
    LEFT = interval.addConstructor("left", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    RIGHT = interval.addConstructor("right", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    ABSTRACT = interval.addConstructor("<abstract>", Abstract.Binding.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
  }

  public static void setUniverses() {
    NAT.setSort(Sort.SetOfLevel(0));
    ZERO.setSort(Sort.SetOfLevel(0));
    SUC.setSort(Sort.SetOfLevel(0));
    LVL.setSort(Sort.SetOfLevel(0));
    ZERO_LVL.setSort(Sort.SetOfLevel(0));
    SUC_LVL.setSort(Sort.SetOfLevel(0));
    CNAT.setSort(Sort.SetOfLevel(0));
    FIN.setSort(Sort.SetOfLevel(0));
    INF.setSort(Sort.SetOfLevel(0));
    INTERVAL.setSort(Sort.PROP);
    LEFT.setSort(Sort.PROP);
    RIGHT.setSort(Sort.PROP);
    ABSTRACT.setSort(Sort.PROP);
  }

  public Preprelude() {
  }

  public static class SucExtrResult {
    public int NumSuc;
    public Expression Arg;

    public SucExtrResult(int numSuc, Expression arg) {
      NumSuc = numSuc;
      Arg = arg;
    }

    public SucExtrResult incr() {
      return new SucExtrResult(NumSuc + 1, Arg);
    }
  }

  public static SucExtrResult extractSuc(Expression expr, Constructor suc) {
    Expression fun = expr.getFunction();
    if (fun.toConCall() != null && fun.toConCall().getDefinition() == suc &&
            expr.getArguments().size() == 1) {
      return extractSuc(expr.getArguments().get(0), suc).incr();
    }
    return new SucExtrResult(0, expr);
  }

  public static SucExtrResult extractSuc(Expression expr, FunctionDefinition suc) {
    Expression fun = expr.getFunction();
    if (fun.toFunCall() != null && fun.toFunCall().getDefinition() == suc &&
            expr.getArguments().size() == 1) {
      return extractSuc(expr.getArguments().get(0), suc).incr();
    }
    return new SucExtrResult(0, expr);
  }

  public static Expression applyNumberOfSuc(Expression expr, FunctionDefinition suc, int num) {
    if (num <= 0) {
      return expr;
    }
    return FunCall(suc).addArgument(applyNumberOfSuc(expr, suc, num - 1), AppExpression.DEFAULT);
  }

  public static Expression applyNumberOfSuc(Expression expr, Constructor suc, int num) {
    if (num <= 0) {
      return expr;
    }
    return ConCall(suc).addArgument(applyNumberOfSuc(expr, suc, num - 1), AppExpression.DEFAULT);
  }

  public static boolean isPolyParam(Abstract.TypeArgument arg) {
    if (arg.getType() instanceof Abstract.DefCallExpression) {
      String typeName = ((Abstract.DefCallExpression) arg.getType()).getName();
      return typeName.equals(LVL.getName()) || typeName.equals(CNAT.getName());
    }
    return false;
  }

  public static DefCallExpression levelTypeByName(String typeName) {
    if (typeName.equals("Lvl")) {
      return Lvl();
    } else if (typeName.equals("CNat")) {
      return CNat();
    }
    return null;
  }

  static class DefinitionBuilder {
    static class Data {
      private final SimpleNamespace myParentNs;
      private final DataDefinition myDefinition;
      private final SimpleNamespace myNs = new SimpleNamespace();

      Data(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, Sort universe, DependentLink parameters, List<Binding> polyParams) {
        myParentNs = parentNs;
        myDefinition = new DataDefinition(name, precedence, universe, parameters);
        myDefinition.setPolyParams(polyParams);
        myParentNs.addDefinition(myDefinition);
      }

      Data(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, Sort universe, DependentLink parameters) {
        this(parentNs, name, precedence, universe, parameters, new ArrayList<Binding>());
      }

      DataDefinition definition() {
        return myDefinition;
      }

      Constructor addConstructor(String name, Abstract.Binding.Precedence precedence, Sort universe, DependentLink parameters) {
        Constructor constructor = new Constructor(name, precedence, universe, parameters, myDefinition);
        myDefinition.addConstructor(constructor);
        myNs.addDefinition(constructor);
        myParentNs.addDefinition(constructor);
        return constructor;
      }
    }

    static class Function {
      private final FunctionDefinition myDefinition;

      public Function(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree, List<Binding> polyParams) {
        myDefinition = new FunctionDefinition(name, precedence, EmptyNamespace.INSTANCE, parameters, resultType, elimTree, resultType.toUniverse() != null ? resultType.toUniverse().getSort() : Sort.PROP);
        myDefinition.setPolyParams(polyParams);
        parentNs.addDefinition(myDefinition);
      }

      public Function(SimpleNamespace parentNs, String name, Abstract.Binding.Precedence precedence, DependentLink parameters, Expression resultType, ElimTreeNode elimTree) {
        this(parentNs, name, precedence, parameters, resultType, elimTree, new ArrayList<Binding>());
      }

      FunctionDefinition definition() {
        return myDefinition;
      }
    }
  }
}
