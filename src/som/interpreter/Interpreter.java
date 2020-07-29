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

  private void doDup(final Frame frame) {
    // Handle the DUP bytecode
    frame.push(frame.getStackElement(0));
  }

  private void doPushLocal(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the PUSH LOCAL bytecode
    frame.push(
        frame.getLocal(method.getBytecode(bytecodeIndex + 1),
            method.getBytecode(bytecodeIndex + 2)));
  }

  private void doPushArgument(final int bytecodeIndex, final Frame frame,
      final SMethod method) {
    // Handle the PUSH ARGUMENT bytecode
    frame.push(
        frame.getArgument(method.getBytecode(bytecodeIndex + 1),
            method.getBytecode(bytecodeIndex + 2)));
  }

  private void doPushField(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the PUSH FIELD bytecode
    int fieldIndex = method.getBytecode(bytecodeIndex + 1);

    // Push the field with the computed index onto the stack
    frame.push(((SObject) getSelf(frame)).getField(fieldIndex));
  }

  private void doPushBlock(final int bytecodeIndex, final Frame frame, final SMethod method)
      throws ProgramDefinitionError {
    // Handle the PUSH BLOCK bytecode
    SMethod blockMethod = (SMethod) method.getConstant(bytecodeIndex);

    // Push a new block with the current getFrame() as context onto the
    // stack
    frame.push(
        universe.newBlock(blockMethod, frame,
            blockMethod.getNumberOfArguments()));
  }

  private void doPushConstant(final int bytecodeIndex, final Frame frame,
      final SMethod method) {
    // Handle the PUSH CONSTANT bytecode
    frame.push(method.getConstant(bytecodeIndex));
  }

  private void doPushGlobal(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the PUSH GLOBAL bytecode
    SSymbol globalName = (SSymbol) method.getConstant(bytecodeIndex);

    // Get the global from the universe
    SAbstractObject global = universe.getGlobal(globalName);

    if (global != null) {
      // Push the global onto the stack
      frame.push(global);
    } else {
      // Send 'unknownGlobal:' to self
      // TODO - reconsider
      CompilerDirectives.transferToInterpreter();
      getSelf(frame).sendUnknownGlobal(globalName, universe, this, frame);
    }
  }

  private void doPop(final Frame frame) {
    // Handle the POP bytecode
    frame.pop();
  }

  private void doPopLocal(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the POP LOCAL bytecode
    frame.setLocal(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2), frame.pop());
  }

  private void doPopArgument(final int bytecodeIndex, final Frame frame,
      final SMethod method) {
    // Handle the POP ARGUMENT bytecode
    frame.setArgument(method.getBytecode(bytecodeIndex + 1),
        method.getBytecode(bytecodeIndex + 2), frame.pop());
  }

  private void doPopField(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the POP FIELD bytecode
    int fieldIndex = method.getBytecode(bytecodeIndex + 1);

    // Set the field with the computed index to the value popped from the stack
    ((SObject) getSelf(frame)).setField(fieldIndex, frame.pop());
  }

  private void doSuperSend(final int bytecodeIndex, final Frame frame, final SMethod method) {
    // Handle the SUPER SEND bytecode
    SSymbol signature = (SSymbol) method.getConstant(bytecodeIndex);

    // Send the message
    // Lookup the invokable with the given signature
    SClass holderSuper = (SClass) method.getHolder().getSuperClass();
    SInvokable invokable = holderSuper.lookupInvokable(signature);

    if (invokable != null) {
      // Invoke the invokable in the current frame
      invokable.indirectInvoke(frame, this);
    } else {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      // Compute the number of arguments
      int numberOfArguments = signature.getNumberOfSignatureArguments();

      // Compute the receiver
      SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);

      receiver.sendDoesNotUnderstand(signature, universe, this, frame);
    }
  }

  private SAbstractObject doReturnLocal(final Frame frame) {
    // Handle the RETURN LOCAL bytecode
    return frame.pop();
  }

  private SAbstractObject doReturnNonLocal(final Frame frame) throws ReturnException {
    // Handle the RETURN NON LOCAL bytecode
    SAbstractObject result = frame.pop();

    // Compute the context for the non-local return
    Frame context = frame.getOuterContext(universe.nilObject);

    // Make sure the block context is still on the stack
    if (!context.hasPreviousFrame(universe.nilObject)) {
      // Try to recover by sending 'escapedBlock:' to the sending object
      // this can get a bit nasty when using nested blocks. In this case
      // the "sender" will be the surrounding block and not the object
      // that actually sent the 'value' message.
      SBlock block = (SBlock) frame.getArgument(0, 0);
      SAbstractObject sender =
          frame.getPreviousFrame().getOuterContext(universe.nilObject).getArgument(0, 0);

      // TODO - check the frame and its stack
      // ... and execute the escapedBlock message instead
      sender.sendEscapedBlock(block, universe, this, frame);
      return frame.pop();
    }

    // throw the exception to pass around the context and pop the right frames
    throw new ReturnException(result, context);
  }

  private void doSend(final int bytecodeIndex, final Frame frame, final SMethod method) {
    SSymbol signature = (SSymbol) method.getConstant(bytecodeIndex);
    int numberOfArguments = signature.getNumberOfSignatureArguments();
    SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);

    ValueProfile receiverClassValueProfile = method.getReceiverProfile(bytecodeIndex);
    if (receiverClassValueProfile == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      receiverClassValueProfile = ValueProfile.createClassProfile();
      method.setReceiverProfile(bytecodeIndex, receiverClassValueProfile);
    }

    send(signature,
        receiverClassValueProfile.profile(receiver).getSOMClass(universe),
        bytecodeIndex, frame, method);
  }

  @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
  public SAbstractObject start(final Frame frame, final SMethod method)
      throws ReturnException, ProgramDefinitionError {

    int bytecodeIndex = 0;

    while (true) {
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
          return frame.getStackElement(0);
        }

        case DUP: {
          doDup(frame);
          break;
        }

        case PUSH_LOCAL: {
          doPushLocal(currentBytecodeIndex, frame, method);
          break;
        }

        case PUSH_ARGUMENT: {
          doPushArgument(currentBytecodeIndex, frame, method);
          break;
        }

        case PUSH_FIELD: {
          doPushField(currentBytecodeIndex, frame, method);
          break;
        }

        case PUSH_BLOCK: {
          doPushBlock(currentBytecodeIndex, frame, method);
          break;
        }

        case PUSH_CONSTANT: {
          doPushConstant(currentBytecodeIndex, frame, method);
          break;
        }

        case PUSH_GLOBAL: {
          doPushGlobal(currentBytecodeIndex, frame, method);
          break;
        }

        case POP: {
          doPop(frame);
          break;
        }

        case POP_LOCAL: {
          doPopLocal(currentBytecodeIndex, frame, method);
          break;
        }

        case POP_ARGUMENT: {
          doPopArgument(currentBytecodeIndex, frame, method);
          break;
        }

        case POP_FIELD: {
          doPopField(currentBytecodeIndex, frame, method);
          break;
        }

        case SEND: {
          doSend(currentBytecodeIndex, frame, method);
          break;
        }

        case SUPER_SEND: {
          doSuperSend(currentBytecodeIndex, frame, method);
          break;
        }

        case RETURN_LOCAL: {
          return doReturnLocal(frame);
        }

        case RETURN_NON_LOCAL: {
          return doReturnNonLocal(frame);
        }

        default:
          Universe.errorPrintln("Nasty bug in interpreter");
          break;
      }
    }
  }

  public SMethod getMethod(final Frame frame) {
    // Get the method from the interpreter
    return frame.getMethod();
  }

  public SAbstractObject getSelf(final Frame frame) {
    // Get the self object from the interpreter
    return frame.getOuterContext(universe.nilObject).getArgument(0, 0);
  }

  private void send(final SSymbol selector, final SClass receiverClass,
      final int bytecodeIndex, Frame frame, final SMethod method) {
    // First try the inline cache
    SInvokable invokableWithoutCacheHit = null;

    SClass cachedClass = method.getInlineCacheClass(bytecodeIndex);
    if (cachedClass == receiverClass) {
      SInvokable invokable = method.getInlineCacheInvokable(bytecodeIndex);
      if (invokable != null) {
        DirectCallNode invokableDirectCallNode =
            method.getInlineCacheDirectCallNode(bytecodeIndex);
        CompilerAsserts.partialEvaluationConstant(invokableDirectCallNode);
        invokable.directInvoke(frame, this, invokableDirectCallNode);
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
            invokable.directInvoke(frame, this, invokableDirectCallNode);
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
    invokeWithoutCacheHit(selector, frame, invokableWithoutCacheHit);
  }

  private void invokeWithoutCacheHit(SSymbol selector, Frame frame,
      SInvokable invokableWithoutCacheHit) {
    if (invokableWithoutCacheHit != null) {
      invokableWithoutCacheHit.indirectInvoke(frame, this);
    } else {
      int numberOfArguments = selector.getNumberOfSignatureArguments();

      // Compute the receiver
      SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);

      receiver.sendDoesNotUnderstand(selector, universe, this, frame);
    }
  }

  public final Frame newFrame(Frame prevFrame, final SMethod method, final Frame context) {
    return this.universe.newFrame(prevFrame, method, context);
  }

  public IndirectCallNode getIndirectCallNode() {
    return indirectCallNode;
  }
}
