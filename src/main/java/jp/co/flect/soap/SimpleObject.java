package jp.co.flect.soap;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.type.DatetimeType;
import jp.co.flect.xmlschema.type.Base64BinaryType;
import jp.co.flect.xmlschema.type.DateType;
import jp.co.flect.xmlschema.type.TimeType;
import jp.co.flect.util.StringUtils;
import jp.co.flect.util.Base64;
import static jp.co.flect.util.StringUtils.checkNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * 名前と値をMapで保持するシンプルなオブジェクトです。
 */
public class SimpleObject implements StAXConstruct<SimpleObject>, Cloneable, Serializable {
	
	public enum OptimizeLevel {
		NONE,
		NORMAL,
		ROBUST
	}
	
	private static final long serialVersionUID = -4070761370065364135L;
	
	private ComplexType typeDef;
	private Map<String, Object> map;
	private OptimizeLevel optimize = OptimizeLevel.NORMAL;
	
	/**
	 * コンストラクタ
	 */
	public SimpleObject() {
		this(null);
	}
	
	/**
	 * スキーマ定義を指定するコンストラクタ
	 */
	public SimpleObject(ComplexType typeDef) {
		this.map = new HashMap<String, Object>();
		this.typeDef = typeDef;
	}
	
	/**
	 * スキーマ定義を返します。nullの場合もあります。
	 */
	public ComplexType getType() { return this.typeDef;}
	/**
	 * スキーマ定義を設定します。
	 */
	public void setType(ComplexType type) {this.typeDef = type;}
	
	/**
	 * ラップしているMapを返します。
	 */
	public Map<String, Object> getMap() { return this.map;}
	
	/**
	 * build時の最適化レベルを返します。<br>
	 * NONE   - 最適化しない
	 * NORMAL - 「null」は追加しない
	 * ROBUST - 「null」「false」「0」は追加しない
	 */
	public OptimizeLevel getOptimizeLevel() { return this.optimize;}
	
	
	/**
	 * build時の最適化レベルを設定します。
	 */
	protected void setOptimizeLevel(OptimizeLevel v) { this.optimize = v;}
	
	/**
	 * 値が設定されている名前の一覧を返します。
	 */
	public List<String> getNameList() {
		List<String> list = new ArrayList<String>(this.map.keySet());
		Collections.sort(list);
		return list;
	}
	
	/**
	 * 子要素のスキーマ定義を返します。
	 * 名前空間は無視されます。
	 * nullの場合もあります。
	 */
	protected TypeDef getSoapType(String name) {
		if (this.typeDef == null) {
			return null;
		}
		Iterator<ElementDef> it = this.typeDef.modelIterator();
		while (it.hasNext()) {
			ElementDef el = it.next();
			if (el.getName().equals(name)) {
				return el.getType();
			}
		}
		return null;
	}
	
	/**
	 * その名前に値が設定されているかどうかを返します。
	 */
	public boolean contains(String name) {
		return this.map.containsKey(name);
	}
	
	/**
	 * 名前に対応する値を返します。
	 */
	public Object get(String name) {
		return this.map.get(name);
	}
	
	/**
	 * 名前に対応する値を設定します。
	 */
	public void set(String name, Object value) {
		this.map.put(name, value);
	}
	
	/**
	 * 名前に対応する値を文字列で返します。<br>
	 * 値がない場合はnullが返ります。
	 */
	public String getString(String name) {
		Object o = get(name);
		if (o instanceof byte[]) {
			o = SimpleType.getBuiltinType(Base64BinaryType.NAME).format(o);
		} else if (!(o instanceof String)) {
			TypeDef type = getSoapType(name);
			if (type != null && type.isSimpleType()) {
				o = ((SimpleType)type).format(o);
			}
		}
		return o == null ? null : o.toString();
	}
	
