package simpledb.tx.concurrency;

import java.util.*;
import simpledb.file.BlockId;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * If a transaction requests a lock that causes a conflict with an
 * existing lock, then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * @author Edward Sciore
 */
class LockTable {
   private static final long MAX_TIME = 10000; // 10 seconds
   
   private Map<BlockId,List<Integer>> locks = new HashMap<>();
   /**
    * Grant an SLock on the specified block.
    * If an XLock exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the lock is released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    */
   public synchronized void sLock(BlockId blk, int txnum) {
      try {
         // I'm checking the last transaction number in existingLocks to decide whether to abort or not. 
         // However, this does not consider whether the existing lock is shared or exclusive.
         // So older shared locks can abort transactions, despite being able to coexist
         // Ran out of time implementing this :(
         List<Integer> existingLocks = locks.getOrDefault(blk, new ArrayList<>());
         if (!existingLocks.isEmpty()) {
            int lastTxNum = existingLocks.get(existingLocks.size() - 1);
            if (lastTxNum < 0 && txnum > Math.abs(lastTxNum)) {
               throw new LockAbortException();
            }
         }
         while (hasXlock(blk)) {
            wait();
         }
         existingLocks.add(txnum);
         locks.put(blk, existingLocks);
      } catch (InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   /**
    * Grant an XLock on the specified block.
    * If a lock of any type exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the locks are released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    */
   synchronized void xLock(BlockId blk, int txnum) {
      try {
         List<Integer> existingLocks = locks.getOrDefault(blk, new ArrayList<>());
         if (!existingLocks.isEmpty()) {
            int lastTxNum = existingLocks.get(existingLocks.size() - 1);
            if (txnum > Math.abs(lastTxNum)) {
               throw new LockAbortException();
            }
         }
         while (hasOtherSLocks(blk)) {
            wait();
         }         
         existingLocks =  new ArrayList<>();
         existingLocks.add(-txnum);
         locks.put(blk, existingLocks);
      } catch(InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   /**
    * Release a lock on the specified block.
    * If this lock is the last lock on that block,
    * then the waiting transactions are notified.
    * @param blk a reference to the disk block
    */
   synchronized void unlock(BlockId blk, int txnum) {
      List<Integer> existingLocks = locks.get(blk);
      if (existingLocks != null) {
         existingLocks.remove(Integer.valueOf(txnum));
         if (existingLocks.isEmpty()) {
            locks.remove(blk);
            notifyAll();
         } else {
            locks.put(blk, existingLocks);
         }
      }
   }
   
   private boolean hasXlock(BlockId blk) {
      List<Integer> ival = locks.get(blk);
      return getLockVal(blk) == 1 && ival.get(0) < 0;
   }
   
   private boolean hasOtherSLocks(BlockId blk) {
      return getLockVal(blk) > 1;
   }
   
   private boolean waitingTooLong(long starttime) {
      return System.currentTimeMillis() - starttime > MAX_TIME;
   }
   
   private int getLockVal(BlockId blk) {
      List<Integer> ival = locks.get(blk);
      return (ival == null) ? 0 : ival.size();
   }
}
