/**
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

package som.vmobjects;

import java.util.List;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.interpreter.Method;
import som.vm.Universe;


public class SMethod extends SAbstractObject implements SInvokable {

  private final @CompilationFinal(dimensions = 1) SClass[]          inlineCacheClass;
  private final @CompilationFinal(dimensions = 1) SInvokable[]      inlineCacheInvokable;
  private final @CompilationFinal(dimensions = 1) DirectCallNode[]  inlineCacheDirectCallNodes;
  private final @CompilationFinal(dimensions = 1) ValueProfile[]    receiverProfiles;
  private final @CompilationFinal(dimensions = 1) SAbstractObject[] literals;

  private final Method     method;
  private final CallTarget callTarget;
  private final SSymbol    signature;
  private SClass           holder;
  private final SInteger   numberOfLocals;
  private final SInteger   maximumNumberOfStackElements;

  public SMethod(final SObject nilObject, final SSymbol signature, final int numberOfBytecodes,
      final SInteger numberOfLocals, final SInteger maxNumStackElements,
      final int numberOfLiterals, final List<SAbstractObject> literals,
      final TruffleLanguage<?> language) {
    this.signature = signature;
    this.numberOfLocals = numberOfLocals;
    this.method = new Method(language, numberOfBytecodes, this);
    this.callTarget = Truffle.getRuntime().createCallTarget(method);
    inlineCacheClass = new SClass[numberOfBytecodes];
    inlineCacheInvokable = new SInvokable[numberOfBytecodes];
    inlineCacheDirectCallNodes = new DirectCallNode[numberOfBytecodes];
    receiverProfiles = new ValueProfile[numberOfBytecodes];
    maximumNumberOfStackElements = maxNumStackElements;
    this.literals = new SAbstractObject[numberOfLiterals];

    // copy literals into the method
    if (numberOfLiterals > 0) {
      int i = 0;
      for (SAbstractObject l : literals) {
        this.literals[i++] = l;
      }
    }
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  public SInteger getNumberOfLocals() {
    // Get the number of locals (converted to a Java integer)
    return numberOfLocals;
  }

  public SInteger getMaximumNumberOfStackElements() {
    // Get the maximum number of stack elements (converted to a Java
    // integer)
    return maximumNumberOfStackElements;
  }

  @Override
  public SSymbol getSignature() {
    return signature;
  }

  @Override
  public SClass getHolder() {
    // Get the holder of this method by reading the field with holder index
    return holder;
  }

  @Override
  public void setHolder(final SClass value) {
    holder = value;

    // Make sure all nested invokables have the same holder
    for (int i = 0; i < literals.length; i++) {
      if (literals[i] instanceof SInvokable) {
        ((SInvokable) literals[i]).setHolder(value);
      }
    }
  }

  public SAbstractObject getConstant(final int bytecodeIndex) {
    // Get the constant associated to a given bytecode index
    return literals[getBytecode(bytecodeIndex + 1)];
  }

  public int getNumberOfArguments() {
    // Get the number of arguments of this method
    return getSignature().getNumberOfSignatureArguments();
  }

  public int getNumberOfBytecodes() {
    // Get the number of bytecodes in this method
    return method.getNumberOfBytecodes();
  }

  public byte getBytecode(final int index) {
    // Get the bytecode at the given index
    return method.getBytecode(index);
  }

  public void setBytecode(final int index, final byte value) {
    // Set the bytecode at the given index to the given value
    method.setBytecode(index, value);
  }

  @Override
  public void indirectInvoke(final Frame frame, final Interpreter interpreter) {
    final Frame newFrame = interpreter.newFrame(frame, this, null);
    newFrame.copyArgumentsFrom(frame);

    IndirectCallNode indirectCallNode = interpreter.getIndirectCallNode();
    SAbstractObject result =
        (SAbstractObject) indirectCallNode.call(callTarget, interpreter, newFrame);

    frame.popArgumentsAndPushResult(result, this);
    newFrame.clearPreviousFrame();
  }

  public void directInvoke(final Frame frame, final Interpreter interpreter,
      DirectCallNode directCallNode) {

    CompilerAsserts.partialEvaluationConstant(directCallNode);

    final Frame newFrame = interpreter.newFrame(frame, this, null);
    newFrame.copyArgumentsFrom(frame);
    SAbstractObject result = (SAbstractObject) directCallNode.call(interpreter, newFrame);

    frame.popArgumentsAndPushResult(result, this);
    newFrame.clearPreviousFrame();
  }

  @Override
  public String toString() {
    return "Method(" + getHolder().getName().getEmbeddedString() + ">>"
        + getSignature().toString() + ")";
  }

  public SClass getInlineCacheClass(final int bytecodeIndex) {
    return inlineCacheClass[bytecodeIndex];
  }

  public SInvokable getInlineCacheInvokable(final int bytecodeIndex) {
    return inlineCacheInvokable[bytecodeIndex];
  }

  public DirectCallNode getInlineCacheDirectCallNode(final int bytecodeIndex) {
    CompilerAsserts.partialEvaluationConstant(bytecodeIndex);
    return inlineCacheDirectCallNodes[bytecodeIndex];
  }

  public void setReceiverProfile(final int bytecodeIndex,
      final ValueProfile valueProfile) {
    receiverProfiles[bytecodeIndex] = valueProfile;
  }

  public ValueProfile getReceiverProfile(final int bytecodeIndex) {
    return receiverProfiles[bytecodeIndex];
  }

  public void setInlineCache(final int bytecodeIndex, final SClass receiverClass,
      final SInvokable invokable) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    inlineCacheClass[bytecodeIndex] = receiverClass;
    inlineCacheInvokable[bytecodeIndex] = invokable;
    if (invokable != null) {
      if (invokable.isPrimitive()) {
        inlineCacheDirectCallNodes[bytecodeIndex] = null;
      } else {
        inlineCacheDirectCallNodes[bytecodeIndex] =
            DirectCallNode.create(((SMethod) invokable).getCallTarget());
      }
    }
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.methodClass;
  }

  public CallTarget getCallTarget() {
    return this.callTarget;
  }
}
