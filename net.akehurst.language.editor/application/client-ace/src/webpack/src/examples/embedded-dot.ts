import {Examples} from "./examples";

const id = 'embedded-dot';
const label = 'Graphviz DOT Language (XML Embedded in DOT) ';

const sentence = `

`;
const grammar = `
namespace net.akehurst.language.example.dot

grammar Xml {

	skip COMMENT = "<!-- [.]* -->" ;

	file = element? ;

	element = elementEmpty | elementContent ;
	elementEmpty = '<' WS? NAME WS? attribute* '/>' ;
    elementContent = startTag content endTag ;
	startTag = '<' WS? NAME WS? attribute* '>' ;
	endTag = '</' WS? NAME WS? '>' ;

	content = (CHARDATA | element)* ;

	attribute = NAME WS? '=' WS? string WS? ;
	string = DOUBLE_QUOTE_STRING | SINGLE_QUOTE_STRING ;
	WS = "\\s+" ;
	CHARDATA = "[^<]+" ;
	NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
	DOUBLE_QUOTE_STRING = "[\\"][^\\"]*[\\"]" ;
	SINGLE_QUOTE_STRING = "['][^']*[']" ;
}

grammar Dot  {

    skip WHITESPACE = "\\s+" ;
	skip SINGLE_LINE_COMMENT = "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/" ;
	skip MULTI_LINE_COMMENT = "//.*?$" ;
	skip C_PREPROCESSOR = "#.*?$" ;

	graph =
	  STRICT? type  ID? '{' stmt_list '}'
	;
    type = GRAPH | DIGRAPH ? ;
	stmt_list = ( stmt ';'? )* ;

	stmt =
	    node_stmt
      | edge_stmt
      | attr_stmt
      | ID '=' ID
      | subgraph
      ;

    node_stmt = node_id attr_list? ;
    node_id = ID port? ;
    port =
        ':' ID (':' compass_pt)?
      | ':' compass_pt
      ;
    compass_pt	=	('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_') ;

    edge_stmt =	(node_id | subgraph) edgeRHS attr_list? ;
    edgeRHS = ( EDGEOP (node_id | subgraph) )+ ;
    EDGEOP = '--' | '->' ;

    attr_stmt = (GRAPH | NODE | EDGE) attr_list ;
    attr_list = ( '[' a_list? ']' )+ ;
    a_list = ID '=' ID (';' | ',')? a_list? ;

    subgraph = ( SUBGRAPH ID? ) ? '{' stmt_list '}' ;


	leaf STRICT = "[Ss][Tt][Rr][Ii][Cc][Tt]";
	leaf GRAPH = "[Gg][Rr][Aa][Pp][Hh]" ;
	leaf DIGRAPH = "[Dd][Ii][Gg][Rr][Aa][Pp][Hh]" ;
	leaf SUBGRAPH = "[Ss][Uu][Bb][Gg][Rr][Aa][Pp][Hh]" ;
	leaf NODE = "[Nn][Oo][Dd][Ee]" ;
	leaf EDGE = "[Ee][Dd][Gg][Ee]" ;

	ID =
	  ALPHABETIC_ID
	| NUMERAL
	| DOUBLE_QUOTE_STRING
	| HTML
	;

	leaf ALPHABETIC_ID = "[a-zA-Z_][a-zA-Z_0-9]*" ; //"[a-zA-Z\\200-\\377_][a-zA-Z\\200-\\377_0-9]*" ;

	leaf NUMERAL = "[-+]?([0-9]*(.[0-9]+)?)" ;
	leaf DOUBLE_QUOTE_STRING = "\\"(?:\\\\?.)*?\\"" ;
	HTML = '<' Xml.elementContent '>' ;

}

`;
const style = `
STRICT {
  color: purple;
  font-weight: bold;
}
GRAPH {
  color: purple;
  font-weight: bold;
}
DIGRAPH {
  color: purple;
  font-weight: bold;
}
SUBGRAPH {
  color: purple;
  font-weight: bold;
}
NODE {
  color: purple;
  font-weight: bold;
}
EDGE {
  color: purple;
  font-weight: bold;
}
ALPHABETIC_ID {
  color: red;
  font-style: italic;
}
`;
const format = `

`;

Examples.add(id, label, sentence, grammar, style, format);