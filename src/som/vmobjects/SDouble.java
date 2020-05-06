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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import som.vm.Universe;


@ExportLibrary(InteropLibrary.class)
public final class SDouble extends SNumber {

  private final double embeddedDouble;

  public SDouble(final double value) {
    embeddedDouble = value;
  }

  public double getEmbeddedDouble() {
    // Get the embedded double
    return embeddedDouble;
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.doubleClass;
  }

  private double coerceToDouble(final SNumber o, final Universe universe) {
    if (o instanceof SDouble) {
      return ((SDouble) o).embeddedDouble;
    }
    if (o instanceof SInteger) {
      return ((SInteger) o).getEmbeddedInteger();
    }
    throw new ClassCastException("Cannot coerce to Double!");
  }

  @Override
  public SString primAsString(final Universe universe) {
    return universe.newString(Double.toString(embeddedDouble));
  }

  public SInteger primAsInteger(final Universe universe) {
    return universe.newInteger((long) embeddedDouble);
  }

  @Override
  public SNumber primSqrt(final Universe universe) {
    return universe.newDouble(Math.sqrt(embeddedDouble));
  }

  @Override
  public SNumber primAdd(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return universe.newDouble(embeddedDouble + r);
  }

  @Override
  public SNumber primSubtract(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return universe.newDouble(embeddedDouble - r);
  }

  @Override
  public SNumber primMultiply(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return universe.newDouble(embeddedDouble * r);
  }

  @Override
  public SNumber primDoubleDivide(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return universe.newDouble(embeddedDouble / r);
  }

  @Override
  public SNumber primIntegerDivide(final SNumber right, final Universe universe) {
    throw new RuntimeException("not yet implemented, SOM doesn't offer it");
  }

  @Override
  public SNumber primModulo(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return universe.newDouble(embeddedDouble % r);
  }

  @Override
  public SNumber primBitAnd(final SNumber right, final Universe universe) {
    throw new RuntimeException("Not supported for doubles");
  }

  @Override
  public SNumber primBitXor(final SNumber right, final Universe universe) {
    throw new RuntimeException("Not supported for doubles");
  }

  @Override
  public SNumber primLeftShift(final SNumber right, final Universe universe) {
    throw new RuntimeException("Not supported for doubles");
  }

  @Override
  public SObject primEqual(final SAbstractObject right, final Universe universe) {
    if (!(right instanceof SNumber)) {
      return universe.falseObject;
    }

    double r = coerceToDouble((SNumber) right, universe);
    return asSBoolean(embeddedDouble == r, universe);
  }

  @Override
  public SObject primLessThan(final SNumber right, final Universe universe) {
    double r = coerceToDouble(right, universe);
    return asSBoolean(embeddedDouble < r, universe);
  }

  /**
   * INTEROP.
   * Return Double only.
   */

  @Override
  @ExportMessage
  boolean fitsInDouble() {
    return true;
  }

  @Override
  @ExportMessage
  double asDouble() {
    return this.embeddedDouble;
  }
}
