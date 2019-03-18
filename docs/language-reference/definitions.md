<h1 id="definitions">Definitions<a class="headerlink" href="#definitions" title="Permanent link">&para;</a></h1>

Arend supports the following kinds definitions: [functions](/language-reference/definitions/functions), [data](/language-reference/definitions/data), [records](/language-reference/definitions/records), [classes](/language-reference/definitions/classes), [instances](/language-reference/definitions/classes), and [coercions](/language-reference/definitions/coercion) definitions.
Every definition has a name which must be a valid identifier as described [here](/language-reference/lexical-structure/#identifiers).
Definitions can be referred by their names.
Once we provided a definition named `def`, we can write the following expression: `def e_1 ... e_n`, where `e_1`, ..., `e_n` are expressions.
Such an expression is called _defcall_.
Expressions `e_1`, ..., `e_n` are called arguments of this defcall.

If we have only two parameters, we can write a defcall in the infix notation: ``e_1 `def` e_2``.
This expression is equivalent to `def e_1 e_2`.
If we have only one parameter, we can use the postfix notation: ``e_1 `def``.
This expression is equivalent to `def e_1`.

## Precedence

To parse expressions which involve more than one infix or postfix notation applications, we need to know the precedence of involved definitions.
It can be specified by keywords `\fixl` for left associative names, `\fixr` for right associative names, and `\fix` for non-associative names.
Such a keyword together with a number between 1 and 9 is specified before the name of the definition in its declaration.

For example, `\fixl 3 def1` defines a definition named `def1` which is left associative with priority 3.
If we have another definition `\fixr 4 def2`, then expression ``e1 `def1` e2 `def2` e3`` is parsed as ``e1 `def1` (e2 `def2` e3)`` since the priority of `def2` is higher than the priority of `def1`.
If they have the same priority and they are both left associative, then this expression is parsed as ``(e1 `def1` e2) `def2` e3``.
If they have the same priority and their associativities differ or one of them is non-associative, then this expression cannot be parsed and produces an error message.
The default precedence is `\fixr 10`.

## Infix operators

A definition can be labeled as an _infix operator_.
This means that its defcalls are parsed as infix notations even without `` ` ` ``.
Infix operators are defined by specifying one of keywords `\infixl`, `\infixr`, `\infix` before the name of the definition.
These keyords have the same syntax and semantics as keywords `\fixl`, `\fixr`, and `\fix` that we described before.

An infix operator can be used in the prefix form as ordinary definition.
For example, if we have a function defined as `\infixl 6 +`, then we can write either `+ 1 2` or `1 + 2`; these expressions are equivalent.
Finally, if `f` is an infix operator or an infix notation, then we can write `e f` which is equivalent to `f e`.
For example, we can write the function that adds 1 to its argument either as `1 +` or as `+ 1`.
If we apply the first function to 2, then we get `1 + 2` and if we apply the second one to 2, then we get `+ 1 2` and as noted before these expressions are equivalent.
