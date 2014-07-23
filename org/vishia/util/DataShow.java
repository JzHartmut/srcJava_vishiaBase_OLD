package org.vishia.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.xmlSimple.SimpleXmlOutputter;

/**This class supports output of the content of any instance maybe with associated other Objects.
 * It shows the data structure of a class with its content as a snap shoot.
 * @author Hartmut Schorrig
 *
 */
public class DataShow
{
  /**Version, history and license.
   * <ul>
   * <li>2014-07-24 Hartmut chg: Argument maxRecurs necessary.
   * <li>2014-07-24 Hartmut chg: Move the algorithm from to this class org.vishia.util.DataShow. It is an own topic.
   * <li>2014-06-29 Hartmut chg move the algorithm form srcJava_Zbnf:/org/vishia/zcmd/OutputDataTree to {@link org/vishia/jzcmd/JZcmdTester}.
   * <li>2013-07-28 Hartmut chg/new: dataTree now writes XML
   * <li>2013-03-10 Hartmut chg/new: dataTree supports superclass content.
   * <li>2012-12-00 Hartmut improved: dataTree circular references with @ 1234 (address) to mark it.
   * <li>2012-10-08 created. dataTree A presentation of the content of a Java data tree was necessary.
   * </ul>
   * 
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
   * 
   */
  static final public String sVersion = "2014-07-24";

  
  public static void dataTreeXml(Object data, Appendable out, int maxRecurs) throws IOException {
    out.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>\n"); 
    out.append("<!-- written with org.vishia.jbat.OutputDataTree -->\n"); 
    out.append("<data "); 
    final Map<Integer, Object> processedAlready = new TreeMap<Integer, Object>();
    outData(0, maxRecurs, data, out, true, processedAlready);
    out.append("\n</data>\n"); 
  }
  
  
  
  public static void dataTree(Object data, Appendable out, int maxRecurs) throws IOException {
    final Map<Integer, Object> processedAlready = new TreeMap<Integer, Object>();
    outData(0, maxRecurs, data, out, false, processedAlready);
  }
  
  
  
