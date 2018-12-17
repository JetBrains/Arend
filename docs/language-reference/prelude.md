<h1 id="prelude">Prelude<a class="headerlink" href="#prelude" title="Permanent link">&para;</a></h1>

Prelude is a built-in module which contains several definitions which behave differently from ordinary definitions.
It is also always imported implicitly in every module.
You can import it explicitly to hide or rename some of its definitions.

We will discuss definitions in this module.
You can find file `Prelude.ard` which contains these definitions, but note that they are only roughly correspond to actual definitions since most of them cannot be actually defined in the ordinary syntax.

# Nat and Int

The definitions of `Nat`, `Int`, `Nat.+`, `Nat.*`, `Nat.-`, and `Int.fromNat` are actually correct, you can define the same definitions in an ordinary file.
The difference is that the definitions from Prelude are implemented more efficiently.

# Interval

The definition of the interval type `\data I | left | right` looks like the definition of the set with two elements, but this is not true actually.
One way to think about this data type is that it has more constructors to which you cannot refer explicitly.
This means that you cannot define a function on `I` by pattern matching.

# Path

The definition of `Path A a a'` is not correct.
By the definition, it should consists of all functions `\Pi (i : I) -> A i`, but actually it consists of all such functions `f` which also satisfy equations `f left = a` and `f right = a'`, where the equality is computational.
This means that when you write `path f` the typechecker also checks that this equations hold and if they don't it produces an error message.
For example, if you write `\func test : 1 = 0 => path (\lam _ => 0)`, you will see the following error message:

```bash
[ERROR] test.ard:1:23: The left path endpoint mismatch
  Expected: 1
    Actual: 0
  In: path (\lam _ => 0)
  While processing: test
```

The type of `Path` is also computed differently.
If `A` is an (n+1)-type, then `Path A a a'` is an n-type.
Otherwise, it has the same level as `A`.

Prelude also contains an infix form of `Path` called `=` which is actually a correctly defined function.
The definition of `@` is also correct, but the typechecker has an eta rule for this definition: `path (\lam i => p @ i) = p`.
This rule does not hold for functions `@` defined in other files.

Finally, function `Path.inProp` is not correct since it does not have a body.
It implies that every two element of a type in `\Prop` are equal.

# coe

Function `coe` imples that `I` is contractible and that `=` satisfies the rules for ordinary identity types.
The definition of `coe` is not correct since it pattern matches on the interval.
This function satisfies one additional reduction rule: `coe (\lam x => A) a i => a` if `x` is not free in `A`.

# iso

The definition of `iso` is not correct since it also pattern matches on the interval.
This definition implies the univalence axiom.
