package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends Namespace {
  public static ClassDefinition PRELUDE_CLASS;

  public static Namespace PRELUDE = new Prelude();

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;

  public static FunctionDefinition ISO;

  //public static DataDefinition PROP_TRUNC;
  //public static DataDefinition SET_TRUNC;

  private static char[] specInfix = {'@', '='};
  private static String[] specPrefix = {"iso", "path", "Path"};

  private static Map<Abstract.Definition, Integer> defToLevel = new HashMap<>();
  private static Map<Integer, LevelPoly> levels = new HashMap<>();

  public static class LevelPoly {
    public final Definition path;
    public final Definition pathCon;
    public final Definition pathInfix;
    public final Definition at;
    public final Definition coe;
    public final Definition iso;
    public final Definition truncP;
    public final Definition truncS;

    private LevelPoly(Definition path, Definition pathCon, Definition pathInfix, Definition at, Definition coe, Definition iso, Definition truncP, Definition truncS) {
      this.path = path;
      this.pathCon = pathCon;
      this.pathInfix = pathInfix;
      this.at = at;
      this.coe = coe;
      this.iso = iso;
      this.truncP = truncP;
      this.truncS = truncS;
    }
  }

  static {
    PRELUDE_CLASS = new ClassDefinition(RootModule.ROOT, new Name("Prelude"));
    RootModule.ROOT.addDefinition(PRELUDE_CLASS);

    NAT = new DataDefinition(PRELUDE, new Name("Nat"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>());
    Namespace natNamespace = PRELUDE.getChild(NAT.getName());
    ZERO = new Constructor(natNamespace, new Name("zero"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), NAT);
    SUC = new Constructor(natNamespace, new Name("suc"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), typeArgs(TypeArg(DataCall(NAT))), NAT);
    NAT.addConstructor(ZERO);
    NAT.addConstructor(SUC);

    PRELUDE.addDefinition(NAT);
    PRELUDE.addDefinition(ZERO);
    PRELUDE.addDefinition(SUC);

    INTERVAL = new DataDefinition(PRELUDE, new Name("I"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    Namespace intervalNamespace = PRELUDE.getChild(INTERVAL.getName());
    LEFT = new Constructor(intervalNamespace, new Name("left"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    RIGHT = new Constructor(intervalNamespace, new Name("right"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    Constructor abstractConstructor = new Constructor(intervalNamespace, new Name("<abstract>"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    INTERVAL.addConstructor(LEFT);
    INTERVAL.addConstructor(RIGHT);
    INTERVAL.addConstructor(abstractConstructor);

    PRELUDE.addDefinition(INTERVAL);
    PRELUDE.addDefinition(LEFT);
    PRELUDE.addDefinition(RIGHT);


    generateLevel(0);
    PATH = (DataDefinition) levels.get(0).path;
    PATH_CON = (Constructor) levels.get(0).pathCon;
    PATH_INFIX = (FunctionDefinition) levels.get(0).pathInfix;
    AT = (FunctionDefinition) levels.get(0).at;
    ISO = (FunctionDefinition) levels.get(0).iso;
    COERCE = (FunctionDefinition) levels.get(0).coe;

    List<Argument> isoP_Arguments = new ArrayList<>(6);
    isoP_Arguments.add(Tele(false, vars("A", "B"), Universe(0, Universe.Type.PROP)));
    isoP_Arguments.add(Tele(vars("f"), Pi(Index(1), Index(0))));
    isoP_Arguments.add(Tele(vars("g"), Pi(Index(1), Index(2))));
    isoP_Arguments.add(Tele(vars("i"), DataCall(INTERVAL)));
    Expression isoResultType = Universe(0, Universe.Type.PROP);
    BranchElimTreeNode isoElimTree = branch(0,
            clause(LEFT, Index(3)),
            clause(RIGHT, Index(2))
    );
    FunctionDefinition isoP = new FunctionDefinition(PRELUDE, new Name("isoP"), Abstract.Definition.DEFAULT_PRECEDENCE, isoP_Arguments, isoResultType, isoElimTree);
    PRELUDE.addDefinition(isoP);
 }

  private Prelude() {
    super("Prelude");
  }

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }

  @Override
  public NamespaceMember getMember(String name) {
    NamespaceMember result = super.getMember(name);
    if (result != null)
      return result;
    for (char sc : specInfix) {
      if (name.charAt(0) == sc) {
        for (char c : name.toCharArray()) {
          if (sc != c)
            return null;
        }
        generateLevel(name.length() - 1);
        return getMember(name);
      }
    }
    for (String sname : specPrefix) {
      if (name.startsWith(sname)) {
        if (name.length() == sname.length()) {
          generateLevel(0);
          return getMember(name);
        }
        String suffix = name.substring(sname.length());
        try {
          Integer level = Integer.parseInt(suffix);
          if (level > 0) {
            generateLevel(level);
            return getMember(name);
          }
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return null;
  }

  private static void generateLevel(int i) {
    String suffix = i == 0 ? "" : Integer.toString(i);
    List<TypeArgument> PathParameters = new ArrayList<>(3);
    PathParameters.add(Tele(vars("A"), Pi(DataCall(INTERVAL), Universe(i, Universe.Type.NOT_TRUNCATED))));
    PathParameters.add(TypeArg(Apps(Index(0), ConCall(LEFT))));
    PathParameters.add(TypeArg(Apps(Index(1), ConCall(RIGHT))));
    DataDefinition path = new DataDefinition(PRELUDE, new Name("Path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), PathParameters);
    PRELUDE.addDefinition(path);
    defToLevel.put(path, i);
    List<TypeArgument> pathArguments = new ArrayList<>(1);
    pathArguments.add(TypeArg(Pi("i", DataCall(INTERVAL), Apps(Index(3), Index(0)))));
    Constructor pathCon = new Constructor(PRELUDE.getChild(path.getName()), new Name("path" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), pathArguments, path);
    path.addConstructor(pathCon);
    PRELUDE.addDefinition(pathCon);
    defToLevel.put(pathCon, i);

    char[] chars = new char[i + 1];

    List<Argument> pathInfixArguments = new ArrayList<>(3);
    pathInfixArguments.add(Tele(false, vars("A"), Universe(i, Universe.Type.NOT_TRUNCATED)));
    pathInfixArguments.add(Tele(vars("a", "a'"), Index(0)));
    Expression pathInfixTerm = Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Lam(teleArgs(Tele(vars("_"), DataCall(INTERVAL))), Index(3)), Index(1), Index(0));
    Arrays.fill(chars, '=');
    FunctionDefinition pathInfix = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixArguments, Universe(i), leaf(pathInfixTerm));
    PRELUDE.addDefinition(pathInfix);
    defToLevel.put(pathInfix, i);

    List<Argument> atArguments = new ArrayList<>(5);
    atArguments.add(Tele(false, vars("A"), PathParameters.get(0).getType()));
    atArguments.add(Tele(false, vars("a"), PathParameters.get(1).getType()));
    atArguments.add(Tele(false, vars("a'"), PathParameters.get(2).getType()));
    atArguments.add(Tele(vars("p"), Apps(DataCall((DataDefinition) PRELUDE.getDefinition("Path" + suffix)), Index(2), Index(1), Index(0))));
    atArguments.add(Tele(vars("i"), DataCall(INTERVAL)));
    Expression atResultType = Apps(Index(4), Index(0));
    BranchElimTreeNode atElimTree = branch(0,
      clause(LEFT, Index(2)),
      clause(RIGHT, Index(1)),
      clause(null, branch(1, clause((Constructor) PRELUDE.getDefinition("path" + suffix), Apps(Index(1), Index(0)))))
    );
    Arrays.fill(chars, '@');
    FunctionDefinition at = new FunctionDefinition(PRELUDE, new Name(new String(chars), Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atArguments, atResultType, atElimTree);
    PRELUDE.addDefinition(at);
    defToLevel.put(at, i);

    List<Argument> isoArguments = new ArrayList<>(6);
    isoArguments.add(Tele(false, vars("A", "B"), Universe(i, Universe.Type.NOT_TRUNCATED)));
    isoArguments.add(Tele(vars("f"), Pi(Index(1), Index(0))));
    isoArguments.add(Tele(vars("g"), Pi(Index(1), Index(2))));
    isoArguments.add(Tele(vars("linv"), Pi(typeArgs(Tele(vars("a"), Index(3))), Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Index(4), false, true)), Apps(Index(1), Apps(Index(2), Index(0))), Index(0)))));
    isoArguments.add(Tele(vars("rinv"), Pi(typeArgs(Tele(vars("b"), Index(3))), Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Index(4), false, true)), Apps(Index(3), Apps(Index(2), Index(0))), Index(0)))));
    isoArguments.add(Tele(vars("i"), DataCall(INTERVAL)));
    Expression isoResultType = Universe(i, Universe.Type.NOT_TRUNCATED);
    BranchElimTreeNode isoElimTree = branch(0,
      clause(LEFT, Index(5)),
      clause(RIGHT, Index(4))
    );
    FunctionDefinition iso = new FunctionDefinition(PRELUDE, new Name("iso" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, isoArguments, isoResultType, isoElimTree);
    PRELUDE.addDefinition(iso);
    defToLevel.put(iso, i);

    List<Argument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DataCall(INTERVAL), Universe(i, Universe.Type.NOT_TRUNCATED))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), ConCall(LEFT))));
    coerceArguments.add(Tele(vars("point"), DataCall(INTERVAL)));
    BranchElimTreeNode coerceElimTreeNode = branch(0, clause(LEFT, Abstract.Definition.Arrow.RIGHT, Index(0)));
    FunctionDefinition coerce = new FunctionDefinition(PRELUDE, new Name("coe" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, coerceArguments, Apps(Index(2), Index(0)), coerceElimTreeNode);

    PRELUDE.addDefinition(coerce);
    defToLevel.put(coerce, i);


    List<TypeArgument> truncArguments = new ArrayList<>(1);
    truncArguments.add(Tele(vars("A"), Universe(i, Universe.Type.NOT_TRUNCATED)));
    DataDefinition propTrunc = new DataDefinition(PRELUDE, new Name("TrP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), truncArguments);
    PRELUDE.addDefinition(propTrunc);
    defToLevel.put(propTrunc, i);

    Constructor propTruncInCon = new Constructor(PRELUDE.getChild(propTrunc.getName()), new Name("inP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), typeArgs(TypeArg(Index(0))), propTrunc);
    List<TypeArgument> propTruncConArguments = new ArrayList<>(3);
    propTruncConArguments.add(Tele(vars("a"), Apps(DataCall(propTrunc), Index(0))));
    propTruncConArguments.add(Tele(vars("a'"), Apps(DataCall(propTrunc), Index(1))));
    propTruncConArguments.add(Tele(vars("i"), DataCall(INTERVAL)));
    Constructor propTruncPathCon = new Constructor(PRELUDE.getChild(propTrunc.getName()), new Name("truncP" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), propTruncConArguments, propTrunc);
    propTrunc.addConstructor(propTruncInCon);
    propTrunc.addConstructor(propTruncPathCon);
    Condition propTruncPathCond = new Condition(propTruncPathCon, branch(0,
            clause(LEFT, Index(1)),
            clause(RIGHT, Index(0))
    ));
    propTrunc.addCondition(propTruncPathCond);

    PRELUDE.addDefinition(propTruncInCon);
    PRELUDE.addDefinition(propTruncPathCon);

    defToLevel.put(propTruncInCon, i);
    defToLevel.put(propTruncPathCon, i);

     DataDefinition setTrunc = new DataDefinition(PRELUDE, new Name("TrS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), truncArguments);
    PRELUDE.addDefinition(setTrunc);
    defToLevel.put(setTrunc, i);

    Constructor setTruncInCon = new Constructor(PRELUDE.getChild(setTrunc.getName()), new Name("inS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(i, Universe.Type.NOT_TRUNCATED), typeArgs(TypeArg(Index(0))), setTrunc);
    List<TypeArgument> setTruncConArguments = new ArrayList<>(6);
    setTruncConArguments.add(Tele(vars("a"), Apps(DataCall(setTrunc), Index(0))));
    setTruncConArguments.add(Tele(vars("a'"), Apps(DataCall(setTrunc), Index(1))));
    setTruncConArguments.add(Tele(vars("p"), Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Apps(DataCall(setTrunc), Index(2)), false, true)), Index(1), Index(0))));
    setTruncConArguments.add(Tele(vars("q"), Apps(Apps(FunCall(pathInfix), new ArgumentExpression(Apps(DataCall(setTrunc), Index(3)), false, true)), Index(2), Index(1))));
    setTruncConArguments.add(Tele(vars("i"), DataCall(INTERVAL)));
    setTruncConArguments.add(Tele(vars("j"), DataCall(INTERVAL)));
    Constructor setTruncPathCon = new Constructor(PRELUDE.getChild(setTrunc.getName()), new Name("truncS" + suffix), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), setTruncConArguments, setTrunc);
    setTrunc.addConstructor(setTruncInCon);
    setTrunc.addConstructor(setTruncPathCon);
    Condition setTruncPathCond = new Condition(setTruncPathCon, branch(0,
            clause(LEFT, Apps(FunCall(at), Index(2), Index(0))),
            clause(RIGHT, Apps(FunCall(at), Index(1), Index(0))),
            clause(null, branch(1,
                    clause(LEFT, Index(4)),
                    clause(RIGHT, Index(3))))
    ));

    setTrunc.addCondition(setTruncPathCond);

    PRELUDE.addDefinition(setTruncInCon);
    PRELUDE.addDefinition(setTruncPathCon);

    defToLevel.put(setTruncInCon, i);
    defToLevel.put(setTruncPathCon, i); /**/

    levels.put(i, new LevelPoly(path, pathCon, pathInfix, at, coerce, iso, propTruncPathCon, setTruncPathCon));
  }

  public static boolean isAt(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).at == definition;
  }

  public static boolean isPathCon(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).pathCon == definition;
  }

  public static boolean isPath(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).path == definition;
  }

  public static boolean isPathInfix(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).pathInfix == definition;
  }

  public static boolean isIso(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).iso == definition;
  }

  public static boolean isCoe(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).coe == definition;
  }

  public static boolean isTruncP(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).truncP == definition;
  }

  public static boolean isTruncS(Abstract.Definition definition) {
    return defToLevel.containsKey(definition) && levels.get(defToLevel.get(definition)).truncS == definition;
  }

  public static int getLevel(Abstract.Definition definition) {
    if (defToLevel.containsKey(definition)) {
      return defToLevel.get(definition);
    }

    throw new IllegalStateException();
  }

  public static LevelPoly getLevelDefs(int level) {
    return levels.get(level);
  }
}
