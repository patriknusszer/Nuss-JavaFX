package com.Nusstudios.Nuss;

import com.Nusstudios.Nuss.Core.ConfigurationCore;
import com.Nusstudios.Nuss.Core.Youtube;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.lang.module.Configuration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {
    public TextField tf_url;
    public TextField tf_output;
    public TextField tf_bps;
    public TextField tf_bpsc;
    public TextField tf_quality;
    public TextField tf_vc;
    public TextField tf_ac;
    public TextField tf_fps;
    public TextField tf_sizec;
    public TextField tf_size;
    public TextField tf_cntnr;
    public TextField tf_mt;
    public TextField tf_cookie;
    public Label lbl_info;
    public Button btn_dl;
    public Button btn_bckp;
    public Button btn_select_output;
    public Button btn_paste;
    public Button btn_plstbckp;
    public Button btn_continue;
    public ComboBox cbb_streams;
    public CheckBox chkb_usecookie;
    public CheckBox chkb_ddc;
    public static Stage stage;
    public static JSONObject map;
    public static String[] ids;
    public static JSONObject current_stream;
    public static String output;
    public static String url;
    public static String status;
    public static int selected;
    public static String cookie;
    public static boolean useCookie = false;
    public static boolean doubleDownloadChecking = false;
    public static boolean streamSourceAPI = false;
    public static boolean cipherSourceNusstudios = false;
    public static JSONObject nusstate = null;

    public static final Object retrySleepLock = new Object();
    public static final Object retrySleepLock2 = new Object();

    @FXML
    protected void initialize() throws Exception {
        if (useCookie) {
            tf_cookie.setText(cookie);
        }

        chkb_ddc.setSelected(doubleDownloadChecking);

        tf_url.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isYoutubeVideoURL(newValue)) {
                try {
                    updateConfigurationUIAsync(getYoutubeVideoID(newValue));
                } catch (Exception e) { }
            }
            else if (isYoutubePlaylistURL(newValue)) {
                updateIDsUIAsync(newValue);
            }
        });

        tf_output.textProperty().addListener((observable, oldValue, newValue) -> {
            if (new File(tf_output.getText()).exists()) {
                lbl_info.setText("Path OK");

                if (isYoutubeVideoURL(tf_url.getText())) {
                    watchdl_state_ui(true);
                }
                else {
                    playlistdl_state_ui(true);
                }
            }
            else {
                lbl_info.setText("Path does not exist");
                watchdl_state_ui(false);
                playlistdl_state_ui(false);
            }
        });

        if (new File(System.getProperty("user.dir") + File.separator + "nusstate.json").exists()) {
            btn_continue.setDisable(false);
        }
        else {
            if (map != null) {
                updateStreamsUI();
                cbb_streams.getSelectionModel().select(selected);
                current_stream = (JSONObject) cbb_streams.getSelectionModel().getSelectedItem();
                tf_output.setText(output);
                update_stats_ui();
                tf_url.setText(url);
                lbl_info.setText(status);
                tf_cookie.setText(cookie);

                if (useCookie) {
                    tf_cookie.setDisable(false);
                }
                else {
                    tf_cookie.setDisable(true);
                }
            }
        }
    }

    public void cbb_streams_selected() {
        current_stream = (JSONObject)cbb_streams.getSelectionModel().getSelectedItem();
        update_stats_ui();
    }

    public void btn_select_output_doselect() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select output directory");
        File f = dc.showDialog(stage);

        if (f != null) {
            tf_output.setText(f.getPath());

            if (isYoutubeVideoURL(tf_url.getText())) {
                watchdl_state_ui(true);
            }
            else {
                playlistdl_state_ui(true);
            }
        }
        else {
            watchdl_state_ui(false);
            playlistdl_state_ui(false);
            lbl_info.setText("Please specify a path");
        }
    }

    public void btn_paste() throws Exception {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if (clipboard.hasString()) {
            String str = clipboard.getString();

            if (isYoutubeVideoURL(str)) {
                tf_url.setText(str);
            }
            else if (isYoutubePlaylistURL(str)) {
                tf_url.setText(str);
            }
            else {
                // Not Youtube URL
            }
        }
        else {
            // No URL was found
        }
    }

    public boolean isYoutubePlaylistURL(String str) {
        Pattern ptrn = Pattern.compile("http(s)?:\\/\\/(www\\.)?youtube\\.com\\/playlist\\?list=.*");
        Matcher mtchr = ptrn.matcher(str);

        if (mtchr.find())
        {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isYoutubeVideoURL(String str) {
        Pattern ptrn = Pattern.compile("http(s)?:\\/\\/(www\\.)?youtube\\.com\\/watch\\?v=.*");
        Matcher mtchr = ptrn.matcher(str);

        if (mtchr.find())
        {
            return true;
        }
        else {
            return false;
        }
    }

    public String getYoutubeVideoID(String URL)
    {
        return URL.split("=")[1];
    }

    public void initialize_stats_ui() {
        tf_quality.setText("Unknown");
        tf_bpsc.setText("Not applicable");
        tf_bps.setText("Not applicable");
        tf_vc.setText("Not applicable");
        tf_ac.setText("Not applicable");
        tf_fps.setText("Not applicable");
        tf_sizec.setText("Unknown");
        tf_size.setText("Unknown");
        tf_cntnr.setText("Unknown");
        tf_mt.setText("Unknown");
    }

    public void output_setdisable_ui(boolean b) {
        tf_output.setDisable(b);
        btn_select_output.setDisable(b);
    }

    public void netcontrols_setdisable_ui(boolean b) {
        tf_url.setDisable(b);
        chkb_ddc.setDisable(b);
        chkb_usecookie.setDisable(b);

        if (!b) {
            if (chkb_usecookie.isSelected()) {
                tf_cookie.setDisable(false);
            }
            else {
                tf_cookie.setDisable(true);
            }
        }
        else {
            tf_cookie.setDisable(true);
        }
    }

    public void streaminfo_setdisable_ui(boolean b) {
        cbb_streams.setDisable(b);
        tf_ac.setDisable(b);
        tf_vc.setDisable(b);
        tf_quality.setDisable(b);
        tf_bps.setDisable(b);
        tf_bpsc.setDisable(b);
        tf_cntnr.setDisable(b);
        tf_fps.setDisable(b);
        tf_size.setDisable(b);
        tf_sizec.setDisable(b);
        tf_mt.setDisable(b);
    }

    public void watchdl_setdisable_ui(boolean b) {
        btn_dl.setDisable(b);
        btn_bckp.setDisable(b);
    }

    public void playlistdl_setdisable_ui(boolean b) {
        btn_plstbckp.setDisable(b);
    }

    public void updating_state_ui() {
        playlistdl_setdisable_ui(true);
        watchdl_setdisable_ui(true);
        output_setdisable_ui(true);
        netcontrols_setdisable_ui(true);
        streaminfo_setdisable_ui(true);
    }

    public void watchdl_state_ui(boolean outputexists) {
        playlistdl_setdisable_ui(true);
        watchdl_setdisable_ui(!outputexists);
        output_setdisable_ui(false);
        netcontrols_setdisable_ui(false);
        streaminfo_setdisable_ui(false);
    }

    public void playlistdl_state_ui(boolean outputexists) {
        playlistdl_setdisable_ui(!outputexists);
        watchdl_setdisable_ui(true);
        output_setdisable_ui(false);
        netcontrols_setdisable_ui(false);
        streaminfo_setdisable_ui(true);
    }

    public void updateIDsUIAsync(String playlistURL) {
        updating_state_ui();
        String cookieOrEmpty;

        if (chkb_usecookie.isSelected()) {
            cookieOrEmpty = tf_cookie.getText();
        }
        else {
            cookieOrEmpty = "";
        }

        Runnable task = () -> {
            try {
                ids = ConfigurationCore.getIDs(playlistURL, cookieOrEmpty);
            }
            catch (Exception ex) { }

            Platform.runLater(() -> {
                boolean exists = new File(tf_output.getText()).exists();
                playlistdl_state_ui(exists);

                if (exists) {
                    lbl_info.setText("OK");
                }
                else {
                    lbl_info.setText("Finished, please select output");
                }
            });
        };

        new Thread(task).start();
        lbl_info.setText("Getting playlist video ids...");
    }

    public void updateConfigurationUIAsync(final String video_id) throws Exception
    {
        updating_state_ui();
        String cookieOrEmpty;
        boolean _streamSourceAPI = false;
        boolean _cipherSourceNusstudios = false;

        if (chkb_usecookie.isSelected()) {
            cookieOrEmpty = tf_cookie.getText();
        }
        else {
            cookieOrEmpty = "";
        }

        Runnable task = () -> {
            try {
                map = Youtube.getStreamInfo(video_id, _streamSourceAPI, _cipherSourceNusstudios, cookieOrEmpty);
            }
            catch (Exception ex) {}

            Platform.runLater(() -> {
                updateStreamsUI();
            });
        };

        new Thread(task).start();
        lbl_info.setText("Getting stream map...");
    }

    public void updateStreamsUI()
    {
        lbl_info.setText(map.getString("title"));
        cbb_streams.getItems().clear();

        Callback cellFactory = new Callback<ListView<JSONObject>,ListCell<JSONObject>>(){
            @Override
            public ListCell<JSONObject> call(ListView<JSONObject> lw){
                return new ListCell<JSONObject>(){
                    @Override
                    protected void updateItem(JSONObject item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setText(item.getString("user_friendly_name"));
                            setFont(this.getFont().font(this.getFont().getName(), 11.5));
                        }
                    }
                } ;
            }
        };

        cbb_streams.setCellFactory(cellFactory);
        // cbb_streams.setButtonCell((ListCell)cellFactory.call(null));
        cbb_streams.setButtonCell(new ListCell<JSONObject>() {
            @Override
            protected void updateItem(JSONObject item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(item.getString("user_friendly_name"));
                    setFont(this.getFont().font(this.getFont().getName(), 10.0));
                }
            }
        });

        JSONArray adaptive_fmts = map.getJSONArray("adaptive_fmts");
        JSONArray url_encoded_fmt_stream_map = map.getJSONArray("url_encoded_fmt_stream_map");
        int bitrate = 0;
        int height = 0;

        for (int i = 0; i < adaptive_fmts.length(); i++)
        {
            JSONObject adaptive_fmt = adaptive_fmts.getJSONObject(i);
            String output = "";
            String type = adaptive_fmt.getString("type");
            String[] arr = type.split("/");
            String mediaType = arr[0];

            if (mediaType.equals("video"))
            {
                output += "Video only, ";
                output += adaptive_fmt.getString("size");
                output += " (" + adaptive_fmt.getString("quality_label") + "), ";
            }
            else if (mediaType.equals("audio"))
            {
                output += "Audio, ";
                output += "~" + adaptive_fmt.getString("bitrate_converted") + "/s, ";
            }

            output += adaptive_fmt.getString("clen_converted") + ", ";
            arr = arr[1].split(";+");
            String ext = arr[0];
            output += ext.toUpperCase();
            adaptive_fmt.put("stream_type", "adaptive_fmt");
            adaptive_fmt.put("user_friendly_name", output);
            cbb_streams.getItems().add(adaptive_fmt);
        }

        boolean lock1 = false;
        boolean lock2 = false;
        boolean lock3 = false;
        JSONObject highest_quality_url_encoded_fmt_stream = new JSONObject();

        for (int i = 0; i < url_encoded_fmt_stream_map.length(); i++)
        {
            JSONObject url_encoded_fmt_stream = url_encoded_fmt_stream_map.getJSONObject(i);
            String output = "";
            String type = url_encoded_fmt_stream.getString("type");
            String[] arr = type.split("/");
            String mediaType = arr[0];
            arr = arr[1].split(";+");
            String ext = arr[0];
            output = "Video with sound, ";
            output += url_encoded_fmt_stream.getString("quality") + " quality, ";
            output += ext.toUpperCase();
            url_encoded_fmt_stream.put("stream_type", "url_encoded_fmt_stream");
            String quality = url_encoded_fmt_stream.getString("quality");
            url_encoded_fmt_stream.put("user_friendly_name", output);
            cbb_streams.getItems().add(url_encoded_fmt_stream);

            if (quality.equals("hd720") && !lock3)
            {
                lock3 = true;
                lock2 = true;
                lock1 = true;
                highest_quality_url_encoded_fmt_stream = url_encoded_fmt_stream;
            }
            else if (quality.equals("medium") && !lock2)
            {
                lock2 = true;
                lock1 = true;
                highest_quality_url_encoded_fmt_stream = url_encoded_fmt_stream;
            }
            else if (quality.equals("small") && !lock1)
            {
                lock1 = true;
                highest_quality_url_encoded_fmt_stream = url_encoded_fmt_stream;
            }
        }

        cbb_streams.getSelectionModel().select(highest_quality_url_encoded_fmt_stream);
        current_stream = highest_quality_url_encoded_fmt_stream;
        update_stats_ui();
    }

    public void update_stats_ui() {
        initialize_stats_ui();
        String st = current_stream.getString("stream_type");
        String mt = current_stream.getString("media_type");
        tf_cntnr.setText(current_stream.getString("container").toUpperCase());
        tf_mt.setText(mt);

        if (st.equals("adaptive_fmt")) {
            tf_bpsc.setText(current_stream.getString("bitrate_converted"));
            tf_bps.setText(current_stream.getString("bitrate"));
            tf_sizec.setText(current_stream.getString("clen"));
            tf_size.setText(current_stream.getString("clen_converted"));

            if (mt.equals("video")) {
                tf_quality.setText(current_stream.getString("size"));
                tf_fps.setText(current_stream.getString("fps"));
                tf_vc.setText(current_stream.getString("codec"));
            }
            else if (mt.equals("audio")) {
                tf_ac.setText(current_stream.getString("codec"));
            }
        }
        else if(st.equals("url_encoded_fmt_stream")) {
            tf_quality.setText(current_stream.getString("quality"));

            if (!current_stream.getString("container").equals("X-FLV")) {
                tf_vc.setText(current_stream.getString("video_codec"));
                tf_ac.setText(current_stream.getString("audio_codec"));
            }
        }

        if (new File(tf_output.getText()).exists()) {
            watchdl_state_ui(true);
            lbl_info.setText("Finished");
        }
        else {
            watchdl_state_ui(false);
            lbl_info.setText("Finished, please select path");
        }
    }

    public void save_state() {
        doubleDownloadChecking = chkb_ddc.isSelected();
        status = lbl_info.getText();
        url = tf_url.getText();
        output = tf_output.getText();
        selected = cbb_streams.getSelectionModel().getSelectedIndex();
        cookie = tf_cookie.getText();
        useCookie = chkb_usecookie.isSelected();
    }

    public void btn_dl_dodl() throws Exception {
        save_state();
        single_dl();
    }

    public void single_dl() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("DownloadStreamView.fxml"));
        stage.setTitle("Nuss Desktop");
        stage.setScene(new Scene(root, 334, 177));
        stage.show();
    }

    public void btn_bckp_dobckp() throws Exception {
        save_state();
        Parent root = FXMLLoader.load(getClass().getResource("DownloadStreamsView.fxml"));
        stage.setTitle("Nuss Desktop");
        stage.setScene(new Scene(root, 334, 214));
        stage.show();
    }

    public void plstdl() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("DownloadPlaylistView.fxml"));
        stage.setTitle("Nuss Desktop");
        stage.setScene(new Scene(root, 334, 318));
        stage.show();
    }

    public void btn_plstbckp_dobckp() throws Exception {
        save_state();
        plstdl();
    }

    public void chkb_usecookie_chng() {
        if (chkb_usecookie.isSelected()) {
            tf_cookie.setDisable(false);
        }
        else {
            tf_cookie.setDisable(true);
        }
    }

    public void btn_continue_docontinue() throws Exception {
        if (new File(System.getProperty("user.dir") + File.separator + "nusstate.json").exists()) {
            nusstate = Nusstate.readState(System.getProperty("user.dir") + File.separator + "nusstate.json", true);
            plstdl();
        }
        else {
            btn_continue.setDisable(true);
        }
    }
}
