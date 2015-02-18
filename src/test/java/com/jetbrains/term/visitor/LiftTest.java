package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.expr.*;
import org.junit.Test;

import static main.java.com.jetbrains.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

public class LiftTest {
    @Test
    public void liftConst() {
        // lift( null -> zero , 0, 1) = null -> zero
        Expression expr = Pi(Zero(), DefCall(null));
        assertEquals(expr, expr.liftIndex(0, 1));
    }

    @Test
    public void liftIndexLess() {
        // lift( var(2) , 4, 3) = var(2)
        Expression expr = Index(2);
        assertEquals(expr, expr.liftIndex(4, 3));
    }

    @Test
    public void liftIndexGreater() throws Exception {
        // lift( var(2) , 1, 3) = var(5)
        Expression expr = Index(2);
        assertEquals(Index(5), expr.liftIndex(1, 3));
    }

    @Test
    public void liftLambdaClosed() throws Exception {
        // lift( \x.x , 0, 1) = \x.x
        Expression expr = Lam("x", Index(0));
        assertEquals(expr, expr.liftIndex(0, 1));
    }

    @Test
    public void liftLambdaOpen() throws Exception {
        // lift( \x.var(1) , 1, 2) = \x.var(3)
        Expression expr = Lam("x", Index(2));
        assertEquals(Lam("x", Index(4)), expr.liftIndex(1, 2));
    }

    @Test
    public void liftLambda2() throws Exception {
        // lift( (\x. x) (\y. var(0)) , 0, 2) = (\x. x) (\z. var(2))
        Expression expr1 = Lam("x", Index(0));
        Expression expr2 = Lam("y", Index(1));
        Expression expr3 = Apps(expr1, expr2);
        assertEquals(Apps(expr1, Lam("z", Index(3))), expr3.liftIndex(0, 2));
    }

    @Test
    public void liftComplex() throws Exception {
        // lift( (\x y. x y x) (\x y. x (var(1)) (var(0)) (\z. z (var(0)) (var(2)))) ), 1, 2) = (\x y. x y x) (\x y. x (var(3)) (var(0)) (\z. z (var(0)) (var(4))))
        Expression expr1 = Lam("x", Lam("y", Apps(Index(1), Index(0), Index(1))));
        Expression expr2 = Lam("x", Lam("y", Apps(Index(1), Index(3), Index(2), Lam("z", Apps(Index(0), Index(3), Index(5))))));
        Expression expr3 = Lam("x", Lam("y", Apps(Index(1), Index(5), Index(2), Lam("z", Apps(Index(0), Index(3), Index(7))))));
        assertEquals(Apps(expr1, expr3), Apps(expr1, expr2).liftIndex(1, 2));
    }

    @Test
    public void liftPiClosed() throws Exception {
        // lift ( (x : N) -> N x, 0, 1) = (x : N) -> N x
        Expression expr = Pi("x", Nat(), Apps(Nat(), Index(0)));
        assertEquals(expr, expr.liftIndex(0, 1));
    }

    @Test
    public void liftPiOpen() throws Exception {
        // lift ( (x : N) -> N (var(0)), 0, 1) = (x : N) -> N (var(1))
        Expression expr1 = Pi("x", Nat(), Apps(Nat(), Index(1)));
        Expression expr2 = Pi("y", Nat(), Apps(Nat(), Index(2)));
        assertEquals(expr2, expr1.liftIndex(0, 1));
    }

    @Test
    public void liftArr() throws Exception {
        // lift ( N -> N (var(0)), 0, 1) = N -> N (var(1))
        Expression expr1 = Pi(Nat(), Apps(Nat(), Index(0)));
        Expression expr2 = Pi(Nat(), Apps(Nat(), Index(1)));
        assertEquals(expr2, expr1.liftIndex(0, 1));
    }
}
