<h1 id="pi">Pi Types<a class="headerlink" href="#pi" title="Permanent link">&para;</a></h1>

A pi type is a type of (dependent) functions.
If `p_1`, ... `p_n` are named or unnamed parameters and `B` is a type, then `\Pi p_1 ... p_n -> B` is also a type.
If `B` does not refer to variables defined in the parameters, you can write `A_1 -> ... A_n -> B` instead, where `A_i` are types of the parameters.
If `A_i` has type `\Type p_i h_i` and `B` has type `\Type p h`, then the type of the pi type is `\Type p_max h`, where `p_max` is the maximum of `p_1`, ... `p_n`, and `p`.
Note that the homotopy level of the pi type is simply the homotopy level of the codomain.

An expression of the form `\Pi p_1 ... p_n -> B` is equivalent to `\Pi p_1 -> \Pi p_2 ... p_n -> B`.
Moreover, if `p_1` equals to `(x_1 ... x_k : A)`, then it is also equivalent to `\Pi (x_1 : A) -> \Pi (x_2 ... x_k : A) -> \Pi p_2 ... p_n -> B`.

A _lambda parameter_ is either a variable or a named parameter.
If `p_1`, ... `p_n` is a sequence of lambda parameters and `e` is an expression of type `E`, then `\lam p_1 ... p_n => e` is an expression which has type `\Pi p_1 ... p_n -> E`.
If some paramters miss types, then they will be inferred by the typechecker.

If `f` is an expression of type `\Pi (x : A) -> B` and `a` is an expression of type `A`, then `f a` is an expression of type `B[a/x]`.
An expression of the form `(\lam x => b) a` reduces to `b[a/x]`.
An expression of the form `\lam x => f x` is equivalent to `f` if `x` is not free in `f`.
