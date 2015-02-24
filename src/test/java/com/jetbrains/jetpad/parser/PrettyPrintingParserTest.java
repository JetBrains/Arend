package com.jetbrains.jetpad.parser;

import com.jetbrains.jetpad.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.term.definition.Signature;
import com.jetbrains.jetpad.term.expr.Expression;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.jetbrains.jetpad.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;
import static com.jetbrains.jetpad.parser.Parser.parseDef;
import static com.jetbrains.jetpad.parser.Parser.parseExpr;

public class PrettyPrintingParserTest {
    private void testExpr(Expression expr) throws UnsupportedEncodingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        expr.prettyPrint(ps, new ArrayList<String>(), 0);
        Expression result = parseExpr(os.toString("UTF8"));
        assertEquals(expr, result);
    }

    private void testDef(FunctionDefinition def) throws UnsupportedEncodingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        def.prettyPrint(ps, new ArrayList<String>(), 0);
        FunctionDefinition result = (FunctionDefinition) parseDef(os.toString("UTF8"));
        assertEquals(def.getSignature().getType(), result.getSignature().getType());
        assertEquals(def.getTerm(), result.getTerm());
    }

    @Test
    public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
        // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
        testExpr(Apps(Lam("x", Lam("y", Apps(Index(1), Apps(Index(1), Index(0))))), Lam("x", Lam("y", Index(1))), Apps(Lam("x", Index(0)), Lam("x", Index(0)))));
    }

    @Test
    public void prettyPrintingParserPi() throws UnsupportedEncodingException {
        // (x y : N) -> N -> N -> (x y -> y x) -> N x y
        testExpr(Pi("x", Nat(), Pi("y", Nat(), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0))))))));
    }

    @Test
    public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
        // (x : N) {y z : N} -> N -> (t z : N) {x : N -> N} -> N x y z t
        testExpr(Pi("x", Nat(), Pi(false, "y", Nat(), Pi(false, "z", Nat(), Pi(Nat(), Pi("t", Nat(), Pi("w", Nat(), Pi(false, "p", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2))))))))));
    }

    @Test
    public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
        // f : (x : N) -> N x = \y z. y z;
        testDef(new FunctionDefinition("f", new Signature(Pi("x", Nat(), Apps(Nat(), Index(0)))), Lam("y", Lam("z", Apps(Index(1), Index(0))))));
    }
}
