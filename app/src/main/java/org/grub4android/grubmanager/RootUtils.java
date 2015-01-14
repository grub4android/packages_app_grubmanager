package org.grub4android.grubmanager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.Constants;
import com.stericson.RootTools.RootTools;

import org.apache.commons.io.FileUtils;
import org.grub4android.grubmanager.models.MountInfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class RootUtils {
    public static String BUSYBOX = null;

    public static boolean initBusybox(Context context) {
        if (BUSYBOX != null) return true;

        File busybox = Utils.copyAssets(context, "busybox");
        if (busybox == null)
            return false;

        busybox.setExecutable(true);
        BUSYBOX = busybox.getAbsolutePath();
        return true;
    }

    private static void commandWait(Shell shell, Command cmd) throws Exception {

        while (!cmd.isFinished()) {

            RootTools.log(Constants.TAG, shell.getCommandQueuePositionString(cmd));
            RootTools.log(Constants.TAG, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.");

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
    }

    public static int[] getBootloaderPartMajorMinor(String name) {
        final int[] majmin = {-1, -1};

        Command command = new Command(0, false,
                BUSYBOX + " ls -lL /dev/block/platform/*/by-name/" + name + " | " + BUSYBOX + " awk -F\\  '{print $5; print $6}' | " + BUSYBOX + " awk -F, '{print $1}' | " + BUSYBOX + " xargs"
        ) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String[] ret = line.split(" ");
                majmin[0] = Integer.valueOf(ret[0]);
                majmin[1] = Integer.valueOf(ret[1]);
            }
        };
        try {
            Shell shell = RootTools.getShell(true);
            shell.add(command);
            commandWait(shell, command);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return majmin;
    }

    public static ArrayList<MountInfo> getMountInfo() {
        LineNumberReader lnr = null;
        FileReader fr = null;

        try {
            Command cmd = new Command(0, false,
                    BUSYBOX + " cat /proc/1/mountinfo > /data/local/RootToolsMountInfo",
                    BUSYBOX + " chmod 0777 /data/local/RootToolsMountInfo"
            );
            Shell shell = RootTools.getShell(true);
            shell.add(cmd);
            commandWait(shell, cmd);

            fr = new FileReader("/data/local/RootToolsMountInfo");
            lnr = new LineNumberReader(fr);
            String line;
            ArrayList<MountInfo> mountinfos = new ArrayList<MountInfo>();
            while ((line = lnr.readLine()) != null) {
                RootTools.log(line);

                String[] fields = line.split(" ");
                String[] majmin = fields[2].split(":");
                mountinfos.add(new MountInfo(
                        Integer.parseInt(fields[0]), // mountID
                        Integer.parseInt(fields[1]), // parentID
                        Integer.parseInt(majmin[0]), // major
                        Integer.parseInt(majmin[1]), // minor
                        fields[3], // root
                        fields[4], // mountPoint
                        fields[5], // mountOptions
                        fields[6], // optionalFields
                        fields[7], // separator
                        fields[8], // fsType
                        fields[9], // mountSource
                        fields[10] // superOptions
                ));
            }

            return mountinfos;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fr.close();
            } catch (Exception e) {
            }

            try {
                lnr.close();
            } catch (Exception e) {
            }
        }
    }

    public static Command unzip(String filename, String dir, final CommandFinished cb, boolean root) throws IOException, TimeoutException, RootDeniedException {
        Command cmd = new Command(0, false,
                BUSYBOX + " unzip \"" + filename + "\" -d \"" + dir + "\""
        ) {
            @Override
            public void commandCompleted(int id, int exitcode) {
                super.commandCompleted(id, exitcode);
                if (cb != null) cb.commandFinished(exitcode);
            }

            @Override
            public void commandTerminated(int id, String reason) {
                super.commandTerminated(id, reason);
                if (cb != null) cb.commandFinished(-1);
            }
        };
        return RootTools.getShell(root).add(cmd);
    }

    public static int dd(String source, String destination) {
        int rc = -1;

        Command command = new Command(0, false,
                BUSYBOX + " dd if=\"" + source + "\" of=\"" + destination + "\""
        );
        try {
            Shell shell = RootTools.getShell(true);
            shell.add(command);
            commandWait(shell, command);
            rc = command.getExitCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rc;
    }

    public static boolean backupExists(String name) {
        File dirExt = Environment.getExternalStoragePublicDirectory("G4ABackup");
        return RootUtils.exists(dirExt.getAbsolutePath() + "/" + name);
    }

    public static void doBackup(String filename, String name) throws Exception {
        // calculate source checksum
        String sha1sum = RootUtils.sha1sum(filename);
        if (sha1sum == null) throw new Exception("Can't get source checksum!");

        // backup file
        File dirExt = Environment.getExternalStoragePublicDirectory("G4ABackup");
        dirExt.mkdirs();
        if (RootUtils.dd(filename, dirExt.getAbsolutePath() + "/" + name) != 0)
            throw new Exception("Can't backup file!");

        // get backup checksum
        String sha1sum_backup = RootUtils.sha1sum(filename);
        if (sha1sum_backup == null) throw new Exception("Can't get backup checksum!");

        // verify backup
        if (!sha1sum.equals(sha1sum_backup))
            throw new Exception("Checksums do not match!");

        // store checksum
        FileUtils.writeStringToFile(new File(dirExt.getAbsolutePath() + "/" + name + ".sha1"), sha1sum);
    }

    public static void restoreBackup(String name, String filename) throws Exception {
        // find backup
        File dirExt = Environment.getExternalStoragePublicDirectory("G4ABackup");
        String backupFile = dirExt.getAbsolutePath() + "/" + name;
        String backupFileSHA1 = dirExt.getAbsolutePath() + "/" + name + ".sha1";
        if (!RootUtils.exists(backupFile) || !RootUtils.exists(backupFileSHA1))
            throw new Exception("Backup doesn't exist!");

        // calculate backup checksum
        String sha1sum = RootUtils.sha1sum(backupFile);
        if (sha1sum == null) throw new Exception("Can't get backup checksum!");

        // verify backup
        String sha1sum_stored = FileUtils.readFileToString(new File(backupFileSHA1));
        Log.e("G4A", sha1sum);
        Log.e("G4A", sha1sum_stored);
        if (!sha1sum.equals(sha1sum_stored))
            throw new Exception("Backup file is corrupted! " + sha1sum + "==" + sha1sum_stored);

        // restore file
        if (RootUtils.dd(backupFile, filename) != 0)
            throw new Exception("Can't restore file!");
    }

    public static Command installPackage(Context context, String script, String installDir, String pkgDir, String checksumSHA1, String lkPart, final CommandFinished cb) throws Exception {
        Command cmd = new Command(0, false,
                // install LK
                BUSYBOX + " dd if=\"" + pkgDir + "/lk.img\" of=\"" + lkPart + "\"",

                // create bootloader directory
                BUSYBOX + " mkdir -p \"" + installDir + "\"",

                // copy boot fs
                BUSYBOX + " cp -RfP " + pkgDir + "/grub_boot_fs/* \"" + installDir + "\"",

                // copy manifest and checksum-file
                BUSYBOX + " cp \"" + pkgDir + "/manifest.json\" \"" + installDir + "/\"",
                BUSYBOX + " echo -n " + checksumSHA1 + " > \"" + installDir + "/package.sha1\"",

                // set permissions
                BUSYBOX + " chown -R 0:0 \"" + installDir + "\"",
                BUSYBOX + " chmod -R 0644 \"" + installDir + "\""
        ) {
            @Override
            public void commandCompleted(int id, int exitcode) {
                super.commandCompleted(id, exitcode);
                if (cb != null) cb.commandFinished(exitcode);
            }

            @Override
            public void commandTerminated(int id, String reason) {
                super.commandTerminated(id, reason);
                if (cb != null) cb.commandFinished(-1);
            }
        };
        return RootTools.getShell(true).add(cmd);
    }

    public static int chmod(String filename, String mode, boolean recursive) {
        int rc = -1;

        Command command = new Command(0, false,
                BUSYBOX + " chmod " + (recursive ? "-R " : "") + mode + " \"" + filename + "\""
        );
        try {
            Shell shell = RootTools.getShell(true);
            shell.add(command);
            commandWait(shell, command);
            rc = command.getExitCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rc;
    }

    public static boolean exists(String filename) {
        boolean rc = false;

        Command command = new Command(0, false,
                BUSYBOX + " ls -l \"" + filename + "\""
        );
        try {
            Shell shell = RootTools.getShell(true);
            shell.add(command);
            commandWait(shell, command);
            rc = command.getExitCode() == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rc;
    }

    public static void copyToCache(String filename, String cacheFilename) throws Exception {
        if (!RootTools.copyFile(filename, cacheFilename, false, false))
            throw new Exception("Couldn't read file");
        int rc = RootUtils.chmod(cacheFilename, "0777", false);
        if (rc != 0)
            throw new Exception("rc = " + rc);
    }

    public static String sha1sum(String filename) {
        int rc = -1;
        final StringBuilder sum = new StringBuilder();

        Command command = new Command(0, false,
                BUSYBOX + " sha1sum " + " \"" + filename + "\"  | " + BUSYBOX + " cut -d' ' -f1"
        ) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sum.append(line);
            }
        };
        try {
            Shell shell = RootTools.getShell(true);
            shell.add(command);
            commandWait(shell, command);
            rc = command.getExitCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rc == 0 ? sum.toString() : null;
    }

    public static interface CommandFinished {
        public abstract void commandFinished(int exitcode);
    }
}
