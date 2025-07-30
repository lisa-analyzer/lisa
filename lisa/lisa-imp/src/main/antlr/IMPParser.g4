/**
 * The parser for the java subset
 * 
 * @author Luca Negrini
 */
parser grammar IMPParser;

@ header
{
    package it.unive.lisa.imp.antlr;
}

options { tokenVocab = IMPLexer; }
/*
 * GENERAL TOKENS
 */


arraySqDeclaration
   : (LBRACK RBRACK)+
   ;
/*
 * TYPES
 */
   
   
primitiveType
   : BOOLEAN
   | INT
   | FLOAT
   | STRING
   ;
/*
 * PARAMETER LIST
 */
   
   
formals
   : LPAREN (formal (COMMA formal)*)? RPAREN
   ;

formal
   : ((annotations?) (name = IDENTIFIER))
   ;
/*
 * LITERALS
 */
   
   
literal
   : SUB? LITERAL_DECIMAL
   | SUB? LITERAL_FLOAT
   | LITERAL_STRING
   | LITERAL_BOOL
   | LITERAL_NULL
   ;
/*
 * CALL PARAMETERS
 */
   
   
arguments
   : LPAREN (arg (COMMA arg)*)? RPAREN
   ;

arg
   : literal
   | IDENTIFIER
   | THIS
   | fieldAccess
   | arrayAccess
   | methodCall
   ;
/*
 * EXPRESSIONS
 */
   
   
expression
   : LPAREN paren = expression RPAREN
   | basicExpr
   | NOT nested = expression
   | left = expression (MUL | DIV | MOD) right = expression
   | left = expression (ADD | SUB) right = expression
   | left = expression (GT | GE | LT | LE) right = expression
   | left = expression (EQUAL | NOTEQUAL) right = expression
   | left = expression (AND | OR) right = expression
   | left = expression (IS | AS) type = unitName
   | SUB nested = expression
   | NEW (newBasicArrayExpr | newReferenceType)
   | BUMP (newBasicArrayExpr | newReferenceType)
   | arrayAccess
   | fieldAccess
   | methodCall
   | assignment
   | stringExpr
   | arrayExpr
   ;

basicExpr
   : THIS
   | SUPER
   | IDENTIFIER
   | literal
   ;

newBasicArrayExpr
   : primitiveType arrayCreatorRest
   ;

newReferenceType
   : IDENTIFIER (arguments | arrayCreatorRest)
   ;

arrayCreatorRest
   : (LBRACK index RBRACK)+
   ;

receiver
   : THIS
   | SUPER
   | IDENTIFIER
   ;

arrayAccess
   : receiver (LBRACK index RBRACK)+
   ;

index
   : LITERAL_DECIMAL
   | IDENTIFIER
   ;

fieldAccess
   : receiver DOT name = IDENTIFIER
   ;

methodCall
   : receiver DOT name = IDENTIFIER arguments
   ;

stringExpr
   : unaryStringExpr
   | binaryStringExpr
   | ternaryStringExpr
   ;

unaryStringExpr
   : STRLEN LPAREN op = expression RPAREN
   ;

binaryStringExpr
   : STRCAT LPAREN left = expression COMMA right = expression RPAREN
   | STRCONTAINS LPAREN left = expression COMMA right = expression RPAREN
   | STRENDS LPAREN left = expression COMMA right = expression RPAREN
   | STREQ LPAREN left = expression COMMA right = expression RPAREN
   | STRINDEXOF LPAREN left = expression COMMA right = expression RPAREN
   | STRSTARTS LPAREN left = expression COMMA right = expression RPAREN
   ;

ternaryStringExpr
   : STRREPLACE LPAREN left = expression COMMA middle = expression COMMA right = expression RPAREN
   | STRSUB LPAREN left = expression COMMA middle = expression COMMA right = expression RPAREN
   ;

arrayExpr
   : ARRAYLEN LPAREN op = expression RPAREN
   ;
