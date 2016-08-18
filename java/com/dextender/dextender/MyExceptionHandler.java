package com.dextender.dextender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

//==================================================================================
// Class: MyExceptionHandler
// Class to catch any unhandled Exceptions
// Author: ! Mike LiVolsi
//
// When debugging, I'll be using this class to peak inside the files on the phone
// to see what's going on, since the hardware device is behaving slightly different
// than the virtual AVD device
//
// Called by: MyActivity
//
//==================================================================================

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;

    private String localPath;
 //   private String url;

    /*
     * if any of the parameters is null, the respective functionality
     * will not be used
     */
    public MyExceptionHandler(String localPath) {
        this.localPath = localPath;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();

        String filename = "dextender.err";

        if (localPath != null) {
            writeToFile(stacktrace, filename);
        }

        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String stacktrace, String filename) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(
                    localPath  + filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
