package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.expr.Expression;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static main.java.com.jetbrains.term.expr.Expression.*;

public class PrettyPrintingTest {
    private class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException { }
    }

    @Test
    public void prettyPrintingLam() {
        // \x. x x
        Expression expr = Lam("x", Apps(Index(0), Index(0)));
        expr.prettyPrint(new PrintStream(new NullOutputStream()), new ArrayList<String>(), 0);
    }

    @Test
    public void prettyPrintingLam2() {
        // \x. x (\y. y x) (\z w. x w z)
        Expression expr = Lam("x", Apps(Index(0), Lam("y", Apps(Index(0), Index(1))), Lam("z", Lam("w", Apps(Index(2), Index(0), Index(1))))));
        expr.prettyPrint(new PrintStream(new NullOutputStream()), new ArrayList<String>(), 0);
    }

    @Test
    public void prettyPrintingPi() {
        // (x y : N) (z w : N -> N) -> ((s : N) -> N (z s) (w x)) -> N
        Expression expr = Pi("x", Nat(), Pi("y", Nat(), Pi("z", Pi(Nat(), Nat()), Pi("w", Pi(Nat(), Nat()), Pi(Pi("s", Nat(), Apps(Nat(), Apps(Index(2), Index(0)), Apps(Index(1), Index(4)))), Nat())))));
        expr.prettyPrint(new PrintStream(new NullOutputStream()), new ArrayList<String>(), 0);
    }
}
