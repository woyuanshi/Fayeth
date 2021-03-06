package fayeth.engine.ub;

import fayeth.subprocess.SubprocessListener;
import fayeth.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UbOutputListener implements SubprocessListener {

    private UbTask ubTask;
    public UbOutputListener(UbTask ubTask) {
        this.ubTask = ubTask;
    }


    @Override
    public void onStdoutLine(String line) {
        //  Log.info("STDOUT:" + line);
        // ASAN error
        if (matchError(line, ".*ERROR: AddressSanitizer: ([a-zA-Z_0-9-]+) .*", "AddressSanitizer:", 1)) return;
        // ASAN alloc failure
        if (matchError(line, ".*AddressSanitizer's (.*?)", "AddressSanitizer: Failure:", 1)) return;
        // UBSAN error
        if (matchError(line, ".*?: runtime error: (.*?)", "UBSanitizer:", 1)) return;
        // Process abortion
        if (matchError(line, ".*? Abort (.*?)", "Process abort:", 1)) return;
    }

    @Override
    public void onStderrLine(String line) {
        Log.info("STDERR:" + line);
    }

    @Override
    public void onExit(int code) {
        Log.info("EXIT:" + code);
        if (code != 0) {
            ubTask.onBugFound("Non-zero exit code: " + code);
        }
        ubTask.onCompletion();
    }

    @Override
    public void onTimeout() {
        Log.info("TIMEOUT");
        ubTask.onCompletion();
    }

    private boolean matchError(String line, String errPattern, String errOutPrefix, int errMatchGroup) {
        Pattern pattern = Pattern.compile(errPattern);
        Matcher matcher = pattern.matcher(line);
        if(matcher.matches()) {
            ubTask.onBugFound(String.format("%s %s", errOutPrefix, matcher.group(errMatchGroup)));
            return true;
        }
        return false;
    }

}
