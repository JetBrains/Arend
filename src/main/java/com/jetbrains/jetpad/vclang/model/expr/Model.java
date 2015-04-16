package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.StringWrapper;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import jetbrains.jetpad.otmodel.node.NodeChildId;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.node.NodePropertyId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import java.util.ArrayList;
import java.util.List;

public class Model {
  public static abstract class Expression extends Node implements Abstract.Expression {
    private Property<com.jetbrains.jetpad.vclang.term.expr.Expression> myWellTypedExpr = new ValueProperty<>();

    public Property<com.jetbrains.jetpad.vclang.term.expr.Expression> wellTypedExpr() {
      return myWellTypedExpr;
    }

    protected Expression(WrapperContext ctx, NodeConceptId conceptId) {
      super(ctx, conceptId);
    }

    protected Expression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped) {
      myWellTypedExpr.set(wellTyped);
    }
  }

  public static class AppExpression extends Expression implements Abstract.AppExpression {
    private final Property<Expression> myFunction = getChild(new NodeChildId("GZZBmZkUz8e.D4E8iS5joHR", "function", true));
    private final Property<Expression> myArgument = getChild(new NodeChildId("Cz7YKcMqAL3.FnOkJuIjK6V", "argument", true));

    public AppExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("CUY_jPbn8rI.DBU3rM03coK", "AppExpression"));
    }

    protected AppExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public Expression getFunction() {
      return myFunction.get();
    }

    @Override
    public Expression getArgument() {
      return myArgument.get();
    }

    @Override
    public boolean isExplicit() {
      if (getArgument() instanceof Argument) {
        return ((Argument) getArgument()).getExplicit();
      } else {
        return true;
      }
    }

    public Property<Expression> function() {
      return myFunction;
    }

    public Property<Expression> argument() {
      return myArgument;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[] { getFunction(), getArgument() };
    }
  }

  public abstract static class Argument extends Expression implements Abstract.Argument {
    private final Property<Boolean> myExplicit = getBoolProperty(new NodePropertyId("G2KgzozI4gL.B5i5OgzWhfR", "explicit"));

    protected Argument(WrapperContext ctx, NodeConceptId conceptId) {
      super(ctx, conceptId);
    }

    protected Argument(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public boolean getExplicit() {
      return myExplicit.get();
    }

    public Property<Boolean> isExplicit() {
      return myExplicit;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      throw new IllegalStateException();
    }
  }

  public static class NameArgument extends Argument implements Abstract.NameArgument {
    private final Property<String> myName = getStringProperty(new NodePropertyId("FkDIGwn6Nb2.Bhjvoc58R9H", "name"));

    public NameArgument(WrapperContext ctx) {
      super(ctx, new NodeConceptId("P52DA3mYba.EwApAS3WxTb", "NameArgument"));
    }

    protected NameArgument(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public String getName() {
      return myName.get();
    }

    public Property<String> name() {
      return myName;
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class TypeArgument extends Argument implements Abstract.TypeArgument {
    public final Property<Expression> myType = getChild(new NodeChildId("H50tCcrjeDL.EMNrA5oDPW6", "type", true));

    public TypeArgument(WrapperContext ctx) {
      this(ctx, new NodeConceptId("BM7LkeWh8wC.EY2-O1lWjNo", "TypeArgument"));
    }

    protected TypeArgument(WrapperContext ctx, NodeConceptId conceptId) {
      super(ctx, conceptId);
    }

    protected TypeArgument(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public Expression getType() {
      return myType.get();
    }

    public Property<Expression> type() {
      return myType;
    }

    @Override
    public Node[] children() {
      return new Node[] { getType() };
    }
  }

  public static class TelescopeArgument extends TypeArgument implements Abstract.TelescopeArgument {
    private final ObservableList<StringWrapper> myNames = getChildren(new NodeChildId("GajZYwv6nwb.Haum7_4VU6J", "names"));

    public TelescopeArgument(WrapperContext ctx) {
      super(ctx, new NodeConceptId("BPBdHALlOw8.FFk5x9lZOAe", "TelescopeArgument"));
    }

    protected TelescopeArgument(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public List<String> getNames() {
      final List<String> result = new ArrayList<>();
      for (StringWrapper wrapper : myNames) {
        result.add(wrapper.string.get());
      }
      return result;
    }

    @Override
    public String getName(int index) {
      return myNames.get(index).string.get();
    }

    public ObservableList<StringWrapper> names() {
      return myNames;
    }
  }

  public static class LamExpression extends Expression implements Abstract.LamExpression {
    private final ObservableList<Argument> myArguments = getChildren(new NodeChildId("FiCZjAUPr4P.GXjdf12PNzI", "arguments", false));
    private final Property<Expression> myBody = getChild(new NodeChildId("F-lxvtNWTaF.FpXVcOOpiNC", "body", true));

    public LamExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("CCgUHe-zV1H.EKXMFikmL4Q", "LamExpression"));
    }

    protected LamExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public ObservableList<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public Argument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getBody() {
      return myBody.get();
    }

    public Property<Expression> body() {
      return myBody;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }

    @Override
    public Node[] children() {
      Node[] result = myArguments.toArray(new Node[myArguments.size() + 1]);
      result[myArguments.size()] = getBody();
      return result;
    }
  }

  public static class NatExpression extends Expression implements Abstract.NatExpression {
    public NatExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("EiWDzyKDzzv.xRZdvTyhUT", "NatExpression"));
    }

    protected NatExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNat(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class NelimExpression extends Expression implements Abstract.NelimExpression {
    public NelimExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("D_7doRSQ3am.B05__p1TvLd", "NelimExpression"));
    }

    protected NelimExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNelim(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class ParensExpression extends Expression {
    private final Property<Expression> myExpression = getChild(new NodeChildId("C_5rL-oIaXI.FxdGj8HgJs", "expression", true));

    protected ParensExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("mFTFmOrIN6.DSua0urwj4R", "ParensExpression"));
    }

    protected ParensExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    public Expression getExpression() {
      return myExpression.get();
    }

    public Property<Expression> expression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return myExpression.get().accept(visitor, params);
    }

    public static Expression parens(boolean p, Expression expr) {
      if (p) {
        ParensExpression pexpr = new ParensExpression(expr.getContext());
        pexpr.myExpression.set(expr);
        return pexpr;
      } else {
        return expr;
      }
    }

    @Override
    public Node[] children() {
      return new Node[] { getExpression() };
    }
  }

  public static class PiExpression extends Expression implements Abstract.PiExpression {
    private final ObservableList<TypeArgument> myArguments = getChildren(new NodeChildId("oU_ZS_ZoBb.Gj3TRU0LX7W", "arguments", false));
    private final Property<Expression> myCodomain = getChild(new NodeChildId("E4eCE_Ek2Dt.IONT_veAoV", "codomain", true));

    public PiExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("GwY8g9EPQwx.H81Owe6_I9X", "PiExpression"));
    }

    protected PiExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public ObservableList<TypeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public TypeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getCodomain() {
      return myCodomain.get();
    }

    public Property<Expression> codomain() {
      return myCodomain;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }

    @Override
    public Node[] children() {
      Node[] result = myArguments.toArray(new Node[myArguments.size() + 1]);
      result[myArguments.size()] = getCodomain();
      return result;
    }
  }

  public static class SucExpression extends Expression implements Abstract.SucExpression {
    public SucExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("BQIZUqw-jjo.GOZ-EUxxpmd", "SucExpression"));
    }

    protected SucExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSuc(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class UniverseExpression extends Expression implements Abstract.UniverseExpression {
    private final Property<Integer> myLevel = getIntProperty(new NodePropertyId("25cYCLnpUH.CQp9lSZ-mzA", "level"));
    // TODO: Replace this with property.
    public Universe universe = new Universe.Type();

    public UniverseExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("FVfU-3nByuh.EMAAAX4zYsZ", "UniverseExpression"));
    }

    protected UniverseExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    public Property<Integer> level() {
      return myLevel;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitUniverse(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }

    @Override
    public Universe getUniverse() {
      return universe;
    }
  }

  public static class VarExpression extends Expression implements Abstract.VarExpression {
    private final Property<String> myName = getStringProperty(new NodePropertyId("HxQdr3tjjMS.H24VxmVL5AA", "name"));

    public VarExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("9K_VmuKDxa.HXRIZnCFyRH", "VarExpression"));
    }

    protected VarExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public String getName() {
      return myName.get();
    }

    public Property<String> name() {
      return myName;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitVar(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }

  public static class ZeroExpression extends Expression implements Abstract.ZeroExpression {
    public ZeroExpression(WrapperContext ctx) {
      super(ctx, new NodeConceptId("q5Qwk1IJ9b.V1qS6aHY96", "ZeroExpression"));
    }

    protected ZeroExpression(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
      super(ctx, node);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitZero(this, params);
    }

    @Override
    public Node[] children() {
      return new Node[0];
    }
  }
}
