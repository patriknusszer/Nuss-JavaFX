package com.Nusstudios.Nuss.Core;

import com.Nusstudios.Core.DownloadCore;
import com.Nusstudios.Nuss.Exceptions.ConfigurationLoadException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by patriknusszer on 05/06/17.
 */
public class Youtube {
    private static JSONObject getYoutubeVideoInfoAPI(String videoid, boolean useExistingAlgo, String cookieOrEmpty) throws Exception
    {
        JSONObject jsonConfig = ConfigurationCore.getYoutubeVideoConfiguration(videoid, cookieOrEmpty);
        JSONObject cipherData = new JSONObject();
        int sts = -1;

        if (useExistingAlgo) {
            cipherData = CipherCore.queryActionList();
            sts = cipherData.getInt("sts");
        }
        else {
            String videoPlayerURL = "https://youtube.com" + jsonConfig.getJSONObject("assets").getString("js");
            sts = jsonConfig.getInt("sts");
            final ByteArrayOutputStream playerSourceBuffer = new ByteArrayOutputStream();
            final JSONObject report = new JSONObject();
            final int singleConnectionMaxErrorCount = 20;
            final JSONArray exception_log = new JSONArray();
            final JSONObject download_config = new JSONObject();
            final Object retrySleepLock = new Object();
            download_config.put("singleConnectionMaxErrorCount", 20);
            download_config.put("hashCheckFailureMaxErrorCount", 5);
            download_config.put("retryLaterMaxCount", 2);
            download_config.put("retryLaterCount", 0);

            DownloadCore.download(
                    videoPlayerURL,
                    null,
                    20000,
                    20000,
                    (status) -> {
                        JSONObject response = null;

                        switch (status.getString("status")) {
                            case "exception":
                                response = new JSONObject();
                                JSONObject exception = status.getJSONObject("exception");
                                exception_log.put(exception.getString("message"));

                                if (exception.getInt("error_count") >= singleConnectionMaxErrorCount) {
                                    if (download_config.getInt("retryLaterCount") == download_config.getInt("retryLaterMaxCount")) {
                                        report.put("result", "exception");
                                        report.put("exception", exception_log);
                                        response.put("retry", false);
                                    }
                                    else {
                                        try {
                                            Runnable awakener = () -> {
                                                try {
                                                    Thread.sleep(300000);

                                                    /* This thread will be able to acquire the lock
                                                    retrySleepLock and be able to enter the block despite
                                                    the fact that the parent thread already has it.
                                                    This is only possible in case wait() is called on this lock,
                                                    thus communicating nothing but that it is waiting for the next
                                                    thread to acquire the same lock and call notify() on the lock object */
                                                    synchronized (retrySleepLock) {
                                                        retrySleepLock.notify();
                                                    }
                                                }
                                                catch (Exception ex) { }
                                            };

                                            synchronized (retrySleepLock) {
                                                new Thread(awakener).start();
                                                retrySleepLock.wait();
                                            }

                                            response.put("retry", true);
                                        }
                                        catch (Exception ex) { }
                                    }
                                }
                                else {
                                    response.put("retry", true);
                                }

                                break;
                            case "success":
                                report.put("result", "success");
                                break;
                        }

                        return response;
                    },
                    (chunk, progressTotal, clen) -> {
                        try {
                            playerSourceBuffer.write(chunk);
                        }
                        catch (Exception ex) {

                        }
                    },
                    null,
                    false
            );

            if (report.getString("result").equals("exception")) {
                ConfigurationLoadException cle = new ConfigurationLoadException("Failed to download player");
                JSONObject extendedDescription = new JSONObject();
                extendedDescription.put("exception", report.getJSONArray("exception"));
                cle.extendedDescription = extendedDescription;
                throw cle;
            }

            String playerSource = new String(playerSourceBuffer.toByteArray(), "UTF-8");
            cipherData = CipherCore.getCipherData(playerSource, sts);
        }

        return ConfigurationCore.reloadYoutubeVideoConfigurationAPI(ConfigurationCore.getYoutubeVideoConfigurationAPI(videoid, sts), cipherData);
    }

    private static JSONObject getYoutubeVideoInfo(String videoid, boolean useExistingAlgo, String cookieOrEmpty) throws Exception
    {
        return ConfigurationCore.reloadYoutubeVideoConfiguration(ConfigurationCore.getYoutubeVideoConfiguration(videoid, cookieOrEmpty), useExistingAlgo);
    }

    public static JSONObject getStreamInfo(String id, boolean streamSourceAPI, boolean cipherSourceNusstudios, String cookieOrEmpty) throws Exception
    {
        if (streamSourceAPI) {
            return getYoutubeVideoInfoAPI(id, cipherSourceNusstudios, cookieOrEmpty);
        }
        else {
            return getYoutubeVideoInfo(id, cipherSourceNusstudios, cookieOrEmpty);
        }
    }
}
