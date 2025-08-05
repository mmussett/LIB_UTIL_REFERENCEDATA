package com.tibco.bw.royallondon.referencedata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import com.tibco.xml.cxf.common.annotations.XPathFunctionGroup;
import com.tibco.xml.cxf.common.annotations.XPathFunction;
import com.tibco.xml.cxf.common.annotations.XPathFunctionParameter;

@XPathFunctionGroup(category = "Reference Data Functions", prefix = "rfd", namespace = "http://www.royallondon.com/xsd/plugin/lib/util/referencedata", helpText = "Reference data lookup and validation functions")
public class ReferenceData {
	// static final Logger LOG = LoggerFactory.getLogger(ReferenceData.class);

	private final static String ERROR_MARKER = "REFDATAERROR:";
	private final static String EXCEPTION_CODE = "INVALID-REFERENCEDATA";
	private final static String ERROR_CODE = "400";
	private final static String PARAM_CODE = "code";
	private final static String PARAM_TYPECODE = "typeCode";
	private final static String PARAM_RLCODE = "rlCode";
	private final static String PARAM_DOMAIN = "domain";
	private final static String PARAM_DOMAINCODE = "domainCode";
	private final static String PARAM_ERRORCODE = "errorCode";
	private final static String PARAM_EXCEPTIONCODE = "exceptionCode";
	private final static String PARAM_DIRECTION_XIN = "XIN";
	private final static String PARAM_DIRECTION_XOUT = "XOUT";
	private final static String PARAM_DIRECTION_XBTH = "XBTH";
	private final static String PARAM_REFDATA_KEYS = "REFDATA-KEYS-IN-MEMORY";
	private final static String PARAM_CODES_FOR_KEY = "CODES-FOR-KEY-";
	private final static String PARAM_NONE = "NONE";
	private final static String PARAM_MAP_HASHCODE = "MAP-HASHCODE";
	private final static String PREFIX_LISTREF = "LISTREF";
	private final static String PREFIX_EXTENDED = "EXTENDED";
	private final static String DELIMITER = ":;:";
	private final static String START_AND_END_MARKER_FOR_LOG = "@@@";
	private final static String DELIMITER_FOR_LOG = "||";
	private final static String DELIMITER_OF_KEY_VALUE_FOR_LOG = ":";
	private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static String makeReferenceErrorString(String code, String error,
			String... params) {
		StringBuilder reply = new StringBuilder();

		reply.append(START_AND_END_MARKER_FOR_LOG + ERROR_MARKER);
		reply.append(code.toUpperCase());
		reply.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
		reply.append(error);
		reply.append(DELIMITER_FOR_LOG);

		for (int count = 0; count < params.length; count = count + 2) {
			if (count > 0)
				reply.append(DELIMITER_FOR_LOG);
			reply.append(params[count].toUpperCase());
			reply.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
			reply.append(params[count + 1]);
		}

		reply.append(START_AND_END_MARKER_FOR_LOG);

		return reply.toString();
	}

	public abstract class ReferenceDataException extends Exception {
		private static final long serialVersionUID = 5593324582462959793L;

		public ReferenceDataException(String arg0) {
			super(arg0);
		}
	}

	private final class HashEntry {
		final String value;
		final Date expirationDate;

		public HashEntry(String value, Date expirationDate) {
			this.value = value;
			this.expirationDate = expirationDate;

		}

		public String responseFormat() {
			return value;
		}
	}

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, HashEntry>> REF_MAPS = new ConcurrentHashMap<>();;

	public static void main(String[] args) throws ReferenceDataException {

	}

	public ReferenceData() {
		super();
	}

	public void done() {
	}

	/**
	 * 
	 * @param type
	 *            - one of a value of domain or LISTREF, EXTENDED
	 * @param String
	 *            [] typecodes - a list of type codes will be found in hashmap
	 * @return String[] - a list of typecodes having only valid ref data which
	 *         is found from hashmap
	 **/
	public final String[] findTypeCodes(String type, String... typeCodes) {
		ArrayList<String> validTypes = new ArrayList<>();
		ArrayList<String> invalidTypes = new ArrayList<>();

		ConcurrentHashMap<String, HashEntry> typeCodeMap;
		Date currentDate = new Date();
		// to find type code
		for (String typecode : typeCodes) {
			String mapName = type + DELIMITER + typecode;
			typeCodeMap = REF_MAPS.get(mapName);
			if (typeCodeMap != null) {
				if (typeCodeMap.size() > 0) {
					boolean isExpired = false;

					// check expired items
					for (String key : typeCodeMap.keySet()) {
						HashEntry hashEntry = typeCodeMap.get(key);
						if (hashEntry.expirationDate != null
								&& hashEntry.expirationDate
										.compareTo(currentDate) < 0) {
							isExpired = true;
							break;
						}

					}
					// if expired
					if (!isExpired) {
						validTypes.add(typecode);
					} else {
						invalidTypes.add(mapName);
					}
				} else {
					validTypes.add(typecode);
				}
			}
		}
		if (invalidTypes.size() > 0) {
			for (String key : invalidTypes) {
				// remove invalid typecode
				REF_MAPS.remove(key);
			}

		}

		return validTypes.toArray(new String[0]);
	}

