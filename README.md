**GROUP: 3B**

* Clara Martins
    * **Nr:** 201806528
    * **Grade:** 19
    * **Contribution:** 24 %
* Daniel Monteiro
    * **Nr:** 201806185
    * **Grade:** 17
    * **Contribution:** 18 %
* Gonçalo Pascoal
    * **Nr:** 201806332
    * **Grade:** 20
    * **Contribution:** 34 %
* Rui Pinto
    * **Nr:** 201806441
    * **Grade:** 19
    * **Contribution:** 24 %

**GLOBAL Grade of the project:** 19

### SUMMARY

All errors detected during the compilation can be seen in error messages. 
These contain the line and column where the error occurred and a small explanation of the error.

The compiler first detects syntactic errors and tries to generate an AST (abstract syntax tree) using the JavaCC grammar.
Afterwards, it begins the semantic analysis step where it constructs the symbol table and checks if there are any semantic
errors in the code.
The AST is then converted to OLLIR code, a low level representation which is fed to a parser. If the compiler is called with any of 
the optimization command line arguments, a series of optimizations will be applied to the AST / OLLIR code. We use the result of the OLLIR parser
to generate Jasmin code that can be compiled into a .class file.

### DEALING WITH SYNTACTIC ERRORS
* The grammar used is mostly LL(1), only using a lookahead of 2 to differentiate between a `VarDeclaration` and a `Statement`;
* The grammar also rejects some productions of `Statement` that constitute invalid Java code;
* The compiler stops execution after the first syntax error is detected, except for `while` loops, as explained below;
* It includes error recovery for `while` loops, only exiting after the entire code has been analysed;
* Syntactic analysis respects operator precedence.

### SEMANTIC ANALYSIS
The following list details the types of verifications that are made during the semantic analysis step.
* There are no variables with the same name in the same scope;
* Detection of uninitialized local variables;
* Some statements that are invalid Java code (like 1 + 2; or this; are not accepted);
* "this" and class fields aren't used in a static context (such as the main function);
* No function is declared more than once (same name, number of parameters and types of parameters);
* No function call is made with wrong argument types, or a wrong number of arguments;
* The operators receive the correct types;
* Array indices and sizes are integers;
* The length operator can only be used with arrays;
* Both sides of an assignment have the same type;
* No missing types are used (types that were not imported).

### CODE GENERATION
* Code generation based on OLLIR specification;
* The OllirVisitor class visits the nodes in the AST and generates the corresponding OLLIR code. Preorder and postorder visits are used depending on
the node type (for example, it is useful to generate code for expressions in a bottom-up way);
* The optimizations are applied in the `OptimizationStage` class;
* The Jasmin code is generated in the `BackendStage` class;
* For functions calls that use imported classes, correct types are assumed and inferred from the context of the function
call;
* To avoid excessive use of branches and to further optimize the generated code, the boolean NOT and LESS THAN operations 
were implemented using bit manipulation instructions. Therefore branches are only used in if / while statements;
* Stack and local variable limits are calculated based on the code generated;

### TASK DISTRIBUTION
Most of the tasks were completed as a group.

#### PROS
* All the required semantic analysis verifications were implemented, as well as additional verifications.
* All required code generation was implemented, and all the included public tests pass.
* Support for method overloading
* Support for several optimizations, such as register allocation, constant propagation and constant folding
* When the `if` condition is a constant, the compiler identifies the correct program flow for constant propagation
* Additional tests were included to showcase some of the additional features 
(`ConstantPropagation`, `FibonacciAndFactorial`, `MaxOverloading`, `NameAlreadyInUse`, `ThisStaticContext`, `NotAStatement`)

### CONS
* Unused variables can reduce the effectiveness of register allocation
* The while loop template optimization wasn't implemented
