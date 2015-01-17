package org.grub4android.grubmanager.models.grubcfg;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GRUBCFG {

    public static GRUBMenu fromFile(String filename) throws IOException {
        Stack<GRUBGroup> contextStack = new Stack<>();
        LineIterator it = FileUtils.lineIterator(new File(filename), "UTF-8");

        GRUBMenu menu = new GRUBMenu();
        contextStack.push(menu);
        menu.isRootMenu = true;

        try {
            while (it.hasNext()) {
                String line = it.nextLine();

                // remove comments
                {
                    int pos = line.indexOf("#");
                    if (pos >= 0)
                        line = line.substring(0, pos);
                }

                // skip empty lines
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // parse arguments
                List<String> args = getArguments(line);

                // parse specific Item
                GRUBItem i = null;
                if (line.equals("}")) {
                    // leave current menu's context
                    contextStack.pop();
                } else if (args.get(args.size() - 1).equals("{")) {
                    // create group
                    GRUBGroup g = null;
                    if (args.get(0).equals("menuentry"))
                        g = new GRUBMenuEntry(args);
                    else
                        g = new GRUBGroup(args);

                    // add group as element to current group
                    contextStack.lastElement().mItems.add(g);

                    // enter new menu's context
                    contextStack.push(g);
                } else {
                    // generic item
                    i = new GRUBItem(args);
                }

                // add element to current menu's context
                if (i != null) {
                    contextStack.lastElement().mItems.add(i);
                }
            }
        } finally {
            LineIterator.closeQuietly(it);
        }

        return menu;
    }

    private static List<String> getArguments(String line) {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(line);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }

        return matchList;
    }

    public static class GRUBMenu extends GRUBGroup {

    }
}
