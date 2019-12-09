package com.Nusstudios.Nuss;

import com.Nusstudios.Core.DownloadCore;
import com.Nusstudios.Core.IO;
import com.Nusstudios.Core.Util;
import com.Nusstudios.Nuss.Core.Youtube;
import com.Nusstudios.Nuss.Exceptions.ConfigurationLoadException;
import com.Nusstudios.Nuss.Exceptions.ConfigurationRegexException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class DownloadPlaylistController {
    public ProgressBar progressbar;
    public ProgressBar subprogressbar;
    public ProgressBar subsubprogressbar;
    public Button btn_cancel;
    public Button btn_pause;
    public Button btn_retry;
    public Label lbl_status;
    public boolean taskFinished = false;
    public static Stage stage;
    public static boolean useCookie;
    public static boolean streamSourceAPI;
    public static boolean cipherSourceNusstudios;
    public static boolean doubleCheck;
    public static String cookie;
    public static int _i = 0;
    public static int _s = 0;
    public static String _p_video = null;
    public static JSONObject nusstate = null;
    public static String playlistURL = null;
    public static String outdir = null;
    public static String[] ids = null;
    public boolean cancel = false;
    public boolean pause = false;
    public boolean retry = false;
    public static final Object retryLock = new Object();
    public static final Object cancelLock = new Object();
    public static final Object pauseLock = new Object();
    public static final Object retrySleepLock = new Object();

    public boolean getRetry() {
        synchronized (retryLock) {
            return retry;
        }
    }

    public void setRetry(boolean b) {
        synchronized (retryLock) {
            retry = b;
        }
    }

    public boolean getCancel() {
        synchronized (cancelLock) {
            return cancel;
        }
    }

    public void setCancel(boolean b) {
        synchronized (cancelLock) {
            cancel = b;
        }
    }

    public boolean getPause() {
        synchronized (pauseLock) {
            return pause;
        }
    }

    public void setPause(boolean b) {
        synchronized (pauseLock) {
            pause = b;
        }
    }

    public void platformCancelSetDisable(boolean setDisable) {
        Platform.runLater(
            () -> {
                btn_cancel.setDisable(setDisable);
            }
        );
    }

    public void platformPauseSetDisable(boolean setDisable) {
        Platform.runLater(
            () -> {
                btn_pause.setDisable(setDisable);
            }
        );
    }

    public void platformRetrySetDisable(boolean setDisable) {
        Platform.runLater(
                () -> {
                    btn_retry.setDisable(setDisable);
                }
        );
    }

    public void platformUpdateProgress(double d) {
        Platform.runLater(
            () -> {
                progressbar.setProgress(d);
            }
        );
    }

    public void platformUpdateSubProgress(double d) {
        Platform.runLater(
            () -> {
                subprogressbar.setProgress(d);
            }
        );
    }

    public void platformUpdateSubSubProgress(double d) {
        Platform.runLater(
            () -> {
                subsubprogressbar.setProgress(d);
            }
        );
    }

    public void platformUpdateMessage(String msg) {
        Platform.runLater(
            () -> {
                lbl_status.setText(msg);
            }
        );
    }

    public static JSONObject createSuccessResult() {
        JSONObject result = new JSONObject();
        result.put("result", "success");
        return result;
    }

    public static JSONObject createCancelResult() {
        JSONObject result = new JSONObject();
        result.put("result", "cancel");
        return result;
    }

    public static JSONObject createPauseResult() {
        JSONObject result = new JSONObject();
        result.put("result", "pause");
        JSONObject pause = new JSONObject();
        Object obj = null;
        pause.put("cleanup_path", obj);
        return result;
    }

    public static JSONObject createPauseResult(String cleanupPath) {
        JSONObject result = new JSONObject();
        result.put("result", "pause");
        JSONObject pause = new JSONObject();
        pause.put("cleanup_path", cleanupPath);
        result.put("pause", pause);
        return result;
    }

    public static String pauseResultTryGetCleanupPath(JSONObject result) {
        Object cleanupPath = result.getJSONObject("pause").get("cleanup_path");

        if (cleanupPath == null) {
            return null;
        }
        else {
            return (String)cleanupPath;
        }
    }

    public void platformCleanupAndLeave(JSONObject result) {
        switch (result.getString("result")) {
            case "cancel":
                IO.deleteNode(outdir);
                setCancel(false);
                break;
            case "pause":
                saveState();
                setPause(false);
                String cleanupPath;

                if ((cleanupPath = pauseResultTryGetCleanupPath(result)) != null) {
                    IO.deleteNode(cleanupPath);
                }

                break;
            case "success":
                break;
        }

        platformBack();
    }

    public Exception platformBack() {
        try {
            TimeUnit.SECONDS.sleep(3);

            Platform.runLater(
                () -> {
                    try {
                        back();
                    }
                    catch (Exception ex) {

                    }
                }
            );

            return null;
        }
        catch (Exception ex) {
            return ex;
        }
    }

    public static void saveState() {
        Nusstate.writeState(
                System.getProperty("user.dir") + File.separator + "nusstate.json",
                outdir,
                ids,
                _i,
                useCookie? cookie : "",
                streamSourceAPI,
                cipherSourceNusstudios,
                doubleCheck
        );
    }

    @FXML
    protected void initialize() throws Exception {
        if (MainController.nusstate != null) {
            nusstate = MainController.nusstate;
            _i = (int) nusstate.query("/nusstate/index");
            cookie = (String)nusstate.query("/nusstate/cookieOrEmpty");

            if (cookie.isEmpty()) {
                useCookie = false;
            }
            else {
                useCookie = true;
            }

            streamSourceAPI = (boolean)nusstate.query("/nusstate/streamSourceAPI");
            cipherSourceNusstudios = (boolean)nusstate.query("/nusstate/cipherSourceNusstudios");
            outdir = (String)nusstate.query("/nusstate/outdir");
            doubleCheck = (boolean)nusstate.query("/nusstate/doubleCheck");
            ids = Util.jstra2stra((JSONArray)nusstate.query("/nusstate/ids"));
        }
        else {
            useCookie = MainController.useCookie;
            cookie = MainController.cookie;
            playlistURL = MainController.url;
            outdir = MainController.output;
            streamSourceAPI = MainController.streamSourceAPI;
            cipherSourceNusstudios = MainController.cipherSourceNusstudios;
            doubleCheck = MainController.doubleDownloadChecking;
            ids = MainController.ids;
        }

        // This will only make a difference when the initial value if _i is not 0
        platformUpdateProgress(Double.valueOf(_i) / Double.valueOf(ids.length));

        new Thread(() -> {
            for (int i = _i; i < ids.length; i++) {
                _i = i;
                String videoId = ids[i];
                JSONObject param = null;

                // if do ... while ends, then it was successful
                do {
                    try {
                        param = Youtube.getStreamInfo(videoId, streamSourceAPI, cipherSourceNusstudios, useCookie? cookie : "");
                        break;
                    }
                    catch (Exception ex) {
                        if (ex instanceof ConfigurationLoadException) {
                            platformUpdateMessage("Failed (down)loading configuration. Check nussexception.json. Retry now, exit and run again later to continue, or cancel");
                            ConfigurationLoadException _ex = (ConfigurationLoadException)ex;
                            IO.writeToFile(_ex.extendedDescription.getJSONArray("exception").toString(), outdir + File.separator + "nussexception.json");
                        }
                        else {
                            if (ex instanceof ConfigurationRegexException) {
                                platformUpdateMessage("A configuration parser regex failed. Check nussexception.json. Retry now, exit and run again later to continue, or cancel");
                            }
                            else {
                                platformUpdateMessage("Unidentified error occured while loading configuration. Check nussexception.json. Retry now, exit and run again later to continue, or cancel");
                            }

                            IO.writeToFile(ex.toString(), outdir + File.separator + "nussexception.json");
                        }

                        platformRetrySetDisable(false);

                        while (true) {
                            if (getRetry()) {
                                platformRetrySetDisable(true);
                                setRetry(false);
                                break;
                            }
                            else if (getCancel()) {
                                platformCleanupAndLeave(createCancelResult());
                                return;
                            }
                            else if (getPause()) {
                                platformCleanupAndLeave(createPauseResult());
                                return;
                            }
                        }
                    }
                } while (true);

                String p_video = outdir + File.separator + (_i + 1);
                _p_video = p_video;

                new File(p_video).mkdir();

                try {
                    IO.writeLinesToFile(new String[] {
                            "id: " + param.getString("video_id"),
                            "title: " + param.getString("title")
                    }, p_video + File.separator + "info.txt");
                }
                catch (Exception ex) {

                }

                String p_adaptive_fmts = p_video + File.separator + "adaptive_fmts";
                new File(p_adaptive_fmts).mkdir();
                String p_adaptive_fmts_audio = p_adaptive_fmts + File.separator + "audio";
                new File(p_adaptive_fmts_audio).mkdir();
                String p_adaptive_fmts_video = p_adaptive_fmts + File.separator + "video";
                new File(p_adaptive_fmts_video).mkdir();
                String p_url_encoded_fmt_stream_map = p_video + File.separator + "url_encoded_fmt_stream_map";
                new File(p_url_encoded_fmt_stream_map).mkdir();
                JSONArray streams = new JSONArray();
                JSONArray adaptive_fmts = param.getJSONArray("adaptive_fmts");

                for (Object adaptive_fmt_ : adaptive_fmts) {
                    JSONObject adaptive_fmt = (JSONObject)adaptive_fmt_;
                    adaptive_fmt.put("stream_type", "adaptive_fmt");
                    streams.put(adaptive_fmt);
                }

                JSONArray url_encoded_fmt_stream_map = param.getJSONArray("url_encoded_fmt_stream_map");

                for (Object url_encoded_fmt_stream_ : url_encoded_fmt_stream_map) {
                    JSONObject url_encoded_fmt_stream = (JSONObject)url_encoded_fmt_stream_;
                    url_encoded_fmt_stream.put("stream_type", "url_encoded_fmt_stream");
                    streams.put(url_encoded_fmt_stream);
                }

                int vdiffer = 1;
                int adiffer = 1;
                int avdiffer = 1;

                for (int s = 0; s < streams.length(); s++) {
                    _s = s;
                    JSONObject stream = streams.getJSONObject(s);
                    String media_type = stream.getString("media_type");
                    String dir_name = "";

                    if (stream.getString("stream_type").equals("adaptive_fmt")) {
                        if (media_type.equals("video")) {
                            dir_name += stream.getString("size") + "_";
                        }

                        dir_name += stream.getString("bitrate_converted") + "PS_";

                        if (media_type.equals("video")) {
                            dir_name += stream.getString("fps") + " FPS_";
                        }

                        dir_name += stream.getString("codec") + " Encoded_";
                    }
                    else {
                        dir_name += stream.getString("quality") + "_";
                    }

                    String container = stream.getString("container");
                    dir_name += container.toUpperCase();
                    dir_name = dir_name.replaceAll(" ", "");
                    String fn = "";

                    if (media_type.equals("audio")) {
                        dir_name = String.valueOf(adiffer) + "_" + dir_name;
                        adiffer++;
                        String p_adaptive_fmts_audio_stream_dir = p_adaptive_fmts_audio + File.separator + dir_name;
                        new File(p_adaptive_fmts_audio_stream_dir).mkdir();

                        try {
                            IO.writeLinesToFile(new String[] {
                                    "Bitrate: " + stream.getInt("bitrate"),
                                    "Codec: " + stream.getString("codec"),
                                    "Size:" + stream.getInt("clen")
                            }, p_adaptive_fmts_audio_stream_dir + File.separator + "stats.txt");
                        }
                        catch (Exception ex) {

                        }

                        fn = p_adaptive_fmts_audio_stream_dir + File.separator + "videoplayback." + container;
                    }
                    else if (media_type.equals("video")) {
                        dir_name = String.valueOf(vdiffer) + "_" + dir_name;
                        vdiffer++;
                        String p_adaptive_fmts_video_stream_dir = p_adaptive_fmts_video + File.separator + dir_name;
                        new File(p_adaptive_fmts_video_stream_dir).mkdir();

                        try {
                            IO.writeLinesToFile(new String[] {
                                    "Quality: " + stream.getString("size"),
                                    "Bitrate: " + stream.getInt("bitrate"),
                                    "Fps: " + stream.getInt("fps"),
                                    "Codec: " + stream.getString("codec"),
                                    "Size: " + stream.getInt("clen"),
                                    "Container: " + stream.getString("container")
                            }, p_adaptive_fmts_video_stream_dir + File.separator + "stats.txt");
                        }
                        catch (Exception ex) {

                        }

                        fn = p_adaptive_fmts_video_stream_dir + File.separator + "videoplayback." + container;
                    }
                    else if (media_type.equals("audiovisual")) {
                        dir_name = String.valueOf(avdiffer) + "_" + dir_name;
                        avdiffer++;
                        String p_url_encoded_fmt_stream_map_stream_dir = p_url_encoded_fmt_stream_map + File.separator + dir_name;
                        new File(p_url_encoded_fmt_stream_map_stream_dir).mkdir();

                        if (!(container.equals("X-FLV"))) {
                            try {
                                IO.writeLinesToFile(new String[] {
                                        "Quality: " + stream.getString("quality"),
                                        "Video codec: " + stream.getString("video_codec"),
                                        "Audio codec: " + stream.getString("audio_codec"),
                                        "Container: " + stream.getString("container")
                                },p_url_encoded_fmt_stream_map_stream_dir + File.separator + "stats.txt");
                            }
                            catch (Exception ex) {

                            }
                        }
                        else {
                            try {
                                IO.writeLinesToFile(new String[] {
                                        "Quality: " + stream.getString("quality"),
                                        "Container: " + stream.getString("container")
                                },p_url_encoded_fmt_stream_map_stream_dir + File.separator + "stats.txt");
                            }
                            catch (Exception ex) {

                            }
                        }

                        fn = p_url_encoded_fmt_stream_map_stream_dir + File.separator + "videoplayback." + container;
                    }

                    // if do ... while ends, then it was successful
                    do {
                        final int singleConnectionMaxErrorCount = 20;
                        final int hashCheckFailureMaxErrorCount = 5;
                        JSONObject download_config = new JSONObject();
                        download_config.put("singleConnectionMaxErrorCount", 20);
                        download_config.put("hashCheckFailureMaxErrorCount", 5);
                        download_config.put("retryLaterMaxCount", 2);
                        download_config.put("retryLaterCount", 0);
                        final JSONArray exception_log = new JSONArray();
                        platformPauseSetDisable(false);

                        DownloadCore.downloadToFile(
                                doubleCheck,
                                stream.getString("url"),
                                fn,
                                null,
                                20000,
                                20000,
                                (status) -> {
                                    JSONObject response = null;

                                    switch (status.getString("status")) {
                                        case "connecting":
                                            if (!doubleCheck) {
                                                platformUpdateMessage("Connecting to video stream " + (_s + 1) + " of " + streams.length() + " for video "  + (_i + 1) + " of " + ids.length + "...");
                                            }
                                            else {
                                                platformUpdateMessage("Connecting to video stream " + (_s + 1) + " of " + streams.length() + " the " + (status.getInt("run_index") + 1) + ". time" + " for video "  + (_i + 1) + " of " + ids.length + "...");
                                            }

                                            break;
                                        case "connected":
                                            if (!doubleCheck) {
                                                platformUpdateMessage("Processing video stream " + (_s + 1) + " of " + streams.length() + " for video "  + (_i + 1) + " of " + ids.length + "...");
                                            }
                                            else {
                                                platformUpdateMessage("Processing video stream " + (_s + 1) + " of " + streams.length() + " the " + (status.getInt("run_index") + 1) + ". time" + " for video "  + (_i + 1) + " of " + ids.length + "...");
                                            }

                                            break;
                                        case "progress":
                                            JSONArray progress = status.getJSONArray("progress");

                                            if (progress.getLong(1) != -1) {
                                                platformUpdateSubSubProgress((Double.valueOf(progress.getLong(0)) / progress.getLong(1)));
                                            }

                                            break;
                                        case "exception":
                                            response = new JSONObject();
                                            JSONObject exception = status.getJSONObject("exception");
                                            exception_log.put(exception.getString("message"));

                                            if (exception.getInt("error_count") >= singleConnectionMaxErrorCount) {
                                                if (download_config.getInt("retryLaterCount") == download_config.getInt("retryLaterMaxCount")) {
                                                    platformUpdateMessage("Stopping with exception after 20th attemp. Check nussexception.json. Retry now, or exit and run again later to continue");
                                                    IO.writeToFile(exception_log.toString(), outdir + File.separator + "nussexception.json");
                                                    platformRetrySetDisable(false);

                                                    while (true) {
                                                        if (getRetry()) {
                                                            platformRetrySetDisable(true);
                                                            setRetry(false);
                                                            response.put("retry", true);
                                                            response.put("reset_error_count", true);
                                                            download_config.put("retryLaterCount", 0);
                                                            break;
                                                        }
                                                        else if (getCancel()) {
                                                            response.put("retry", false);
                                                            break;
                                                        }
                                                        else if (getPause()) {
                                                            response.put("retry", false);
                                                            break;
                                                        }
                                                    }
                                                }
                                                else {
                                                    platformUpdateMessage("Sleeping 5 minutes before retrying...");

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

                                                        if (getCancel()) {
                                                            response.put("retry", false);
                                                        }
                                                        else if (getPause()) {
                                                            response.put("retry", false);
                                                        }
                                                        else {
                                                            response.put("retry", true);
                                                        }
                                                    }
                                                    catch (Exception ex) { }
                                                }
                                            }
                                            else {
                                                response.put("retry", true);
                                            }

                                            break;
                                        case "cancel":
                                            response = new JSONObject();
                                            response.put("delete_file", true);

                                            if (getCancel()) {
                                                platformUpdateMessage("Cancelled...");
                                            }
                                            else {
                                                platformUpdateMessage("Paused... about to exit");
                                            }

                                            break;
                                        case "hashcheck_failure":
                                            response = new JSONObject();

                                            if (status.getJSONObject("hashcheck_failure").getInt("error_count") >= hashCheckFailureMaxErrorCount) {
                                                platformUpdateMessage("Stopping with hash check failure after fifth comparison. Retry now, or exit and run again later to continue");
                                                IO.writeToFile("[\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\"", outdir + File.separator + "nussexception.json");
                                                platformRetrySetDisable(false);

                                                while (true) {
                                                    if (getRetry()) {
                                                        platformRetrySetDisable(true);
                                                        response.put("retry", true);
                                                        break;
                                                    }
                                                    else if (getCancel()) {
                                                        response.put("retry", false);
                                                        break;
                                                    }
                                                    else if (getPause()) {
                                                        response.put("retry", false);
                                                        break;
                                                    }
                                                }

                                                response.put("retry", true);
                                            }
                                            else {
                                                response.put("retry", false);
                                            }

                                            break;
                                        case "success":
                                            download_config.put("retryLaterCount", 0);
                                    }

                                    return response;
                                },
                                () -> {
                                    JSONObject signal = new JSONObject();

                                    if (getCancel() || getPause()) {
                                        signal.put("request", "cancel");
                                    }

                                    return signal;
                                },
                                false
                        );

                        if (getCancel()) {
                            platformCleanupAndLeave(createCancelResult());
                            return;
                        }
                        else if (getPause()) {
                            platformCleanupAndLeave(createPauseResult(p_video));
                            return;
                        }
                        else {
                            // partial success
                            break;
                        }
                    } while (true);

                    platformUpdateSubProgress(Double.valueOf(s + 1) / Double.valueOf(streams.length()));
                }

                platformUpdateProgress(Double.valueOf(i + 1) / Double.valueOf(ids.length));
                platformPauseSetDisable(true);
            }

            taskFinished = true;
            platformUpdateMessage("Success");
            platformCleanupAndLeave(createSuccessResult());
            return;
        }).start();
    }

    public void back() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainView.fxml"));
        stage.setScene(new Scene(root, 525, 407));
        stage.show();
    }

    public void btn_retry_doretry() {
        btn_cancel.setDisable(true);
        btn_pause.setDisable(true);
        btn_retry.setDisable(true);
        setRetry(true);
    }

    public void btn_cancel_docancel() {
        btn_cancel.setDisable(true);
        btn_pause.setDisable(true);
        btn_retry.setDisable(true);
        setCancel(true);

        synchronized (retrySleepLock) {
            retrySleepLock.notifyAll();
        }
    }

    public void btn_pause_dopause() {
        btn_cancel.setDisable(true);
        btn_pause.setDisable(true);
        btn_retry.setDisable(true);
        setPause(true);

        synchronized (retrySleepLock) {
            retrySleepLock.notify();
        }
    }
}
