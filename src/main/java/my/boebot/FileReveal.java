package my.boebot;

import java.io.File;

/**
 * FileReveal - opens the desktop file manager at a saved file's folder and, if
 * the file manager supports it, highlights (selects) the file.
 *
 * Best-effort and non-blocking: it only runs when a display is available, tries
 * a few file managers in order, and never throws. On Raspberry Pi OS the default
 * file manager (pcmanfm) opens the folder; nautilus/nemo (if installed) also
 * highlight the specific file.
 */
public final class FileReveal {

    private FileReveal() {}

    public static void reveal(File file) {
        if (file == null) return;

        String display = System.getenv("DISPLAY");
        String wayland = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (display != null && !display.isEmpty())
                          || (wayland != null && !wayland.isEmpty());
        if (!hasDisplay) {
            System.out.println("  (No display - file manager not opened. See the full path above.)");
            return;
        }

        String path   = file.getAbsolutePath();
        String folder = file.getParentFile() != null
            ? file.getParentFile().getAbsolutePath()
            : ".";

        // Ordered attempts: highlight-capable file managers first, then plain
        // folder open. First one that launches wins.
        String[][] attempts = {
            {"nautilus", "--select", path},
            {"nemo", "--no-desktop", path},
            {"pcmanfm", folder},
            {"xdg-open", folder},
        };

        for (String[] cmd : attempts) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.start();   // fire-and-forget; do NOT waitFor (GUI stays open)
                System.out.println("  Opened file manager (" + cmd[0] + ") at: " + folder);
                return;
            } catch (Exception ignore) {
                // try the next file manager
            }
        }
        System.out.println("  (Could not open a file manager automatically - open " + folder + " manually.)");
    }
}
