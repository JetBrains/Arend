package main.java.com.jetbrains.parser;

import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class BuildVisitor extends VcgrammarBaseVisitor {
    private List<String> names = new ArrayList<String>();

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
        Expression type = (Expression) visit(ctx.expr(0));
        Expression term = (Expression) visit(ctx.expr(1));
        return new FunctionDefinition(name, type, term);
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
        return new VarExpression(ctx.ID().getText());
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
        for (int i = 0; i < telescopeSize; ++i) {
            ListIterator<TerminalNode> it = ctx.tele(i).ID().listIterator(ctx.tele(i).ID().size());
            while (it.hasPrevious()) {
                expr = new PiExpression(it.previous().getText(), lefts[telescopeSize - 1 - i], expr);
                names.remove(names.size() - 1);
            }
        }
        return expr;
    }
}
