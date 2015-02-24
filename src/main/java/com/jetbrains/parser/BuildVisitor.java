package main.java.com.jetbrains.parser;

import main.java.com.jetbrains.term.definition.Argument;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.definition.Signature;
import main.java.com.jetbrains.term.expr.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

import static main.java.com.jetbrains.term.expr.Expression.Apps;
import static main.java.com.jetbrains.parser.VcgrammarParser.*;

public class BuildVisitor extends VcgrammarBaseVisitor {
    private List<String> names = new ArrayList<String>();
    private Map<String, Definition> signature = new HashMap<String, Definition>();
    private List<String> unknownVariables = new ArrayList<String>();

    public Expression visitExpr(ExprContext expr) {
        return (Expression) visit(expr);
    }

    public Expression visitExpr(Expr1Context expr) {
        return (Expression) visit(expr);
    }

    @Override
    public List<Definition> visitDefs(DefsContext ctx) {
        List<Definition> defs = new ArrayList<Definition>();
        for (DefContext def : ctx.def()) {
            defs.add(visitDef(def));
        }
        return defs;
    }

    @Override
    public Definition visitDef(DefContext ctx) {
        String name = ctx.ID().getText();
        Expression type = visitExpr(ctx.expr1());
        Expression term = visitExpr(ctx.expr());
        Definition def = new FunctionDefinition(name, new Signature(new Argument[0], type), term);
        signature.put(name, def);
        return def;
    }

    @Override
    public NatExpression visitNat(NatContext ctx) {
        return new NatExpression();
    }

    @Override
    public ZeroExpression visitZero(ZeroContext ctx) {
        return new ZeroExpression();
    }

    @Override
    public SucExpression visitSuc(SucContext ctx) {
        return new SucExpression();
    }

    @Override
    public PiExpression visitArr(ArrContext ctx) {
        Expression left = visitExpr(ctx.expr1(0));
        Expression right = visitExpr(ctx.expr1(1));
        return new PiExpression(left, right);
    }

    @Override
    public Expression visitApp(AppContext ctx) {
        Expression left = visitExpr(ctx.expr1(0));
        Expression right = visitExpr(ctx.expr1(1));
        return Apps(left, right);
    }

    @Override
    public Expression visitParens(ParensContext ctx) {
        return visitExpr(ctx.expr());
    }

    @Override
    public NelimExpression visitNelim(NelimContext ctx) {
        return new NelimExpression();
    }

    @Override
    public Expression visitLam(LamContext ctx) {
        for (TerminalNode var : ctx.ID()) {
            names.add(var.getText());
        }
        Expression expr = visitExpr(ctx.expr());
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
    public Expression visitId(IdContext ctx) {
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
    public UniverseExpression visitUniverse(UniverseContext ctx) {
        return new UniverseExpression(Integer.valueOf(ctx.UNIVERSE().getText().substring("Type".length())));
    }

    @Override
    public Expression visitPi(PiContext ctx) {
        int telescopeSize = ctx.tele().size();
        Expression[] lefts = new Expression[telescopeSize];
        for (int i = 0; i < telescopeSize; ++i) {
            boolean explicit = ctx.tele(i) instanceof ExplicitContext;
            Expr1Context expr1 = explicit ? ((ExplicitContext) ctx.tele(i)).expr1() : ((ImplicitContext) ctx.tele(i)).expr1();
            List<TerminalNode> ids = explicit ? ((ExplicitContext) ctx.tele(i)).ID() : ((ImplicitContext) ctx.tele(i)).ID();
            lefts[i] = visitExpr(expr1);
            for (TerminalNode var : ids) {
                names.add(var.getText());
            }
        }
        Expression expr = visitExpr(ctx.expr1());
        for (int i = telescopeSize - 1; i >= 0; --i) {
            boolean explicit = ctx.tele(i) instanceof ExplicitContext;
            List<TerminalNode> ids = explicit ? ((ExplicitContext) ctx.tele(i)).ID() : ((ImplicitContext) ctx.tele(i)).ID();
            ListIterator<TerminalNode> it = ids.listIterator(ids.size());
            while (it.hasPrevious()) {
                expr = new PiExpression(explicit, it.previous().getText(), lefts[i], expr);
                names.remove(names.size() - 1);
            }
        }
        return expr;
    }

    public List<String> getUnknownVariables() {
        return unknownVariables;
    }
}