	public final void setEntry(String mapname, String key, String value,
			Date expirationDate) {
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(mapname);
		if (map == null) {
			map = new ConcurrentHashMap<String, HashEntry>();
			REF_MAPS.put(mapname, map);
		}
		HashEntry entry = new HashEntry(value, expirationDate);
		map.put(key, entry);
	}

	public final void clear() {
		REF_MAPS.clear();
	}

	public final void clear(String[] typecodes) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, HashEntry>> temp = new ConcurrentHashMap<String, ConcurrentHashMap<String, HashEntry>>(
				REF_MAPS);
		for (String typecode : typecodes) {
			for (String key : temp.keySet()) {
				if (key.endsWith(DELIMITER + typecode)) {
					REF_MAPS.remove(key);
				}
			}
		}
	}

	/**
	 * 
	 * @param type
	 *            possible value - LISTREF, EXTENDED, or domain Name
	 * @param typecode
	 */
	public final void clear(String type, String[] typecodes) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, HashEntry>> temp = new ConcurrentHashMap<String, ConcurrentHashMap<String, HashEntry>>(
				REF_MAPS);
		for (String typecode : typecodes) {
			for (String key : temp.keySet()) {
				if (key.equals(type + DELIMITER + typecode)) {
					REF_MAPS.remove(key);
				}
			}
		}
	}

	public final void removeEntry(String mapname, String key) {
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(mapname);
		if (map != null) {
			map.remove(key);
		}
	}

	/* set the entries */
	public final void setEntries(String mapname, String key[], String value[],
			Date expirationDate[]) {

		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(mapname);
		if (map == null) {
			map = new ConcurrentHashMap<String, HashEntry>();
			REF_MAPS.put(mapname, map);
		}

		for (int iterator = 0; iterator < key.length; ++iterator) {
			HashEntry entry = new HashEntry(value[iterator],
					expirationDate[iterator]);
			map.put(key[iterator], entry);

		}

	}

	/* set the entries for xrefdata */
	public final void setXRefEntry(String domain[], String typeCode[],
			String rlCode[], String domainCode[], String direction[],
			String expirationDate[]) throws ParseException {
		for (int i = 0; i < expirationDate.length; i++) {
			setEntry(domain[i] + DELIMITER + typeCode[i], rlCode[i] + DELIMITER
					+ domainCode[i], direction[i], new SimpleDateFormat(
					DATE_FORMAT).parse(expirationDate[i]));
		}
	}
	
	/* set emptyRef */
	public final void setEmpty(String type, String[] typeCode) {

		for (int i = 0; i < typeCode.length; i++) {
			REF_MAPS.put(type + DELIMITER + typeCode[i], new ConcurrentHashMap<>());
		}
	}
	
	/* set the entries for listRef */
	public final void setListRefEntry(String[] typeCode, String[] code,
			String[] value, String[] expirationDate) throws ParseException {

		for (int i = 0; i < expirationDate.length; i++) {
			setEntry(PREFIX_LISTREF + DELIMITER + typeCode[i], code[i],
					value[i],
					new SimpleDateFormat(DATE_FORMAT).parse(expirationDate[i]));
		}
	}

	/* set the entries for Extended refdata */
	public final void setExtendedEntry(String typeCode, String value,
			Date expirationDate) throws ParseException {
		setEntry(PREFIX_EXTENDED + DELIMITER + typeCode, typeCode, value,
				expirationDate);
	}

	/* get the entries from Extended refdata */
	public final String getExtendedEntry(String typeCode) {
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(PREFIX_EXTENDED
				+ DELIMITER + typeCode);
		if (map != null) {
			Date currentDate = new Date();

			if (map.get(typeCode).expirationDate != null
					&& map.get(typeCode).expirationDate.compareTo(currentDate) > -1)
				return map.get(typeCode).value;
		}
		return null;

	}

	/* find the entries */

	@XPathFunction(helpText = "Lookup the reference data value for a given typeCode and code combination.  An exception will be thrown if any of the parameters are unknown", parameters = {
			@XPathFunctionParameter(name = PARAM_TYPECODE, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_CODE, optional = false, optionalValue = "") })
	public final String findEntriesForTypeCode(String typeCode, String code) {
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(typeCode);
		if (map != null) {
			HashEntry entry = map.get(code);
			if (entry != null) {
				return entry.responseFormat();

			}
		}
		return new String();
	}

	// getDomainCode function to fetch domain code for combination of domain,
	// typeCode and rlCode
	@XPathFunction(helpText = "Lookup the reference data domaincode value for a given domain , typeCode and rlCode combination.  An exception will be thrown if any of the parameters are unknown", parameters = {
			@XPathFunctionParameter(name = PARAM_DOMAIN, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_TYPECODE, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_RLCODE, optional = false, optionalValue = "") })
	public final String getDomainCode(String domain, String typeCode,
			String rlCode) throws ReferenceDataException {
		String result = null;
		String mapName = domain + DELIMITER + typeCode; // example of mapname -
														// XSON:;:ACTY
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(mapName);
		if (map != null) {
			for (String key : map.keySet()) {
				// example of key - RLCODE:;:DOMAINCODE
				String tempKey = rlCode + DELIMITER; // RLCODE:;:
				if (key.startsWith(tempKey)) {
					HashEntry hashEntry = map.get(key);
					if (hashEntry.value.endsWith(PARAM_DIRECTION_XOUT)) {
						result = key.substring(tempKey.length());
						break;
					} else if (hashEntry.value.endsWith(PARAM_DIRECTION_XBTH)) {
						result = key.substring(tempKey.length());
						break;
					}
				}
			}
		}

		if (result == null) {
			result = validateEntriesForTypeCode(typeCode, rlCode);
		}
		return result;
	}

	// getRLCode function to fetch RL code for combination of domain, typeCode
	// and domainCode

	@XPathFunction(helpText = "Lookup the reference data RLCode value for a given domain , typeCode and domainCode combination.  An exception will be thrown if any of the parameters are unknown", parameters = {
			@XPathFunctionParameter(name = PARAM_DOMAIN, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_TYPECODE, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_DOMAINCODE, optional = false, optionalValue = "") })
	public final String getRLCode(String domain, String typeCode,
			String domainCode) throws ReferenceDataException {
		String result = null;
		// example of mapname = sonata:;:PRDT
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(domain
				+ DELIMITER + typeCode);
		if (map != null) {
			for (String key : map.keySet()) {
				// example of key - RLCODE:;:DOMAINCODE
				String tempKey = DELIMITER + domainCode; // :;:DOMAINCODE
				if (key.endsWith(tempKey)) {
					HashEntry hashEntry = map.get(key);
					if (hashEntry.value.endsWith(PARAM_DIRECTION_XIN)) {
						result = key.substring(0,
								key.length() - tempKey.length());
						break;
					} else if (hashEntry.value.endsWith(PARAM_DIRECTION_XBTH)) {
						result = key.substring(0,
								key.length() - tempKey.length());
						break;
					}
				}
			}
		}

		if (result == null) {
			result = validateEntriesForTypeCode(typeCode, domainCode);
		}
		return result;

	}

	// validateEntriesForTypeCode function to fetch/validate code for
	// combination of typeCode and code
	@XPathFunction(helpText = "validate the reference data value for a given typeCode and code combination.  An exception will be thrown if any of the parameters are unknown", parameters = {
			@XPathFunctionParameter(name = PARAM_TYPECODE, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_CODE, optional = false, optionalValue = "") })
	public final String validateEntriesForTypeCode(String typeCode, String code)
			throws ReferenceDataException {
		boolean isFound = false;
		String mapName = PREFIX_LISTREF + DELIMITER + typeCode;
		ConcurrentHashMap<String, HashEntry> validatetypecode = REF_MAPS
				.get(mapName);

		if (validatetypecode != null) {
			HashEntry typeCodeRecord = validatetypecode.get(code);
			if (typeCodeRecord != null) {
				Date currentDate = new Date();
				if (typeCodeRecord.expirationDate != null
						|| typeCodeRecord.expirationDate.compareTo(currentDate) > -1) {
					isFound = true;
				}
			}
		}
		if (isFound) {
			return code;
		} else {
			String typeCodesInMemory = retrieveTypeCodesInMemory();
			String codesForTypeCode = retrieveCodesForTypeCode(mapName);
			String objectHashCode = getMapHashCode();
			throw new ReferenceDataTypeCodeException(typeCode, code,
					typeCodesInMemory, codesForTypeCode, objectHashCode);
		}
	}

	public final String getMapHashCode() {
		String mapHashCode = "";
		if (System.identityHashCode(REF_MAPS) > 0) {
			return Integer.toString(System.identityHashCode(REF_MAPS));
		}
		return mapHashCode;
	}

	public final String retrieveTypeCodesInMemory() {
		StringBuilder typeCodes = new StringBuilder("");

		if (REF_MAPS.keySet().size() > 0) {
			for (String mapKey : REF_MAPS.keySet()) {
				typeCodes.append(mapKey + ",");
			}

			typeCodes.deleteCharAt(typeCodes.length() - 1);

		} else {
			typeCodes.append(PARAM_NONE);
		}

		return typeCodes.toString();

	}

	public final String retrieveCodesForTypeCode(String typeCode) {
		StringBuilder codes = new StringBuilder("");
		ConcurrentHashMap<String, HashEntry> map = REF_MAPS.get(typeCode);

		if (map == null) {
			codes.append(PARAM_NONE);
		} else {
			for (String key : map.keySet()) {
				codes.append(key);
				codes.append(",");

			}
			codes.deleteCharAt(codes.length() - 1);
		}

		return codes.toString();
	}

	private class ReferenceDataTypeCodeException extends ReferenceDataException {
		private static final long serialVersionUID = 7368708856165953841L;

		public ReferenceDataTypeCodeException(String typeCode, String code,
				String keysInMemory, String codesForKey, String mapHashCode) {
			super(makeReferenceErrorString(PARAM_ERRORCODE, ERROR_CODE,
					PARAM_EXCEPTIONCODE, EXCEPTION_CODE, PARAM_TYPECODE,
					typeCode, PARAM_CODE, code, PARAM_REFDATA_KEYS,
					keysInMemory, PARAM_CODES_FOR_KEY + typeCode, codesForKey,
					PARAM_MAP_HASHCODE, mapHashCode));
		}

	}

	@XPathFunction(helpText = "validate the reference data value for a given typeCode and code combination.  An exception will be thrown if any of the parameters are unknown", parameters = {
			@XPathFunctionParameter(name = PARAM_DOMAIN, optional = false, optionalValue = ""),
			@XPathFunctionParameter(name = PARAM_TYPECODE, optional = false, optionalValue = "") })
	public final boolean validateEntriesForDomain(String domain, String typeCode) {
		ConcurrentHashMap<String, HashEntry> validatedomain = REF_MAPS
				.get(domain + DELIMITER + domain);
		if (validatedomain != null) {
			return true;
		} else {
			return false;
		}

	}

	// findMapsEntries will return the amount of data in a particular hashmap
	// based on mapnames
	public final String[] findMapsEntries() {
		String[] mapRecords = new String[REF_MAPS.size() + 3];
		mapRecords[0] = "Start--" + System.identityHashCode(REF_MAPS);
		mapRecords[1] = "COUNT-OF-KEYS-IN-MEMORY:" + REF_MAPS.size();
		int counter = 2;
		int mapSize = 0;
		StringBuilder mapRecord;
		if (REF_MAPS.size() > 0) {
			for (String key : REF_MAPS.keySet()) {
				mapSize = REF_MAPS.get(key) != null ? REF_MAPS.get(key).size()
						: 0;
				mapRecord = new StringBuilder();

				mapRecord.append("KEY");
				mapRecord.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
				mapRecord.append(key);
				mapRecord.append(DELIMITER_FOR_LOG);
				mapRecord.append("COUNT-OF-CODES-FOR-KEY-");
				mapRecord.append(key);
				mapRecord.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
				mapRecord.append(mapSize);
				mapRecords[counter] = mapRecord.toString();
				counter++;
			}
		}
		mapRecords[counter] = "End--" + System.identityHashCode(REF_MAPS);
		return mapRecords;
	}

	public final String[] findMapDetails(String mapName) {

		String[] mapRecords = new String[REF_MAPS.size() + 1];
		mapRecords[0] = "COUNT-OF-KEYS-IN-MEMORY:" + REF_MAPS.size();
		int counter = 1;
		int mapSize = 0;
		StringBuilder mapRecord;
		if (REF_MAPS.size() > 0) {
			for (String key : REF_MAPS.keySet()) {
				ConcurrentHashMap<String, HashEntry> submap = REF_MAPS.get(key);
				mapSize = submap != null ? submap.size() : 0;
				mapRecord = new StringBuilder();

				mapRecord.append("KEY");
				mapRecord.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
				mapRecord.append(key);
				mapRecord.append(DELIMITER_FOR_LOG);
				mapRecord.append("NUMBER-OF-ROWS-FOR-KEY-");
				mapRecord.append(key);
				mapRecord.append(DELIMITER_OF_KEY_VALUE_FOR_LOG);
				mapRecord.append(mapSize);
				mapRecords[counter] = mapRecord.toString();
				counter++;
			}
		}

		return mapRecords;
	}
}