  /**Output of a complex data class.
   * @param recurs
   * @param data
   * @param out
   * @param bXML
   * @throws IOException
   * @return true: one line data, end with "/ >", false: more as one line.
   */
  private static boolean outData(int recurs, int maxRecurs, Object data, Appendable out, boolean bXML, Map<Integer, Object> processedAlready) throws IOException {
    boolean bOneline = false;
    int hash = data.hashCode();
    if(bXML){
      out
      .append(" hash=\"").append(Integer.toHexString(hash))
      .append("\" objtype=\"").append(data.getClass().getName())
      .append("\" toString=\"").append(SimpleXmlOutputter.convertString(data.toString()))
      .append("\""); 
    }
    if(processedAlready.get(hash) !=null){ //prevent circular associations
      if(bXML){
        out.append(" circular=\"1\" />"); bOneline = true; 
      } else {
        out.append(" = ").append(data.toString()).append(" (circular=").append("@"+hash).append(")");
      }
    } else { 
      processedAlready.put(hash, data);
      if(!bXML){
        out.append(" (").append("@" + data.hashCode()).append(") = ");  ///
      }
      if(recurs > 200){
            out.append("\n========================too many recursions\n");
        return false;
      }
      if(bXML){ out.append(" >\n"); }
      Class<?> clazz = data.getClass();
      
      while(clazz !=null){
        
        Field[] fields = clazz.getDeclaredFields();
        for(Field field: fields){
          int modi = field.getModifiers();
          if((modi & Modifier.STATIC)==0){
            field.setAccessible(true);
            outIndent(recurs, out, bXML);
            String sName = field.getName();
            if(sName.equals("whatisit")){
              Debugutil.stop();
            }
            Class<?> type = field.getType();
            String sType = type.getName();
            if(bXML){
              out.append("<data name=\"").append(sName)
              .append("\" reftype=\"").append(sType)
              .append("\"");
            } else {
              out.append(sName).append(" : ").append(sType).append(" = ");
            }
            if(type.isPrimitive()){
              if(bXML){
                out.append("\" value=\"");
              }
              try{
                if(sType.equals("int")){
                  out.append("" + field.getInt(data));
                } else if(sType.equals("short")){
                  out.append("" + field.getShort(data));
                } else if(sType.equals("byte")){
                  out.append("" + field.getByte(data));
                } else if(sType.equals("boolean")){
                  out.append("" + field.getBoolean(data));
                } else if(sType.equals("char")){
                  out.append("" + field.getChar(data));
                } else if(sType.equals("float")){
                  out.append("" + field.getFloat(data));
                } else if(sType.equals("double")){
                  out.append("" + field.getDouble(data));
                } else if(sType.equals("long")){
                  out.append("" + field.getLong(data));
                }
              }catch (Exception exc){
                out.append(" ?access ").append(exc.getMessage());
              }
              if(bXML){
                out.append("\" />");
              }
            } else {
              try{
                Object elementData = field.get(data);
                if(elementData ==null){
                  if(bXML){ out.append(" value=\"null\" />");} else { out.append("null"); }
                } else if(elementData instanceof CharSequence){
                  if(bXML){ 
                    out.append(" value=\"").append(SimpleXmlOutputter.convertString(elementData.toString())).append("\" />");
                  } else {
                    out.append(elementData.toString());
                  }
                } else if(elementData instanceof List<?>){
                  List<?> list = (List<?>)elementData;
                  if(bXML){ 
                    out.append(" listsize=\"").append(Integer.toString(list.size())).append("\" />");
                  } else {
                    out.append(" listsize=\"").append(Integer.toString(list.size()));
                  }
                  if(recurs < maxRecurs){
                    for(Object listElement: list){
                      outData(recurs+1, maxRecurs, listElement, out, bXML, processedAlready);
                    }
                  }
                } else if(elementData instanceof Map<?,?>){
                  Map<?,?> map = (Map<?,?>)elementData;
                  if(bXML){ 
                    out.append(" mapsize=\"").append(Integer.toString(map.size())).append("\" >");
                  } else {
                    out.append(" mapsize=\"").append(Integer.toString(map.size()));
                  }
                  if(recurs < maxRecurs){
                    for(Map.Entry<?,?> element: map.entrySet()){
                      Object key = element.getKey();
                      Object value = element.getValue();
                      outIndent(recurs+1, out, bXML);
                      if(bXML){ 
                        out.append("<Map-Entry key=\"").append(key.toString()).append("\" >");
                      } else {
                        out.append("Map-Entry key=").append(key.toString());
                      }
                      if(recurs+1 < maxRecurs){
                        outData(recurs+2, maxRecurs, value, out, bXML, processedAlready);
                      }
                    }
                  }
                  if(bXML){ 
                    out.append("</Map>");
                  }
                } else {
                  if(bXML){ 
                    out.append(" value=\"").append(SimpleXmlOutputter.convertString(elementData.toString())).append("\" />");
                  } else {
                    out.append(elementData.toString());
                  }
                  if(recurs < maxRecurs){
                    boolean bOneLineElement = outData(recurs+1, maxRecurs, elementData, out, bXML, processedAlready);
                    if(bXML && !bOneLineElement){ outIndent(recurs, out, bXML); out.append("</data>"); }
                  }
                }
              } catch(IllegalAccessException exc){
                out.append(" notAccessible=\"1\" />");
              }
            }
            if(bXML){
              //out.append("\"");
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }
    //out.append("\n");
    return bOneline;
  }
  
  
  static void outIndent(int recurs, Appendable out, boolean bXml) throws IOException{
    if(bXml){
      out.append("\n");
      for(int i = 0; i < recurs; ++i){
        out.append("  ");
      }
      out.append("  ");
      
    } else {
      out.append("\n");
      for(int i = 0; i < recurs; ++i){
        out.append(" |");
      }
      out.append(" +-");
    }
  }
  
  
  
  private static boolean outData(int recurs, String sName, Object dataField, Appendable out, boolean bXML) throws IOException{
    boolean bOneLine = false;
    //if(bXML){ out.append(">"); }
    if(dataField == null){ out.append("null"); }
    else if(dataField instanceof Iterable<?>){
      if(bXML){ out.append(" >"); }
      
      Iterator<?> iter = ((Iterable<?>)dataField).iterator();
      while(iter.hasNext()){
        outIndent(recurs+1, out, bXML); 
        if(bXML){ out.append("<element "); }
        else { out.append(sName).append("[]"); }
        Object element = iter.next();
        if(element == null){
          out.append("value=\"null\" ");
        } else {
          boolean bOneLineElement = outData(recurs+1, sName, element, out, bXML);
          //boolean bOneLineELement = outData(recurs+1, "", iterData, out, bXML);
          if(bXML && !bOneLineElement){ outIndent(recurs+1, out, bXML); out.append("</element>"); }
        }
      }
    } 
    else if(dataField instanceof CharSequence){
      appendContent(dataField, out);
    } 
    //else if(dataField)
    else {
      bOneLine = outData(recurs+1, sName, dataField, out, bXML);
    }
    return bOneLine;
  }
  
  
  private static void appendContent(Object data, Appendable out) throws IOException{
    String content = data.toString();
    content = content.replace("\r\n", "|").replace("\n", "|");
    out.append(content);
  }

  

  
  
  
}
