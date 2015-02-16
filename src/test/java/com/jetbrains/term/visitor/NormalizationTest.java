package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.expr.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NormalizationTest {
    private final Expression zero = new ZeroExpression();
    private final Expression nelim = new NelimExpression();
    // plus = \x. N-elim x (\y'. suc)
    private final Expression plus = new LamExpression("x", new AppExpression(new AppExpression(nelim, new IndexExpression(0)), new LamExpression("y'", new SucExpression())));
    // mul = \x. N-elim 0 (\y'. plus x)
    private final Expression mul = new LamExpression("x", new AppExpression(new AppExpression(nelim, zero), new LamExpression("y'", new AppExpression(plus, new IndexExpression(1)))));
    // fac = N-elim 1 (\x'. mul (suc x'))
    private final Expression fac = new AppExpression(new AppExpression(nelim, suc(zero)), new LamExpression("x'", new AppExpression(mul, suc(new IndexExpression(0)))));

    private Expression suc(Expression n) {
        return new AppExpression(new SucExpression(), n);
    }

    @Test
    public void normalizeLamId() {
        // normalize( (\x.x) (suc zero) ) = suc zero
        Expression expr1 = new LamExpression("x", new IndexExpression(0));
        Expression expr2 = new AppExpression(expr1, suc(zero));
        assertEquals(suc(zero), expr2.normalize());
    }

    @Test
    public void normalizeLamK() {
        // normalize( (\x y. x) (suc zero) ) = \z. suc zero
        Expression expr1 = new LamExpression("x", new LamExpression("y", new IndexExpression(1)));
        Expression expr2 = new AppExpression(expr1, suc(zero));
        assertEquals(new LamExpression("z", suc(zero)), expr2.normalize());
    }

    @Test
    public void normalizeLamKstar() {
        // normalize( (\x y. y) (suc zero) ) = \z. z
        Expression expr1 = new LamExpression("x", new LamExpression("y", new IndexExpression(0)));
        Expression expr2 = new AppExpression(expr1, suc(zero));
        assertEquals(new LamExpression("z", new IndexExpression(0)), expr2.normalize());
    }

    @Test
    public void normalizeLamKOpen() {
        // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
        Expression expr1 = new LamExpression("x", new LamExpression("y", new IndexExpression(1)));
        Expression expr2 = new AppExpression(expr1, suc(new IndexExpression(0)));
        assertEquals(new LamExpression("z", suc(new IndexExpression(1))), expr2.normalize());
    }

    @Test
    public void normalizeNelimZero() {
        // normalize( N-elim (suc zero) suc 0 ) = suc zero
        Expression expr = new AppExpression(new AppExpression(new AppExpression(nelim, suc(zero)), new SucExpression()), zero);
        assertEquals(suc(zero), expr.normalize());
    }

    @Test
    public void normalizeNelimOne() {
        // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
        Expression sucClause = new LamExpression("x", new LamExpression("y", new AppExpression(new IndexExpression(2), new IndexExpression(0))));
        Expression expr = new AppExpression(new AppExpression(new AppExpression(nelim, suc(zero)), sucClause), suc(zero));
        assertEquals(new AppExpression(new IndexExpression(0), suc(zero)), expr.normalize());
    }

    @Test
    public void normalizeNelimArg() {
        // normalize( N-elim (suc zero) (var(0) ((\x. x) zero) ) = suc zero
        Expression arg = new AppExpression(new LamExpression("x", new IndexExpression(0)), zero);
        Expression expr = new AppExpression(new AppExpression(new AppExpression(nelim, suc(zero)), new IndexExpression(0)), arg);
        assertEquals(suc(zero), expr.normalize());
    }

    @Test
    public void normalizePlus0a3() {
        // normalize (plus 0 3) = 3
        Expression expr = new AppExpression(new AppExpression(plus, zero), suc(suc(suc(zero))));
        assertEquals(suc(suc(suc(zero))), expr.normalize());
    }

    @Test
    public void normalizePlus3a0() {
        // normalize (plus 3 0) = 3
        Expression expr = new AppExpression(new AppExpression(plus, suc(suc(suc(zero)))), zero);
        assertEquals(suc(suc(suc(zero))), expr.normalize());
    }

    @Test
    public void normalizePlus3a3() {
        // normalize (plus 3 3) = 6
        Expression expr = new AppExpression(new AppExpression(plus, suc(suc(suc(zero)))), suc(suc(suc(zero))));
        assertEquals(suc(suc(suc(suc(suc(suc(zero)))))), expr.normalize());
    }

    @Test
    public void normalizeMul3a0() {
        // normalize (mul 3 0) = 0
        Expression expr = new AppExpression(new AppExpression(mul, suc(suc(suc(zero)))), zero);
        assertEquals(zero, expr.normalize());
    }

    @Test
    public void normalizeMul0a3() {
        // normalize (mul 0 3) = 0
        Expression expr = new AppExpression(new AppExpression(mul, zero), suc(suc(suc(zero))));
        assertEquals(zero, expr.normalize());
    }

    @Test
    public void normalizeMul3a3() {
        // normalize (mul 3 3) = 9
        Expression expr = new AppExpression(new AppExpression(mul, suc(suc(suc(zero)))), suc(suc(suc(zero))));
        assertEquals(suc(suc(suc(suc(suc(suc(suc(suc(suc(zero))))))))), expr.normalize());
    }

    @Test
    public void normalizeFac3() {
        // normalize (fac 3) = 6
        Expression expr = new AppExpression(fac, suc(suc(suc(zero))));
        assertEquals(suc(suc(suc(suc(suc(suc(zero)))))), expr.normalize());
    }
}
