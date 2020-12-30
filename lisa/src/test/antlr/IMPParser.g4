/**
 * The parser for the java subset
 * 
 * @author Luca Negrini
 */
parser grammar IMPParser;

@ header
{
    package it.unive.lisa.test.antlr;
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
   : name = IDENTIFIER
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
   | SUB nested = expression
   | NEW (newBasicArrayExpr | newReferenceType)
   | arrayAccess
   | fieldAccess
   | methodCall
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

arrayAccess
   : IDENTIFIER (LBRACK index RBRACK)+
   ;

index
   : LITERAL_DECIMAL
   | IDENTIFIER
   ;

receiver
   : THIS
   | SUPER
   | IDENTIFIER
   ;

fieldAccess
   : receiver DOT name = IDENTIFIER
   ;

methodCall
   : receiver DOT name = IDENTIFIER arguments
   ;
/*
 * STATEMENT
 */
   
   
statement
   : assignment SEMI
   | ASSERT expression SEMI
   | IF condition = parExpr then = blockOrStatement (ELSE otherwise = blockOrStatement)?
   | loop
   | RETURN expression? SEMI
   | THROW expression SEMI
   | skip = SEMI
   | command = expression SEMI
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
   : init = assignment? SEMI condition = expression SEMI post = assignment?
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
 * CLASS MEMBERS
 */
   
   
memberDeclarations
   : (methodDeclaration | fieldDeclaration)*
   ;

fieldDeclaration
   : name = IDENTIFIER SEMI
   ;

methodDeclaration
   : name = IDENTIFIER pars = formals code = block
   ;
/*
 * CLASS
 */
   
   
unit
   : CLASS name = IDENTIFIER (EXTENDS superclass = IDENTIFIER)? LBRACE declarations = memberDeclarations RBRACE
   ;

file
   : unit*
   ;

