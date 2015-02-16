package main.java.com.jetbrains.parser;

import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.*;

import java.util.ArrayList;
import java.util.List;

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
        String var = ctx.ID().getText();
        names.add(var);
        Expression expr = (Expression) visit(ctx.expr());
        names.remove(names.size() - 1);
        return new LamExpression(var, expr);
    }

    @Override
    public Object visitId(VcgrammarParser.IdContext ctx) {
        return new VarExpression(ctx.ID().getText());
    }
}
