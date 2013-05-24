package jp.co.flect.xmlschema.type;

import jp.co.flect.xmlschema.XMLSchema;
import jp.co.flect.xmlschema.SimpleType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.text.ParseException;
import java.text.Format;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.lang.time.DateUtils;

public class DatetimeType extends SimpleType {
	
	public static final String NAME = "dateTime";
	
	private static final long serialVersionUID = -369850237806138462L;
	
	private transient FastDateFormat format;
	private transient String[] parseFormats;
	
	private static String[] calcParseFormats(String s) {
		String ret[] = null;
		if (s.endsWith(".SSSZZ")) {
			ret = new String[4];
			ret[0] = s;
			ret[1] = s.substring(0, s.length()-2);
			ret[2] = s.substring(0, s.length()-6);
			ret[3] = s.substring(0, s.length()-6) + "ZZ";
		} else {
			ret = new String[1];
			ret[0] = s;
		}
		return ret;
	}
	
	public DatetimeType(XMLSchema schema, String name) {
		super(schema, name);
		this.format = FastDateFormat.getInstance(getFormat());
		this.parseFormats = calcParseFormats(getFormat());
	}
	
	protected String getFormat() { return "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";}
	
	public Format getFormatObject() { return this.format;}
	
	@Override
	protected boolean doValid(Object o) {
		if (o instanceof Date) {
			return true;
		}
		try {
			doParse(o.toString());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	@Override
	protected String doFormat(Object o) {
		if (o instanceof Date) {
			return this.format.format((Date)o);
		}
		return o.toString();
	}
	
	@Override
	protected Object doParse(String s) {
		try {
			if (s.endsWith("Z")) {
				s = s.substring(0, s.length()-1) + "+00:00";
			}
			return DateUtils.parseDate(s, parseFormats);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(s);
		}
	}
	
	@Override
	public boolean isDateType() { 
		return true;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.format = FastDateFormat.getInstance(getFormat());
		this.parseFormats = calcParseFormats(getFormat());
	}
}
