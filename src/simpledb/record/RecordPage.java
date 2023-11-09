package simpledb.record;

import static java.sql.Types.INTEGER;
import simpledb.file.*;
import simpledb.tx.Transaction;

/**
 * Store a record at a given location in a block. 
 * @author Edward Sciore
 */
public class RecordPage {
   public static final int EMPTY = 0, USED = 1;
   public static final int NULL = 1, NOTNULL = 0;
   private Transaction tx;
   private BlockId blk;
   private Layout layout;

   public RecordPage(Transaction tx, BlockId blk, Layout layout) {
      this.tx = tx;
      this.blk = blk;
      this.layout = layout;
      tx.pin(blk);
   }

   /**
    * Return the integer value stored for the
    * specified field of a specified slot.
    * @param fldname the name of the field.
    * @return the integer stored in that field
    */
   public int getInt(int slot, String fldname) {
      int fldpos = offset(slot) + layout.offset(fldname);
      return tx.getInt(blk, fldpos);
   }

   /**
    * Return the string value stored for the
    * specified field of the specified slot.
    * @param fldname the name of the field.
    * @return the string stored in that field
    */
   public String getString(int slot, String fldname) {
      int fldpos = offset(slot) + layout.offset(fldname);
      return tx.getString(blk, fldpos);
   }

   /**
    * Store an integer at the specified field
    * of the specified slot.
    * @param fldname the name of the field
    * @param val the integer value stored in that field
    */
   public void setInt(int slot, String fldname, int val) {
      int fldpos = offset(slot) + layout.offset(fldname);
      tx.setInt(blk, fldpos, val, true);

      // set field to non-null
      int bitpos = layout.bitPosition(fldname);
      int flag = tx.getInt(blk, offset(slot));
      flag = setBitVal(flag, bitpos, NOTNULL);
      tx.setInt(blk, offset(slot), flag, true);
   }

   /**
    * Store a string at the specified field
    * of the specified slot.
    * @param fldname the name of the field
    * @param val the string value stored in that field
    */
   public void setString(int slot, String fldname, String val) {
      int fldpos = offset(slot) + layout.offset(fldname);
      tx.setString(blk, fldpos, val, true);

      // set field to non-null
      int bitpos = layout.bitPosition(fldname);
      int flag = tx.getInt(blk, offset(slot));
      flag = setBitVal(flag, bitpos, NOTNULL);
      tx.setInt(blk, offset(slot), flag, true);
   }
   
   public void delete(int slot) {
      setFlag(slot, EMPTY);
   }
   
   /** Use the layout to format a new block of records.
    *  These values should not be logged 
    *  (because the old values are meaningless).
    */ 
   public void format() {
      int slot = 0;
      while (isValidSlot(slot)) {
         tx.setInt(blk, offset(slot), EMPTY, false); 
         Schema sch = layout.schema();
         for (String fldname : sch.fields()) {
            int fldpos = offset(slot) + layout.offset(fldname);
            if (sch.type(fldname) == INTEGER)
               tx.setInt(blk, fldpos, 0, false);
            else
               tx.setString(blk, fldpos, "", false);
         }
         slot++;
      }
   }

   public int nextAfter(int slot) {
      return searchAfter(slot, USED);
   }
 
   public int insertAfter(int slot) {
      int newslot = searchAfter(slot, EMPTY);
      if (newslot >= 0)
         setFlag(newslot, USED);
      return newslot;
   }
  
   public BlockId block() {
      return blk;
   }

   public void setNull(int slot, String fldname) {
      int bitpos = layout.bitPosition(fldname);
      int flag = tx.getInt(blk, offset(slot));
      flag = setBitVal(flag, bitpos, NULL);
      tx.setInt(blk, offset(slot), flag, true);
   }
   
   public boolean isNull(int slot, String fldname) {
      int bitpos = layout.bitPosition(fldname);
      int flag = tx.getInt(blk, offset(slot));
      return getBitVal(flag, bitpos) == NULL;
   }

   public int slots() {
      return tx.blockSize() / layout.slotSize();
   }

   public int nextBefore(int slot) {
      int prevSlot = slot - 1;
        while (prevSlot >= 0) {
            if (isSlotUsed(prevSlot)) {
                return prevSlot;
            }
            prevSlot--;
        }
        return -1;
      }
   
   // Private auxiliary methods
   
   /**
    * Set the record's empty/inuse flag.
    */
   private void setFlag(int slot, int flag) {
      tx.setInt(blk, offset(slot), flag, true); 
   }

   private int searchAfter(int slot, int flag) {
      slot++;
      while (isValidSlot(slot)) {
         int currentFlag = tx.getInt(blk, offset(slot));
         if (getBitVal(currentFlag, 0) == flag)
            return slot;
         slot++;
      }
      return -1;
   }

   private boolean isValidSlot(int slot) {
      return offset(slot+1) <= tx.blockSize();
   }

   private int offset(int slot) {
      return slot * layout.slotSize();
   }
     
   private int getBitVal(int val, int bitpos) {
      return (val >> bitpos) % 2;
   }
  
   private int setBitVal(int val, int bitpos, int flag) {
      int mask = (1 << bitpos);
      if (flag == 0) return val & ~mask;
      else return val | mask;
   }

   private boolean isSlotUsed(int slot) {
      int flag = tx.getInt(blk, offset(slot));
      return getBitVal(flag, 0) == USED;
  }
}








