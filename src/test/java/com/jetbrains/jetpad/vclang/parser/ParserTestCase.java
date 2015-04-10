package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTestCase {
  public static VcgrammarParser parse(String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    return new VcgrammarParser(tokens);
  }

  public static Expression parseExpr(String text) {
    BuildVisitor builder = new BuildVisitor(Prelude.OPERATOR_PRECEDENCE);
    assertEquals(0, builder.getErrors().size());
    return builder.visitExpr(parse(text).expr());
  }

  public static Definition parseDef(String text) {
    BuildVisitor builder = new BuildVisitor(Prelude.OPERATOR_PRECEDENCE);
    assertEquals(0, builder.getErrors().size());
    return builder.visitDef(parse(text).def());
  }

  public static List<Definition> parseDefs(String text) {
    BuildVisitor builder = new BuildVisitor(Prelude.OPERATOR_PRECEDENCE);
    assertEquals(0, builder.getErrors().size());
    return builder.visitDefs(parse(text).defs());
  }
}
