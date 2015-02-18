package test.java.com.jetbrains.parser;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.expr.Expression;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import static main.java.com.jetbrains.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

public class ParserTest {
    @Test
    public void parserLam() {
        ANTLRInputStream input = new ANTLRInputStream("\\x y z -> y");
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VcgrammarParser parser = new VcgrammarParser(tokens);
        ParseTree tree = parser.expr();
        BuildVisitor builder = new BuildVisitor();
        Object expr = builder.visit(tree);
        assertEquals(Lam("x", Lam("y", Lam("z", Var("y")))), expr);
    }

    @Test
    public void parserPi() {
        ANTLRInputStream input = new ANTLRInputStream("(x y z : N) (w t : N -> N) -> (a b : ((c : N) -> N b)) -> N y w");
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VcgrammarParser parser = new VcgrammarParser(tokens);
        ParseTree tree = parser.expr();
        BuildVisitor builder = new BuildVisitor();
        Object expr = builder.visit(tree);

        Expression natNat = Pi(Nat(), Nat());
        Expression piNat = Pi("c", Nat(), Apps(Nat(), Var("b")));
        assertEquals(Pi("x", Nat(), Pi("y", Nat(), Pi("z", Nat(), Pi("w", natNat, Pi("t", natNat, Pi("a", piNat, Pi("b", piNat, Apps(Nat(), Var("y"), Var("w"))))))))), expr);
    }
}
