package com.db;

import android.util.SparseIntArray;
import android.util.Log;
import java.util.Iterator;

public class Base128
{
  private static final String TAG = "BASE128";
  /**
   *  encode() given a blob in the form of a byte array, encode it to base128.
   *  Base128 consists of the set of characters common to the gsm alphabet.
   *
   *  decode(encode(b)) == b
   *  Procedure:
   *    read 7 bits
   *    map 7 bits to ascii char or unicode char
   *
   *  @param buf the buffer to encode to a String.
   *  @return String encoding of buf.
   */
  public static String encode(byte[] buf)
  {
    StringBuilder s = new StringBuilder();
    for(byte b : new Septets(buf))
    {
      s.append(findChar(b));
    }
    return s.toString();
  }

  protected static char[] map =
  {
    '@', '£', '$', '¥', 'è', 'é', 'ù', 'ì', 'ò', 'Ç', '\n', 'Ø', 'ø', '\r',
    'Å', 'å', '\u0394', '_', 'Φ', '\u0393', '\u039b', '\u03a9', '\u03a0',
    '\u03a8', '\u03a3', '\u0398', '\u039e', '|', 'Æ', 'æ', 'ß', 'É', ' ',
    '!', '"', '#', '¤', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
    '0', '1', '2', '3', '4', '5', '6', '7','8', '9', ':', ';', '<', '=', '>',
    '?', '¡', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'Ä', 'Ö',
    'Ñ', 'Ü', '§', '¿', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
    'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    'ä', 'ö', 'ñ', 'ü', 'à'
  };

  protected static SparseIntArray reverseMap = new SparseIntArray();

  static
  {
    int i = 0;
    reverseMap.put('@', i++);
    reverseMap.put('\u00a3', i++);
    reverseMap.put('$', i++);
    reverseMap.put('\u00a5', i++);
    reverseMap.put('\u00e8', i++);
    reverseMap.put('\u00e9', i++);
    reverseMap.put('\u00f9', i++);
    reverseMap.put('\u00ec', i++);
    reverseMap.put('\u00f2', i++);
    reverseMap.put('\u00c7', i++);
    reverseMap.put('\n', i++);
    reverseMap.put('\u00d8', i++);
    reverseMap.put('\u00f8', i++);
    reverseMap.put('\r', i++);
    reverseMap.put('\u00c5', i++);
    reverseMap.put('\u00e5', i++);
    reverseMap.put('\u0394', i++);
    reverseMap.put('_', i++);
    reverseMap.put('\u03a6', i++);
    reverseMap.put('\u0393', i++);
    reverseMap.put('\u039b', i++);
    reverseMap.put('\u03a9', i++);
    reverseMap.put('\u03a0', i++);
    reverseMap.put('\u03a8', i++);
    reverseMap.put('\u03a3', i++);
    reverseMap.put('\u0398', i++);
    reverseMap.put('\u039e', i++);
    reverseMap.put('|', i++);
    reverseMap.put('\u00c6', i++);
    reverseMap.put('\u00e6', i++);
    reverseMap.put('\u00df', i++);
    reverseMap.put('\u00c9', i++);
    reverseMap.put(' ', i++);
    reverseMap.put('!', i++);
    reverseMap.put('"', i++);
    reverseMap.put('#', i++);
    reverseMap.put('\u00a4', i++);
    reverseMap.put('%', i++);
    reverseMap.put('&', i++);
    reverseMap.put('\'', i++);
    reverseMap.put('(', i++);
    reverseMap.put(')', i++);
    reverseMap.put('*', i++);
    reverseMap.put('+', i++);
    reverseMap.put(',', i++);
    reverseMap.put('-', i++);
    reverseMap.put('.', i++);
    reverseMap.put('/', i++);
    reverseMap.put('0', i++);
    reverseMap.put('1', i++);
    reverseMap.put('2', i++);
    reverseMap.put('3', i++);
    reverseMap.put('4', i++);
    reverseMap.put('5', i++);
    reverseMap.put('6', i++);
    reverseMap.put('7', i++);
    reverseMap.put('8', i++);
    reverseMap.put('9', i++);
    reverseMap.put(':', i++);
    reverseMap.put(';', i++);
    reverseMap.put('<', i++);
    reverseMap.put('=', i++);
    reverseMap.put('>', i++);
    reverseMap.put('?', i++);
    reverseMap.put('\u00a1', i++);
    reverseMap.put('A', i++);
    reverseMap.put('B', i++);
    reverseMap.put('C', i++);
    reverseMap.put('D', i++);
    reverseMap.put('E', i++);
    reverseMap.put('F', i++);
    reverseMap.put('G', i++);
    reverseMap.put('H', i++);
    reverseMap.put('I', i++);
    reverseMap.put('J', i++);
    reverseMap.put('K', i++);
    reverseMap.put('L', i++);
    reverseMap.put('M', i++);
    reverseMap.put('N', i++);
    reverseMap.put('O', i++);
    reverseMap.put('P', i++);
    reverseMap.put('Q', i++);
    reverseMap.put('R', i++);
    reverseMap.put('S', i++);
    reverseMap.put('T', i++);
    reverseMap.put('U', i++);
    reverseMap.put('V', i++);
    reverseMap.put('W', i++);
    reverseMap.put('X', i++);
    reverseMap.put('Y', i++);
    reverseMap.put('Z', i++);
    reverseMap.put('\u00c4', i++);
    reverseMap.put('\u00d6', i++);
    reverseMap.put('\u00d1', i++);
    reverseMap.put('\u00dc', i++);
    reverseMap.put('\u00a7', i++);
    reverseMap.put('\u00bf', i++);
    reverseMap.put('a', i++);
    reverseMap.put('b', i++);
    reverseMap.put('c', i++);
    reverseMap.put('d', i++);
    reverseMap.put('e', i++);
    reverseMap.put('f', i++);
    reverseMap.put('g', i++);
    reverseMap.put('h', i++);
    reverseMap.put('i', i++);
    reverseMap.put('j', i++);
    reverseMap.put('k', i++);
    reverseMap.put('l', i++);
    reverseMap.put('m', i++);
    reverseMap.put('n', i++);
    reverseMap.put('o', i++);
    reverseMap.put('p', i++);
    reverseMap.put('q', i++);
    reverseMap.put('r', i++);
    reverseMap.put('s', i++);
    reverseMap.put('t', i++);
    reverseMap.put('u', i++);
    reverseMap.put('v', i++);
    reverseMap.put('w', i++);
    reverseMap.put('x', i++);
    reverseMap.put('y', i++);
    reverseMap.put('z', i++);
    reverseMap.put('\u00e4', i++);
    reverseMap.put('\u00f6', i++);
    reverseMap.put('\u00f1', i++);
    reverseMap.put('\u00fc', i++);
    reverseMap.put('\u00e0', i++);
  }

