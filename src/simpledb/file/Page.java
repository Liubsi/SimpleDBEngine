package simpledb.file;

import java.nio.ByteBuffer;
import java.nio.charset.*;

public class Page {
   private ByteBuffer bb;
   public static Charset CHARSET = StandardCharsets.US_ASCII;

   // For creating data buffers
   public Page(int blocksize) {
      bb = ByteBuffer.allocateDirect(blocksize);
   }
   
   // For creating log pages
   public Page(byte[] b) {
      bb = ByteBuffer.wrap(b);
   }

   public int getInt(int offset) {
      return bb.getInt(offset);
   }

   public void setInt(int offset, int n) {
      if (offset + Integer.BYTES <= bb.capacity()) {
         bb.putInt(offset, n);
      } else {
         System.out.printf("ERROR: The integer %d does not fit at location %d of the page %n", n, offset);
      }
   }

   public byte[] getBytes(int offset) {
      bb.position(offset);
      int length = bb.getInt();
      byte[] b = new byte[length];
      bb.get(b);
      return b;
   }

   public void setBytes(int offset, byte[] b) {      
      if (offset + b.length <= bb.capacity()) {
         bb.position(offset);
         bb.putInt(b.length);
         bb.put(b);
      } else {
         System.out.printf("ERROR: The supplied byte array does not fit at location %d of the page %n", offset);
      }
   }
   
   public String getString(int offset) {
      bb.position(offset);
      StringBuilder sb = new StringBuilder();

      char c = bb.getChar();
      while (c != '\0') {
         sb.append(c);
         c = bb.getChar();
      }

      // Consume the delimiter '\0'
      bb.getChar();

      return sb.toString();
   }

   public void setString(int offset, String s) {
      if (offset + (s.length() * Character.BYTES) + Character.BYTES <= bb.capacity()) {
         bb.position(offset);
         for (char c : s.toCharArray()) {
            bb.putChar(c);
         }
         bb.putChar('\0');
      } else {
         System.out.printf("ERROR: The string %s does not fit at location %d of the page %n", s, offset);
      }
   }

   public static int maxLength(int strlen) {
      return (strlen * Character.BYTES) + Character.BYTES;
   }

   // a package private method, needed by FileMgr
   ByteBuffer contents() {
      bb.position(0);
      return bb;
   }
}
