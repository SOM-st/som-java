/**
 * Copyright (c) 2017 Michael Haupt, github@haupz.de
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

import static som.interpreter.Bytecodes.DUP;
import static som.interpreter.Bytecodes.HALT;
import static som.interpreter.Bytecodes.POP;
import static som.interpreter.Bytecodes.POP_ARGUMENT;
import static som.interpreter.Bytecodes.POP_FIELD;
import static som.interpreter.Bytecodes.POP_LOCAL;
import static som.interpreter.Bytecodes.PUSH_ARGUMENT;
import static som.interpreter.Bytecodes.PUSH_BLOCK;
import static som.interpreter.Bytecodes.PUSH_CONSTANT;
import static som.interpreter.Bytecodes.PUSH_FIELD;
import static som.interpreter.Bytecodes.PUSH_GLOBAL;
import static som.interpreter.Bytecodes.PUSH_LOCAL;
import static som.interpreter.Bytecodes.RETURN_LOCAL;
import static som.interpreter.Bytecodes.RETURN_NON_LOCAL;
import static som.interpreter.Bytecodes.SEND;
import static som.interpreter.Bytecodes.SUPER_SEND;

import java.util.ArrayList;
import java.util.List;

import som.compiler.Parser.ParseError;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SInvokable;
import som.vmobjects.SMethod;
import som.vmobjects.SPrimitive;
import som.vmobjects.SSymbol;


public class MethodGenerationContext {

  private final ClassGenerationContext  holderGenc;
  private final MethodGenerationContext outerGenc;
  private final boolean                 blockMethod;

  private SSymbol                     signature;
  private final List<String>          arguments = new ArrayList<String>();
  private boolean                     primitive;
  private final List<String>          locals    = new ArrayList<String>();
  private final List<SAbstractObject> literals  = new ArrayList<SAbstractObject>();
  private boolean                     finished;
  private final ArrayList<Byte>       bytecode  = new ArrayList<>();

  /**
   * Constructor used for block methods.
   */
  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this.holderGenc = holderGenc;
    this.outerGenc = outerGenc;
    blockMethod = outerGenc != null;
  }

  /**
   * Constructor used for normal methods.
   */
  public MethodGenerationContext(final ClassGenerationContext holderGenc) {
    this(holderGenc, null);
  }

  public void addArgument(final String arg) throws ProgramDefinitionError {
    if (arguments.contains(arg)) {
      throw new ProgramDefinitionError(
          "A method cannot have two arguments with the same name `" + arg + "` in class "
              + holderGenc.getName().getEmbeddedString() + ".");
    }
    arguments.add(arg);
  }

  public boolean isPrimitive() {
    return primitive;
  }

  public SInvokable assemble(final Universe universe) {
    if (primitive) {
      return SPrimitive.getEmptyPrimitive(signature.getEmbeddedString(), universe);
    } else {
      return assembleMethod(universe);
    }
  }

  public SMethod assembleMethod(final Universe universe) {
    // create a method instance with the given number of bytecodes
    int numLocals = locals.size();

    SMethod meth = universe.newMethod(signature, bytecode.size(),
        numLocals, computeStackDepth(),
        literals);

    // copy bytecodes into method
    int i = 0;
    for (byte bc : bytecode) {
      meth.setBytecode(i++, bc);
    }

    // return the method - the holder field is to be set later on!
    return meth;
  }

  private int computeStackDepth() {
    int depth = 0;
    int maxDepth = 0;
    int i = 0;

    while (i < bytecode.size()) {
      switch (bytecode.get(i)) {
        case HALT:
          i++;
          break;
        case DUP:
          depth++;
          i++;
          break;
        case PUSH_LOCAL:
        case PUSH_ARGUMENT:
          depth++;
          i += 3;
          break;
        case PUSH_FIELD:
        case PUSH_BLOCK:
        case PUSH_CONSTANT:
        case PUSH_GLOBAL:
          depth++;
          i += 2;
          break;
        case POP:
          depth--;
          i++;
          break;
        case POP_LOCAL:
        case POP_ARGUMENT:
          depth--;
          i += 3;
          break;
        case POP_FIELD:
          depth--;
          i += 2;
          break;
        case SEND:
        case SUPER_SEND: {
          // these are special: they need to look at the number of
          // arguments (extractable from the signature)
          SSymbol sig = (SSymbol) literals.get(bytecode.get(i + 1));

          depth -= sig.getNumberOfSignatureArguments();

          depth++; // return value
          i += 2;
          break;
        }
        case RETURN_LOCAL:
        case RETURN_NON_LOCAL:
          i++;
          break;
        default:
          throw new IllegalStateException("Illegal bytecode "
              + bytecode.get(i));
      }

      if (depth > maxDepth) {
        maxDepth = depth;
      }
    }

    return maxDepth;
  }

  public void markAsPrimitive() {
    primitive = true;
  }

  public void setSignature(final SSymbol sig) {
    signature = sig;
  }

  public boolean isFinished() {
    return finished;
  }

  public void markAsFinished() {
    this.finished = false;
  }

  public boolean addLocalIfAbsent(final String local) {
    if (locals.contains(local)) {
      return false;
    }

    locals.add(local);
    return true;
  }

  public void addLocal(final String local) {
    locals.add(local);
  }

  public boolean hasBytecodes() {
    return !bytecode.isEmpty();
  }

  public void removeLastBytecode() {
    bytecode.remove(bytecode.size() - 1);
  }

  public boolean isBlockMethod() {
    return blockMethod;
  }

  public void setFinished() {
    finished = true;
  }

  public boolean addLiteralIfAbsent(final SAbstractObject lit, final Parser parser)
      throws ParseError {
    if (literals.contains(lit)) {
      return false;
    }

    addLiteral(lit, parser);
    return true;
  }

  public ClassGenerationContext getHolder() {
    return holderGenc;
  }

  public byte addLiteral(final SAbstractObject lit, final Parser parser) throws ParseError {
    int i = literals.size();
    if (i > Byte.MAX_VALUE) {
      String methodSignature = holderGenc.getName().getEmbeddedString() + ">>" + signature;
      throw new ParseError(
          "The method " + methodSignature + " has more than the supported " +
              Byte.MAX_VALUE
              + " literal values. Please split the method. The literal to be added is: " + lit,
          Symbol.NONE, parser);
    }
    literals.add(lit);
    return (byte) i;
  }

  public void updateLiteral(final SAbstractObject oldVal, final byte index,
      final SAbstractObject newVal) {
    assert literals.get(index) == oldVal;
    literals.set(index, newVal);
  }

  public boolean findVar(final String var, final Triplet<Byte, Byte, Boolean> tri) {
    // triplet: index, context, isArgument
    tri.setX((byte) locals.indexOf(var));
    if (tri.getX() == -1) {
      tri.setX((byte) arguments.indexOf(var));
      if (tri.getX() == -1) {
        if (outerGenc == null) {
          return false;
        } else {
          tri.setY((byte) (tri.getY() + 1));
          return outerGenc.findVar(var, tri);
        }
      } else {
        tri.setZ(true);
      }
    }

    return true;
  }

  public boolean hasField(final SSymbol field) {
    return holderGenc.hasField(field);
  }

  public byte getFieldIndex(final SSymbol field) {
    return holderGenc.getFieldIndex(field);
  }

  public int getNumberOfArguments() {
    return arguments.size();
  }

  public void addBytecode(final byte code) {
    bytecode.add(code);
  }

  public byte findLiteralIndex(final SAbstractObject lit) {
    return (byte) literals.indexOf(lit);
  }

  public MethodGenerationContext getOuter() {
    return outerGenc;
  }
}
