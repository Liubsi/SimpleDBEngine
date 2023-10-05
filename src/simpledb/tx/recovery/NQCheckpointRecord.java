package simpledb.tx.recovery;

import simpledb.log.LogMgr;
import simpledb.file.Page;
import simpledb.tx.Transaction;
import java.util.ArrayList;
import java.util.List;

/**
 * A non-quiescent checkpoint record.
 */
public class NQCheckpointRecord implements LogRecord {
    
    private List<Integer> activeTxList;

    /**
     * Creates a new NQCheckpoint log record.
     * @param p the page to be read
     */
    public NQCheckpointRecord(Page p) {
        int pos = Integer.BYTES;  // move past the record type
        int numTx = p.getInt(pos);
        pos += Integer.BYTES;
        activeTxList = new ArrayList<>();

        for (int i = 0; i < numTx; i++) {
            activeTxList.add(p.getInt(pos));
            pos += Integer.BYTES;
        }
    }

    /**
     * Writes a non-quiescent checkpoint record to the log.
     * @param logMgr the log manager
     * @param txList the list of active transactions
     * @return the LSN of the log record
     */
    public static int writeToLog(LogMgr logMgr, List<Integer> txList) {
        int recsize = Integer.BYTES * (2 + txList.size());
        byte[] rec = new byte[recsize];
        Page p = new Page(rec);
        
        p.setInt(0, NQCKPT);
        p.setInt(Integer.BYTES, txList.size());

        int pos = 2 * Integer.BYTES;
        for (int txnum : txList) {
            p.setInt(pos, txnum);
            pos += Integer.BYTES;
        }
        
        return logMgr.append(rec);
    }

    @Override
    public int op() {
        return NQCKPT;
    }

    @Override
    public int txNumber() {
        return -1;
    }

    @Override
    public void undo(Transaction tx) {}

    /**
     * Gets the list of active transaction numbers from the log record.
     * @return the list of active transactions
     */
    public List<Integer> txList() {
        return activeTxList;
    }


    /**
     * @return the NQCKPT record string formatted
     */
    public String toString() {
        return "<NQCKPT " + activeTxList + ">";
     }
}
