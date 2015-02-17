package test.java.com.jetbrains.parser;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.expr.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

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
        assertEquals(new LamExpression("x", new LamExpression("y", new LamExpression("z", new VarExpression("y")))), expr);
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

        Expression nat = new NatExpression();
        Expression natNat = new PiExpression(nat, nat);
        Expression piNat = new PiExpression("c", nat, new AppExpression(nat, new VarExpression("b")));
        assertEquals(new PiExpression("x", nat, new PiExpression("y", nat, new PiExpression("z", nat, new PiExpression("w", natNat, new PiExpression("t", natNat, new PiExpression("a", piNat, new PiExpression("b", piNat, new AppExpression(new AppExpression(nat, new VarExpression("y")), new VarExpression("w"))))))))), expr);
    }
}
