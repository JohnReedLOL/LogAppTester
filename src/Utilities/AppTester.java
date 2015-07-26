package Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Use this for various testing/debugging purposes including multi-threaded
 * print statements with built in stack trace, assertions that stop the entire
 * application rather than just the current thread, and application termination
 * with a full stack trace for terminal error conditions. Can also check and
 * respond to background events.
 *
 * @author johnmichaelreed2
 */
public class AppTester {

    // <editor-fold defaultstate="collapsed" desc="Enums">
    /**
     * Used to rank terminal readouts based on relative importance.
     */
    public static enum Rank {

        UNIMPORTANT(0), NORMAL(1), IMPORTANT(2);
        /**
         * My importance level starts at zero and increases with increasing
         * importance.
         */
        private final int myImportanceLevel_;

        private Rank(int severity) {
            myImportanceLevel_ = severity;
        }

        /**
         * @return the importance level of this enum used for comparison
         * purposes. Starts at zero and increases with increasing importance.
         */
        public int getImportance() {
            return myImportanceLevel_;
        }
    }

    /**
     * Used to specify which standard output terminal bound strings should be
     * sent through.
     */
    public static enum DefaultPrintStream {

        ONLY_STANDARD_OUT, ONLY_STANDARD_ERROR, EITHER_STD_OUT_OR_STD_ERROR
    }

    /**
     * Used to specify whether something is being printed under an error
     * situation or a non-error situation.
     */
    public static enum ReadoutCondition {

        ERROR, NON_ERROR
    }

    /**
     * Used for readouts that are important, such as error messages.
     */
    public static final Rank IMPORTANT = Rank.IMPORTANT;

    /**
     * The default Rank for terminal readouts.
     */
    public static final Rank NORMAL = Rank.NORMAL;

    /**
     * Used for readouts that are unimportant, such as explanations of code
     * flow.
     */
    public static final Rank UNIMPORTANT = Rank.UNIMPORTANT;

    /**
     * Used to specify that all terminal output should go through standard out.
     */
    public static DefaultPrintStream ONLY_STANDARD_OUT = DefaultPrintStream.ONLY_STANDARD_OUT;

    /**
     * Used to specify that all terminal output should go through standard
     * error.
     */
    public static DefaultPrintStream ONLY_STANDARD_ERROR = DefaultPrintStream.ONLY_STANDARD_ERROR;

    /**
     * Used to specify that some terminal output should go through standard out
     * and some terminal output, such as exception stack traces and assertion
     * failure, should go through standard error.
     */
    public static DefaultPrintStream EITHER_STD_OUT_OR_STD_ERROR = DefaultPrintStream.EITHER_STD_OUT_OR_STD_ERROR;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Vars, Getters, and Setters">
    
    /**
     * The system independent line separator, shortened to two letters for
     * convenience.
     */
    public static final String ls = System.getProperty("line.separator");
    static {
        AppTester.check(ls != null);
    }
    
    /**
     * The number of important rows in a stack trace.
     */
    private static int numberOfRowsIDisplayInStackTraces_ = 6;
    
    public static int getNumberOfRowsIDisplayInStackTraces_() {
        return numberOfRowsIDisplayInStackTraces_;
    }

    public static void setNumberOfRowsIDisplayInStackTraces_(int aNumberOfRowsIDisplayInStackTraces_) {
        AppTester.check(aNumberOfRowsIDisplayInStackTraces_ >= 0, "You can't display a negative number of rows in a stack trace.");
        numberOfRowsIDisplayInStackTraces_ = aNumberOfRowsIDisplayInStackTraces_;
    }
    
    /**
     * When this variable is true, the log file will be printed to. If it is false,
     * the log file will not be used even if it can be written to.
     */
    private static boolean printToLogFile_ = true;
    
    /**
     * When this variable is true, the terminal will be printed to. If it is false,
     * the terminal will not be printed to.
     */
    private static boolean printToTerminal_ = true;

    /**
     * All messages that are at this debug level or higher are printed. By
     * default set to {@link #NORMAL}
     */
    private static Rank myRank_ = NORMAL;

