package com.Nusstudios.Nuss.Core;

import com.Nusstudios.Core.DownloadCore;
import com.Nusstudios.Core.IO;
import com.Nusstudios.Core.Util;
import com.Nusstudios.Nuss.Exceptions.ConfigurationLoadException;
import com.Nusstudios.Nuss.Exceptions.ConfigurationRegexException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationCore {
    public static JSONObject getYoutubeVideoConfiguration(String videoid, String cookieOrEmpty) throws Exception {
        final ByteArrayOutputStream videoPageBuffer = new ByteArrayOutputStream();
        String url;
        Map<String, String> reqHeaders = null;

        if (!cookieOrEmpty.isEmpty()) {
            url = "https://www.youtube.com/watch?v=" + videoid + "&has_verified=1";
            reqHeaders = new HashMap<>();
            reqHeaders.put("Cookie", cookieOrEmpty);
        }
        else {
            url = "https://www.youtube.com/watch?v=" + videoid;
            reqHeaders = new HashMap<>();
            reqHeaders.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.1 Safari/605.1.15");
            reqHeaders.put("Accept", " text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            reqHeaders.put("Accept-Language", "en-gb");
            reqHeaders.put("Connection", "keep-alive");
            reqHeaders.put("Host", "www.youtube.com");
        }

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
                url,
                reqHeaders,
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
                        videoPageBuffer.write(chunk);
                    }
                    catch (Exception ex) {

                    }
                },
                null,
                false
        );

        if (report.getString("result").equals("exception")) {
            ConfigurationLoadException cle = new ConfigurationLoadException("Failed to download video page");
            JSONObject extendedDescription = new JSONObject();
            extendedDescription.put("exception", report.getJSONArray("exception"));
            cle.extendedDescription = extendedDescription;
            throw cle;
        }

        String body = new String(videoPageBuffer.toByteArray(), "UTF-8");
        Pattern jsonDetector = Pattern.compile(RegexCore.VideoPageStreamConfigRegex);
        Matcher jsonMatch = jsonDetector.matcher(body);

        if (jsonMatch.find()) {
            String jsonConfigStr = jsonMatch.group(1);
            JSONObject config = new JSONObject(jsonConfigStr);
            String adaptive_fmtsData = (String)config.query("/args/adaptive_fmts");
            String url_encoded_fmt_stream_mapData = (String)config.query("/args/url_encoded_fmt_stream_map");
            JSONArray adaptive_fmts = decodeStreamData(adaptive_fmtsData);
            JSONArray url_encoded_fmt_stream_map = decodeStreamData(url_encoded_fmt_stream_mapData);
            config = (JSONObject) Util.update(adaptive_fmts, "/args/adaptive_fmts", config);
            config = (JSONObject) Util.update(url_encoded_fmt_stream_map, "/args/url_encoded_fmt_stream_map", config);
            return config;
        }
        else {
            throw new ConfigurationRegexException("Video configuration regex failed on video page");
        }
    }

    public static JSONObject getYoutubeVideoConfigurationAPI(String videoid, int sts) throws Exception {
        final ByteArrayOutputStream getVideoInfoBuffer = new ByteArrayOutputStream();
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
            "https://www.youtube.com/get_video_info?video_id=" + videoid + "&asv=3&eurl=" + "https://www.youtube.com/watch?v=" + videoid + "&sts=" + sts,
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
                    getVideoInfoBuffer.write(chunk);
                }
                catch (Exception ex) {

                }
            },
            null,
            false
        );

        if (report.getString("result").equals("exception")) {
            ConfigurationLoadException cle = new ConfigurationLoadException("Failed to download get_video_info data");
            JSONObject extendedDescription = new JSONObject();
            extendedDescription.put("exception", report.getJSONArray("exception"));
            cle.extendedDescription = extendedDescription;
            throw cle;
        }

        return decodeAPIVideoConfiguration(new String(getVideoInfoBuffer.toByteArray(), "UTF-8"));
    }

    private static JSONArray decodeStreamData(String streamData) throws Exception {
        String[] streams = mapArray(streamData);
        JSONArray _streams = new JSONArray();

        for (int i = 0; i < streams.length; i++) {
            JSONObject stream = mapNextJSONLevel(streams[i]);
            _streams.put(stream);
        }

        return _streams;
    }

    private static JSONObject decodeAPIVideoConfiguration(String configurationAPI) throws Exception {
        JSONObject configMap = mapNextJSONLevel(configurationAPI);
        String adaptive_fmtsData = configMap.getString("adaptive_fmts");
        String url_encoded_fmt_stream_mapData = configMap.getString("url_encoded_fmt_stream_map");
        JSONArray adaptive_fmts = decodeStreamData(adaptive_fmtsData);
        JSONArray url_encoded_fmt_stream_map = decodeStreamData(url_encoded_fmt_stream_mapData);
        configMap.put("adaptive_fmts", adaptive_fmts);
        configMap.put("url_encoded_fmt_stream_map", url_encoded_fmt_stream_map);
        return configMap;
    }

    private static JSONObject mapNextJSONLevel(String and) throws Exception {
        String[] kEqV = and.split("&");
        JSONObject jobject = new JSONObject();

        for (int i = 0; i < kEqV.length; i++)
        {
            String key = kEqV[i].substring(0, kEqV[i].indexOf('='));
            String value = kEqV[i].substring(kEqV[i].indexOf('=') + 1);
            // Current level ands already removed, encoded next level ands can be exposed
            value = URLDecoder.decode(value, "UTF-8");
            jobject.put(key, value);
        }

        return jobject;
    }

    private static String[] mapArray(String commaData) {
        String[] subValueArray = commaData.split("\\s*,\\s*");
        return subValueArray;
    }

    public static String[] getIDs(String playlistURL, String cookie) throws Exception {
        List<String> ids = null;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
        requestHeaders.put("Cookie", cookie);
        ByteArrayOutputStream playlistPageBuffer = new ByteArrayOutputStream();
        final JSONObject report1 = new JSONObject();
        final int singleConnectionMaxErrorCount = 20;
        final JSONArray exception_log1 = new JSONArray();
        final JSONObject download_config = new JSONObject();
        final Object retrySleepLock1 = new Object();
        download_config.put("singleConnectionMaxErrorCount", 20);
        download_config.put("hashCheckFailureMaxErrorCount", 5);
        download_config.put("retryLaterMaxCount", 2);
        download_config.put("retryLaterCount", 0);

        DownloadCore.download(
            playlistURL,
            requestHeaders,
            100000,
            100000,
            (status) -> {
                JSONObject response = null;

                switch (status.getString("status")) {
                    case "exception":
                        response = new JSONObject();
                        JSONObject exception = status.getJSONObject("exception");
                        exception_log1.put(exception.getString("message"));

                        if (exception.getInt("error_count") >= singleConnectionMaxErrorCount) {
                            if (download_config.getInt("retryLaterCount") == download_config.getInt("retryLaterMaxCount")) {
                                report1.put("result", "exception");
                                report1.put("exception", exception_log1);
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
                                            synchronized (retrySleepLock1) {
                                                retrySleepLock1.notify();
                                            }
                                        }
                                        catch (Exception ex) { }
                                    };

                                    synchronized (retrySleepLock1) {
                                        new Thread(awakener).start();
                                        retrySleepLock1.wait();
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
                        report1.put("result", "success");
                        break;
                }

                return null;
            },
            (chunk, progressTotal, clen) -> {
                try {
                    playlistPageBuffer.write(chunk);
                }
                catch (Exception ex) {

                }
            },
            null,
            false
        );

        if (report1.getString("result").equals("exception")) {
            ConfigurationLoadException cle = new ConfigurationLoadException("Failed to download playlist page");
            JSONObject extendedDescription = new JSONObject();
            extendedDescription.put("exception", report1.getJSONArray("exception"));
            cle.extendedDescription = extendedDescription;
            throw cle;
        }

        String playlistPage = new String(playlistPageBuffer.toByteArray(), "UTF-8");
        String playlistConfigStr = null;

        for (String pattern : RegexCore.getPlaylistPageStreamConfigPatterns()) {
            Pattern plstptrn = Pattern.compile(pattern);
            Matcher plstmtchr = plstptrn.matcher(playlistPage);

            if (plstmtchr.find()) {
                playlistConfigStr = plstmtchr.group(1);
                break;
            }
        }

        if (playlistConfigStr == null) {
            throw new ConfigurationRegexException("Playlist page stream config regex failed");
        }

        JSONObject playlistConfig = new JSONObject(playlistConfigStr);
        ids = new ArrayList<>();
        String browse_ajax = null;
        JSONObject nextContinuationData = null;
        JSONArray tabs = (JSONArray)playlistConfig.query("/contents/twoColumnBrowseResultsRenderer/tabs");
        JSONObject tab = tabs.getJSONObject(0);
        JSONArray sectionListRendererContents = (JSONArray) tab.query("/tabRenderer/content/sectionListRenderer/contents");
        JSONObject sectionListRendererContent = sectionListRendererContents.getJSONObject(0);
        JSONArray itemsSectionRendererContents = (JSONArray) sectionListRendererContent.query("/itemSectionRenderer/contents");
        JSONObject itemSectionRendererContent = itemsSectionRendererContents.getJSONObject(0);
        JSONArray playlistVideoListRendererContents = (JSONArray) itemSectionRendererContent.query("/playlistVideoListRenderer/contents");
        JSONObject playlistVideoListRenderer = itemSectionRendererContent.getJSONObject("playlistVideoListRenderer");

        for (int n = 0; n < playlistVideoListRendererContents.length(); n++) {
            JSONObject playlistVideoListRendererContent = playlistVideoListRendererContents.getJSONObject(n);
            JSONObject playlistVideoRenderer = playlistVideoListRendererContent.getJSONObject("playlistVideoRenderer");
            String videoId = playlistVideoRenderer.getString("videoId");
            ids.add(videoId);
        }

        if (playlistVideoListRenderer.has("continuations")) {
            nextContinuationData = (JSONObject) playlistVideoListRenderer.query("/continuations/0/nextContinuationData");

            do {
                String ctokenOrContinuation = nextContinuationData.getString("continuation");
                String itctOrClickTrackingParams = nextContinuationData.getString("clickTrackingParams");
                browse_ajax = "https://www.youtube.com/browse_ajax?ctoken=" + ctokenOrContinuation + "&continuation=" + ctokenOrContinuation + "&itct=" + itctOrClickTrackingParams;
                requestHeaders = getYoutubeHeaders(playlistPage, YoutubeSourceType.PlaylistPage);
                requestHeaders.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");

                if (!cookie.isEmpty()) {
                    requestHeaders.put("Cookie", cookie);
                }

                ByteArrayOutputStream continuationBuffer = new ByteArrayOutputStream();
                final JSONObject report2 = new JSONObject();
                final JSONArray exception_log2 = new JSONArray();
                final Object retrySleepLock2 = new Object();
                download_config.put("singleConnectionMaxErrorCount", 20);
                download_config.put("hashCheckFailureMaxErrorCount", 5);
                download_config.put("retryLaterMaxCount", 2);
                download_config.put("retryLaterCount", 0);

                DownloadCore.download(
                    browse_ajax,
                    requestHeaders,
                    20000,
                    20000,
                    (status) -> {
                        JSONObject response = null;

                        switch (status.getString("status")) {
                            case "exception":
                                response = new JSONObject();
                                JSONObject exception = status.getJSONObject("exception");
                                exception_log2.put(exception.getString("message"));

                                if (exception.getInt("error_count") >= singleConnectionMaxErrorCount) {
                                    if (download_config.getInt("retryLaterCount") == download_config.getInt("retryLaterMaxCount")) {
                                        report2.put("result", "exception");
                                        report2.put("exception", exception_log2);
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
                                                    synchronized (retrySleepLock2) {
                                                        retrySleepLock2.notify();
                                                    }
                                                }
                                                catch (Exception ex) { }
                                            };

                                            synchronized (retrySleepLock2) {
                                                new Thread(awakener).start();
                                                retrySleepLock2.wait();
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
                                report2.put("result", "success");
                                break;
                        }

                        return response;
                    },
                    (chunk, progressTotal, clen) -> {
                        try {
                            continuationBuffer.write(chunk);
                        }
                        catch (Exception ex) {

                        }
                    },
                    null,
                    false
                );

                if (report2.getString("result").equals("exception")) {
                    ConfigurationLoadException cle = new ConfigurationLoadException("Failed to download playlist continuation");
                    JSONObject extendedDescription = new JSONObject();
                    extendedDescription.put("exception", report2.getJSONArray("exception"));
                    cle.extendedDescription = extendedDescription;
                    throw cle;
                }

                String str = new String(continuationBuffer.toByteArray(), "UTF-8");
                JSONArray continuationConfig = new JSONArray(str);
                JSONObject playlistVideolistContinuation = (JSONObject) continuationConfig.query("/1/response/continuationContents/playlistVideoListContinuation");
                JSONArray playlistVideoListContinuationContents = playlistVideolistContinuation.getJSONArray("contents");

                for (int i = 0; i < playlistVideoListContinuationContents.length(); i++) {
                    JSONObject playlistVideoListContinuationContent = playlistVideoListContinuationContents.getJSONObject(i);
                    JSONObject playlistVideoRenderer = playlistVideoListContinuationContent.getJSONObject("playlistVideoRenderer");
                    String videoId = playlistVideoRenderer.getString("videoId");
                    ids.add(videoId);
                }

                if (playlistVideolistContinuation.has("continuations")) {
                    nextContinuationData = (JSONObject) playlistVideolistContinuation.query("/continuations/0/nextContinuationData");
                }
                else {
                    nextContinuationData = null;
                }

            } while (nextContinuationData != null);
        }

        return Util.strl2a(ids);
    }

    private static Map<String, String> getYoutubeHeaders(String source, YoutubeSourceType yst) {
        if (yst.equals(YoutubeSourceType.PlaylistPage)) {
            Pattern ytcfgConfigPtrn = Pattern.compile(RegexCore.PlaylistPageHeadersConfigRegex);
            Matcher ytfgConfigMtchr = ytcfgConfigPtrn.matcher(source);

            if (ytfgConfigMtchr.find()) {
                Map<String, String> headers = new HashMap<>();
                JSONObject ytcfg = new JSONObject(ytfgConfigMtchr.group(1));

                if (ytcfg.has("ID_TOKEN")) {
                    headers.put("x-youtube-identity-token", ytcfg.optString("ID_TOKEN"));
                }

                if (ytcfg.has("VARIANTS_CHECKSUM")) {
                    headers.put("x-youtube-variants-checksum", ytcfg.optString("VARIANTS_CHECKSUM"));
                }

                if (ytcfg.has("INNERTUBE_CONTEXT_CLIENT_NAME")) {
                    headers.put("x-youtube-client-name", String.valueOf(ytcfg.optString("INNERTUBE_CONTEXT_CLIENT_NAME")));
                }

                if (ytcfg.has("INNERTUBE_CONTEXT_CLIENT_VERSION")) {
                    headers.put("x-youtube-client-version", String.valueOf(ytcfg.optString("INNERTUBE_CONTEXT_CLIENT_VERSION")));
                }

                // Unnecessary but present headers below

                if (ytcfg.has("PAGE_BUILD_LABEL")) {
                    headers.put("x-youtube-page-label", ytcfg.optString("PAGE_BUILD_LABEL"));
                }

                if (ytcfg.has("PAGE_CL")) {
                    headers.put("x-youtube-page-cl", ytcfg.optString("PAGE_CL"));
                }

                headers.put("x-youtube-utc-offset", String.valueOf(TimeZone.getTimeZone("UTC").getOffset(new Date().getTime())));
                return headers;
            }

            return null;
        }
        else {
            return null;
        }
    }

    private enum YoutubeSourceType {
        PlaylistPage
    }

    private enum StreamDataType {
        adaptive_fmts,
        url_encoded_fmt_stream_map
    }

    private static JSONArray reloadStreams(JSONArray streams, StreamDataType streamDataType, JSONObject cipherData) throws Exception {
        JSONArray _streams = new JSONArray();

        for (int i = 0; i < streams.length(); i++)
        {
            JSONObject stream = streams.getJSONObject(i);

            if (stream.has("s")) {
                String signatureFieldName = stream.getString("sp");
                String deciphered_signature = CipherCore.decryptYoutubeVideoSignature(cipherData, stream.getString("s"));
                stream.put("deciphered_signature", deciphered_signature);
                stream.put("url", stream.getString("url") + "&" + signatureFieldName + "=" + deciphered_signature);
            }

            String type = stream.getString("type");
            String[] tmp1 = type.split("\\s*;+\\s*");
            String mediaTypePerContainer = tmp1[0];
            String[] tmp2 = mediaTypePerContainer.split("/");
            String mediaType = tmp2[0];
            String container = tmp2[1];
            stream.put("container", container);

            if (streamDataType.equals(StreamDataType.adaptive_fmts)) {
                String codecprop = tmp1[1];
                String codec = codecprop.substring(codecprop.indexOf("\"") + 1, codecprop.lastIndexOf("\""));
                stream.put("codec", codec);
                stream.put("media_type", mediaType);
            }
            else if (streamDataType.equals(StreamDataType.url_encoded_fmt_stream_map)) {
                stream.put("media_type", "audiovisual");

                if (tmp1.length == 2) {
                    stream.put("has_codecs", true);
                    String codecsprop = tmp1[1];
                    String codecs = codecsprop.substring(codecsprop.indexOf("\"") + 1, codecsprop.lastIndexOf("\""));
                    String[] tmp3 = codecs.split("\\s*,+\\s*");
                    String vCodec = tmp3[0];
                    String aCodec = tmp3[1];
                    stream.put("video_codec", vCodec);
                    stream.put("audio_codec", aCodec);
                }
                else {
                    stream.put("has_codecs", false);
                }
            }

            if (streamDataType.equals(StreamDataType.adaptive_fmts)) {
                stream.put("clen_converted", Util.getSize(stream.getBigDecimal("clen"), "Byte", 400, true, true));
                stream.put("bitrate_converted", Util.getSize(stream.getBigDecimal("bitrate"), "Bit", 400, true, true));
            }

            _streams.put(stream);
        }

        return _streams;
    }

    public static JSONObject reloadYoutubeVideoConfigurationAPI(JSONObject decodedAPIVideoConfiguration, JSONObject cipherData) throws Exception
    {
        JSONArray adaptive_fmts = decodedAPIVideoConfiguration.getJSONArray("adaptive_fmts");
        JSONArray url_encoded_fmt_stream_map = decodedAPIVideoConfiguration.getJSONArray("url_encoded_fmt_stream_map");
        String title = decodedAPIVideoConfiguration.getString("title");
        String video_id = decodedAPIVideoConfiguration.getString("video_id");
        JSONArray adaptive_fmtsStreamMap = reloadStreams(adaptive_fmts, ConfigurationCore.StreamDataType.adaptive_fmts, cipherData);
        JSONArray url_encoded_fmt_stream_mapStreamMap = reloadStreams(url_encoded_fmt_stream_map, ConfigurationCore.StreamDataType.url_encoded_fmt_stream_map, cipherData);
        JSONObject youtubeStreamWrapper = new JSONObject();
        youtubeStreamWrapper.put("adaptive_fmts", adaptive_fmtsStreamMap);
        youtubeStreamWrapper.put("url_encoded_fmt_stream_map", url_encoded_fmt_stream_mapStreamMap);
        youtubeStreamWrapper.put("title", URLDecoder.decode(title.replaceAll("\\+", " "), "UTF-8"));
        youtubeStreamWrapper.put("video_id", video_id);
        return youtubeStreamWrapper;
    }

    public static JSONObject reloadYoutubeVideoConfiguration(JSONObject decodedVideoConfiguration, boolean useExistingAlgo) throws Exception
    {


        JSONArray adaptive_fmts = decodedVideoConfiguration.getJSONObject("args").getJSONArray("adaptive_fmts");
        JSONArray url_encoded_fmt_stream_map = decodedVideoConfiguration.getJSONObject("args").getJSONArray("url_encoded_fmt_stream_map");
        // String title = decodedVideoConfiguration.getJSONObject("args").getString("title");
        // String video_id = decodedVideoConfiguration.getJSONObject("args").getString("video_id");
        JSONObject videoDetails = new JSONObject((decodedVideoConfiguration.getJSONObject("args").getString("player_response"))).getJSONObject("videoDetails");
        String title = videoDetails.getString("title");
        String videoId = videoDetails.getString("videoId");
        // sts is provided but in this case unnecessary
        int sts = -1;

        if (decodedVideoConfiguration.has("sts")) {
            sts = decodedVideoConfiguration.getInt("sts");
        }

        JSONObject cipherData = new JSONObject();

        if (!useExistingAlgo) {
            String playerURL = "https://youtube.com" + decodedVideoConfiguration.getJSONObject("assets").getString("js");
            final ByteArrayOutputStream playerBuffer = new ByteArrayOutputStream();
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
                    playerURL,
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
                            playerBuffer.write(chunk);
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

            String obfuscatedPlayerSource = new String(playerBuffer.toByteArray(), "UTF-8");
            cipherData = CipherCore.getCipherData(obfuscatedPlayerSource, sts);
        }
        else {
            cipherData = CipherCore.queryActionList();
        }

        JSONArray adaptive_fmtsStreamMap = reloadStreams(adaptive_fmts, ConfigurationCore.StreamDataType.adaptive_fmts, cipherData);
        JSONArray url_encoded_fmt_stream_mapStreamMap = reloadStreams(url_encoded_fmt_stream_map, ConfigurationCore.StreamDataType.url_encoded_fmt_stream_map, cipherData);
        JSONObject youtubeStreamWrapper = new JSONObject();
        youtubeStreamWrapper.put("adaptive_fmts", adaptive_fmtsStreamMap);
        youtubeStreamWrapper.put("url_encoded_fmt_stream_map", url_encoded_fmt_stream_mapStreamMap);
        youtubeStreamWrapper.put("title", title);
        youtubeStreamWrapper.put("video_id", videoId);
        return youtubeStreamWrapper;
    }
}
