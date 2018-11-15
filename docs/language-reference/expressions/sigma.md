<h1 id="sigma">Sigma Types<a class="headerlink" href="#sigma" title="Permanent link">&para;</a></h1>

A sigma type is a type of (dependent) tuples.
If `p_1`, ... `p_n` are named or unnamed parameters, then `\Sigma p_1 ... p_n` is also a type.
If `A_i` has type `\Type p_i h_i`, then the type of the sigma type is `\Type p_max h_max`, where `p_max` is the maximum of `p_1`, ... `p_n` and `h_max` is the maximum of `h_1`, ... `h_n`.

An expression of the form `\Sigma p_1 ... p_n (x_1 ... x_k : A) q_1 ... q_m` is equivalent to `\Sigma p_1 ... p_n (x_1 : A) ... (x_k : A) q_1 ... q_m`.

If `a_i` is an expression of type `A_i[a_1/x_1, ... a_{i-1}/x_{i-1}]`, then `(a_1, ... a_n)` is an expression of type `\Sigma (x_1 : A_1) ... (x_n : A_n)`.
Note that the typechecker often cannot infer the correct type of such an expression.
If it does not know it already, it always infer the non-dependent version.
In this case, you can simply specify the type explicitly: `((a_1, ... a_n) : \Sigma (x_1 : A_1) ... (x_n : A_n))`.
You can also explicitly specify type of each field: `(b_1 : B_1, ... b_n : B_n)`, but `B_i` cannot refer to previous parameters, so this allows you to define only non-dependent sigma types.

If `p` is an expression of type `\Sigma (x_1 : A_1) ... (x_n : A_n)` and 1 ≤ i ≤ n, then `p.i` is an expression of type `A_i[p.1/x_1, ... p_{i-1}/x_{i-1}]`.
An expression of the form `(a_1, ... a_n).i` reduces to `a_i`.
An expression of the form `(p.1, ... p.n)` is equivalent to `p`.
