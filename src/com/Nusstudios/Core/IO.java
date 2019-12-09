package com.Nusstudios.Core;

import java.io.*;
import java.util.Arrays;

public class IO {
    public static void writeLinesToFile(String[] lines, String fn) throws Exception {
        BufferedWriter tw = new BufferedWriter(new FileWriter(fn));

        for (int i = 0; i < lines.length; i++) {
            tw.write(lines[i]);

            if (i != lines.length - 1) {
                tw.newLine();
            }
        }

        tw.flush();
        tw.close();
    }

    public static Exception writeToFile(String content, String fn) {
        try {
            BufferedWriter tw = new BufferedWriter(new FileWriter(fn));
            tw.write(content);
            tw.flush();
            tw.close();
            return null;
        }
        catch (Exception ex) {
            return ex;
        }
    }

    public static byte[] readAllBytes(String fn) throws Exception {
        FileInputStream fis = new FileInputStream(fn);
        byte[] buff = new byte[fis.available()];
        fis.read(buff);
        return buff;
    }

    public interface WalkCallback {
        void report(File node, File subNode, WalkReportType reportType);
    }

    public static Exception writeAllBytes(String fn, byte[] buffer) {
        try {
            FileOutputStream fs = new FileOutputStream(fn);
            fs.write(buffer);
            fs.flush();
            fs.close();
            return null;
        }
        catch (Exception ex) {
            return ex;
        }
    }

    public static String readAllInUTF8(String fn) throws Exception {
        return new String(readAllBytes(fn), "UTF-8");
    }

    public static String readAllInEncoding(String fn, String encoding) throws Exception {
        return new String(readAllBytes(fn), encoding);
    }

    public static void deleteNode(String node) {
        File _node = new File(node);

        if (_node.exists()) {
            if (!_node.isDirectory()) {
                _node.delete();
            }
            else {
                deleteDirectory(node);
            }
        }
    }

    public static Exception deleteDirectory(String dir) {
        if (new File(dir).exists()) {
            WalkCallback wc = (node, subNode, reportType) -> {
                if (reportType.equals(WalkReportType.Root)) {
                    node.delete();
                }
                else if (reportType.equals(WalkReportType.BackwardsWalk)) {
                    // This should only be true if this subNode was a root (therefore is already deleted)
                    if (subNode.exists()) {
                        subNode.delete();
                    }
                }
            };

             Exception ex = walk(dir, wc);
             new File(dir).delete();
             return ex;
        }

        return null;
    }

    public enum WalkReportType {
        ForwardsWalk,
        BackwardsWalk,
        Root,
    }

    public static Exception walk(String dir, WalkCallback wc) {
        try {
            File node = new File(dir);

            if (node.isDirectory()) {
                if (node.list().length != 0) {
                    String[] subNodeStrings = node.list();

                    for (String subNodeString : subNodeStrings) {
                        File subNode = new File(node.getCanonicalPath() + File.separator + subNodeString);
                        wc.report(node, subNode, WalkReportType.ForwardsWalk);
                        walk(subNode.getCanonicalPath(), wc);
                        wc.report(node, subNode, WalkReportType.BackwardsWalk);
                    }
                }
                else {
                    wc.report(node, null, WalkReportType.Root);
                }
            }
            else {
                wc.report(node, null, WalkReportType.Root);
            }
        }
        catch (Exception ex) {
            return ex;
        }

        return null;
    }
}
