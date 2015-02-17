package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.expr.*;
import main.java.com.jetbrains.term.typechecking.TypeMismatchException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

public class TypeCheckingTest {
    @Test
    public void typeCheckingLam() {
        // \x. x : N -> N
        Expression expr = new LamExpression("x", new IndexExpression(0));
        Expression nat = new NatExpression();
        expr.checkType(new ArrayList<Definition>(), new PiExpression(nat, nat));
    }

    @Test(expected = TypeMismatchException.class)
    public void typeCheckingLamError() {
        // \x. x : N -> N -> N
        Expression expr = new LamExpression("x", new IndexExpression(0));
        Expression nat = new NatExpression();
        expr.checkType(new ArrayList<Definition>(), new PiExpression(nat, new PiExpression(nat, nat)));
    }

    @Test
    public void typeCheckingApp() {
        // \x y. y (y x) : N -> (N -> N) -> N
        Expression expr = new LamExpression("x", new LamExpression("y", new AppExpression(new IndexExpression(0), new AppExpression(new IndexExpression(0), new IndexExpression(1)))));
        Expression nat = new NatExpression();
        expr.checkType(new ArrayList<Definition>(), new PiExpression(nat, new PiExpression(new PiExpression(nat, nat), nat)));
    }

    @Test
    public void typeCheckingAppPi() {
        // \f g. g zero (f zero) : (f : (x : N) -> N x) -> ((x : N) -> N x -> N (f x)) -> N (f zero)
        Expression zero = new ZeroExpression();
        Expression expr = new LamExpression("f", new LamExpression("g", new AppExpression(new AppExpression(new IndexExpression(0), zero), new AppExpression(new IndexExpression(1), zero))));
        Expression nat = new NatExpression();
        Expression type = new PiExpression("f", new PiExpression("x", nat, new AppExpression(nat, new IndexExpression(0))), new PiExpression(new PiExpression("x", nat, new PiExpression(new AppExpression(nat, new IndexExpression(0)), new AppExpression(nat, new AppExpression(new IndexExpression(1), new IndexExpression(0))))), new AppExpression(nat, new AppExpression(new IndexExpression(0), zero))));
        expr.checkType(new ArrayList<Definition>(), type);
    }

    @Test
    public void typeCheckingAppLamPi() {
        // \f h. h (\k -> k (suc zero)) : (f : (g : N -> N) -> N (g zero)) -> ((z : (N -> N) -> N) -> N (f (\x. z (\_. x)))) -> N (f (\x. x))
        Expression zero = new ZeroExpression();
        Expression expr = new LamExpression("f", new LamExpression("h", new AppExpression(new IndexExpression(0), new LamExpression("k", new AppExpression(new IndexExpression(0), new AppExpression(new SucExpression(), zero))))));
        Expression nat = new NatExpression();
        Expression type = new PiExpression("f", new PiExpression("g", new PiExpression(nat, nat), new AppExpression(nat, new AppExpression(new IndexExpression(0), zero))), new PiExpression(new PiExpression("z", new PiExpression(new PiExpression(nat, nat), nat), new AppExpression(nat, new AppExpression(new IndexExpression(1), new LamExpression("x", new AppExpression(new IndexExpression(1), new LamExpression("_", new IndexExpression(1))))))), new AppExpression(nat, new AppExpression(new IndexExpression(0), new LamExpression("x", new IndexExpression(0))))));
        expr.checkType(new ArrayList<Definition>(), type);
    }

    @Test
    public void typeCheckingPi() {
        // (X : Type1) -> X -> X : Type2
        Expression expr = new PiExpression("X", new UniverseExpression(1), new PiExpression(new IndexExpression(0), new IndexExpression(0)));
        assertEquals(new UniverseExpression(2), expr.inferType(new ArrayList<Definition>()));
    }
}
