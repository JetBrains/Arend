package com.jetbrains.jetpad.parser;

import com.jetbrains.jetpad.parser.BuildVisitor;
import com.jetbrains.jetpad.parser.VcgrammarLexer;
import com.jetbrains.jetpad.parser.VcgrammarParser;
import com.jetbrains.jetpad.term.definition.Definition;
import com.jetbrains.jetpad.term.expr.Expression;
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
