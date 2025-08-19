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

import java.math.BigDecimal;
import java.math.BigInteger;

import som.vm.Universe;


public final class SBigInteger extends SNumber {

  // Private variable holding the embedded big integer
  private final BigInteger embeddedBiginteger;

  public SBigInteger(final BigInteger value) {
    embeddedBiginteger = value;
  }

  public BigInteger getEmbeddedBiginteger() {
    // Get the embedded big integer
    return embeddedBiginteger;
  }

  @Override
  public String toString() {
    return super.toString() + "(" + embeddedBiginteger + ")";
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.integerClass;
  }

  @Override
  public SString primAsString(final Universe universe) {
    return universe.newString(embeddedBiginteger.toString());
  }

  @Override
  public SNumber primAsDouble(final Universe universe) {
    return universe.newDouble(embeddedBiginteger.doubleValue());
  }

  private SNumber asSNumber(final BigInteger result, final Universe universe) {
    if (result.bitLength() >= Long.SIZE) {
      return universe.newBigInteger(result);
    } else {
      return universe.newInteger(result.longValue());
    }
  }

  private BigInteger asBigInteger(final SNumber right) {
    BigInteger r;
    if (right instanceof SInteger r1) {
      r = BigInteger.valueOf(r1.getEmbeddedInteger());
    } else if (right instanceof SDouble r1) {
      r = BigDecimal.valueOf(r1.getEmbeddedDouble()).toBigInteger();
    } else {
      r = ((SBigInteger) right).embeddedBiginteger;
    }
    return r;
  }

  @Override
  public SNumber primAdd(final SNumber right, final Universe universe) {
    if (right instanceof SDouble) {
      return universe.newDouble(
          embeddedBiginteger.doubleValue() + ((SDouble) right).getEmbeddedDouble());
    }
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.add(r);
    return asSNumber(result, universe);
  }

  @Override
  public SNumber primSubtract(final SNumber right, final Universe universe) {
    if (right instanceof SDouble) {
      return universe.newDouble(
          embeddedBiginteger.doubleValue() - ((SDouble) right).getEmbeddedDouble());
    }
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.subtract(r);
    return asSNumber(result, universe);
  }

  @Override
  public SNumber primMultiply(final SNumber right, final Universe universe) {
    if (right instanceof SDouble) {
      return universe.newDouble(
          embeddedBiginteger.doubleValue() * ((SDouble) right).getEmbeddedDouble());
    }
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.multiply(r);
    return asSNumber(result, universe);
  }

  @Override
  public SNumber primDoubleDivide(final SNumber right, final Universe universe) {
    double r;
    if (right instanceof SInteger i) {
      r = i.getEmbeddedInteger();
    } else if (right instanceof SDouble d) {
      r = d.getEmbeddedDouble();
    } else {
      r = ((SBigInteger) right).embeddedBiginteger.doubleValue();
    }
    double result = embeddedBiginteger.doubleValue() / r;
    return universe.newDouble(result);
  }

  @Override
  public SNumber primIntegerDivide(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.divide(r);
    return asSNumber(result, universe);
  }

  @Override
  public SNumber primModulo(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.mod(r);
    return asSNumber(result, universe);
  }

  @Override
  public SNumber primSqrt(final Universe universe) {
    double result = Math.sqrt(embeddedBiginteger.doubleValue());

    if (result == Math.rint(result)) {
      return intOrBigInt(result, universe);
    } else {
      return universe.newDouble(result);
    }
  }

  @Override
  public SNumber primBitAnd(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.and(r);
    return asSNumber(result, universe);
  }

  @Override
  public SObject primEqual(final SAbstractObject right, final Universe universe) {
    if (!(right instanceof SNumber)) {
      return universe.falseObject;
    }

    BigInteger r = asBigInteger((SNumber) right);

    if (embeddedBiginteger.compareTo(r) == 0) {
      return universe.trueObject;
    } else {
      return universe.falseObject;
    }
  }

  @Override
  public SObject primLessThan(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    if (embeddedBiginteger.compareTo(r) < 0) {
      return universe.trueObject;
    } else {
      return universe.falseObject;
    }
  }

  @Override
  public SNumber primLeftShift(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    return universe.newBigInteger(embeddedBiginteger.shiftLeft(r.intValue()));
  }

  @Override
  public SNumber primBitXor(final SNumber right, final Universe universe) {
    BigInteger r = asBigInteger(right);
    BigInteger result = embeddedBiginteger.xor(r);
    return asSNumber(result, universe);
  }
}
