/**
 * Copyright (c) 2016 Michael Haupt, github@haupz.de
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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.library.ExportMessage;

import som.vm.Universe;

public final class SInteger extends SNumber {

  /**
   * Language convention requires integers up to this value to be identical.
   */
  private static final long MAX_IDENTICAL_INT = 1073741823L;

  /**
   * Cache to store integers up to {@link #MAX_IDENTICAL_INT}.
   */
  private static Map<Long, SInteger> CACHE = new HashMap<>();

  // Private variable holding the embedded integer
  private final long embeddedInteger;

  private SInteger(final long value) {
    embeddedInteger = value;
  }

  public static SInteger getInteger(final long value) {
    if (value > MAX_IDENTICAL_INT) {
      return new SInteger(value);
    }
    return CACHE.computeIfAbsent(value, SInteger::new);
  }

  public long getEmbeddedInteger() {
    // Get the embedded integer
    return embeddedInteger;
  }

  @Override
  public String toString() {
    return "" + embeddedInteger;
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.integerClass;
  }

  @Override
  public SString primAsString(final Universe universe) {
    return universe.newString(Long.toString(embeddedInteger));
  }

  @Override
  public SNumber primSqrt(final Universe universe) {
    double result = Math.sqrt(embeddedInteger);

    if (result == Math.rint(result)) {
      return intOrBigInt(result, universe);
    } else {
      return universe.newDouble(result);
    }
  }

  @Override
  public SNumber primAdd(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).add(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else if (right instanceof SDouble) {
      double result = embeddedInteger + ((SDouble) right).getEmbeddedDouble();
      return universe.newDouble(result);
    } else {
      SInteger r = (SInteger) right;

      try {
        long result = Math.addExact(embeddedInteger, r.getEmbeddedInteger());
        return universe.newInteger(result);
      } catch (ArithmeticException e) {
        BigInteger result = BigInteger.valueOf(embeddedInteger).add(
            BigInteger.valueOf(r.getEmbeddedInteger()));
        return universe.newBigInteger(result);
      }
    }
  }

  @Override
  public SNumber primSubtract(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).subtract(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else if (right instanceof SDouble) {
      double result = embeddedInteger - ((SDouble) right).getEmbeddedDouble();
      return universe.newDouble(result);
    } else {
      SInteger r = (SInteger) right;

      try {
        long result = Math.subtractExact(embeddedInteger, r.getEmbeddedInteger());
        return universe.newInteger(result);
      } catch (ArithmeticException e) {
        BigInteger result = BigInteger.valueOf(embeddedInteger).subtract(
            BigInteger.valueOf(r.getEmbeddedInteger()));
        return universe.newBigInteger(result);
      }
    }
  }

  @Override
  public SNumber primMultiply(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).multiply(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else if (right instanceof SDouble) {
      double result = embeddedInteger * ((SDouble) right).getEmbeddedDouble();
      return universe.newDouble(result);
    } else {
      SInteger r = (SInteger) right;

      try {
        long result = Math.multiplyExact(embeddedInteger, r.getEmbeddedInteger());
        return universe.newInteger(result);
      } catch (ArithmeticException e) {
        BigInteger result = BigInteger.valueOf(embeddedInteger).multiply(
            BigInteger.valueOf(r.getEmbeddedInteger()));
        return universe.newBigInteger(result);
      }
    }
  }

  @Override
  public SNumber primDoubleDivide(final SNumber right, final Universe universe) {
    double result;

    if (right instanceof SBigInteger) {
      result = embeddedInteger / ((SBigInteger) right).getEmbeddedBiginteger().doubleValue();
    } else if (right instanceof SDouble) {
      result = embeddedInteger / ((SDouble) right).getEmbeddedDouble();
    } else {
      result = (double) embeddedInteger / ((SInteger) right).getEmbeddedInteger();
    }

    return universe.newDouble(result);
  }

  @Override
  public SNumber primIntegerDivide(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).divide(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else if (right instanceof SDouble) {
      long result = (long) (embeddedInteger / ((SDouble) right).getEmbeddedDouble());
      return universe.newInteger(result);
    } else {
      long result = embeddedInteger / ((SInteger) right).getEmbeddedInteger();
      return universe.newInteger(result);
    }
  }

  @Override
  public SNumber primModulo(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      // Note: modulo semantics of SOM differ from Java, with respect to
      // negative operands, but BigInteger doesn't support a negative
      // second operand, so, we should get an exception, which we can
      // properly handle once an application actually needs it.
      BigInteger result = BigInteger.valueOf(embeddedInteger).mod(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else if (right instanceof SDouble) {
      double result = embeddedInteger % ((SDouble) right).getEmbeddedDouble();
      return universe.newDouble(result);
    } else {
      long r = ((SInteger) right).getEmbeddedInteger();
      long result = Math.floorMod(embeddedInteger, r);
      return universe.newInteger(result);
    }
  }

  public SInteger primRemainder(final SNumber right, final Universe universe) {
    if (right instanceof SInteger) {
      return universe.newInteger(embeddedInteger % ((SInteger) right).embeddedInteger);
    } else {
      throw new RuntimeException("TODO");
    }
  }

  @Override
  public SNumber primBitAnd(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).and(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else {
      long result = embeddedInteger & ((SInteger) right).embeddedInteger;
      return universe.newInteger(result);
    }
  }

  @Override
  public SNumber primBitXor(final SNumber right, final Universe universe) {
    if (right instanceof SBigInteger) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).xor(
          ((SBigInteger) right).getEmbeddedBiginteger());
      return universe.newBigInteger(result);
    } else {
      long result = embeddedInteger ^ ((SInteger) right).embeddedInteger;
      return universe.newInteger(result);
    }
  }

  @Override
  public SNumber primLeftShift(final SNumber right, final Universe universe) {
    long r = ((SInteger) right).embeddedInteger;
    assert r > 0;

    if (Long.SIZE - Long.numberOfLeadingZeros(embeddedInteger) + r > Long.SIZE - 1) {
      BigInteger result = BigInteger.valueOf(embeddedInteger).shiftLeft((int) r);
      return universe.newBigInteger(result);
    }

    long result = embeddedInteger << r;
    return universe.newInteger(result);
  }

  @Override
  public SObject primEqual(final SAbstractObject right, final Universe universe) {
    boolean result;

    if (right instanceof SBigInteger) {
      result = BigInteger.valueOf(embeddedInteger).equals(
          ((SBigInteger) right).getEmbeddedBiginteger());
    } else if (right instanceof SDouble) {
      result = embeddedInteger == ((SDouble) right).getEmbeddedDouble();
    } else if (right instanceof SInteger) {
      result = embeddedInteger == ((SInteger) right).getEmbeddedInteger();
    } else {
      result = false;
    }

    return asSBoolean(result, universe);
  }

  @Override
  public SObject primLessThan(final SNumber right, final Universe universe) {
    boolean result;
    if (right instanceof SBigInteger) {
      result = BigInteger.valueOf(embeddedInteger).compareTo(
          ((SBigInteger) right).getEmbeddedBiginteger()) < 0;
    } else if (right instanceof SDouble) {
      result = embeddedInteger < ((SDouble) right).getEmbeddedDouble();
    } else {
      result = embeddedInteger < ((SInteger) right).getEmbeddedInteger();
    }

    return asSBoolean(result, universe);
  }


  /**
   * INTEROP.
   * Return Long only.
   */

  @Override
  @ExportMessage
  boolean fitsInLong() {
    return true;
  }

  @Override
  @ExportMessage
  long asLong()  {
      return this.embeddedInteger;
  }

}
