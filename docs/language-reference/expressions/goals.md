<h1 id="goals">Goals<a class="headerlink" href="#goals" title="Permanent link">&para;</a></h1>

A _goal_ marks an unfinished expression.
It always produce an error message which contains the expected type of a expression that should replace the goal and the context of the goal, that is the list of available variables with their types.
A goal is writen as `{?}` or `{?id}`, where `id` is any identifier which denotes the name of the goal.
The name of a goal only appears in error messages and does not affect the code in any way.

For example, consider the following code:
```arend
\func f (x y : Nat) (p : x = y) : y = x
  => {?}
```

It will produce the following error message:

```bash
[GOAL] test.ard:1:44:
  Expected type: y = {Nat} x
  Context:
    y : Nat
    x : Nat
    p : x = {Nat} y
  In: {?}
  While processing: f
```

The information in the error message might be even more useful when the expected type or types of variables are inferred by the typechecker.
