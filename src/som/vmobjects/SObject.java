package som.vmobjects;

import som.vm.Universe;


public class SObject extends SAbstractObject {

  private final SAbstractObject[] fields;
  private SClass                  clazz;
  static final int                numberOfObjectFields = 0;

  public SObject(final SObject nilObject) {
    fields = new SAbstractObject[getDefaultNumberOfFields()];

    // Clear each and every field by putting nil into them
    for (int i = 0; i < getNumberOfFields(); i++) {
      setField(i, nilObject);
    }
  }

  public SObject(int numberOfFields, final SObject nilObject) {
    fields = new SAbstractObject[numberOfFields];

    // Clear each and every field by putting nil into them
    for (int i = 0; i < getNumberOfFields(); i++) {
      setField(i, nilObject);
    }
  }

  public SClass getSOMClass() {
    return clazz;
  }

  public void setClass(SClass value) {
    // Set the class of this object by writing to the field with class index
    clazz = value;
  }

  public SSymbol getFieldName(int index) {
    // Get the name of the field with the given index
    return getSOMClass().getInstanceFieldName(index);
  }

  public int getFieldIndex(SSymbol name) {
    // Get the index for the field with the given name
    return getSOMClass().lookupFieldIndex(name);
  }

  public int getNumberOfFields() {
    // Get the number of fields in this object
    return fields.length;
  }

  public int getDefaultNumberOfFields() {
    // Return the default number of fields in an object
    return numberOfObjectFields;
  }

  public SAbstractObject getField(long index) {
    // Get the field with the given index
    return fields[(int) index];
  }

  public void setField(long index, SAbstractObject value) {
    // Set the field with the given index to the given value
    fields[(int) index] = value;
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return clazz;
  }
}
