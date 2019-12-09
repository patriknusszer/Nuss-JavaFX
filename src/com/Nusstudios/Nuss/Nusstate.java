package com.Nusstudios.Nuss;

import com.Nusstudios.Core.IO;
import com.Nusstudios.Core.Util;
import org.json.JSONObject;

import java.io.File;

public class Nusstate {
    public static boolean writeState(String fn, String outdir, String[] ids, int i, String cookieOrEmpty, boolean streamSourceAPI, boolean cipherSourceNusstudios, boolean doubleCheck) {
        JSONObject nusstate = new JSONObject();
        JSONObject details = new JSONObject();
        details.put("type", "PLAYLIST_BACKUP");
        details.put("outdir", outdir);
        details.put("ids", Util.stra2jstra(ids));
        details.put("index", i);
        details.put("cookieOrEmpty", cookieOrEmpty);
        details.put("streamSourceAPI", streamSourceAPI);
        details.put("cipherSourceNusstudios", cipherSourceNusstudios);
        details.put("doubleCheck", doubleCheck);
        nusstate.put("nusstate", details);

        try {
            IO.writeToFile(nusstate.toString(), fn);
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    public static JSONObject readState(String fn, boolean delete) throws Exception {
        JSONObject nusstate = new JSONObject(IO.readAllInUTF8(fn));

        if (delete) {
            new File(fn).delete();
        }

        return nusstate;
    }
}
