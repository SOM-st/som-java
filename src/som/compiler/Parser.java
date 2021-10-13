/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.compiler;

import static som.compiler.Symbol.And;
import static som.compiler.Symbol.Assign;
import static som.compiler.Symbol.At;
import static som.compiler.Symbol.Colon;
import static som.compiler.Symbol.Comma;
import static som.compiler.Symbol.Div;
import static som.compiler.Symbol.Double;
import static som.compiler.Symbol.EndBlock;
import static som.compiler.Symbol.EndTerm;
import static som.compiler.Symbol.Equal;
import static som.compiler.Symbol.Exit;
import static som.compiler.Symbol.Identifier;
import static som.compiler.Symbol.Integer;
import static som.compiler.Symbol.Keyword;
import static som.compiler.Symbol.KeywordSequence;
import static som.compiler.Symbol.Less;
import static som.compiler.Symbol.Minus;
import static som.compiler.Symbol.Mod;
import static som.compiler.Symbol.More;
import static som.compiler.Symbol.NONE;
import static som.compiler.Symbol.NewBlock;
import static som.compiler.Symbol.NewTerm;
import static som.compiler.Symbol.Not;
import static som.compiler.Symbol.OperatorSequence;
import static som.compiler.Symbol.Or;
import static som.compiler.Symbol.Per;
import static som.compiler.Symbol.Period;
import static som.compiler.Symbol.Plus;
import static som.compiler.Symbol.Pound;
import static som.compiler.Symbol.Primitive;
import static som.compiler.Symbol.STString;
import static som.compiler.Symbol.Separator;
import static som.compiler.Symbol.Star;

import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SInteger;
import som.vmobjects.SMethod;
import som.vmobjects.SString;
import som.vmobjects.SSymbol;


public class Parser {

  private final Universe               universe;
  private final String                 filename;
  private final ClassGenerationContext cgenc;

  private final Lexer lexer;

  private static final BytecodeGenerator bcGen = new BytecodeGenerator();;

  private Symbol sym;
  private String text;
  private Symbol nextSym;

  private static final List<Symbol> singleOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> binaryOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> keywordSelectorSyms = new ArrayList<Symbol>();