/*
 * STATEMENT
 */
   
   
statement
   : localDeclaration SEMI
   | ASSERT expression SEMI
   | IF condition = parExpr then = blockOrStatement (ELSE otherwise = blockOrStatement)?
   | loop
   | try
   | RETURN expression? SEMI
   | THROW expression SEMI
   | skip = SEMI
   | command = expression SEMI
   ;

localDeclaration
   : annotations? DEFINE IDENTIFIER ASSIGN expression
   ;

assignment
   : (IDENTIFIER | fieldAccess | arrayAccess) ASSIGN expression
   ;

parExpr
   : LPAREN expression RPAREN
   ;

loop
   : forLoop
   | whileLoop
   ;

forLoop
   : FOR LPAREN forDeclaration RPAREN blockOrStatement
   ;

whileLoop
   : WHILE parExpr blockOrStatement
   ;

forDeclaration
   : (initDecl = localDeclaration | initExpr = expression)? SEMI condition = expression? SEMI post = expression?
   ;

try
   : TRY body = block catchBlock catchBlock* (ELSE else = block)? (FINALLY final = block)?
   ;

catchBlock
   : CATCH LPAREN exceptions = unitNames IDENTIFIER? RPAREN body = block
   ;
/*
 * ANNOTATIONS
 */
   
   
unitName
   : IDENTIFIER (DOT IDENTIFIER)*
   ;

annotation
   : name = unitName annotationMembers?
   ;

annotationMembers
   : LPAREN annotationMember (COMMA annotationMember)* RPAREN
   ;

annotationMember
   : IDENTIFIER ASSIGN annotationValue
   ;

annotationValue
   : basicAnnotationValue
   | arrayAnnotationValue
   ;

arrayAnnotationValue
   : LBRACK (basicAnnotationValue (COMMA basicAnnotationValue)*)? RBRACK
   ;

basicAnnotationValue
   : SUB? LITERAL_DECIMAL
   | SUB? LITERAL_FLOAT
   | LITERAL_STRING
   | LITERAL_BOOL
   | unit_name = unitName
   ;

annotations
   : LBRACK annotation (COMMA annotation)* RBRACK
   ;
/*
 * BLOCK
 */
   
   
block
   : LBRACE blockOrStatement* RBRACE
   ;

blockOrStatement
   : block
   | statement
   ;
/*
 * CLASS AND INTERFACE MEMBERS
 */
   
   
classMemberDeclarations
   : (methodDeclaration | fieldDeclaration | constantDeclaration | constructorDeclaration | signatureDeclaration)*
   ;

interfaceMemberDeclarations
   : (methodDeclaration | fieldDeclaration | constantDeclaration | signatureDeclaration)*
   ;

constantDeclaration
   : annotations? CONSTANT name = IDENTIFIER ASSIGN value = literal SEMI
   ;

fieldDeclaration
   : annotations? name = IDENTIFIER SEMI
   ;

constructorDeclaration
   : annotations? TILDE name = IDENTIFIER pars = formals code = block
   ;

methodDeclaration
   : annotations? FINAL? name = IDENTIFIER pars = formals code = block
   ;

signatureDeclaration
   : annotations? FINAL? name = IDENTIFIER formals SEMI
   ;
/*
 * CLASS
 */
   
   
unit
   : classUnit
   | interfaceUnit
   ;

classUnit
   : annotations? ABSTRACT? CLASS name = unitName (EXTENDS superclass = unitName)? (IMPLEMENTS superinterfaces = unitNames)? LBRACE declarations = classMemberDeclarations RBRACE
   ;

interfaceUnit
   : annotations? INTERFACE name = unitName (EXTENDS superinterfaces = unitNames)? LBRACE declarations = interfaceMemberDeclarations RBRACE
   ;

unitNames
   : unitName (COMMA unitName)*
   ;

file
   : unit*
   ;

