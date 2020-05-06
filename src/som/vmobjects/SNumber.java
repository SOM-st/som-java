package som.vmobjects;

import java.math.BigInteger;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import som.vm.Universe;


@ExportLibrary(InteropLibrary.class)
public abstract class SNumber extends SAbstractObject {
  public abstract SString primAsString(Universe universe);

  public abstract SNumber primSqrt(Universe universe);

  public abstract SNumber primAdd(SNumber right, Universe universe);

  public abstract SNumber primSubtract(SNumber right, Universe universe);

  public abstract SNumber primMultiply(SNumber right, Universe universe);

  public abstract SNumber primDoubleDivide(SNumber right, Universe universe);

  public abstract SNumber primIntegerDivide(SNumber right, Universe universe);

  public abstract SNumber primModulo(SNumber right, Universe universe);

  public abstract SNumber primBitAnd(SNumber right, Universe universe);

  public abstract SNumber primBitXor(SNumber right, Universe universe);

  public abstract SNumber primLeftShift(SNumber right, Universe universe);

  public abstract SObject primEqual(SAbstractObject right, Universe universe);

  public abstract SObject primLessThan(SNumber right, Universe universe);

  protected final SNumber intOrBigInt(final double value, final Universe universe) {
    if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
      return universe.newBigInteger(new BigInteger(Double.toString(Math.rint(value))));
    } else {
      return universe.newInteger((long) Math.rint(value));
    }
  }

  protected final SObject asSBoolean(final boolean result, final Universe universe) {
    if (result) {
      return universe.trueObject;
    } else {
      return universe.falseObject;
    }
  }

  /**
   * INTEROP.
   * Exporting `isNumber`, so have to implement the following methods, as stated in the
   * InteropLibrary class.
   */

  @ExportMessage
  boolean isNumber() {
    return true;
  }

  @ExportMessage
  boolean fitsInByte() {
    return false;
  }

  @ExportMessage
  boolean fitsInShort() {
    return false;
  }

  @ExportMessage
  boolean fitsInFloat() {
    return false;
  }

  @ExportMessage
  boolean fitsInLong() {
    return false;
  }

  @ExportMessage
  boolean fitsInInt() {
    return false;
  }

  @ExportMessage
  boolean fitsInDouble() {
    return false;
  }

  /*
   * The relevant methods asLong and asDouble are being overriden in respectively the SInteger
   * and SDouble subclasses
   */

  @ExportMessage
  byte asByte() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  };

  @ExportMessage
  short asShort() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  int asInt() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  long asLong() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  float asFloat() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  }

  @ExportMessage
  double asDouble() throws UnsupportedMessageException {
    throw UnsupportedMessageException.create();
  }

}
