<h1 id="definitions">Definitions<a class="headerlink" href="#definitions" title="Permanent link">&para;</a></h1>

Arend supports the following kinds of definitions: [functions](/language-reference/definitions/functions), [data](/language-reference/definitions/data), [records](/language-reference/definitions/records), [classes](/language-reference/definitions/classes), [instances](/language-reference/definitions/classes), and [coercions](/language-reference/definitions/coercion).
Every definition has a name which must be a valid identifier as described [here](/language-reference/lexical-structure/#identifiers).

Definitions can be referred by _defcall_ expressions.
If `def` is a name of a definition, defcall is an expression of the form:
`def e_1 ... e_n`, where `e_1`, ..., `e_n` are expressions.
Expressions `e_1`, ..., `e_n` are called arguments of the defcall.

There are alternative notations in case a defcall has precisely one or two arguments.
In case of two arguments, it is possible to use the infix notation: `def e_1 e_2` is equivalent
to ``e_1 `def` e_2``.
In case of one argument, it is possible to use the postfix notation: `def e_1` is equivalent to ``e_1 `def``.

## Precedence

The parsing of complex defcall expressions, containing several infix and postfix notations, is regulated by precedence
of involved definitions: their priority and the type of associativity. Precedence of a definition can be specified
in the beginning of definition just after the keyword, reserved for the kind of definition (`\data`, `\func`, etc):
`DEF_KW FIX_KW N`, where `DEF_KW` is the corresponding keyword (`\data`, `\func`, etc), `FIX_KW` is one of keywords
`\fixl`, `\fixr` or `\fix`,
which mark the definition as left, right associative or non-associative respectively, and `N` is the priority, 
which is an integer between 1 and 9. For example, `\func \fixl 3 op (a b : Nat) => 0` defines a binary function
named `op` which is left associative with priority 3. The default precedence is `\fixr 10`. 

If `op1` and `op2` are two definitions and `e1,e2,e3` are expressions, the expression
``e1 `op1` e2 `op2` e3 `` is parsed according to the following rules:

* If priorities of `op1` and `op2` are different and, say, the priority of `op1` is higher, then the expression
is parsed as ``(e1 `op1` e2) `op2` e3 ``.
* Assume priorities of `op1` and `op2` are the same. If they are both left or both right associative, the expression is
parsed as ``(e1 `op1` e2) `op2` e3 `` or ``e1 `op1` (e2 `op2` e3)`` respectively. If `op1` and `op2` have
different types of associativity or are non-associative, then the parsing error is generated.

## Infix operators

A definition can be labeled as an _infix operator_.
This means that its defcalls are parsed as infix notations even without `` ` ` ``.
An infix operator is defined by specifying one of keywords `\infixl`, `\infixr`, `\infix` before the name of operator.
These keywords have the same syntax and semantics as keywords `\fixl`, `\fixr`, and `\fix`, which are described above.

An infix operator can be used in the prefix form as an ordinary definition.
For example, if the function `+` is defined as `\infixl 6 +`, then it is allowed to write either `+ 1 2` or `1 + 2`; 
these expressions are equivalent.

Finally, if `f` is an infix operator or an operator surrounded with `` ` ` ``, then it is allowed to write `e f` and
this is equivalent to `f e`.
For example, the function that adds 1 to its argument can be written either as `1 +` or as `+ 1`.
The result of application of the first function to 2 is `1 + 2`, the result of application of the second one to 2
is `+ 1 2`, and as noted before these expressions are equivalent.
