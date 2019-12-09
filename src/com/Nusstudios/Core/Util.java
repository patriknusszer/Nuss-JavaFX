package com.Nusstudios.Core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static String StringEscaper(String unescapedString)
    {
        return unescapedString.replaceAll("[-\\/\\^$*+?.() |\\[\\]{ }]", "\\$&");
    }

    public static Object update(Object newItem, String itemJPtr, Object base) {
        if (itemJPtr.lastIndexOf('/') != -1) {
            if (itemJPtr.lastIndexOf('/') != 0) {
				String tokenSupersetJPtr = itemJPtr.substring(0, itemJPtr.lastIndexOf('/'));
				String tokenKey = itemJPtr.substring(itemJPtr.lastIndexOf('/') + 1);
				Object token = newItem;

            	do {
            		// java substring's startIndex is inclusive, endIndex is exclusive
	            	Object tokenSuperset = null;

	            	if (base instanceof JSONObject) {
	            		tokenSuperset = ((JSONObject)base).query(tokenSupersetJPtr);
	            	}
	            	else {
	            		tokenSuperset = ((JSONArray)base).query(tokenSupersetJPtr);
	            	}

	            	if (tokenSuperset instanceof JSONObject) {
	            		JSONObject _tokenSuperset = ((JSONObject)tokenSuperset);
	            		_tokenSuperset.put(tokenKey, token);
	            		tokenSuperset = _tokenSuperset;
	            	}
	            	else if (tokenSuperset instanceof JSONArray) {
	            		JSONArray _tokenSuperset = ((JSONArray)tokenSuperset);
	            		_tokenSuperset.put(Integer.valueOf(tokenKey), token);
	            		tokenSuperset = _tokenSuperset;
	            	}

	            	token = tokenSuperset;
                    tokenKey = tokenSupersetJPtr.substring(tokenSupersetJPtr.lastIndexOf('/') + 1);
	            	tokenSupersetJPtr = tokenSupersetJPtr.substring(0, tokenSupersetJPtr.lastIndexOf('/'));
            	} while (tokenSupersetJPtr.lastIndexOf('/') != -1);

                if (base instanceof JSONObject) {
                    JSONObject _base = (JSONObject)base;
                    _base.put(tokenKey, token);
                    return _base;
                }
                else {
                    JSONArray _base = (JSONArray)base;
                    _base.put(Integer.valueOf(tokenKey), token);
                    return _base;
                }
            }
            else {
            	if (base instanceof JSONObject) {
            		JSONObject _base = (JSONObject)base;
            		_base.put(itemJPtr.substring(1), newItem);
            		return _base;
            	}
            	else {
            		JSONArray _base = (JSONArray)base;
            		_base.put(Integer.valueOf(itemJPtr.substring(1)), newItem);
            		return _base;
            	}
            }
        }
        else {
            return null;
        }
    }

    public static AbstractMap.SimpleEntry<Integer, Boolean> toNUnit(String unit)
    {
        int nUnit = 0;

        if (unit.equals("B") || unit.equals("b") || unit.equals("Byte") || unit.equals("byte") || unit.equals("Bit") || unit.equals("bit"))
        {
            nUnit = 0;
        }
        else if (unit.equals("kB") || unit.equals("kb") || unit.equals("KByte") || unit.equals("kbyte") || unit.equals("KBit") || unit.equals("kbit"))
        {
            nUnit = 1;
        }
        else if (unit.equals("MB") || unit.equals("mb") || unit.equals("MByte") || unit.equals("mbyte") || unit.equals("MBit") || unit.equals("mbit"))
        {
            nUnit = 2;
        }
        else if (unit.equals("GB") || unit.equals("gb") || unit.equals("GByte") || unit.equals("gbyte") || unit.equals("GBit") || unit.equals("gbit"))
        {
            nUnit = 3;
        }
        else if (unit.equals("TB") || unit.equals("tb") || unit.equals("TByte") || unit.equals("tbyte") || unit.equals("TBit") || unit.equals("tbit"))
        {
            nUnit = 4;
        }

        AbstractMap.SimpleEntry<Integer, Boolean> ret;


        if (unit.contains("it")) {
            ret = new AbstractMap.SimpleEntry<Integer, Boolean>(nUnit, true);
        }
        else {
            ret= new AbstractMap.SimpleEntry<Integer, Boolean>(nUnit, false);
        }

        return ret;
    }

    public static String toUnit(int nUnit, boolean inBits, boolean inShortIfInByte)
    {
        String unit = "Byte";

        if (nUnit == 0)
        {
            unit = "Byte";
        }
        else if (nUnit == 1)
        {
            unit = "KByte";
        }
        else if (nUnit == 2)
        {
            unit = "MByte";
        }
        else if (nUnit == 3)
        {
            unit = "GByte";
        }
        else if (nUnit == 4)
        {
            unit = "TByte";
        }

        if (inBits) {
            unit = unit.replace("Byte", "Bit");
        }
        else if (inShortIfInByte) {
            unit = unit.replace("Byte", "B");
        }

        return unit;
    }

    public static String getSize(BigDecimal length, String unit, int limit, boolean doRound, boolean inShortIfInByte)
    {
        AbstractMap.SimpleEntry<Integer, Boolean> nUnit = toNUnit(unit);
        int exp = nUnit.getKey();

        while (length.toBigInteger().compareTo(BigInteger.valueOf(limit)) == 1)
        {
            length =  length.divide(BigDecimal.valueOf(1024));

            if (doRound) {
                length = length.setScale(2, RoundingMode.HALF_UP);
            }

            exp++;
        }

        return length + " " + toUnit(exp, nUnit.getValue(), inShortIfInByte);
    }

    public static List<String> charArr2ToStrLst(char[] array) {
        List<String> list = new ArrayList<String>();

        for (char c : array) {
            list.add(String.valueOf(c));
        }

        return  list;
    }

    public static char[] strLst2CharArr(List<String> list) {
        char[] array = new char[list.size()];

        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).charAt(0);
        }

        return array;
    }

    public static String[] strl2a(List<String> arr) {
        String[] _arr = new String[arr.size()];

        for ( int i = 0; i < arr.size(); i++) {
            _arr[i] = arr.get(i);
        }

        return _arr;
    }

    public static String[] jstra2stra(JSONArray array) {
        String[] arr = new String[array.length()];

        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getString(i);
        }

        return arr;
    }

    public static JSONArray stra2jstra(String[] array) {
        JSONArray arr = new JSONArray();

        for (int i = 0; i < array.length; i++) {
            arr.put(array[i]);
        }

        return arr;
    }
}
