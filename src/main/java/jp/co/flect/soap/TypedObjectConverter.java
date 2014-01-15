package jp.co.flect.soap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jp.co.flect.soap.annotation.SoapField;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.SimpleType;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.util.ExtendedMap;

public class  TypedObjectConverter {
	
	public interface FieldResolver {
		
		public Object resolve(Class clazz, Object value);
		
	}
	
	private static Map<Class, FieldResolver> fieldResolverMap = new HashMap<Class, FieldResolver>();
	
	public static void addFieldResolver(Class c, FieldResolver resolver) {
		fieldResolverMap.put(c, resolver);
	}
	
	public static void removeFieldResolver(Class c) {
		fieldResolverMap.remove(c);
	}
	
	private static FieldResolver getFieldResolver(Class c) {
		FieldResolver ret = fieldResolverMap.get(c);
		if (ret != null) {
			return ret;
		}
		for (Map.Entry<Class, FieldResolver> entry : fieldResolverMap.entrySet()) {
			Class ec = entry.getKey();
			if (ec.isAssignableFrom(c)) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	private ComplexType type;
	private Class clazz;
	
	private Map<String, Field> fieldMap = new HashMap<String, Field>();
	private Map<String, MethodInfo> methodMap = new HashMap<String, MethodInfo>();
	
	public <T extends TypedObject> TypedObjectConverter(ComplexType type, Class<T> clazz) {
		this.type = type;
		this.clazz = clazz;
		
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			String fieldName = f.getName();
			SoapField api = f.getAnnotation(SoapField.class);
			if (api != null) {
				String apiName = api.value();
				if (apiName == null || apiName.length() == 0) {
					apiName = fieldName;
				}
				f.setAccessible(true);
				this.fieldMap.put(apiName, f);
			}
		}
		Method[] methods = clazz.getDeclaredMethods();
		for (Method m : methods) {
			String fieldName = m.getName();
			SoapField api = m.getAnnotation(SoapField.class);
			if (api != null) {
				String apiName = api.value();
				if (apiName == null || apiName.length() == 0) {
					apiName = fieldName;
				}
				MethodInfo info = this.methodMap.get(apiName);
				if (info == null) {
					info = new MethodInfo();
					this.methodMap.put(apiName, info);
				}
				info.set(m);
				m.setAccessible(true);
			}
		}
	}
	
	public String getTargetName() { return this.type.getName();}
	public ComplexType getTargetType() { return this.type;}
	public Class getTargetClass() { return this.clazz;}
	
	public ExtendedMap toMap(TypedObject obj) {
		if (obj.getClass() != this.clazz) {
			throw new IllegalArgumentException(obj.getClass().getName());
		}
		ExtendedMap map = new ExtendedMap();
		for (Map.Entry<String, Field> entry : this.fieldMap.entrySet()) {
			String name = entry.getKey();
			Field f = entry.getValue();
			
			ElementDef el = type.getModel(type.getNamespace(), name);
			if (el == null) {
				continue;
			}
			try {
				Object value = f.get(obj);
				if (value != null) {
					putValue(map, name, value, el);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		for (Map.Entry<String, MethodInfo> entry : this.methodMap.entrySet()) {
			String name = entry.getKey();
			MethodInfo info = entry.getValue();
			if (info.getter == null) {
				continue;
			}
			Method m = info.getter;
			ElementDef el = type.getModel(type.getNamespace(), name);
			if (el == null) {
				continue;
			}
			try {
				Object value = m.invoke(obj);
				if (value != null) {
					putValue(map, name, value, el);
				}
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		return map;
	}
	
	public TypedObject toObject(XMLStreamReader reader) throws XMLStreamException {
		try {
			TypedObject ret = (TypedObject)this.clazz.newInstance();
			
			boolean hasValue = false;
			boolean endValue = false;
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						hasValue = true;
						String nsuri = reader.getNamespaceURI();
						String name = reader.getLocalName();
						ElementDef el = type.getModel(nsuri, name);
						if (el == null) {
							throw new IllegalStateException("Unknown element: " + nsuri + ", " + name);
						}
						TypeDef elType = el.getType();
						if (elType.isSimpleType()) {
							Object value = parseSimple((SimpleType)elType, reader);
							if (value != null) {
								setValue(ret, name, value);
							}
						} else {
							ComplexType ct = (ComplexType)elType;
							TypedObjectConverter cc = ct.getTypedObjectConverter();
							if (cc == null) {
								throw new IllegalStateException("Unknown type.: " + nsuri + ", " + name);
							}
							TypedObject value = cc.toObject(reader);
							if (value != null) {
								setValue(ret, name, value);
							}
						}
						break;
					case XMLStreamReader.CHARACTERS:
					case XMLStreamReader.CDATA:
						if (!reader.isWhiteSpace()) {
							throw new IllegalStateException();
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						endValue = true;
						break;
					case XMLStreamReader.START_DOCUMENT:
					case XMLStreamReader.END_DOCUMENT:
					case XMLStreamReader.ATTRIBUTE:
					case XMLStreamReader.NAMESPACE:
					case XMLStreamReader.SPACE:
					case XMLStreamReader.COMMENT:
					case XMLStreamReader.PROCESSING_INSTRUCTION:
					case XMLStreamReader.ENTITY_REFERENCE:
					case XMLStreamReader.DTD:
						throw new IllegalStateException();
				}
				if (endValue) {
					break;
				}
			}
			return hasValue ? ret : null;
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private Object parseSimple(SimpleType type, XMLStreamReader reader) throws XMLStreamException {
		StringBuilder buf = new StringBuilder();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					buf.append(reader.getText());
					break;
				case XMLStreamReader.END_ELEMENT:
					return buf.length() > 0 ? type.parse(buf.toString()) : null;
				case XMLStreamReader.START_DOCUMENT:
				case XMLStreamReader.END_DOCUMENT:
				case XMLStreamReader.START_ELEMENT:
				case XMLStreamReader.ATTRIBUTE:
				case XMLStreamReader.NAMESPACE:
				case XMLStreamReader.SPACE:
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.PROCESSING_INSTRUCTION:
				case XMLStreamReader.ENTITY_REFERENCE:
				case XMLStreamReader.DTD:
					throw new IllegalStateException();
			}
		}
		throw new IllegalStateException();
	}
	private void putValue(ExtendedMap map, String name, Object value, ElementDef el) {
		if (el.hasOccurs() && value instanceof List) {
			List list = new ArrayList();
			for (Object obj : (List)value) {
				list.add(convertObject(el, obj));
			}
			map.put(name, list);
		} else {
			map.put(name, convertObject(el, value));
		}
	}
	
	private Object convertObject(ElementDef el, Object value) {
		if (el.getType().isSimpleType()) {
			SimpleType type = (SimpleType)el.getType();
			return type.format(value);
		} else if (value instanceof TypedObject) {
			ComplexType type = (ComplexType)el.getType();
			TypedObjectConverter cc = type.getTypedObjectConverter();
			if (cc != null) {
				return cc.toMap((TypedObject)value);
			}
		}
		throw new IllegalStateException("Unknown type.: " + el.getNamespace() + ", " + el.getName());
	}
	
	private void setValue(TypedObject obj, String name, Object value) {
		try {
			Field f = fieldMap.get(name);
			if (f != null) {
				if (List.class.isAssignableFrom(f.getType())) {
					List list = (List)f.get(obj);
					if (list == null) {
						list = new ArrayList();
						f.set(obj, list);
					}
					list.add(value);
					return;
				}
				value = convertType(f.getType(), value);
				f.set(obj, value);
				return;
			}
			MethodInfo info = methodMap.get(name);
			if (info != null && info.setter != null) {
				Method m = info.setter;
				value = convertType(m.getParameterTypes()[0], value);
				m.invoke(obj, value);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private Object convertType(Class c, Object value) {
		FieldResolver resolver = getFieldResolver(c);
		if (resolver != null) {
			return resolver.resolve(c, value);
		}
		if (c == String.class) return value.toString();
		if (value instanceof Number) {
			Number n = (Number)value;
			if (c == Integer.class) return n.intValue();
			if (c == Long.class) return n.longValue();
			if (c == Double.class) return n.doubleValue();
			if (c == Float.class) return n.floatValue();
			if (c == Byte.class) return n.byteValue();
			if (c == Short.class) return n.shortValue();
			if (c == BigDecimal.class) return new BigDecimal(n.toString());
			if (c == BigInteger.class) return new BigInteger(n.toString());
		}
		if (c.isEnum()) {
			String str = value.toString();
			try {
				Method m = c.getDeclaredMethod("values");
				Object[] enums = (Object[])m.invoke(null);
				for (Object o : enums) {
					if (str.equals(o.toString())) {
						return o;
					}
				}
				throw new IllegalArgumentException(c.getName() + ": " + str);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
		return value;
	}
	
	private static class MethodInfo {
		
		public Method getter;
		public Method setter;
		
		public void set(Method m) {
			if (isGetter(m)) {
				this.getter = m;
			} else if (isSetter(m)) {
				this.setter = m;
			} else {
				throw new IllegalStateException("Invalid method: " + m.getName());
			}
		}
		
		private boolean isGetter(Method m) {
			Class rt = m.getReturnType();
			if (rt == null || rt == Void.TYPE) {
				return false;
			}
			Class[] params = m.getParameterTypes();
			return params == null || params.length == 0;
		}
		
		private boolean isSetter(Method m) {
			Class[] params = m.getParameterTypes();
			return params != null && params.length == 1;
		}
	}
}
