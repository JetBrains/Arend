package main.java.com.jetbrains.parser;

import main.java.com.jetbrains.term.definition.Argument;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
    private List<String> names = new ArrayList<String>();
    private Map<String, Definition> signature = new HashMap<String, Definition>();
    private List<String> unknownVariables = new ArrayList<String>();

    @Override
    public Object visitDefs(VcgrammarParser.DefsContext ctx) {
        List<FunctionDefinition> defs = new ArrayList<FunctionDefinition>();
        for (VcgrammarParser.DefContext def : ctx.def()) {
            defs.add((FunctionDefinition) visit(def));
        }
        return defs;
    }

    @Override
    public Object visitDef(VcgrammarParser.DefContext ctx) {
        String name = ctx.ID().getText();
        TypeTopData typeTopData = (TypeTopData) visit(ctx.typeTop());
        Expression term = (Expression) visit(ctx.expr());
        Argument[] arguments = typeTopData.arguments.toArray(new Argument[typeTopData.arguments.size()]);
        Definition def = new FunctionDefinition(name, arguments, typeTopData.resultType, term);
        signature.put(name, def);
        return def;
    }

    @Override
    public Object visitNat(VcgrammarParser.NatContext ctx) {
        return new NatExpression();
    }

    @Override
    public Object visitZero(VcgrammarParser.ZeroContext ctx) {
        return new ZeroExpression();
    }

    @Override
    public Object visitSuc(VcgrammarParser.SucContext ctx) {
        return new SucExpression();
    }

    @Override
    public Object visitArr(VcgrammarParser.ArrContext ctx) {
        Expression left = (Expression) visit(ctx.expr1(0));
        Expression right = (Expression) visit(ctx.expr1(1));
        return new PiExpression(left, right);
    }

    @Override
    public Object visitApp(VcgrammarParser.AppContext ctx) {
        Expression left = (Expression) visit(ctx.expr1(0));
        Expression right = (Expression) visit(ctx.expr1(1));
        return new AppExpression(left, right);
    }

    @Override
    public Object visitParens(VcgrammarParser.ParensContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Object visitNelim(VcgrammarParser.NelimContext ctx) {
        return new NelimExpression();
    }

    @Override
    public Object visitLam(VcgrammarParser.LamContext ctx) {
        for (TerminalNode var : ctx.ID()) {
            names.add(var.getText());
        }
        Expression expr = (Expression) visit(ctx.expr());
        for (TerminalNode ignored : ctx.ID()) {
            names.remove(names.size() - 1);
        }
        ListIterator<TerminalNode> it = ctx.ID().listIterator(ctx.ID().size());
        while (it.hasPrevious()) {
            expr = new LamExpression(it.previous().getText(), expr);
        }
        return expr;
    }

    @Override
    public Object visitId(VcgrammarParser.IdContext ctx) {
        String name = ctx.ID().getText();
        int index = names.lastIndexOf(name);
        if (index == -1) {
            Definition def = signature.get(name);
            if (def == null) {
                unknownVariables.add(name);
                return new VarExpression(name);
            } else {
                return new DefCallExpression(def);
            }
        } else {
            return new IndexExpression(names.size() - 1 - index);
        }
    }

    @Override
    public Object visitUniverse(VcgrammarParser.UniverseContext ctx) {
        return new UniverseExpression(Integer.valueOf(ctx.UNIVERSE().getText().substring("Type".length())));
    }

    @Override
    public Object visitPi(VcgrammarParser.PiContext ctx) {
        int telescopeSize = ctx.tele().size();
        Expression[] lefts = new Expression[telescopeSize];
        for (int i = 0; i < telescopeSize; ++i) {
            lefts[i] = (Expression) visit(ctx.tele(i).expr1());
            for (TerminalNode var : ctx.tele(i).ID()) {
                names.add(var.getText());
            }
        }
        Expression expr = (Expression) visit(ctx.expr1());
        for (int i = telescopeSize - 1; i >= 0; --i) {
            ListIterator<TerminalNode> it = ctx.tele(i).ID().listIterator(ctx.tele(i).ID().size());
            while (it.hasPrevious()) {
                expr = new PiExpression(it.previous().getText(), lefts[i], expr);
                names.remove(names.size() - 1);
            }
        }
        return expr;
    }

    @Override
    public Object visitTypeTopPi(VcgrammarParser.TypeTopPiContext ctx) {
        List<Argument> arguments = new ArrayList<Argument>();
        int telescopeSize = ctx.typeTopTele().size();
        Expression[] argumentTypes = new Expression[telescopeSize];
        for (int i = 0; i < telescopeSize; ++i) {
            boolean explicit = ctx.typeTopTele(i) instanceof VcgrammarParser.TypeTopExplicitContext;
            VcgrammarParser.Expr1Context expr1 = explicit
                    ? ((VcgrammarParser.TypeTopExplicitContext)ctx.typeTopTele(i)).expr1()
                    : ((VcgrammarParser.TypeTopImplicitContext)ctx.typeTopTele(i)).expr1();
            List<TerminalNode> ids = explicit
                    ? ((VcgrammarParser.TypeTopExplicitContext)ctx.typeTopTele(i)).ID()
                    : ((VcgrammarParser.TypeTopImplicitContext)ctx.typeTopTele(i)).ID();
            argumentTypes[i] = (Expression) visit(expr1);
            for (TerminalNode var : ids) {
                arguments.add(new Argument(explicit, var.getText(), argumentTypes[i]));
                names.add(var.getText());
            }
        }
        TypeTopData typeTopData = (TypeTopData) visit(ctx.typeTop());
        for (Argument ignored : arguments) {
            names.remove(names.size() - 1);
        }
        arguments.addAll(typeTopData.arguments);
        typeTopData.arguments = arguments;
        return typeTopData;
    }

    @Override
    public Object visitTypeTopExpr1(VcgrammarParser.TypeTopExpr1Context ctx) {
        return new TypeTopData((Expression) visit(ctx.expr1()), new ArrayList<Argument>());
    }

    public List<String> getUnknownVariables() {
        return unknownVariables;
    }

    private static class TypeTopData {
        Expression resultType;
        List<Argument> arguments;

        TypeTopData(Expression resultType, List<Argument> arguments) {
            this.resultType = resultType;
            this.arguments = arguments;
        }
    }

}
