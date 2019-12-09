package com.Nusstudios.Nuss.Core;

import com.Nusstudios.Core.DownloadCore;
import com.Nusstudios.Core.Util;
import com.Nusstudios.Nuss.Exceptions.SignatureDecipherException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CipherCore {
    private static char[] slice(char[] signature, int from)
    {
        List<String> list = Util.charArr2ToStrLst(signature);
        String g = String.join("", list);
        g = g.substring(from);
        return g.toCharArray();
    }

    private static char[] splice(char[] signature, int from)
    {
        return slice(signature, from);
    }

    private static char[] swap(char[] signature, int at)
    {
        char c = signature[0];
        signature[0] = signature[at % signature.length];
        signature[at] = c;
        return signature;
    }

    private static char[] reverse(char[] signature)
    {
        List<String> list = Util.charArr2ToStrLst(signature);
        Collections.reverse(list);
        return Util.strLst2CharArr(list);
    }

    public static JSONObject getCipherData(String obfuscatedPlayerSource, int sts) throws SignatureDecipherException {
        Map<String, Object> _result = getCipherData(obfuscatedPlayerSource);
        List<Map<String, String>> functionList = (List<Map<String, String>>)_result.get("root_function_list");
        JSONArray _functionList = new JSONArray();

        for (int i = 0; i < functionList.size(); i++) {
            Map<String, String> function = functionList.get(i);
            JSONObject _function = new JSONObject();
            _function.put("function", function.get("function"));
            _function.put("parameter", function.get("parameter"));
            _functionList.put(_function);
        }

        JSONObject result = new JSONObject();
        result.put("root_function_list", _functionList);
        result.put("do_url_coding", _result.get("do_url_coding"));
        result.put("sts", sts);
        return result;
    }

    // JSON independent algorithm detector
    public static Map<String, Object> getCipherData(String obfuscatedPlayerSource) throws SignatureDecipherException {
        Map<String, Object> result = new HashMap<>();
        String decryptionFunctionName = "NULL";

        for (String pattern : RegexCore.getDecryptionFunctionNamePatterns()) {
            Pattern decryptionFunctionNamePattern = Pattern.compile(pattern);
            Matcher decryptionFunctionNameMatch = decryptionFunctionNamePattern.matcher(obfuscatedPlayerSource);

            if (decryptionFunctionNameMatch.find()) {
                try {
                    decryptionFunctionName = decryptionFunctionNameMatch.group(2);
                    result.put("do_url_coding", false);
                }
                catch (IndexOutOfBoundsException ex) {
                    decryptionFunctionName = decryptionFunctionNameMatch.group(1);
                    result.put("do_url_coding", true);
                }

                break;
            }
        }

        if (decryptionFunctionName.equals("NULL")) {
            throw new SignatureDecipherException("The decryption function name regex failed");
        }

        String escapedFunctionName = Util.StringEscaper(decryptionFunctionName);
        String decryptionFunction = "NULL";
        String signatureObject = "NULL";

        for (String pattern : RegexCore.getDecryptionFunctionPatterns(escapedFunctionName)) {
            Pattern decryptionFunctionPattern = Pattern.compile(pattern);
            Matcher decryptionFunctionMatch = decryptionFunctionPattern.matcher(obfuscatedPlayerSource);

            if (decryptionFunctionMatch.find()) {
                decryptionFunction = decryptionFunctionMatch.group(0);
                signatureObject = decryptionFunctionMatch.group(1);
            }
        }

        if (decryptionFunction.equals("NULL")) {
            throw new SignatureDecipherException("The decryption function regex failed");
        }

        /* in full: wrapper_object_of_wrapped_and_wrapper_functionPattern
        An object wraps a function, which is thus wrapped, but it is also a
        wrapper of the root function that we need
        This variable's full length name is wrapper_object_of_wrapped_and_wrapper_functionPattern */
        Pattern wr_o_of_wd_and_wr_fPattern = Pattern.compile(RegexCore.getWrOfWrAndWdFPattern(signatureObject));
        Matcher wr_o_of_wd_and_wr_fMatch = wr_o_of_wd_and_wr_fPattern.matcher(decryptionFunction);

        if (wr_o_of_wd_and_wr_fMatch.find()) {
            String args_of_wrapped_and_wrapper_function = wr_o_of_wd_and_wr_fMatch.group(1);
            List<Map<String,String>> wrapper_functionList = new ArrayList<Map<String,String>>();
            Pattern all_wr_o_of_wd_and_wr_fGrouperPattern = Pattern.compile(RegexCore.getWrOfWrAndWdFPatternFull());
            Matcher all_wr_o_of_wd_and_wr_fGrouperMatch = all_wr_o_of_wd_and_wr_fGrouperPattern.matcher(decryptionFunction);

            while (all_wr_o_of_wd_and_wr_fGrouperMatch.find())
            {
                Map<String,String> wrappper_function = new HashMap<String,String>();

                if (all_wr_o_of_wd_and_wr_fGrouperMatch.group(3) != null) {
                    wrappper_function.put("wrapper_function_name", all_wr_o_of_wd_and_wr_fGrouperMatch.group(4));
                }
                else {
                    wrappper_function.put("wrapper_function_name", all_wr_o_of_wd_and_wr_fGrouperMatch.group(6));
                }

                wrappper_function.put("wrapper_function_param", all_wr_o_of_wd_and_wr_fGrouperMatch.group(8));
                wrapper_functionList.add(wrappper_function);
            }

            List<Map<String,String>> root_functionList = new ArrayList<Map<String,String>>();

            for (int i = 0; i < wrapper_functionList.size(); i++)
            {
                Map<String,String> wrapper_function = wrapper_functionList.get(i);
                Pattern wrapper_and_function_bodyPattern = Pattern.compile(RegexCore.getWrappedAndWrapperFunctionPattern(wrapper_function.get("wrapper_function_name")));
                Matcher wrapper_andwrapped_function_bodyMatch = wrapper_and_function_bodyPattern.matcher(obfuscatedPlayerSource);

                if (wrapper_andwrapped_function_bodyMatch.find()) {
                    String wrapper_function_body = wrapper_andwrapped_function_bodyMatch.group(1);
                    Map<String,String> root_function = new HashMap<String,String>();

                    if (wrapper_function_body.contains("splice"))
                    {
                        root_function.put("function", "splice");
                        root_function.put("parameter", wrapper_function.get("wrapper_function_param"));
                    }
                    else if (wrapper_function_body.contains("slice"))
                    {
                        root_function.put("function", "slice");
                        root_function.put("parameter", wrapper_function.get("wrapper_function_param"));
                    }
                    else if (wrapper_function_body.contains("reverse"))
                    {
                        root_function.put("function", "reverse");
                        root_function.put("parameter" , null);
                    }
                    else
                    {
                        /* In real code there is no "swap". But since this is the only function
                        to be implemented, it was given the name "swap". This code work because all
                        other functions contain real JS calls with the respective names. So if
                        none of the names are found, it can only be "swap" */
                        root_function.put("function", "swap");
                        root_function.put("parameter", wrapper_function.get("wrapper_function_param"));
                    }

                    root_functionList.add(root_function);
                }
                else {
                    throw new SignatureDecipherException("Root function regex failed");
                }
            }

            result.put("root_function_list", root_functionList);
            return result;
        }
        else {
            // No such example yet
            throw new SignatureDecipherException("Wrapper of wrapper of root function regex failed");
        }
    }

    public static String decryptYoutubeVideoSignature(Map<String, Object> cipherData, String signature) {
        List<Map<String,String>> rootFunctionList = (List<Map<String,String>>)cipherData.get("root_function_list");
        boolean do_url_coding = (boolean)cipherData.get("do_url_coding");
        char[] sigArray = null;

        try {
            sigArray = (do_url_coding ? URLDecoder.decode(signature, "UTF-8") : signature).toCharArray();
        }
        catch (Exception ex) {}

        for (int i = 0; i < rootFunctionList.size(); i++)
        {
            if (rootFunctionList.get(i).get("function").equals("slice"))
            {
                sigArray = slice(sigArray, Integer.valueOf(rootFunctionList.get(i).get("parameter")));
            }
            else if (rootFunctionList.get(i).get("function").equals("splice"))
            {
                sigArray = splice(sigArray, Integer.valueOf(rootFunctionList.get(i).get("parameter")));
            }
            else if (rootFunctionList.get(i).get("function").equals("reverse"))
            {
                sigArray = reverse(sigArray);
            }
            else
            {
                sigArray = swap(sigArray, Integer.valueOf(rootFunctionList.get(i).get("parameter")));
            }
        }

        String str = String.join("", Util.charArr2ToStrLst(sigArray));

        if (do_url_coding) {
            try {
                str = URLEncoder.encode(str, "UTF-8");
            }
            catch (Exception ex) {}
        }

        return str;
    }

    public static String decryptYoutubeVideoSignature(JSONObject cipherData, String signature) throws SignatureDecipherException {
        JSONArray functionList = cipherData.getJSONArray("root_function_list");
        boolean do_url_coding = cipherData.getBoolean("do_url_coding");
        char[] sigArray = null;

        try {
            sigArray = (do_url_coding ? URLDecoder.decode(signature, "UTF-8") : signature).toCharArray();
        }
        catch (Exception ex) {}

        for (int i = 0; i < functionList.length(); i++) {
            String function = functionList.getJSONObject(i).getString("function");

            if (function.equals("slice")) {
                sigArray = slice(sigArray, functionList.getJSONObject(i).getInt("parameter"));
            }
            else if (function.equals("splice")) {
                sigArray = splice(sigArray, functionList.getJSONObject(i).getInt("parameter"));
            }
            else if (function.equals("reverse")) {
                sigArray = reverse(sigArray);
            }
            else if (function.equals("swap")) {
                sigArray = swap(sigArray, functionList.getJSONObject(i).getInt("parameter"));
            }
            else {
                throw new SignatureDecipherException("Unidentified root function found");
            }
        }

        String str = "";

        for (Character c : sigArray) {
            str += c.toString();
        }

        if (do_url_coding) {
            try {
                str = URLEncoder.encode(str, "UTF-8");
            }
            catch (Exception ex) {}
        }

        return str;
    }

    public static JSONObject queryActionList() throws Exception {
        final ByteArrayOutputStream algoQueryBuffer = new ByteArrayOutputStream();

        DownloadCore.download(
                "http://nusstudios.azurewebsites.net/nussapi/queryalgo",
                null,
                20000,
                20000,
                null,
                (chunk, progressTotal, clen) -> {
                    try {
                        algoQueryBuffer.write(chunk);
                    }
                    catch (Exception ex) {

                    }
                },
                null,
                false
        );

        String algoQueryStr = new String(algoQueryBuffer.toByteArray(), "UTF-8");
        return new JSONObject(algoQueryStr);
    }
}