	/**
	 * 名前に対応する値をlongで返します。<br>
	 * 値がない場合は0が返ります。
	 */
	public long getLong(String name) {
		Object o = get(name);
		if (o == null) {
			return 0;
		} else if (o instanceof Number) {
			return ((Number)o).longValue();
		} else if (o instanceof String) {
			return Long.parseLong((String)o);
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をintで返します。<br>
	 * 値がない場合は0が返ります。
	 */
	public int getInt(String name) {
		Object o = get(name);
		if (o == null) {
			return 0;
		} else if (o instanceof Number) {
			return ((Number)o).intValue();
		} else if (o instanceof String) {
			return Integer.parseInt((String)o);
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をdoubleで返します。<br>
	 * 値がない場合は0が返ります。
	 */
	public double getDouble(String name) {
		Object o = get(name);
		if (o == null) {
			return 0;
		} else if (o instanceof Number) {
			return ((Number)o).doubleValue();
		} else if (o instanceof String) {
			return Double.parseDouble((String)o);
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をbooleanで返します。<br>
	 * 値がない場合はfalseが返ります。
	 */
	public boolean getBoolean(String name) {
		Object o = get(name);
		if (o == null) {
			return false;
		} else if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue();
		} else if (o instanceof String) {
			return Boolean.valueOf((String)o).booleanValue();
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をDateで返します。<br>
	 * 値がない場合はnullが返ります。
	 */
	public Date getDate(String name) {
		Object o = get(name);
		if (o == null) {
			return null;
		} else if (o instanceof Date) {
			return (Date)o;
		} else if (o instanceof String) {
			String s = (String)o;
			SimpleType type = null;
			if (s.indexOf('T') != -1) {
				type = SimpleType.getBuiltinType(DatetimeType.NAME);
			} else if (s.indexOf('-') != -1) {
				type = SimpleType.getBuiltinType(DateType.NAME);
			} else if (s.indexOf(':') != -1) {
				type = SimpleType.getBuiltinType(TimeType.NAME);
			} else {
				throw new IllegalArgumentException(name);
			}
			return (Date)type.parse(s);
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * 名前に対応する値をbyte[]で返します。<br>
	 * 値がない場合はnullが返ります。
	 */
	public byte[] getBinary(String name) {
		Object o = get(name);
		if (o == null) {
			return null;
		} else if (o instanceof byte[]) {
			return (byte[])o;
		} else if (o instanceof String) {
			String s = (String)o;
			if (Base64.isBase64(s)) {
				return Base64.decode(s);
			}
			throw new IllegalArgumentException(name);
		} else {
			throw new IllegalArgumentException(name);
		}
	}
	
	/**
	 * XMLStreamReaderからこのオブジェクトを構築します。
	 */
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		int depth = 1;
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
					if (!startElement(reader)) {
						depth++;
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
			}
		}
	}
	
	protected boolean startElement(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		String name = reader.getLocalName();
		ElementDef el = null;
		TypeDef type = null;
		ComplexType selfDef = getType();
		if (selfDef != null) {
			el = selfDef.getModel(selfDef.getNamespace(), name);
			if (el != null) {
				type = el.getType();
				if (!type.isSimpleType()) {
					return false;
				}
			}
		}
		String strValue = checkNull(reader.getElementText());
		Object value = strValue;
		if (type != null) {
			value = checkType((SimpleType)type, strValue);
		} else  if (this.optimize == OptimizeLevel.ROBUST) {
			//最適化 -「false」「0」は追加しない
			if ("false".equalsIgnoreCase(strValue)) {
				value = null;
			} else if ("0".equalsIgnoreCase(strValue)) {
				value = null;
			}
		}
		if (optimize == OptimizeLevel.NONE || value != null) {
			if (el != null && el.hasOccurs()) {
				Object oldValue = get(name);
				if (oldValue != null) {
					if (oldValue instanceof List) {
						((List)oldValue).add(value);
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(oldValue);
						list.add(value);
						set(name, list);
					}
				} else {
					set(name, value);
				}
			} else {
				set(name, value);
			}
		}
		return true;
	}
	
	private Object checkType(SimpleType type, String value) {
		if (value == null) {
			return null;
		}
		Object ret = type.parse(value);
		if (this.optimize == OptimizeLevel.ROBUST) {
			if (Boolean.FALSE.equals(ret)) {
				ret = null;
			} else if (ret instanceof Number && ((Number)ret).intValue() == 0) {
				ret = null;
			}
		}
		return ret;
	}
	
	public String toString() { 
		StringBuilder buf = new StringBuilder();
		buildString(buf, 0);
		return buf.toString();
	}
	
	/**
	 * このオブジェクトの内容を文字列化します。
	 */
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		List<String> list = getNameList();
		for (String key : list) {
			Object o = get(key);
			if (o == null) {
				continue;
			}
			if (this.optimize == OptimizeLevel.ROBUST) {
				if (o instanceof Boolean && !((Boolean)o).booleanValue()) {
					continue;
				}
				if (o instanceof Number && ((Number)o).intValue() == 0) {
					continue;
				}
			}
			
			String value = getString(key);
			if (buf.length() > 0) {
				buf.append("\n");
			}
			buf.append(strIndent)
				.append(key).append(": ")
				.append(value);
		}
	}
	
	/**
	 * このオブジェクトの新しいインスタンスを返します。
	 */
	public SimpleObject newInstance() {
		try {
			SimpleObject ret = (SimpleObject)super.clone();
			ret.map = new HashMap<String, Object>();
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	protected <T extends Enum<T>> T getEnumValue(Class<T> enumClass, String name) {
		String s = getString(name);
		return s == null ? null : Enum.valueOf(enumClass, s);
	}
}
