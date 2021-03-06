package com.caucho.quercus.env.xdebug;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.program.ClassField;

public abstract class XdebugCommand
{
	public final static String PARAM_TRANSACTION_ID = "-i";
	public final static String PARAM_DATA = "--";
	public final static String CONTEXT_ID_LOCALS = "0";
    public final static String CONTEXT_ID_GLOBALS = "1";
	
	public XdebugResponse getResponse(XdebugConnection conn, String[] requestParts) {
		if (requestParts.length  < 1) {
			throw new RuntimeException("invalid command (no command name given)");
		}
		String commandName = requestParts[0];
		Map<String, String> parameters = new HashMap<String, String>();
		String currentKey = null;
		
		for (int i = 1;  i < requestParts.length; i++) {
			String currentPart = requestParts[i];
			if (currentKey != null) {
				parameters.put(currentKey, currentPart);
				currentKey = null;
			} else if (currentPart.startsWith("-")){
				currentKey = currentPart;
			} else {
				throw new RuntimeException("Did not expect value without leading dash '-'");
			}
		}
		String transactionId = parameters.get(PARAM_TRANSACTION_ID);
		return getInternalResponse(commandName, parameters, transactionId, conn);
	}
	
	protected String getBase64DecodedData(Map<String, String> parameters) {
		String base64String = parameters.get(PARAM_DATA);
		if (base64String == null) {
			return null;
		} else {
			return new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(base64String));
		}
	}
	
	protected abstract XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters, String transactionId, XdebugConnection conn);

  protected String createPropertyElement(Value value, XdebugConnection conn,
      String name, String fullname, String facet, boolean includeChildren) {
    try {
        String type = null;
        String size = null;
        String serializedValue = null;
        String stringValue = null;
        if (value instanceof Var) {
          value = ((Var) value).toValue();
        }
        if (value instanceof UnsetValue) {
          type = "undefined";
          serializedValue = "";
        } else if (value == null || value.isNull()) {
          type = "null";
          serializedValue = "";
        } else if (value.isBoolean()) {
          type = "bool";
          serializedValue = value.toBoolean() ? "1" : "0";
        } else if (value.isString()) {
          type = "string";
          stringValue = value.toString();
        } else if (value.isLongConvertible()) {
          type = "int";
          stringValue = value.toString();
        } else if (value.isDoubleConvertible()) {
          type = "float";
          stringValue = value.toString();
        }
        if (stringValue != null) {
          size = "" + stringValue.length();
          serializedValue = javax.xml.bind.DatatypeConverter
              .printBase64Binary(stringValue.getBytes());
        }
        String propertyPrefix = "<property " + (name != null ? "name=\"" + safe(name) + "\" " : "")
            + (fullname != null ? "fullname=\"" + safe(fullname) + "\" " : "") 
            + (facet != null ? "facet=\"" + facet + "\" " : "");
        if (serializedValue != null) {
          return propertyPrefix + "type=\"" + type + "\""
            + (size == null ? "" : " size=\"" + size + "\" encoding=\"base64\"")
            + ">" + "<![CDATA[" + serializedValue + "]]></property>";
        } else {
          QuercusClass quercusClass = value.getQuercusClass();
          if (conn.getEnv().getQuercus().getStdClass().equals(quercusClass)) {
            StringBuilder childrenSb = new StringBuilder();
            int childrenCount = 0;
            Iterator<Entry<Value, Value>> iter = value.getBaseIterator(conn.getEnv());
            while (iter.hasNext()) {
              Entry<Value, Value> entry = iter.next();
              childrenCount++;
              String propName = entry.getKey().toString();
              childrenSb.append(createPropertyElement(entry.getValue(), conn, propName, fullname == null ? null : fullname + "->" + propName, null, false));
            }
            StringBuilder sb = new StringBuilder();
            sb.append(propertyPrefix + "type=\"object\" classname=\"" + quercusClass.getClassName() + "\" children=\"" + (childrenCount < 1 ? "0" : "1") 
                + "\" numchildren=\"" + childrenCount + "\"" + (name == null ? " page=\"0\" pagesize=\"100\"" : "") + ">");
            if (includeChildren) {
              sb.append(childrenSb);
            }
            sb.append("</property>");
            return sb.toString();
          } else if (quercusClass != null && !quercusClass.isArray()) {
            HashMap<StringValue, ClassField> fields = quercusClass.getClassFields();
            StringBuilder sb = new StringBuilder();
            sb.append(propertyPrefix + "type=\"object\" classname=\"" + quercusClass.getClassName() + "\" children=\"" + (fields.isEmpty() ? "0" : "1") 
                + "\" numchildren=\"" + fields.size() + "\"" + (name == null ? " page=\"0\" pagesize=\"100\"" : "") + ">");
            if (includeChildren) {
              for (Entry<StringValue, ClassField> entry : fields.entrySet()) {
                String propName = entry.getKey().toString();
                sb.append(createPropertyElement(getField(conn, value, entry.getKey()), conn, propName, fullname == null ? null : fullname + "->" + propName, getFacet(entry.getValue()), false));
              }
            }
            sb.append("</property>");
            return sb.toString();
          } else if (value.isArray()) {
            StringBuilder sb = new StringBuilder();
            int count = value.getCount(conn.getEnv());
            sb.append(propertyPrefix + "type=\"array\" children=\"" + (count < 1 ? "0" : "1") 
                + "\" numchildren=\"" + count + "\"" + (name == null ? " page=\"0\" pagesize=\"100\"" : "") + ">");
            if (includeChildren) {
              Iterator<Entry<Value, Value>> iter = value.getBaseIterator(conn.getEnv());
              while (iter.hasNext()) {
                Entry<Value, Value> entry = iter.next();
                String indexName = entry.getKey().toString();
                sb.append(createPropertyElement(entry.getValue(), conn, indexName, fullname == null ? null : fullname + "['" + indexName.replaceAll("'", "\\'") + "']", null, false));
              }
            }
            sb.append("</property>");
            return sb.toString();
          } else {
            throw new IllegalStateException("Could not handle value " + value);
          }
        }
    } catch (Exception e) {
      e.printStackTrace(System.err);
      return "";
    }
      }

  private String getFacet(ClassField classField) {
    if (classField.isPrivate()) {
      return "private";
    }
    if (classField.isProtected()) {
      return "protected";
    }
    if (classField.isPublic()) {
      return "public";
    }
    return null;
  }
  
  private String safe(String propValue) {
    return propValue.replaceAll("<", "&lt;");
  }
  
  protected Value getField(XdebugConnection conn, Value value,
      StringValue fieldName) {
    try {
      Value result = value.getField(conn.getEnv(), fieldName);
      if (result.isNull()) {
        result = value.getThisField(conn.getEnv(), fieldName);
      }
      if (result.isNull()) {
        ClassField field = value.getQuercusClass().getClassField(fieldName);
        if (field != null) {
          result = value.getField(conn.getEnv(), field.getCanonicalName());
        }
      }
      return result;
    } catch (Exception e) {
      return NullValue.NULL;
    }
  }
}
