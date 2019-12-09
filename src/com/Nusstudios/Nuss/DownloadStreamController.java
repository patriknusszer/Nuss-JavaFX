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
public class DownloadStreamController {
    public Button btn_cancel;
    public Label lbl_status;
    public ProgressBar progressbar;
    public boolean cancel = false;
    public static Stage stage;

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
        JSONObject o = MainController.current_stream;
        String outdir = MainController.output;
        final boolean doubleCheck = MainController.doubleDownloadChecking;
        final int singleConnectionMaxErrorCount = 20;
        final int hashCheckFailureMaxErrorCount = 5;
        final JSONArray exception_log = new JSONArray();

        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            DownloadCore.downloadToFile(
                doubleCheck,
                o.getString("url"),
                outdir + File.separator + "video." + o.getString("container"),
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
                                platformUpdateMessage("Downloading stream...");
                            }
                            else {
                                platformUpdateMessage("Downloading stream the " + (status.getInt("run_index") + 1) + ". time...");
                            }
                            
                            break;
                        case "progress":
                            JSONArray progress = status.getJSONArray("progress");
                            platformUpdateProgress(Double.valueOf(progress.getLong(0)) / Double.valueOf(progress.getLong(1)));
                            break;
                        case "exception":
                            JSONObject exception = status.getJSONObject("exception");
                            exception_log.put(exception.getString("message"));
                            response = new JSONObject();

                            if (exception.getInt("error_count") >= singleConnectionMaxErrorCount) {
                                platformUpdateMessage("Stopping after 20 subsequent exceptions. Check nussexception.json");
                                IO.writeToFile(exception_log.toString(), outdir + File.separator + "nussexception.json");
                                platformBack();
                                response.put("retry", false);
                            }
                            else {
                                response.put("retry", true);
                            }

                            break;
                        case "cancel":
                            platformUpdateMessage("Cancelled");
                            platformCancelSetDisable(true);
                            response = new JSONObject();
                            response.put("delete_file", true);
                            platformBack();
                            return response;
                        case "success":
                            if (!doubleCheck) {
                                platformUpdateMessage("Success");
                                platformCancelSetDisable(true);
                                platformBack();
                            }

                            break;
                        case "hashcheck_failure":
                            JSONObject hashcheck_failure = status.getJSONObject("hashcheck_failure");
                            response = new JSONObject();

                            if (hashcheck_failure.getInt("error_count") >= hashCheckFailureMaxErrorCount) {
                                platformUpdateMessage("Stopping with hash check failure after fifth comparison. Retry now, or exit and run again later to continue");
                                IO.writeToFile("[\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\",\"hashcheck_failure]\"", outdir + File.separator + "nussexception.json");
                                response.put("retry", false);
                            }
                            else {
                                response.put("retry", true);
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
        btn_cancel.setDisable(true);
    }
}
