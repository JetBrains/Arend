package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.expr.*;
import org.junit.Test;

import static main.java.com.jetbrains.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

public class NormalizationTest {
    // plus = \x. N-elim x (\y'. suc)
    private final Expression plus = Lam("x", Apps(Nelim(), Index(0), Lam("y'", Suc())));
    // mul = \x. N-elim 0 (\y'. plus x)
    private final Expression mul = Lam("x", Apps(Nelim(), Zero(), Lam("y'", Apps(plus, Index(1)))));
    // fac = N-elim 1 (\x'. mul (suc x'))
    private final Expression fac = Apps(Nelim(), Suc(Zero()), Lam("x'", Apps(mul, Suc(Index(0)))));

    @Test
    public void normalizeLamId() {
        // normalize( (\x.x) (suc zero) ) = suc zero
        Expression expr = Apps(Lam("x", Index(0)), Suc(Zero()));
        assertEquals(Suc(Zero()), expr.normalize());
    }

    @Test
    public void normalizeLamK() {
        // normalize( (\x y. x) (suc zero) ) = \z. suc zero
        Expression expr = Apps(Lam("x", Lam("y", Index(1))), Suc(Zero()));
        assertEquals(Lam("z", Suc(Zero())), expr.normalize());
    }

    @Test
    public void normalizeLamKstar() {
        // normalize( (\x y. y) (suc zero) ) = \z. z
        Expression expr = Apps(Lam("x", Lam("y", Index(0))), Suc(Zero()));
        assertEquals(Lam("z", Index(0)), expr.normalize());
    }

    @Test
    public void normalizeLamKOpen() {
        // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
        Expression expr = Apps(Lam("x", Lam("y", Index(1))), Suc(Index(0)));
        assertEquals(Lam("z", Suc(Index(1))), expr.normalize());
    }

    @Test
    public void normalizeNelimZero() {
        // normalize( N-elim (suc zero) suc 0 ) = suc zero
        Expression expr = Apps(Nelim(), Suc(Zero()), Suc(), Zero());
        assertEquals(Suc(Zero()), expr.normalize());
    }

    @Test
    public void normalizeNelimOne() {
        // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
        Expression expr = Apps(Nelim(), Suc(Zero()), Lam("x", Lam("y", Apps(Index(2), Index(0)))), Suc(Zero()));
        assertEquals(Apps(Index(0), Suc(Zero())), expr.normalize());
    }

    @Test
    public void normalizeNelimArg() {
        // normalize( N-elim (suc zero) (var(0) ((\x. x) zero) ) = suc zero
        Expression arg = Apps(Lam("x", Index(0)), Zero());
        Expression expr = Apps(Nelim(), Suc(Zero()), Index(0), arg);
        assertEquals(Suc(Zero()), expr.normalize());
    }

    @Test
    public void normalizePlus0a3() {
        // normalize (plus 0 3) = 3
        Expression expr = Apps(plus, Zero(), Suc(Suc(Suc(Zero()))));
        assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize());
    }

    @Test
    public void normalizePlus3a0() {
        // normalize (plus 3 0) = 3
        Expression expr = Apps(plus, Suc(Suc(Suc(Zero()))), Zero());
        assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize());
    }

    @Test
    public void normalizePlus3a3() {
        // normalize (plus 3 3) = 6
        Expression expr = Apps(plus, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
        assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize());
    }

    @Test
    public void normalizeMul3a0() {
        // normalize (mul 3 0) = 0
        Expression expr = Apps(mul, Suc(Suc(Suc(Zero()))), Zero());
        assertEquals(Zero(), expr.normalize());
    }

    @Test
    public void normalizeMul0a3() {
        // normalize (mul 0 3) = 0
        Expression expr = Apps(mul, Zero(), Suc(Suc(Suc(Zero()))));
        assertEquals(Zero(), expr.normalize());
    }

    @Test
    public void normalizeMul3a3() {
        // normalize (mul 3 3) = 9
        Expression expr = Apps(mul, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
        assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize());
    }

    @Test
    public void normalizeFac3() {
        // normalize (fac 3) = 6
        Expression expr = Apps(fac, Suc(Suc(Suc(Zero()))));
        assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize());
    }
}
