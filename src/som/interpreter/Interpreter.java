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

package som.interpreter;

import static som.interpreter.Bytecodes.*;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.compiler.ProgramDefinitionError;
import som.vm.Universe;
import som.vmobjects.*;


public class Interpreter {

  private final Universe         universe;
  private final IndirectCallNode indirectCallNode =
      Truffle.getRuntime().createIndirectCallNode();

  public Interpreter(final Universe universe) {
    this.universe = universe;
  }

  private void doDup(final Frame frame, final VirtualFrame truffleFrame, final SMethod method)
      throws FrameSlotTypeException {

    SAbstractObject value = frame.getStackElement(0);
    SAbstractObject valueT = StackUtils.getRelativeStackElement(truffleFrame, 0);

    assert value.equals(valueT) : "values differ";

    frame.push(value);
    StackUtils.push(truffleFrame, valueT);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void doPushLocal(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {
    // Handle the PUSH LOCAL bytecode
    SAbstractObject value = frame.getLocal(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2));

    SAbstractObject valueT = StackUtils.getLocal(truffleFrame,
        method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2));

    frame.push(value);
    StackUtils.push(truffleFrame, valueT);

    assert value == valueT : "values differ";

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void doPushArgument(final int bytecodeIndex, final VirtualFrame truffleFrame,
      Frame frame,
      final SMethod method) throws FrameSlotTypeException {
    SAbstractObject valueT =
        StackUtils.getArgumentFromStack(truffleFrame, method.getBytecode(bytecodeIndex + 1),
            method.getBytecode(bytecodeIndex + 2));

    SAbstractObject value = frame.getArgument(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2));

    assert value == valueT : "values differ";

    StackUtils.push(truffleFrame, valueT);
    frame.push(value);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void doPushField(final int bytecodeIndex, final Frame frame,
      final SMethod method, final VirtualFrame truffleFrame) throws FrameSlotTypeException {
    // Handle the PUSH FIELD bytecode
    int fieldIndex = method.getBytecode(bytecodeIndex + 1);

    // Push the field with the computed index onto the stack
    SAbstractObject value = ((SObject) getSelf(frame, truffleFrame)).getField(fieldIndex);
    frame.push(value);
    StackUtils.push(truffleFrame, value);
  }

  private void doPushBlock(final int bytecodeIndex, final Frame frame,
      final SMethod method, final VirtualFrame truffleFrame)
      throws ProgramDefinitionError, FrameSlotTypeException {

    SMethod blockMethod = (SMethod) method.getConstant(bytecodeIndex);
    SBlock block = universe.newBlock(blockMethod, frame,
        blockMethod.getNumberOfArguments(), truffleFrame.materialize());

    // TODO - change block creation to stop relying on SOM Frames
    StackUtils.push(truffleFrame, block);
    frame.push(block);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void doPushConstant(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {

    StackUtils.push(truffleFrame,
        method.getConstant(bytecodeIndex));

    frame.push(method.getConstant(bytecodeIndex));

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void doPushGlobal(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {

    SSymbol globalName = (SSymbol) method.getConstant(bytecodeIndex);

    SAbstractObject global = universe.getGlobal(globalName);

    if (global != null) {
      // Push the global onto the stack
      StackUtils.push(truffleFrame, global);
      frame.push(global);
      assert StackUtils.areStackEqual(truffleFrame,
          frame) : "Stacks are different";
      assert StackUtils.getCurrentStackPointer(
          truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
    } else {
      // Send 'unknownGlobal:' to self
      // TODO - reconsider
      CompilerDirectives.transferToInterpreter();
      getSelf(frame, truffleFrame).sendUnknownGlobal(globalName, universe, this, frame,
          truffleFrame);
    }
  }

  private void doPop(final Frame frame, final VirtualFrame truffleFrame, final SMethod method)
      throws FrameSlotTypeException {

    frame.pop();
    StackUtils.pop(truffleFrame);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stack are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";

  }

  private void doPopLocal(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {

    SAbstractObject value = frame.pop();
    SAbstractObject valueT = StackUtils.pop(truffleFrame);

    assert value == valueT : "values differ";

    frame.setLocal(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2), value);

    StackUtils.setLocal(truffleFrame,
        method.getBytecode(bytecodeIndex + 1), method.getBytecode(bytecodeIndex + 2),
        valueT);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";

  }

  private void doPopArgument(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {

    SAbstractObject value = frame.pop();
    SAbstractObject valueT = StackUtils.pop(truffleFrame);

    assert value == valueT : "values differ";

    StackUtils.setArgument(truffleFrame,
        method.getBytecode(bytecodeIndex + 1), method.getBytecode(bytecodeIndex + 2),
        valueT);

    frame.setArgument(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2), value);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";

  }

  private void doPopField(final int bytecodeIndex, final Frame frame,
      final SMethod method, final VirtualFrame truffleFrame) throws FrameSlotTypeException {
    // Handle the POP FIELD bytecode
    int fieldIndex = method.getBytecode(bytecodeIndex + 1);

    // Set the field with the computed index to the value popped from the stack
    ((SObject) getSelf(frame, truffleFrame)).setField(fieldIndex, frame.pop());
    StackUtils.pop(truffleFrame);
  }

  private void doSuperSend(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {
    // Handle the SUPER SEND bytecode
    SSymbol signature = (SSymbol) method.getConstant(bytecodeIndex);

    // Send the message
    // Lookup the invokable with the given signature
    SClass holderSuper = (SClass) method.getHolder().getSuperClass();
    SInvokable invokable = holderSuper.lookupInvokable(signature);

    if (invokable != null) {
      // Invoke the invokable in the current frame
      invokable.indirectInvoke(frame, truffleFrame, this);
    } else {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      // Compute the number of arguments
      int numberOfArguments = signature.getNumberOfSignatureArguments();

      // Compute the receiver
      SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);

      receiver.sendDoesNotUnderstand(signature, universe, this, frame, truffleFrame);
    }
  }

  private SAbstractObject doReturnLocal(final Frame frame, final VirtualFrame truffleFrame)
      throws FrameSlotTypeException {
    SAbstractObject value = frame.pop();
    SAbstractObject valueT = StackUtils.pop(truffleFrame);

    assert value == valueT : "values differ";

    return valueT;
  }

  private SAbstractObject doReturnNonLocal(final Frame frame, final VirtualFrame truffleFrame,
      final SMethod method)
      throws ReturnException, FrameSlotTypeException {

    SAbstractObject value = frame.pop();
    SAbstractObject valueT = StackUtils.pop(truffleFrame);

    assert value == valueT : "values differ";

    Frame context = frame.getOuterContext(universe.nilObject);

    boolean notOnStackT = !StackUtils.getCurrentOnStackMarker(truffleFrame);
    boolean notOnStack = !context.hasPreviousFrame(universe.nilObject);

    // assert notOnStack == notOnStackT : "the on stack markers differ";

    // TODO - implement properly the on stack marker
    // Make sure the block context is still on the stack
    if (notOnStack) {
      // Try to recover by sending 'escapedBlock:' to the sending object
      // this can get a bit nasty when using nested blocks. In this case
      // the "sender" will be the surrounding block and not the object
      // that actually sent the 'value' message.
      SBlock blockT = (SBlock) StackUtils.getAllArgumentsFrom(truffleFrame)[0];
      SBlock block = (SBlock) frame.getArgument(0, 0);

      assert block.equals(blockT) : "the 2 blocks differ";

      // TODO - get the sender from truffle frame
      SAbstractObject sender =
          frame.getPreviousFrame().getOuterContext(universe.nilObject).getArgument(0, 0);

      // TODO - check the frame and its stack
      // ... and execute the escapedBlock message instead
      sender.sendEscapedBlock(block, universe, this, frame, truffleFrame);
      return frame.pop();
    }

    // throw the exception to pass around the context and pop the right frames
    throw new ReturnException(valueT, context);
  }

  private void doSend(final int bytecodeIndex, final Frame frame,
      final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {
    SSymbol signature = (SSymbol) method.getConstant(bytecodeIndex);

    int numberOfArguments = signature.getNumberOfSignatureArguments();
    SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);
    SAbstractObject receiver2 = StackUtils.getRelativeStackElement(truffleFrame,
        numberOfArguments - 1);

    assert receiver.equals(receiver2) : "the receivers are different";

    ValueProfile receiverClassValueProfile = method.getReceiverProfile(bytecodeIndex);
    if (receiverClassValueProfile == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      receiverClassValueProfile = ValueProfile.createClassProfile();
      method.setReceiverProfile(bytecodeIndex, receiverClassValueProfile);
    }

    send(signature,
        receiverClassValueProfile.profile(receiver).getSOMClass(universe),
        bytecodeIndex, frame, truffleFrame, method);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stack are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
  public SAbstractObject start(final Frame frame, final VirtualFrame truffleFrame,
      final SMethod method)
      throws ReturnException, ProgramDefinitionError, FrameSlotTypeException {

    int bytecodeIndex = 0;

    while (true) {

      assert StackUtils.areStackEqual(truffleFrame,
          frame) : "Stack are different";
      assert StackUtils.getCurrentStackPointer(
          truffleFrame) == frame.getStackPointer() : "Stack pointers differ";

      int currentBytecodeIndex = bytecodeIndex;
      byte bytecode = method.getBytecode(currentBytecodeIndex);

      // Compute the next bytecode index
      int bytecodeLength = getBytecodeLength(bytecode);
      bytecodeIndex = currentBytecodeIndex + bytecodeLength;

      CompilerAsserts.partialEvaluationConstant(bytecodeIndex);
      CompilerAsserts.partialEvaluationConstant(currentBytecodeIndex);
      CompilerAsserts.partialEvaluationConstant(method);
      CompilerAsserts.partialEvaluationConstant(bytecode);

      // Handle the current bytecode
      switch (bytecode) {
        case HALT: {
          SAbstractObject resultT = StackUtils.getRelativeStackElement(truffleFrame, 0);
          SAbstractObject result = frame.getStackElement(0);

          assert result.equals(resultT) : "final return values differ";
          return resultT;
        }

        case DUP: {
          doDup(frame, truffleFrame, method);
          break;
        }

        case PUSH_LOCAL: {
          doPushLocal(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case PUSH_ARGUMENT: {
          doPushArgument(currentBytecodeIndex, truffleFrame, frame, method);
          break;
        }

        case PUSH_FIELD: {
          doPushField(currentBytecodeIndex, frame, method, truffleFrame);
          break;
        }

        case PUSH_BLOCK: {
          doPushBlock(currentBytecodeIndex, frame, method, truffleFrame);
          break;
        }

        case PUSH_CONSTANT: {
          doPushConstant(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case PUSH_GLOBAL: {
          doPushGlobal(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case POP: {
          doPop(frame, truffleFrame, method);
          break;
        }

        case POP_LOCAL: {
          doPopLocal(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case POP_ARGUMENT: {
          doPopArgument(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case POP_FIELD: {
          doPopField(currentBytecodeIndex, frame, method, truffleFrame);
          break;
        }

        case SEND: {
          doSend(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case SUPER_SEND: {
          doSuperSend(currentBytecodeIndex, frame, truffleFrame, method);
          break;
        }

        case RETURN_LOCAL: {
          return doReturnLocal(frame, truffleFrame);
        }

        case RETURN_NON_LOCAL: {
          return doReturnNonLocal(frame, truffleFrame, method);
        }

        default:
          Universe.errorPrintln("Nasty bug in interpreter");
          break;
      }
      assert StackUtils.areStackEqual(truffleFrame,
          frame) : "Stack are different";
      assert StackUtils.getCurrentStackPointer(
          truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
    }
  }

  public SAbstractObject getSelf(final Frame frame, final VirtualFrame truffleFrame)
      throws FrameSlotTypeException {
    // Get the self object from the interpreter
    Frame outerContext = frame.getOuterContext(universe.nilObject);
    SAbstractObject result = outerContext.getArgument(0, 0);

    VirtualFrame outerContextT = StackUtils.getOuterContext(truffleFrame);
    SAbstractObject resultT = StackUtils.getArgument(outerContextT, 0, 0);

    assert StackUtils.areStackEqual(outerContextT, outerContext);
    assert result == resultT;

    return result;
  }

  private void send(final SSymbol selector, final SClass receiverClass,
      final int bytecodeIndex, final Frame frame, final VirtualFrame truffleFrame,
      final SMethod method) throws FrameSlotTypeException {
    // First try the inline cache
    SInvokable invokableWithoutCacheHit = null;

    SClass cachedClass = method.getInlineCacheClass(bytecodeIndex);
    if (cachedClass == receiverClass) {
      SInvokable invokable = method.getInlineCacheInvokable(bytecodeIndex);
      if (invokable != null) {
        DirectCallNode invokableDirectCallNode =
            method.getInlineCacheDirectCallNode(bytecodeIndex);
        CompilerAsserts.partialEvaluationConstant(invokableDirectCallNode);
        invokable.directInvoke(frame, truffleFrame, this, invokableDirectCallNode);
        return;
      }
    } else {
      if (cachedClass == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // Lookup the invokable with the given signature
        invokableWithoutCacheHit = receiverClass.lookupInvokable(selector);
        method.setInlineCache(bytecodeIndex, receiverClass, invokableWithoutCacheHit);
      } else {
        // the bytecode index after the send is used by the selector constant, and can be used
        // safely as another cache item
        cachedClass = method.getInlineCacheClass(bytecodeIndex + 1);
        if (cachedClass == receiverClass) {
          SInvokable invokable = method.getInlineCacheInvokable(bytecodeIndex + 1);
          if (invokable != null) {
            DirectCallNode invokableDirectCallNode =
                method.getInlineCacheDirectCallNode(bytecodeIndex + 1);
            CompilerAsserts.partialEvaluationConstant(invokableDirectCallNode);
            invokable.directInvoke(frame, truffleFrame, this, invokableDirectCallNode);
            return;
          }
        } else {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          invokableWithoutCacheHit = receiverClass.lookupInvokable(selector);
          if (cachedClass == null) {
            method.setInlineCache(bytecodeIndex + 1, receiverClass, invokableWithoutCacheHit);
          }
        }
      }
    }
    CompilerDirectives.transferToInterpreterAndInvalidate();
    invokeWithoutCacheHit(selector, frame, truffleFrame, invokableWithoutCacheHit);

    assert StackUtils.areStackEqual(truffleFrame,
        frame) : "Stacks are different";
    assert StackUtils.getCurrentStackPointer(
        truffleFrame) == frame.getStackPointer() : "Stack pointers differ";
  }

  private void invokeWithoutCacheHit(SSymbol selector, Frame frame, VirtualFrame truffleFrame,
      SInvokable invokableWithoutCacheHit) throws FrameSlotTypeException {
    if (invokableWithoutCacheHit != null) {
      invokableWithoutCacheHit.indirectInvoke(frame, truffleFrame, this);
    } else {
      int numberOfArguments = selector.getNumberOfSignatureArguments();

      SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);
      SAbstractObject receiverT =
          StackUtils.getRelativeStackElement(truffleFrame, numberOfArguments - 1);

      assert receiver == receiverT;

      receiver.sendDoesNotUnderstand(selector, universe, this, frame, truffleFrame);
    }
  }

  public final Frame newFrame(Frame prevFrame, final SMethod method, final Frame context) {
    return this.universe.newFrame(prevFrame, method, context);
  }

  public IndirectCallNode getIndirectCallNode() {
    return indirectCallNode;
  }

  /**
   * Previous helpers from SOM>Frame, transposed to TruffleFrame
   */

}
