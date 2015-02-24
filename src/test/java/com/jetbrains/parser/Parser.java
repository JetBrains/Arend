package test.java.com.jetbrains.parser;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.expr.Expression;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class Parser {
    public static VcgrammarParser parse(String text) {
        ANTLRInputStream input = new ANTLRInputStream(text);
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new VcgrammarParser(tokens);
    }

    public static Expression parseExpr(String text) {
        BuildVisitor builder = new BuildVisitor();
        return (Expression) builder.visit(parse(text).expr());
    }

    public static Definition parseDef(String text) {
        BuildVisitor builder = new BuildVisitor();
        return (Definition) builder.visit(parse(text).def());
    }
}
