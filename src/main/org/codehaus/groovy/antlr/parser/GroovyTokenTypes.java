// $ANTLR 2.7.5 (20050128): "groovy.g" -> "GroovyRecognizer.java"$

package org.codehaus.groovy.antlr.parser;
import org.codehaus.groovy.antlr.*;
import java.util.*;
import java.io.InputStream;
import java.io.Reader;
import antlr.InputBuffer;
import antlr.LexerSharedInputState;

public interface GroovyTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int BLOCK = 4;
	int MODIFIERS = 5;
	int OBJBLOCK = 6;
	int SLIST = 7;
	int METHOD_DEF = 8;
	int VARIABLE_DEF = 9;
	int INSTANCE_INIT = 10;
	int STATIC_INIT = 11;
	int TYPE = 12;
	int CLASS_DEF = 13;
	int INTERFACE_DEF = 14;
	int PACKAGE_DEF = 15;
	int ARRAY_DECLARATOR = 16;
	int EXTENDS_CLAUSE = 17;
	int IMPLEMENTS_CLAUSE = 18;
	int PARAMETERS = 19;
	int PARAMETER_DEF = 20;
	int LABELED_STAT = 21;
	int TYPECAST = 22;
	int INDEX_OP = 23;
	int POST_INC = 24;
	int POST_DEC = 25;
	int METHOD_CALL = 26;
	int EXPR = 27;
	int IMPORT = 28;
	int UNARY_MINUS = 29;
	int UNARY_PLUS = 30;
	int CASE_GROUP = 31;
	int ELIST = 32;
	int FOR_INIT = 33;
	int FOR_CONDITION = 34;
	int FOR_ITERATOR = 35;
	int EMPTY_STAT = 36;
	int FINAL = 37;
	int ABSTRACT = 38;
	int UNUSED_GOTO = 39;
	int UNUSED_CONST = 40;
	int UNUSED_DO = 41;
	int STRICTFP = 42;
	int SUPER_CTOR_CALL = 43;
	int CTOR_CALL = 44;
	int CTOR_IDENT = 45;
	int VARIABLE_PARAMETER_DEF = 46;
	int STRING_CONSTRUCTOR = 47;
	int STRING_CTOR_MIDDLE = 48;
	int CLOSED_BLOCK = 49;
	int IMPLICIT_PARAMETERS = 50;
	int SELECT_SLOT = 51;
	int REFLECT_MEMBER = 52;
	int DYNAMIC_MEMBER = 53;
	int LABELED_ARG = 54;
	int SPREAD_ARG = 55;
	int OPTIONAL_ARG = 56;
	int SCOPE_ESCAPE = 57;
	int LIST_CONSTRUCTOR = 58;
	int MAP_CONSTRUCTOR = 59;
	int FOR_IN_ITERABLE = 60;
	int RANGE_EXCLUSIVE = 61;
	int STATIC_IMPORT = 62;
	int ENUM_DEF = 63;
	int ENUM_CONSTANT_DEF = 64;
	int FOR_EACH_CLAUSE = 65;
	int ANNOTATION_DEF = 66;
	int ANNOTATIONS = 67;
	int ANNOTATION = 68;
	int ANNOTATION_MEMBER_VALUE_PAIR = 69;
	int ANNOTATION_FIELD_DEF = 70;
	int ANNOTATION_ARRAY_INIT = 71;
	int TYPE_ARGUMENTS = 72;
	int TYPE_ARGUMENT = 73;
	int TYPE_PARAMETERS = 74;
	int TYPE_PARAMETER = 75;
	int WILDCARD_TYPE = 76;
	int TYPE_UPPER_BOUNDS = 77;
	int TYPE_LOWER_BOUNDS = 78;
	int SH_COMMENT = 79;
	int LITERAL_package = 80;
	int LITERAL_import = 81;
	int LITERAL_static = 82;
	int LITERAL_def = 83;
	int AT = 84;
	int IDENT = 85;
	int LBRACK = 86;
	int RBRACK = 87;
	int LPAREN = 88;
	int LITERAL_class = 89;
	int LITERAL_interface = 90;
	int LITERAL_enum = 91;
	int DOT = 92;
	int QUESTION = 93;
	int LITERAL_extends = 94;
	int LITERAL_super = 95;
	int LT = 96;
	int COMMA = 97;
	int GT = 98;
	int SR = 99;
	int BSR = 100;
	int LITERAL_void = 101;
	int LITERAL_boolean = 102;
	int LITERAL_byte = 103;
	int LITERAL_char = 104;
	int LITERAL_short = 105;
	int LITERAL_int = 106;
	int LITERAL_float = 107;
	int LITERAL_long = 108;
	int LITERAL_double = 109;
	int LITERAL_any = 110;
	int STAR = 111;
	int LITERAL_as = 112;
	int LITERAL_private = 113;
	int LITERAL_public = 114;
	int LITERAL_protected = 115;
	int LITERAL_transient = 116;
	int LITERAL_native = 117;
	int LITERAL_threadsafe = 118;
	int LITERAL_synchronized = 119;
	int LITERAL_volatile = 120;
	int RPAREN = 121;
	int ASSIGN = 122;
	int BAND = 123;
	int LCURLY = 124;
	int RCURLY = 125;
	int SEMI = 126;
	int NLS = 127;
	int LITERAL_default = 128;
	int LITERAL_implements = 129;
	int LITERAL_this = 130;
	int STRING_LITERAL = 131;
	int LITERAL_throws = 132;
	int TRIPLE_DOT = 133;
	int CLOSURE_OP = 134;
	int LOR = 135;
	int BOR = 136;
	int COLON = 137;
	int LITERAL_if = 138;
	int LITERAL_else = 139;
	int LITERAL_while = 140;
	int LITERAL_with = 141;
	int LITERAL_switch = 142;
	int LITERAL_for = 143;
	int LITERAL_in = 144;
	int LITERAL_return = 145;
	int LITERAL_break = 146;
	int LITERAL_continue = 147;
	int LITERAL_throw = 148;
	int LITERAL_assert = 149;
	int PLUS = 150;
	int MINUS = 151;
	int LITERAL_case = 152;
	int LITERAL_try = 153;
	int LITERAL_finally = 154;
	int LITERAL_catch = 155;
	int PLUS_ASSIGN = 156;
	int MINUS_ASSIGN = 157;
	int STAR_ASSIGN = 158;
	int DIV_ASSIGN = 159;
	int MOD_ASSIGN = 160;
	int SR_ASSIGN = 161;
	int BSR_ASSIGN = 162;
	int SL_ASSIGN = 163;
	int BAND_ASSIGN = 164;
	int BXOR_ASSIGN = 165;
	int BOR_ASSIGN = 166;
	int STAR_STAR_ASSIGN = 167;
	int INC = 168;
	int DEC = 169;
	int STAR_DOT = 170;
	int QUESTION_DOT = 171;
	int LAND = 172;
	int BXOR = 173;
	int REGEX_FIND = 174;
	int REGEX_MATCH = 175;
	int NOT_EQUAL = 176;
	int EQUAL = 177;
	int COMPARE_TO = 178;
	int LE = 179;
	int GE = 180;
	int LITERAL_instanceof = 181;
	int SL = 182;
	int RANGE_INCLUSIVE = 183;
	int DIV = 184;
	int MOD = 185;
	int STAR_STAR = 186;
	int DOLLAR = 187;
	int BNOT = 188;
	int LNOT = 189;
	int STRING_CTOR_START = 190;
	int STRING_CTOR_END = 191;
	int LITERAL_new = 192;
	int LITERAL_true = 193;
	int LITERAL_false = 194;
	int LITERAL_null = 195;
	int NUM_INT = 196;
	int NUM_FLOAT = 197;
	int NUM_LONG = 198;
	int NUM_DOUBLE = 199;
	int NUM_BIG_INT = 200;
	int NUM_BIG_DECIMAL = 201;
	int WS = 202;
	int ONE_NL = 203;
	int SL_COMMENT = 204;
	int ML_COMMENT = 205;
	int STRING_CH = 206;
	int ESC = 207;
	int HEX_DIGIT = 208;
	int VOCAB = 209;
	int LETTER = 210;
	int DIGIT = 211;
	int EXPONENT = 212;
	int FLOAT_SUFFIX = 213;
	int BIG_SUFFIX = 214;
}
