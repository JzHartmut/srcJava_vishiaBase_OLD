package org.vishia.util;

/**Interface common use-able to append or returns an information about the instance adequate meaning like toString() for debugging and showing. 
 * <br><br>
 * Advantage in comparison of toString(): If a complex information should be necessary only one time a StringBuilder is need to create
 * which gathers all info. Often the content is used on the fly, for example for
 * <pre>
 *   StringBuilder buffer = new StringBuilder(100);
 *   anyInstance.infoAppend(buffer);
 *   buffer.append("some more information");
 *   System.out.append(buffer);
 * </pre> 
 * If this information should be stored it is possible persistent as reference of type {@link java.lang.CharSequence}. 
 * A reference which may changed the StringBuilder content should not be used. 
 * <br>
 * On the other hand
 * {@link #infoAppend(StringBuilder)}.toString() builds a persistent String of this information. The copy of content is done here at least only.
 * <br><br>
 * The info should end with a semicolon. Therewith it is able to parse or use for example in an Excel-csv-Format.
 */  
public interface InfoAppend
{
  /**Version, history and license.
   * <ul>
   * <li>2015-01-04 Hartmut created. 
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String version = "2015-01-04";

  /**Appends or returns the info.
   * 
   * @param u if not null then the info is appended to u, u is returned.
   *   if null then a String can be returned if it is sufficient.
   * @return u if given or a new StringBuilder instance which contains the info or a String if it is sufficient.
   */
  CharSequence infoAppend(StringBuilder u);
  
}
