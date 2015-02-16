package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.expr.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubstTest {
    private final Expression suc = new SucExpression();
    private final Expression zero = new ZeroExpression();
    private final Expression sucZero = new AppExpression(suc, zero);

    @Test
    public void substConst() {
        // zero -> null [0 := null] = zero -> null
        Expression expr1 = new ZeroExpression();
        Expression expr2 = new DefCallExpression(null);
        Expression expr3 = new PiExpression(expr1, expr2);
        assertEquals(expr3, expr3.subst(null, 0));
    }

    @Test
    public void substIndexLess() {
        // var(2) [3 := null] = var(2)
        Expression expr = new IndexExpression(2);
        assertEquals(expr, expr.subst(null, 3));
    }

    @Test
    public void substIndexEquals() {
        // var(2) [2 := suc zero] = suc zero
        Expression expr = new IndexExpression(2);
        assertEquals(sucZero, expr.subst(sucZero, 2));
    }

    @Test
    public void substIndexGreater() {
        // var(4) [1 := null] = var(3)
        Expression expr = new IndexExpression(4);
        assertEquals(new IndexExpression(3), expr.subst(null, 1));
    }

    @Test
    public void substLam1() {
        // \x.x [0 := suc zero] = \x.x
        Expression expr = new LamExpression("x", new IndexExpression(0));
        assertEquals(expr, expr.subst(sucZero, 0));
    }

    @Test
    public void substLam2() {
        // \x y. y x [0 := suc zero] = \x y. y x
        Expression expr = new LamExpression("x", new LamExpression("y", new AppExpression(new IndexExpression(0), new IndexExpression(1))));
        assertEquals(expr, expr.subst(sucZero, 0));
    }

    @Test
    public void substLamConst() {
        // \x. var(1) [1 := suc zero] = \x. suc zero
        Expression expr = new LamExpression("x", new IndexExpression(2));
        assertEquals(new LamExpression("y", sucZero), expr.subst(sucZero, 1));
    }

    @Test
    public void substLamInLam() {
        // \x. var(1) [1 := \y.y] = \z y. y
        Expression expr = new LamExpression("x", new IndexExpression(2));
        Expression substExpr = new LamExpression("y", new IndexExpression(0));
        assertEquals(new LamExpression("z", substExpr), expr.subst(substExpr, 1));
    }

    @Test
    public void substLamInLamOpenConst() {
        // \x. var(1) [0 := \y. var(0)] = \z. var(0)
        Expression expr = new LamExpression("x", new IndexExpression(2));
        Expression substExpr = new LamExpression("y", new IndexExpression(1));
        assertEquals(new LamExpression("z", new IndexExpression(1)), expr.subst(substExpr, 0));
    }

    @Test
    public void substLamInLamOpen() {
        // \x. var(1) [1 := \y. var(0)] = \z t. var(0)
        Expression expr = new LamExpression("x", new IndexExpression(2));
        Expression substExpr = new LamExpression("y", new IndexExpression(1));
        assertEquals(new LamExpression("z", new LamExpression("t", new IndexExpression(2))), expr.subst(substExpr, 1));
    }

    @Test
    public void substComplex() {
        // \x y. x (var(1)) (\z. var(0) z y) [0 := \w t. t (var(0)) (w (var(1)))] = \x y. x (var(0)) (\z. (\w t. t (var(0)) (w (var(1)))) z y)
        Expression expr = new LamExpression("x", new LamExpression("y", new AppExpression(new AppExpression(new IndexExpression(1), new IndexExpression(3)), new LamExpression("z", new AppExpression(new AppExpression(new IndexExpression(3), new IndexExpression(0)), new IndexExpression(1))))));
        Expression substExpr = new LamExpression("w", new LamExpression("t", new AppExpression(new AppExpression(new IndexExpression(0), new IndexExpression(2)), new AppExpression(new IndexExpression(1), new IndexExpression(3)))));
        Expression result = new LamExpression("x", new LamExpression("y", new AppExpression(new AppExpression(new IndexExpression(1), new IndexExpression(2)), new LamExpression("z", new AppExpression(new AppExpression(new LamExpression("w", new LamExpression("t", new AppExpression(new AppExpression(new IndexExpression(0), new IndexExpression(5)), new AppExpression(new IndexExpression(1),new IndexExpression(6))))), new IndexExpression(0)), new IndexExpression(1))))));
        assertEquals(result, expr.subst(substExpr, 0));
    }

    // TODO: Add tests for pi
}
