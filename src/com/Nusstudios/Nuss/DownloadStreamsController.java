package com.Nusstudios.Nuss;

import com.Nusstudios.Core.DownloadCore;
import com.Nusstudios.Core.IO;
import javafx.application.Platform;
import javafx.concurrent.Task;
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

/**
 * Created by patriknusszer on 05/06/17.
 */
public class DownloadStreamsController {
    public Button btn_cancel;
    public Label lbl_status;
    public ProgressBar progressbar;
    public ProgressBar subprogressbar;
    public boolean cancel = false;
    public static Stage stage;
    public static int _i = 0;

    public void platformUpdateMessage(String msg) {
        Platform.runLater(
            () -> {
                lbl_status.setText(msg);
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

    public void platformCancelSetDisable(boolean setDisable) {
        Platform.runLater(
            () -> {
                btn_cancel.setDisable(setDisable);
            }
        );
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

    @FXML
    protected void initialize()throws Exception {
        _i = 0;
        final JSONObject o = MainController.map;
        final String outdir = MainController.output;
        final boolean doubleCheck = MainController.doubleDownloadChecking;

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JSONObject param = o;
                String path = outdir;

                IO.writeLinesToFile(new String[] {
                        "id: " + param.getString("video_id"),
                        "title: " + param.getString("title")
                }, path + File.separator + "info.txt");

                String p_adaptive_fmts = path + File.separator + "adaptive_fmts";
                new File(p_adaptive_fmts).mkdir();
                String p_adaptive_fmts_audio = p_adaptive_fmts + File.separator + "audio";
                new File(p_adaptive_fmts_audio).mkdir();
                String p_adaptive_fmts_video = p_adaptive_fmts + File.separator + "video";
                new File(p_adaptive_fmts_video).mkdir();
                String p_url_encoded_fmt_stream_map = path + File.separator + "url_encoded_fmt_stream_map";
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

                for (int i = 0; i < streams.length(); i++) {
                    _i = i;
                    platformUpdateProgress(Double.valueOf(_i) / Double.valueOf(streams.length()));
                    JSONObject stream = streams.getJSONObject(i);
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
                    FileOutputStream fs = null;
                    String fn = "";

                    if (media_type.equals("audio")) {
                        dir_name = String.valueOf(adiffer) + "_" + dir_name;
                        adiffer++;
                        String p_adaptive_fmts_audio_stream_dir = p_adaptive_fmts_audio + File.separator + dir_name;
                        new File(p_adaptive_fmts_audio_stream_dir).mkdir();

                        IO.writeLinesToFile(new String[] {
                                "Bitrate: " + stream.getInt("bitrate"),
                                "Codec: " + stream.getString("codec"),
                                "Size:" + stream.getInt("clen")
                        }, p_adaptive_fmts_audio_stream_dir + File.separator + "stats.txt");

                        fn = p_adaptive_fmts_audio_stream_dir + File.separator + "videoplayback." + container;
                    }
                    else if (media_type.equals("video")) {
                        dir_name = String.valueOf(vdiffer) + "_" + dir_name;
                        vdiffer++;
                        String p_adaptive_fmts_video_stream_dir = p_adaptive_fmts_video + File.separator + dir_name;
                        new File(p_adaptive_fmts_video_stream_dir).mkdir();

                        IO.writeLinesToFile(new String[] {
                                "Quality: " + stream.getString("size"),
                                "Bitrate: " + stream.getInt("bitrate"),
                                "Fps: " + stream.getInt("fps"),
                                "Codec: " + stream.getString("codec"),
                                "Size: " + stream.getInt("clen"),
                                "Container: " + stream.getString("container")
                        }, p_adaptive_fmts_video_stream_dir + File.separator + "stats.txt");

                        fn = p_adaptive_fmts_video_stream_dir + File.separator + "videoplayback." + container;
                    }
                    else if (media_type.equals("audiovisual")) {
                        dir_name = String.valueOf(avdiffer) + "_" + dir_name;
                        avdiffer++;
                        String p_url_encoded_fmt_stream_map_stream_dir = p_url_encoded_fmt_stream_map + File.separator + dir_name;
                        new File(p_url_encoded_fmt_stream_map_stream_dir).mkdir();

                        if (!(container.equals("X-FLV"))) {
                            IO.writeLinesToFile(new String[] {
                                    "Quality: " + stream.getString("quality"),
                                    "Video codec: " + stream.getString("video_codec"),
                                    "Audio codec: " + stream.getString("audio_codec"),
                                    "Container: " + stream.getString("container")
                            },p_url_encoded_fmt_stream_map_stream_dir + File.separator + "stats.txt");
                        }
                        else {
                            IO.writeLinesToFile(new String[] {
                                    "Quality: " + stream.getString("quality"),
                                    "Container: " + stream.getString("container")
                            },p_url_encoded_fmt_stream_map_stream_dir + File.separator + "stats.txt");
                        }

                        fn = p_url_encoded_fmt_stream_map_stream_dir + File.separator + "videoplayback." + container;
                    }

                    final int singleConnectionMaxErrorCount = 20;
                    final int hashCheckFailureMaxErrorCount = 5;
                    final JSONArray exception_log = new JSONArray();

                    DownloadCore.downloadToFile(
                            doubleCheck,
                            stream.getString("url"),
                            fn,
                            null,
                            100000,
                            100000,
                            (status) -> {
                                JSONObject response = null;

                                switch (status.getString("status")) {
                                    case "connecting":
                                        if (!doubleCheck) {
                                            platformUpdateMessage("Connecting to resource...");
                                        }
                                        else {
                                            platformUpdateMessage("Connecting to resource the " + (status.getInt("run_index") + 1) + ". time...");
                                        }

                                        break;
                                    case "connected":
                                        if (!doubleCheck) {
                                            platformUpdateMessage("Downloading stream " + (_i + 1) + " of " + streams.length());
                                        }
                                        else {
                                            platformUpdateMessage("Downloading stream " + (_i + 1) + " the " + (status.getInt("run_index") + 1) + ". time of " + streams.length());
                                        }

                                        break;
                                    case "progress":
                                        JSONArray progress = status.getJSONArray("progress");

                                        if (progress.getLong(1) != -1) {
                                            platformUpdateSubProgress((Double.valueOf(progress.getLong(0)) / progress.getLong(1)) / 2);
                                        }

                                        break;
                                    case "exception":
                                        response = new JSONObject();
                                        JSONObject exception = status.getJSONObject("exception");
                                        exception_log.put(exception.getString("message"));

                                        if (status.getJSONObject("exception").getInt("error_count") >= singleConnectionMaxErrorCount) {
                                            platformUpdateMessage("Stopping with exception after 20th attemp. Check nussexception.json.");
                                            IO.writeToFile(exception_log.toString(), outdir + File.separator + "nussexception.json");
                                            response.put("retry", false);
                                        }
                                        else {
                                            response.put("retry", true);
                                        }

                                        break;
                                    case "cancel":
                                        if (cancel) {
                                            platformUpdateMessage("Cancelled...");
                                        }
                                        else {
                                            platformUpdateMessage("Paused... about to exit");
                                        }

                                        response = new JSONObject();
                                        response.put("delete_file", true);
                                        return response;
                                    case "hashcheck_failure":
                                        response = new JSONObject();

                                        if (status.getJSONObject("hashcheck_failure").getInt("error_count") >= hashCheckFailureMaxErrorCount) {
                                            platformUpdateMessage("Stopping with hash check failure after fifth comparison. Retry now, or exit and run again later to continue");
                                            IO.writeToFile("[\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\"", outdir + File.separator + "nussexception.json");
                                            response.put("retry", true);
                                        }
                                        else {
                                            response.put("retry", false);
                                        }

                                        break;
                                }

                                return response;
                            },
                            () -> {
                                JSONObject signal = new JSONObject();

                                if (cancel) {
                                    signal.put("request", "cancel");
                                }

                                return signal;
                            },
                            false
                    );
                }

                updateMessage("Finished");
                platformUpdateProgress(1);
                platformBack();
                return null;
            }
        };

        new Thread(task).start();
    }

    public void back() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainView.fxml"));
        stage.setScene(new Scene(root, 525, 407));
        stage.show();
    }

    public void btn_cancel_docancel() {
        cancel = true;
    }
}