    /**
     * Specified where print stream messages should go to. By default set to
     * {@link #EITHER_STD_OUT_OR_STD_ERROR}
     */
    private static DefaultPrintStream myTargetPrintStream_ = DefaultPrintStream.ONLY_STANDARD_OUT;

    /**
     * This thread polls for a variety of background events, where each
     * background events is an materialization of the
     * {@link Utilities.BackgroundEvent_Interface} interface. Its name is
     * "Event_Checker". Its initialization is deferred until its first use.
     */
    private static ScheduledExecutorService myScheduler_
            = null; //Executors.newSingleThreadScheduledExecutor();

    /**
     * This represents the main thread. If this thread is dead,
     * {@link #myScheduler_} thread must die as well, otherwise, the application
     * won't exit at the end of the main method.
     */
    private static final Thread myMainThread_;

    static {
        myMainThread_ = Thread.currentThread();
    }

    public static Thread getMyMainThread() {
        return myMainThread_;
    }
    
    /**
     * @return the variable {@link #printToLogFile_}
     */
    public static boolean getGenerateLogFiles() {
        return printToLogFile_;
    }

    /**
     * Determines whether or not the log file should be printed to.
     * @param useLogFileOrNot true for printing to the log file and false
     * for no printing to the log file.
     */
    public static void setPrintToLogFile(boolean useLogFileOrNot) {
        printToLogFile_ = useLogFileOrNot;
    }
    
    /**
     * @return the variable {@link #printToTerminal_}
     */
    public static boolean getPrintToTerminal() {
        return printToTerminal_;
    }
    
    /**
     * Determines whether of not the terminal should be printed to.
     * @param toPrint true for printing to the terminal or false for no printing
     * to the terminal.
     */
    public static void setPrintToTerminal(boolean toPrint) {
        printToTerminal_ = toPrint;
    }

    /**
     * @return the currently in use {@link Utilities.AppTester.Rank}
     */
    public static Rank getMyDebugLevel() {
        return myRank_;
    }

    /**
     * @param level all messages that are at this debug level or higher are
     * printed. Messages that are below (less important than) this debug level
     * are not printed.
     */
    public static void setMyDebugLevel(Rank level) {
        myRank_ = level;
    }

    /**
     * @return the currently in use
     * {@link Utilities.AppTester.DefaultPrintStream}
     */
    public static DefaultPrintStream getMyDefaultPrintStream() {
        return myTargetPrintStream_;
    }