  static {
    for (Symbol s : new Symbol[] {Not, And, Or, Star, Div, Mod, Plus, Equal,
        More, Less, Comma, At, Per, NONE}) {
      singleOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Or, Comma, Minus, Equal, Not, And, Or, Star,
        Div, Mod, Plus, Equal, More, Less, Comma, At, Per, NONE}) {
      binaryOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Keyword, KeywordSequence}) {
      keywordSelectorSyms.add(s);
    }
  }

  public static class ParseError extends ProgramDefinitionError {
    private static final long serialVersionUID = 425390202979033628L;

    private final int line;
    private final int column;

    private final String text;
    private final String rawBuffer;
    private final String fileName;
    private final Symbol expected;
    private final Symbol found;

    ParseError(final String message, final Symbol expected, final Parser parser) {
      super(message);
      if (parser.lexer == null) {
        this.line = 0;
        this.column = 0;
        this.rawBuffer = "";
      } else {
        this.line = parser.lexer.getCurrentLineNumber();
        this.column = parser.lexer.getCurrentColumn();
        this.rawBuffer = parser.lexer.getRawBuffer();
      }
      this.text = parser.text;
      this.fileName = parser.filename;
      this.expected = expected;
      this.found = parser.sym;
    }

    protected String expectedSymbolAsString() {
      return expected.toString();
    }

    @Override
    public String getMessage() {
      String msg = super.getMessage();

      String foundStr;
      if (Parser.printableSymbol(found)) {
        foundStr = found + " (" + text + ")";
      } else {
        foundStr = found.toString();
      }
      String expectedStr = expectedSymbolAsString();

      msg = msg.replace("%(expected)s", expectedStr);
      msg = msg.replace("%(found)s", foundStr);

      return msg;
    }

    @Override
    public String toString() {
      String msg = "%(file)s:%(line)d:%(column)d: error: " + super.getMessage();
      String foundStr;
      if (Parser.printableSymbol(found)) {
        foundStr = found + " (" + text + ")";
      } else {
        foundStr = found.toString();
      }
      msg += ": " + rawBuffer;
      String expectedStr = expectedSymbolAsString();

      msg = msg.replace("%(file)s", fileName);
      msg = msg.replace("%(line)d", "" + line);
      msg = msg.replace("%(column)d", "" + column);
      msg = msg.replace("%(expected)s", expectedStr);
      msg = msg.replace("%(found)s", foundStr);
      return msg;
    }
  }

  public Parser(final Reader reader, final Universe universe, final String filename) {
    this.universe = universe;
    this.filename = filename;
    this.cgenc = new ClassGenerationContext(universe);

    sym = NONE;
    lexer = new Lexer(reader);
    nextSym = NONE;
    getSymbolFromLexer();
  }

  @Override
  public String toString() {
    return filename + ":" + lexer.getCurrentLineNumber() + ":" + lexer.getCurrentColumn();
  }

  public ClassGenerationContext classdef() throws ProgramDefinitionError {
    cgenc.setName(universe.symbolFor(text));
    expect(Identifier);
    expect(Equal);

    superclass();

    expect(NewTerm);
    classBody();

    if (accept(Separator)) {
      cgenc.startClassSide();
      classBody();
    }
    expect(EndTerm);

    return cgenc;
  }

  private void classBody() throws ProgramDefinitionError {
    fields();
    while (symIsMethod()) {
      MethodGenerationContext mgenc = new MethodGenerationContext(cgenc);
      mgenc.addArgument("self");

      method(mgenc);
      cgenc.addMethod(mgenc.assemble(universe), this);
    }
  }

  private boolean symIsMethod() {
    return sym == Identifier || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms);
  }

  private void superclass() throws ProgramDefinitionError {
    SSymbol superName;
    if (sym == Identifier) {
      superName = universe.symbolFor(text);
      accept(Identifier);
    } else {
      superName = universe.symbolFor("Object");
    }
    cgenc.setSuperName(superName);

    // Load the super class, if it is not nil (break the dependency cycle)
    if (!superName.getEmbeddedString().equals("nil")) {
      initalizeFromSuperClass(superName);
    }
  }

  private void initalizeFromSuperClass(final SSymbol superName)
      throws ProgramDefinitionError, ParseError {
    SClass superClass = universe.loadClass(superName);
    if (superClass == null) {
      throw new ParseError(
          "Was not able to load super class: " + superName.getEmbeddedString(),
          Symbol.NONE, this);
    }
    cgenc.setInstanceFieldsOfSuper(superClass.getInstanceFields());
    cgenc.setClassFieldsOfSuper(superClass.getSOMClass().getInstanceFields());
  }

  private boolean symIn(final List<Symbol> ss) {
    return ss.contains(sym);
  }

  private boolean accept(final Symbol s) {
    if (sym == s) {
      getSymbolFromLexer();
      return true;
    }
    return false;
  }

  private boolean acceptOneOf(final List<Symbol> ss) {
    if (symIn(ss)) {
      getSymbolFromLexer();
      return true;
    }
    return false;
  }

  private boolean expect(final Symbol s) {
    if (accept(s)) {
      return true;
    }
    StringBuilder err = new StringBuilder("Error: " + filename + ":" +
        lexer.getCurrentLineNumber() +
        ": unexpected symbol, expected: " + s.toString()
        + ", but found: " + sym.toString());
    if (printableSymbol(sym)) {
      err.append(" (" + text + ")");
    }
    err.append(": " + lexer.getRawBuffer());
    throw new IllegalStateException(err.toString());
  }

  private boolean expectOneOf(final List<Symbol> ss) {
    if (acceptOneOf(ss)) {
      return true;
    }
    StringBuilder err = new StringBuilder("Error: " + filename + ":" +
        lexer.getCurrentLineNumber() + ": unexpected symbol, expected one of: ");
    for (Symbol s : ss) {
      err.append(s.toString() + ", ");
    }
    err.append("but found: " + sym.toString());
    if (printableSymbol(sym)) {
      err.append(" (" + text + ")");
    }
    err.append(": " + lexer.getRawBuffer());
    throw new IllegalStateException(err.toString());
  }

  private void fields() {
    if (accept(Or)) {
      while (sym == Identifier) {
        String var = variable();
        cgenc.addField(universe.symbolFor(var));
      }
      expect(Or);
    }
  }

  private void method(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    pattern(mgenc);
    expect(Equal);
    if (sym == Primitive) {
      mgenc.markAsPrimitive();
      primitiveBlock();
    } else {
      methodBlock(mgenc);
    }
  }

  private void primitiveBlock() {
    expect(Primitive);
  }

  private void pattern(final MethodGenerationContext mgenc) {
    switch (sym) {
      case Identifier:
        unaryPattern(mgenc);
        break;
      case Keyword:
        keywordPattern(mgenc);
        break;
      default:
        binaryPattern(mgenc);
        break;
    }
  }

  private void unaryPattern(final MethodGenerationContext mgenc) {
    mgenc.setSignature(unarySelector());
  }

  private void binaryPattern(final MethodGenerationContext mgenc) {
    mgenc.setSignature(binarySelector());
    mgenc.addArgumentIfAbsent(argument());
  }

  private void keywordPattern(final MethodGenerationContext mgenc) {
    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keyword());
      mgenc.addArgumentIfAbsent(argument());
    } while (sym == Keyword);

    mgenc.setSignature(universe.symbolFor(kw.toString()));
  }

  private void methodBlock(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    expect(NewTerm);
    blockContents(mgenc);
    // if no return has been generated so far, we can be sure there was no .
    // terminating the last expression, so the last expression's value must
    // be popped off the stack and a ^self be generated
    if (!mgenc.isFinished()) {
      bcGen.emitPOP(mgenc);
      bcGen.emitPUSHARGUMENT(mgenc, (byte) 0, (byte) 0);
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.setFinished();
    }

    expect(EndTerm);
  }

  private SSymbol unarySelector() {
    return universe.symbolFor(identifier());
  }

  private SSymbol binarySelector() {
    String s = text;

    // Checkstyle: stop @formatter:off
    if (accept(Or)) {
    } else if (accept(Comma)) {
    } else if (accept(Minus)) {
    } else if (accept(Equal)) {
    } else if (acceptOneOf(singleOpSyms)) {
    } else if (accept(OperatorSequence)) {
    } else { expect(NONE); }
    // Checkstyle: resume @formatter:on

    return universe.symbolFor(s);
  }

  private String identifier() {
    String s = text;
    boolean isPrimitive = accept(Primitive);
    if (!isPrimitive) {
      expect(Identifier);
    }
    return s;
  }

  private String keyword() {
    String s = text;
    expect(Keyword);

    return s;
  }

  private String argument() {
    return variable();
  }

  private void blockContents(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    if (accept(Or)) {
      locals(mgenc);
      expect(Or);
    }
    blockBody(mgenc, false);
  }

  private void locals(final MethodGenerationContext mgenc) {
    while (sym == Identifier) {
      mgenc.addLocalIfAbsent(variable());
    }
  }

  private void blockBody(final MethodGenerationContext mgenc, final boolean seenPeriod)
      throws ProgramDefinitionError {
    if (accept(Exit)) {
      result(mgenc);
    } else if (sym == EndBlock) {
      if (seenPeriod) {
        // a POP has been generated which must be elided (blocks always
        // return the value of the last expression, regardless of
        // whether it
        // was terminated with a . or not)
        mgenc.removeLastBytecode();
      }
      if (mgenc.isBlockMethod() && !mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        SSymbol nilSym = universe.symbolFor("nil");
        mgenc.addLiteralIfAbsent(nilSym, this);
        bcGen.emitPUSHGLOBAL(mgenc, nilSym);
      }
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.setFinished();
    } else if (sym == EndTerm) {
      // it does not matter whether a period has been seen, as the end of
      // the method has been found (EndTerm) - so it is safe to emit a "return
      // self"
      bcGen.emitPUSHARGUMENT(mgenc, (byte) 0, (byte) 0);
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.setFinished();
    } else {
      expression(mgenc);
      if (accept(Period)) {
        bcGen.emitPOP(mgenc);
        blockBody(mgenc, true);
      }
    }
  }

  private void result(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    expression(mgenc);

    if (mgenc.isBlockMethod()) {
      bcGen.emitRETURNNONLOCAL(mgenc);
    } else {
      bcGen.emitRETURNLOCAL(mgenc);
    }

    mgenc.markAsFinished();
    accept(Period);
  }

  private void expression(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    peekForNextSymbolFromLexer();

    if (nextSym == Assign) {
      assignation(mgenc);
    } else {
      evaluation(mgenc);
    }
  }

  private void assignation(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    List<String> l = new ArrayList<String>();

    assignments(mgenc, l);
    evaluation(mgenc);

    for (int i = 1; i <= l.size(); i++) {
      bcGen.emitDUP(mgenc);
    }
    for (String s : l) {
      genPopVariable(mgenc, s);
    }
  }

  private void assignments(final MethodGenerationContext mgenc, final List<String> l)
      throws ParseError {
    if (sym == Identifier) {
      l.add(assignment(mgenc));
      peekForNextSymbolFromLexer();
      if (nextSym == Assign) {
        assignments(mgenc, l);
      }
    }
  }

  private String assignment(final MethodGenerationContext mgenc) throws ParseError {
    String v = variable();
    expect(Assign);
    return v;
  }

  private void evaluation(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    boolean superSend = primary(mgenc);
    if (symIsMethod()) {
      messages(mgenc, superSend);
    }
  }

  private boolean primary(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    boolean superSend = false;
    switch (sym) {
      case Identifier: {
        String v = variable();
        if (v.equals("super")) {
          superSend = true;
          // sends to super push self as the receiver
          v = "self";
        }

        genPushVariable(mgenc, v);
        break;
      }
      case NewTerm:
        nestedTerm(mgenc);
        break;
      case NewBlock: {
        MethodGenerationContext bgenc = new MethodGenerationContext(mgenc.getHolder(), mgenc);
        nestedBlock(bgenc);

        SMethod blockMethod = bgenc.assembleMethod(universe);
        mgenc.addLiteral(blockMethod, this);
        bcGen.emitPUSHBLOCK(mgenc, blockMethod);
        break;
      }
      default:
        literal(mgenc);
        break;
    }
    return superSend;
  }

  private String variable() {
    return identifier();
  }

  private void messages(final MethodGenerationContext mgenc, boolean superSend)
      throws ProgramDefinitionError {
    if (sym == Identifier) {
      do {
        // only the first message in a sequence can be a super send
        unaryMessage(mgenc, superSend);
        superSend = false;
      } while (sym == Identifier);

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        binaryMessage(mgenc, false);
      }

      if (sym == Keyword) {
        keywordMessage(mgenc, false);
      }
    } else if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      do {
        // only the first message in a sequence can be a super send
        binaryMessage(mgenc, superSend);
        superSend = false;
      } while (sym == OperatorSequence || symIn(binaryOpSyms));

      if (sym == Keyword) {
        keywordMessage(mgenc, false);
      }
    } else {
      keywordMessage(mgenc, superSend);
    }
  }

  private void unaryMessage(final MethodGenerationContext mgenc,
      final boolean superSend) throws ParseError {
    SSymbol msg = unarySelector();
    mgenc.addLiteralIfAbsent(msg, this);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private void binaryMessage(final MethodGenerationContext mgenc,
      final boolean superSend) throws ProgramDefinitionError {
    SSymbol msg = binarySelector();
    mgenc.addLiteralIfAbsent(msg, this);

    binaryOperand(mgenc);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private boolean binaryOperand(final MethodGenerationContext mgenc)
      throws ProgramDefinitionError {
    boolean superSend = primary(mgenc);

    while (sym == Identifier) {
      unaryMessage(mgenc, superSend);
      superSend = false;
    }

    return superSend;
  }

  private void keywordMessage(final MethodGenerationContext mgenc,
      final boolean superSend) throws ProgramDefinitionError {
    StringBuilder kw = new StringBuilder();
    do {
      kw.append(keyword());
      formula(mgenc);
    } while (sym == Keyword);

    SSymbol msg = universe.symbolFor(kw.toString());

    mgenc.addLiteralIfAbsent(msg, this);

    if (superSend) {
      bcGen.emitSUPERSEND(mgenc, msg);
    } else {
      bcGen.emitSEND(mgenc, msg);
    }
  }

  private void formula(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    boolean superSend = binaryOperand(mgenc);

    // only the first message in a sequence can be a super send
    if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc, superSend);
    }

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      binaryMessage(mgenc, false);
    }
  }

  private void nestedTerm(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    expect(NewTerm);
    expression(mgenc);
    expect(EndTerm);
  }

  private void literal(final MethodGenerationContext mgenc) throws ParseError {
    switch (sym) {
      case Pound: {
        peekForNextSymbolFromLexerIfNecessary();
        if (nextSym == NewTerm) {
          literalArray(mgenc);
        } else {
          literalSymbol(mgenc);
        }
        break;
      }
      case STString: {
        literalString(mgenc);
        break;
      }
      default: {
        literalNumber(mgenc);
        break;
      }
    }
  }

  private void literalNumber(final MethodGenerationContext mgenc) throws ParseError {
    SAbstractObject lit;

    if (sym == Minus) {
      lit = negativeDecimal();
    } else {
      lit = literalDecimal(false);
    }
    mgenc.addLiteralIfAbsent(lit, this);
    bcGen.emitPUSHCONSTANT(mgenc, lit);
  }

  private SAbstractObject literalDecimal(final boolean isNegative) throws ParseError {
    if (sym == Integer) {
      return literalInteger(isNegative);
    } else {
      return literalDouble(isNegative);
    }
  }

  private SAbstractObject negativeDecimal() throws ParseError {
    expect(Minus);
    return literalDecimal(true);
  }

  private SAbstractObject literalInteger(final boolean isNegative) {
    try {
      long i = Long.parseLong(text);
      if (isNegative) {
        i = 0 - i;
      }
      expect(Integer);
      return universe.newInteger(i);
    } catch (NumberFormatException first) {
      try {
        BigInteger big = new BigInteger(text);
        if (isNegative) {
          big = big.negate();
        }
        expect(Integer);
        return universe.newBigInteger(big);
      } catch (NumberFormatException e) {
        String err = "Error: " + filename + ":" +
            lexer.getCurrentLineNumber() +
            ": parsing number literal failed: '" + text.toString()
            + "'";
        throw new IllegalStateException(err);
      }
    }
  }

  private SAbstractObject literalDouble(final boolean isNegative) throws ParseError {
    try {
      double d = java.lang.Double.parseDouble(text);
      if (isNegative) {
        d = 0.0 - d;
      }
      expect(Double);
      return universe.newDouble(d);
    } catch (NumberFormatException e) {
      throw new ParseError("Could not parse double. Expected a number but " +
          "got '" + text + "'", NONE, this);
    }
  }

  private void literalSymbol(final MethodGenerationContext mgenc) throws ParseError {
    SSymbol symb;
    expect(Pound);
    if (sym == STString) {
      String s = string();
      symb = universe.symbolFor(s);
    } else {
      symb = selector();
    }

    mgenc.addLiteralIfAbsent(symb, this);
    bcGen.emitPUSHCONSTANT(mgenc, symb);
  }

  private void literalString(final MethodGenerationContext mgenc) throws ParseError {
    String s = string();

    SString str = universe.newString(s);
    mgenc.addLiteralIfAbsent(str, this);

    bcGen.emitPUSHCONSTANT(mgenc, str);
  }

  private void literalArray(final MethodGenerationContext mgenc) throws ParseError {
    expect(Pound);
    expect(NewTerm);

    SSymbol arrayClassName = universe.symbolFor("Array");
    SSymbol arraySizePlaceholder = universe.symbolFor("ArraySizeLiteralPlaceholder");
    SSymbol newMessage = universe.symbolFor("new:");
    SSymbol atPutMessage = universe.symbolFor("at:put:");

    mgenc.addLiteralIfAbsent(arrayClassName, this);
    mgenc.addLiteralIfAbsent(newMessage, this);
    mgenc.addLiteralIfAbsent(atPutMessage, this);
    final byte arraySizeLiteralIndex = mgenc.addLiteral(arraySizePlaceholder, this);

    // create empty array
    bcGen.emitPUSHGLOBAL(mgenc, arrayClassName);
    bcGen.emitPUSHCONSTANT(mgenc, arraySizeLiteralIndex);
    bcGen.emitSEND(mgenc, newMessage);

    int i = 1;

    while (sym != EndTerm) {
      SInteger pushIndex = universe.newInteger(i);
      mgenc.addLiteralIfAbsent(pushIndex, this);
      bcGen.emitPUSHCONSTANT(mgenc, pushIndex);
      literal(mgenc);
      bcGen.emitSEND(mgenc, atPutMessage);
      i += 1;
    }

    // replace the placeholder with the actual array size
    mgenc.updateLiteral(
        arraySizePlaceholder, arraySizeLiteralIndex, universe.newInteger(i - 1));
    expect(EndTerm);
  }

  private SSymbol selector() {
    if (sym == OperatorSequence || symIn(singleOpSyms)) {
      return binarySelector();
    } else if (sym == Keyword || sym == KeywordSequence) {
      return keywordSelector();
    } else {
      return unarySelector();
    }
  }

  private SSymbol keywordSelector() {
    String s = new String(text);
    expectOneOf(keywordSelectorSyms);
    SSymbol symb = universe.symbolFor(s);
    return symb;
  }

  private String string() {
    String s = new String(text);
    expect(STString);
    return s;
  }

  private void nestedBlock(final MethodGenerationContext mgenc) throws ProgramDefinitionError {
    mgenc.addArgumentIfAbsent("$block self");

    expect(NewBlock);
    if (sym == Colon) {
      blockPattern(mgenc);
    }

    // generate Block signature
    String blockSig = "$block method";
    int argSize = mgenc.getNumberOfArguments();
    for (int i = 1; i < argSize; i++) {
      blockSig += ":";
    }

    mgenc.setSignature(universe.symbolFor(blockSig));

    blockContents(mgenc);

    // if no return has been generated, we can be sure that the last
    // expression
    // in the block was not terminated by ., and can generate a return
    if (!mgenc.isFinished()) {
      if (!mgenc.hasBytecodes()) {
        // if the block is empty, we need to return nil
        SSymbol nilSym = universe.symbolFor("nil");
        mgenc.addLiteralIfAbsent(nilSym, this);
        bcGen.emitPUSHGLOBAL(mgenc, nilSym);
      }
      bcGen.emitRETURNLOCAL(mgenc);
      mgenc.markAsFinished();
    }

    expect(EndBlock);
  }

  private void blockPattern(final MethodGenerationContext mgenc) {
    blockArguments(mgenc);
    expect(Or);
  }

  private void blockArguments(final MethodGenerationContext mgenc) {
    do {
      expect(Colon);
      mgenc.addArgumentIfAbsent(argument());
    } while (sym == Colon);
  }

  private void genPushVariable(final MethodGenerationContext mgenc,
      final String var) throws ParseError {
    // The purpose of this function is to find out whether the variable to be
    // pushed on the stack is a local variable, argument, or object field.
    // This is done by examining all available lexical contexts, starting with
    // the innermost (i.e., the one represented by mgenc).

    // triplet: index, context, isArgument
    Triplet<Byte, Byte, Boolean> tri = new Triplet<Byte, Byte, Boolean>(
        (byte) 0, (byte) 0, false);

    if (mgenc.findVar(var, tri)) {
      if (tri.getZ()) {
        bcGen.emitPUSHARGUMENT(mgenc, tri.getX(), tri.getY());
      } else {
        bcGen.emitPUSHLOCAL(mgenc, tri.getX(), tri.getY());
      }
    } else {
      SSymbol identifier = universe.symbolFor(var);
      if (mgenc.hasField(identifier)) {
        SSymbol fieldName = identifier;
        mgenc.addLiteralIfAbsent(fieldName, this);
        bcGen.emitPUSHFIELD(mgenc, fieldName);
      } else {
        SSymbol global = identifier;
        mgenc.addLiteralIfAbsent(global, this);
        bcGen.emitPUSHGLOBAL(mgenc, global);
      }
    }
  }

  private void genPopVariable(final MethodGenerationContext mgenc,
      final String var) throws ParseError {
    // The purpose of this function is to find out whether the variable to be
    // popped off the stack is a local variable, argument, or object field.
    // This is done by examining all available lexical contexts, starting with
    // the innermost (i.e., the one represented by mgenc).

    // triplet: index, context, isArgument
    Triplet<Byte, Byte, Boolean> tri = new Triplet<Byte, Byte, Boolean>(
        (byte) 0, (byte) 0, false);

    if (mgenc.findVar(var, tri)) {
      if (tri.getZ()) {
        bcGen.emitPOPARGUMENT(mgenc, tri.getX(), tri.getY());
      } else {
        bcGen.emitPOPLOCAL(mgenc, tri.getX(), tri.getY());
      }
    } else {
      SSymbol varName = universe.symbolFor(var);
      if (!mgenc.hasField(varName)) {
        throw new ParseError("Trying to write to field with the name '" + var
            + "', but field does not seem exist in class.", Symbol.NONE, this);
      }
      bcGen.emitPOPFIELD(mgenc, varName);
    }
  }

  private void getSymbolFromLexer() {
    sym = lexer.getSym();
    text = lexer.getText();
  }

  private void peekForNextSymbolFromLexerIfNecessary() {
    if (!lexer.getPeekDone()) {
      peekForNextSymbolFromLexer();
    }
  }

  private void peekForNextSymbolFromLexer() {
    nextSym = lexer.peek();
  }

  private static boolean printableSymbol(final Symbol sym) {
    return sym == Integer || sym == Double || sym.compareTo(STString) >= 0;
  }
}
