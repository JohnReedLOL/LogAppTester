package Test;

import Utilities.AppTester;

/**
 *
 * @author johnmichaelreed2
 */
class Test {

    public static void main(String args[]) throws Exception {
        
        // only print to standard out
        AppTester.setMyDefaultPrintStream(AppTester.ONLY_STANDARD_OUT);
        AppTester.printerr("I am to be printed in standard out");
        Thread.sleep(20);
        // only print to standard error
        AppTester.setMyDefaultPrintStream(AppTester.ONLY_STANDARD_ERROR);
        AppTester.printerr("I am to be printed in standard error");
        
        try {
            two();
        } catch (Exception e) {
            AppTester.printEx(e);
            AppTester.printEx(new Exception("I am another exception"));
            AppTester.printEx("Input mismatch", new Exception("I am a third exception"));
        }

    }

    public static void two() throws Exception {
        three();
        AppTester.printerr("I am a method called by another method.");
        throw new Exception("Hi, I am an exception.");
    }

    public static void three() {
        four();
        AppTester.uPrint("The 'u' in u-Print stands for 'unimportant'.\n"
                + "I am not important enough to appear in the terminal, although I am in the log file.");
        
        AppTester.setMyDebugLevel(AppTester.Rank.IMPORTANT); // only important messages will show.
        AppTester.iPrint("The 'i' in i-Print stands for 'important'.\n"
                + "I am so important that I cannot be squelched.");
        AppTester.setMyDebugLevel(AppTester.Rank.UNIMPORTANT); // Now even unimportant messages will show.
        AppTester.setMyDebugLevel(AppTester.Rank.NORMAL); // This is the default output level.
    }

    public static void four() {
        five();
        AppTester.printerr("lalala 4");
    }

    public static void five() {
        six();
    }

    public static void six() {
        seven();
    }

    public static void seven() {
        AppTester.printerr("lalala 7");
        // only display 4 rows of stack trace.
        AppTester.setNumberOfRowsIDisplayInStackTraces_(4); 
        // The rest of the stack trace will only appear in the log file.
        AppTester.check(false, "I am an assertion and you failed me.");
        // The above line is an assertion.
    }

}