    /**
     * All terminal error messages will go through this
     * {@link Utilities.AppTester.DefaultPrintStream}
     *
     * @param streamType
     */
    public static void setMyDefaultPrintStream(DefaultPrintStream streamType) {
        myTargetPrintStream_ = streamType;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Delayed Initialization Vars">
    /**
     * The name of the folder that holds all the log files, or null if there are
     * no log files.
     */
    private static final String myLogFolderNameOrNull_;// = "Log_Files";

    static {
        // initialize the above variable in the static block.
        final String intendedFolderName = "Log_Files";
        final File logFilesFolder = new File(FileFinder.WORKING_DIRECTORY
                + FileFinder.FILE_SEPARATOR + intendedFolderName);
        if (!logFilesFolder.exists()) {
            // No folder exists, so the folder needs to be created.
            boolean madeFolder = false;
            try {
                madeFolder = logFilesFolder.mkdir();
            } catch (SecurityException se) {
                // madeFolder remains false.
            }

            if (madeFolder) {
                //Tester.print("A log file folder was created.", NORMAL);
                myLogFolderNameOrNull_ = intendedFolderName;
            } else {
                //Tester.print("The log file folder could not be created.", NORMAL);
                myLogFolderNameOrNull_ = null;
            }
        } else {
            // The folder already exists.
            //Tester.print("The log file folder already exists.", NORMAL);
            myLogFolderNameOrNull_ = intendedFolderName;
        }
    }

    /**
     * The name of the log file if log file creation was successful, or null if
     * no log file was created. Perform null checks before use.
     */
    private static final String myLogFileNameOrNull_;
    
    /**
     * @return Either the name of the log file or empty string if the log file
     * was not successfully created.
     */
    public static String getLogFileNameOrEmptyString() {
        if(myLogFileNameOrNull_ != null) {
            return myLogFileNameOrNull_;
        } else {
            return "";
        }
    }
    /**
     * If initialization was successful, this variable will be non-null,
     * otherwise it will remain null. Perform null checks before use.
     */
    private static FileOutputStream myLogFileOutputStreamOrNull_ = null;
    /**
     * If initialization was successful, this variable will be non-null,
     * otherwise it will remain null. Perform null checks before use.
     */
    private static OutputStreamWriter myLogOutputStreamWriterOrNull_ = null;
    /**
     * If initialization was successful, this variable will be non-null,
     * otherwise it will by null. Perform null checks before use.
     */
    private static final BufferedWriter myLogBufferedWriterOrNull_; // this does the actual writing

    static {
        // initialize myLogFileNameOrNull_ and the writers.
        if (!(myLogFolderNameOrNull_ == null)) {
            final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd___HH:mm:ss");
            final Calendar cal = Calendar.getInstance();
            final String dateTimeForLogFile = dateFormat.format(cal.getTime()); /* 2014_08_06___16:00:22 */

            final String expectedFileName = dateTimeForLogFile + ".txt";
            final String fullPath = FileFinder.WORKING_DIRECTORY
                    + FileFinder.FILE_SEPARATOR + myLogFolderNameOrNull_
                    + FileFinder.FILE_SEPARATOR + expectedFileName;

            final File logFile = new File(fullPath);

            boolean wasFileCreated = false;
            try {
                wasFileCreated = logFile.createNewFile();
            } catch (IOException ioe) {
                // wasFileCreated remains false.
            } catch (SecurityException se) {
                // wasFileCreated remains false.
            }

            boolean success = false;
            if (wasFileCreated == true) {
                final String wasFileFound = FileFinder.tryFindPathToFileWhoseNameIs(expectedFileName);
                AppTester.check(wasFileFound != null, "I made the file so I should be able to find it.");
                //Tester.printEx("Managed to create log file.", AppTester.IMPORTANT);
                try {
                    myLogFileOutputStreamOrNull_ = new FileOutputStream(logFile, true); // File not found exception???
                    myLogOutputStreamWriterOrNull_ = new OutputStreamWriter(myLogFileOutputStreamOrNull_, "utf-8"); // unsupported encoding exception
                } catch (FileNotFoundException fnfe) {
                    AppTester.killApplication("It's impossible for a newly created file to not be found.", fnfe);
                } catch (UnsupportedEncodingException uee) {
                    AppTester.killApplication("It's impossible for UTF-8 to be an unsupported text format.", uee);
                } catch (Exception e) {
                    //just ignore it, the things we are trying to initialize will be null.
                    myLogFileOutputStreamOrNull_ = null;
                    myLogOutputStreamWriterOrNull_ = null;
                }

                if (myLogOutputStreamWriterOrNull_ != null) {
                    myLogBufferedWriterOrNull_ = new BufferedWriter(myLogOutputStreamWriterOrNull_);
                    try {
                        myLogBufferedWriterOrNull_.write("Starting log file" + ls);
                        success = true;
                    } catch (IOException ioe) {
                        // Just ignore it - don't kill the thread.
                    }
                } else {
                    myLogBufferedWriterOrNull_ = null;
                }
            } else {
                //Tester.printEx("Failed to create log file.", AppTester.IMPORTANT);
                myLogFileOutputStreamOrNull_ = null;
                myLogOutputStreamWriterOrNull_ = null;
                myLogBufferedWriterOrNull_ = null;
            }

            if (success == true) {
                myLogFileNameOrNull_ = expectedFileName;
            } else {
                myLogFileNameOrNull_ = null;
            }
        } else {
            // No log folder, so definetely no log files.
            AppTester.printerr("Could not create log file because log folder does not exist.");
            myLogFileNameOrNull_ = null;
            myLogBufferedWriterOrNull_ = null;
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    /**
     * Closes the AppTester by killing any background threads it may be running
     * and closing its log file writer. Must be called at or before termination.
     */
    public static void close() {
        
        try {
            if (myScheduler_ != null) {
                myScheduler_.shutdownNow();
                printlnToReadout("\n" + "The scheduler has been shut down", ReadoutCondition.NON_ERROR, UNIMPORTANT);
            }
        } catch (Exception e) {
            // ignore it.
            //e.printStackTraceNoLeadingLineNumber();
        }
        try {
            if (myLogBufferedWriterOrNull_ != null) {
                printlnToReadout("\n" + "The log file is being shut down.", ReadoutCondition.NON_ERROR, NORMAL);
                myLogBufferedWriterOrNull_.close();
            } 
        } catch (Exception ioe) {
            // ioe.printStackTraceNoLeadingLineNumber();
            // ignore the error. myLogBufferedWriterOrNull_ is already closed.
        }
    }

    /**
     * Tries to write text to log file. Replaces all "\n" newline
     * characters that may have been sent to the terminal with OS specific
     * end of line characters before printing to the text file.
     *
     * @param text the text to be written to the log file.
     * @return false if no text is written or true if text is successfully
     * written.
     */
    private static boolean tryWritingSomethingToLogFileNoNewline(String text) {
        if (myLogBufferedWriterOrNull_ == null) {
            return false;
        } else {
            try {
                String updatedText = text.replaceAll("\n", ls);
                myLogBufferedWriterOrNull_.write(updatedText);
                myLogBufferedWriterOrNull_.flush();
                return true;
            } catch (IOException ioe) {
                // means the writer was closed
                return false;
            }
        }
    }
    
    public static void printlnToReadout(final String message, ReadoutCondition condition, Rank severity) {
        printToReadout((message + "\n"), condition, severity);
    }

    /**
     * This print statement is meant for use by other print statements in
     * this class, but it can also be used as a more verbose alternative to
     * {@link #print(java.lang.String) } or {@link #printerr(java.lang.String) }
     * if you desire not to include file names or line numbers.. It also appends 
 text to the log file, but unlike printEx and print, this
 method does NOT include the line number, method name, or class name at the
 top of the print statement.
     *
     * @param message the message to be printed
     * @param condition whether the message is an error or non-error message
     */
    public static synchronized void printToReadout(final String message, ReadoutCondition condition, Rank severity) {

        // Logging happens regardless of severity.
        if (printToLogFile_) {
            // log stuff
            if (!(AppTester.myLogFileNameOrNull_ == null)) {
                final String logFilesPathString = FileFinder.tryFindPathToFileWhoseNameIs(myLogFileNameOrNull_);
                AppTester.check(logFilesPathString != null, "The log file name is non-null, so the log file must exist.");
                boolean success = AppTester.
                        tryWritingSomethingToLogFileNoNewline(message.replaceAll("\n", ls));
                // this success is being silently ignored if it doesn't write to log file,
                // I'm not doing anything about it.
            } else {
                // Don't bother, it won't work.
            }
        }
        if(! printToTerminal_) {
            // Do not print anything.
            return;
        } else if (myRank_.getImportance() > severity.getImportance()) {
            // This message is not important enough to be printed. 
            return; // return without printing to terminal.
        } else {
            // This message is important enough to be printed to terminal.
            if (myTargetPrintStream_ == ONLY_STANDARD_OUT) {
                System.out.print(message);
            } else if (myTargetPrintStream_ == ONLY_STANDARD_ERROR) {
                System.err.print(message);
            } else {
                // myTargetPrintStream_ == EITHER_STD_OUT_OR_STD_ERROR
                if (condition == ReadoutCondition.ERROR) {
                    System.err.print(message);
                } else if (condition == ReadoutCondition.NON_ERROR) {
                    System.out.print(message);
                } else {
                    AppTester.killApplication("This condition is logically impossible");
                }
            }
        }
    }

    /**
     * Kills the entire application and leaves a stack trace.
     *
     * @param message - message to print before the application terminates.
     */
    public static void killApplication(String message) {
        AppTester.check(false, message, 3);
    }

    /**
     * Kills the application, prints a message, and also print the throwable who
     * is responsible for the crash. Prints the entire throwable.
     *
     * @param message message to print before the application terminates.
     * @param t throwable whose stack trace is to be included in the
     * termination.
     */
    public static void killApplication(String message, Throwable t) {
        StackTraceElement[] ste = t.getStackTrace();
        String concatenation = message + "\n";
        concatenation += t.toString() + "\n";
        for (int i = 0; i < ste.length; ++i) {
            concatenation += (ste[i].toString() + "\n");
        }
        AppTester.killApplicationNoStackTrace(concatenation);
    }
    
    /**
     * Alternate form of {@link #killApplication(java.lang.String, java.lang.Throwable) }.
     * Prints the entire stack trace.
     */
    public static void killApplication(Throwable t, String messsage) {
        killApplication(messsage, t);
    }

    /**
     * Kills the entire application and prints out the stack trace elements of
     * the throwable responsible
     *
     * @param t the throwable responsible.
     */
    public static void killApplication(Throwable t) {
        killApplication("", t);
    }

    /**
     * Kills the entire application without leaving a stack trace.
     *
     * @param message - message to print before the application terminates.
     */
    public static void killApplicationNoStackTrace(String message) {
        printlnToReadout("\n" + message, ReadoutCondition.ERROR, Rank.IMPORTANT);
        close();
        System.exit(-1);
    }

    /**
     * Checks to see if an assertion is true. Prints stack trace and crashes the
     * program if not true. USE THIS INSTEAD OF REGULAR "assert" STATEMENTS. For
     * information on how to use an assert statement, see:
     * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/language/assert.html">Programming
     * With Assertions</a>
     *
     * @param assertion - assertion to be checked
     */
    public static void check(boolean assertion) {
        check(assertion, "Empty_Assertion", 3); // nomally it would be 2 but the indirections bumps it up to 3.
    }

    /**
     * Checks to see if an assertion is true and prints an error message if
     * false. Also prints a stack trace and crashes the program if false. For
     * information on how to use an assert statement, see:
     * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/language/assert.html">Programming
     * With Assertions</a>
     *
     * @param assertion - assertion to be checked
     * @param message - error message to print
     */
    public static void check(final boolean assertion, final String message) {
        check(assertion, message, 3); // nomally it would be 2 but the indirections bumps it up to 3.
    }

    private static void check(final boolean assertion, final String message, int firstRowOfStackTrace) {
        if (!assertion) {
            
            String toBePrinted = "\n" + "Assertion failed in Thread: \""
                    + Thread.currentThread().getName() + "\"" + "\n" + message;
            // length of stack trace is 5 (thread called by check called by check called by handle called by main)
            //printlnToReadout(toBePrinted, ReadoutCondition.ERROR, Rank.IMPORTANT); // print rows 3 & 4
            final StackTraceElement[] stackTraceArray = Thread.currentThread().getStackTrace();
            // This should print the stack trace from firstRowOfStackTrace down.
            printStackTraceNoLeadingLineNumberWithLeadingMessageAndNewline(toBePrinted, stackTraceArray, firstRowOfStackTrace);
            close();
            System.exit(-1);
        }
    }

    private static void print(String message, Rank severityLevel, int stackTraceStart) {
        final String thread_name = Thread.currentThread().getName();
        final String location_of_print_statement = Thread.currentThread().getStackTrace()[stackTraceStart].toString();
        printlnToReadout("\n" + "Thread \"" + thread_name + "\": "
                + location_of_print_statement + "\n" + message, ReadoutCondition.NON_ERROR, severityLevel);
    }

    /**
     * Prints the name of the current thread and where the print statement comes
     * from to standard output or to the stream specified by
     * {@link #myTargetPrintStream_}. The {@link #myTargetPrintStream_} variable
     * acts as a global override for the preferred output stream of the
     * application.
     *
     * @param message - message to be printed
     */
    private static void print(String message, Rank severityLevel) {
        print(message, severityLevel, 3);
    }
    
    /**
     * Prints an unimportant non-error message.
     * Short for "unimportant print".
     */
    public static void uPrint(String message) {
        print(message, UNIMPORTANT, 3);
    }

    /**
     * Same as {@link #print(java.lang.String, Utilities.AppTester.Rank)  },
     * but with the {@link #NORMAL} severity level specified by default.
     *
     * @param message the message to be printed as if it were a regular
     * non-error message.
     */
    public static void print(String message) {
        print(message, NORMAL, 3);
    }
    
    /**
     * Prints an important non-error message.
     * Short for "important print."
     */
    public static void iPrint(String message) {
        print(message, IMPORTANT, 3);
    }

    /**
     * Prints an error message to terminal and/or log file including
     * accompanying stack trace element.
     *
     * @param message The error message
     * @param severityLevel How important the message is
     * @param stackTraceStart The position of the current line on the stack
     * trace
     */
    private static void printerr(String message, Rank severityLevel, int stackTraceStart) {
        final String thread_name = Thread.currentThread().getName();
        final String location_of_print_statement = Thread.currentThread().getStackTrace()[stackTraceStart].toString();
        printlnToReadout("\n" + "Thread \"" + thread_name + "\": "
                + location_of_print_statement + "\n" + message, ReadoutCondition.ERROR, severityLevel);
    }

    /**
     * Prints the name of the current thread and where the print statement comes
     * from to the error output stream or to the stream specified by
     * {@link #myTargetPrintStream_}. The {@link #myTargetPrintStream_} variable
     * acts as a global override for the preferred output stream of the
     * application.
     *
     * @param message - message to be printed
     */
    private static void printerr(String message, Rank severityLevel) {
        printerr(message, severityLevel, 3);
    }
    
    /**
     * Short for print error not important. Prints an unimportant error.
     * Short for "unimportant print error".
     */
    public static void uPrinterr(String message) {
        printerr(message, UNIMPORTANT, 3);
    }

    /**
     * Same as {@link #printerr(java.lang.String, Utilities.AppTester.Rank) 
     * }, but with the {@link #NORMAL} severity level specified by default.
     *
     * @param message the message to be printed as if it were an error.
     */
    public static void printerr(String message) {
        printerr(message, NORMAL, 3);
    }
    
    /**
     * Short for print error important. Prints an important error.
     * Short for "important print error".
     */
    public static void iPrinterr(String message) {
        printerr(message, IMPORTANT, 3);
    }

    /**
     * Prints out a throwable as if it were and error and logs it appropriately.
     * Short for "print exception".
     *
     * @param t Exception or error to be printed as an error.
     */
    public static void printEx(Throwable t) {
        printThrowableNoLeadingLineNumber(t);
    }

    /**
     * Prints out a message and a stack trace caused by a throwable as an error.
     * Message cannot be an empty string or null.
     *
     * @param message message to be printed
     * @param t throwable that caused the message to be printed
     */
    public static void printEx(String message, Throwable t) {
        printThrowableNoLeadingLineNumber(message, t);
    }
    
    /**
     * Same as {@link #printEx(java.lang.String, java.lang.Throwable) }, but
     * with a leading message.
     */
    public static void printEx(Throwable t, String message) {
        printThrowableNoLeadingLineNumber(message, t);
    }
    
    /**
     * Prints a stack trace starting from firstRow preceded by a leading message and a newline.
     * @param message the message which appears just before the stack trace.
     * @param stackTrace The stack trace to be printed.
     * @param firstRow The first row of the stack trace, between zero and length.
     */
    private static void printStackTraceNoLeadingLineNumberWithLeadingMessageAndNewline(final String message, StackTraceElement[] stackTrace, int firstRow) {
        /**
         * This will be the important (front) part of the stack trace.
         */
        final int numImportantRows = numberOfRowsIDisplayInStackTraces_;
        String importantConcatenation = "";
        importantConcatenation += (message + "\n"); // add leading message and newline
        final int length = stackTrace.length;
        AppTester.check(firstRow <= length, "The first row of the stack trace is outside of the bounds of the stack trace array.");
        final int totalNumRowsToPrint = length - firstRow;
        if(totalNumRowsToPrint <= numImportantRows) {
            // This loops totalNumRowsToPrint times.
            for(int i = firstRow; i < firstRow + totalNumRowsToPrint; ++i) {
                importantConcatenation += stackTrace[i] + "\n";
            }
            printToReadout(importantConcatenation, ReadoutCondition.ERROR, IMPORTANT); // This println statement adds an extra trailing newline
            return;
        } else {
            // Both of these for loops together loop totalNumRowsToPrint times.
            // First for loop iterates numImportantRows times.
            for(int i = firstRow; i < firstRow + numImportantRows; ++i) {
                importantConcatenation += stackTrace[i] + "\n";
            }
            printToReadout(importantConcatenation, ReadoutCondition.ERROR, IMPORTANT); // This println statement adds an extra trailing newline
            /**
             * This will be the less important (back) part of the stack trace.
             */
            String unimportantConcatenation = "";
            for(int i = firstRow + numImportantRows; i < firstRow + totalNumRowsToPrint; ++i) {
                unimportantConcatenation += stackTrace[i] + "\n";
            }
            printToReadout(unimportantConcatenation, ReadoutCondition.ERROR, UNIMPORTANT);
            return;
        }
    } 
    
    private static void printThrowableNoLeadingLineNumber(String nonNullMessage, Throwable t) {
        AppTester.check(nonNullMessage != null, "The message is not supposed to be null");
        String toPrint = "\n" + nonNullMessage + "\n" + t.toString();
        // printlnToReadout(toPrint, ReadoutCondition.ERROR, IMPORTANT);
        printStackTraceNoLeadingLineNumberWithLeadingMessageAndNewline(toPrint, t.getStackTrace(), 0);
        printlnToReadout(Thread.currentThread().getStackTrace()[3].toString() + "\n", ReadoutCondition.ERROR, IMPORTANT);
    }
    
    private static void printThrowableNoLeadingLineNumber(Throwable t) {
        String toPrint = "\n" + t.toString();
        //printlnToReadout(toPrint, ReadoutCondition.ERROR, IMPORTANT);
        printStackTraceNoLeadingLineNumberWithLeadingMessageAndNewline(toPrint, t.getStackTrace(), 0);
        printlnToReadout(Thread.currentThread().getStackTrace()[3].toString() + "\n", ReadoutCondition.ERROR, IMPORTANT);
    }

    /**
     * Same as {@link #tryPollForBackgroundEventAfterEveryXmsStartingInYms(Utilities.BackgroundEvent_Interface, long, long)
     * }
     * , but the first poll occurs after a time delay equal to the delay between
     * subsequent polls.
     *
     * @return true on successful polling event submission, false if event
     * cannot be polled for.
     */
    public static boolean tryPollForBackgroundEventAfterEveryXms(BackgroundEvent_Interface event, long milliseconds) {
        return tryPollForBackgroundEventAfterEveryXmsStartingInYms(event, milliseconds, milliseconds);
    }

    /**
     * Polls for and responds to a background event at regular intervals
     *
     * @param event the event to poll for and respond to
     * @param millisecondInterval the number of milliseconds between the end of
     * one poll/response and the start of the next.
     * @param millisecondsToPollingStart the number of milliseconds until the
     * first poll for background event occurs.
     * @return true if the event can be polled for and false if an exception
     * occurs preventing the polling events from happening.
     */
    public static boolean tryPollForBackgroundEventAfterEveryXmsStartingInYms(final BackgroundEvent_Interface event, final long millisecondInterval, final long millisecondsToPollingStart) {

        AppTester.check(event != null, "Event canot be null");
        AppTester.check(millisecondInterval > 0, "Interval must be positive");
        AppTester.check(millisecondsToPollingStart > 0, "Delay must be positive");

        if (myScheduler_ == null) {
            // deferred instantiation.
            myScheduler_ = Executors.newSingleThreadScheduledExecutor();
            // deferred name assignment.
            myScheduler_.execute(new Runnable() {
                // Set the name of myEventCheckerThread_ to "Event_Checker".
                @Override
                public void run() {
                    Thread.currentThread().setName("Event_Checker");
                }
            });
        }
        final Runnable handleEvent = new Runnable() {

            @Override
            public void run() {
                if (!myMainThread_.isAlive()) {
                    // Initiate shutdown procedures.
                    close();
                    return;
                } else {
                    final boolean didEventOccur = event.checkForEventOccurance();
                    if (didEventOccur) {
                        event.respondToEventOccurance();
                    }
                }
            }
        };
        try {
            myScheduler_.scheduleWithFixedDelay(handleEvent, millisecondsToPollingStart, millisecondInterval, TimeUnit.MILLISECONDS);
            return true;
        } catch (RejectedExecutionException ree) {
            AppTester.printEx("myScheduler_ is already shut down", ree);
            return false;
        } catch (Exception someOtherException) {
            return false;
        }
    }
    // </editor-fold>
}