  /**
   *  septet value to java char as dictated by the gsm alphabet
   *  ESC is mapped to \uffff
   *
   *  @param septet septet for which char value to lookup
   *  @return char representation of the septet.
   */
  private static char findChar(byte septet)
  {
    return map[septet];
  }

  /**
   *  java char to septet
   */
  private static int findSeptet(char c)
  {
    return reverseMap.get(c);
  }

  /**
   *  decode() given a String produced from encode() will return the original
   *  blob.
   */
  public static byte[] decode(String septets)
  {
    byte[] ret = new byte[(septets.length()*7)>>3];
    int curr, next;
    int i = 0;
    int k = 0;
    for(int j = 0; (j < ret.length) && (i < septets.length()); ++j)
    {
      curr = findSeptet(septets.charAt(i));
      next = ((i+1) < septets.length()) ? findSeptet(septets.charAt(i+1)) : 0;
      int rec = (curr<<(k+1))|(next>>>(6-k));
      //shift next by 1: advance next by 2
      if((j%6 == 0) && (j>0) && ((j+2) < ret.length))
      {
        ++i;
        ++k;
      }
      ++i;
      k = (k < 7) ? k+1 : 0;

      ret[j] = (byte)(rec&0xff);
    }
    return ret;
  }

  /**
   *  Septets class provides a way to iterate over the packed septets in a byte
   *  array.
   *
   *  This class is both Iterable and an Iterator.
   *  for(byte b : new Septets({0x7f, ..., 0x48}))
   */
  static class Septets implements Iterable<Byte>, Iterator<Byte>
  {
    /**
     *  Member Variables.
     *
     *  i for memory access
     *  j for shifting
     */
    private int septetCount;
    private int i = 0;
    private int j = 0;
    private int prev = 0;
    private byte[] b;

    /**
     *  Septets constructs a new iterator over the septets in given byte array.
     *
     *  @param b byte array to iterate over.
     */
    Septets(byte[] b)
    {
      this.b = b;
      septetCount = (b.length<<3)/7;
    }

    /**
     *  iterator() returns an iterator over the septets in a given byte array.
     */
    @Override
    public Septets iterator()
    {
      return this;
    }

    /**
     *  hasNext() returns whether this iterator can produce another septet.
     */
    @Override
    public boolean hasNext()
    {
      return (septetCount > 0);
    }

    /**
     *  next() produces the next septet in the sequence.
     *
     *  @return next septet in the byte array.
     */
    @Override
    public Byte next()
    {
      int curr = ((i < b.length) ? b[i] : 0)&(0xff);
      int ret = (curr>>>((j&7)+1)|(prev<<(7-(j&7))))&0x7f;

      --septetCount;
      prev = 0;
      if(((j%7) == 0) && (j>0) && (septetCount>1))
      {
        --i;
      }
      prev = curr;
      ++i;
      ++j;

      return (byte)(ret&0x7f);
    }

    @Override
    /**
     *  remove() does nothing.
     */
    public void remove() { }
  }
}
