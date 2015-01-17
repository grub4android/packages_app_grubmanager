package org.grub4android.grubmanager.models.grubcfg;

import java.util.ArrayList;
import java.util.List;

public class GRUBItem {
    final List<String> mArgs;

    public GRUBItem(List<String> args) {
        mArgs = args;
    }

    public GRUBItem() {
        mArgs = new ArrayList<>();
    }

    public boolean isSubMenu() {
        return false;
    }

    public GRUBGroup asSubMenu() {
        return (GRUBGroup) this;
    }

    public boolean isCommand() {
        return false;
    }

    public GRUBGroup asCommand() {
        return (GRUBGroup) this;
    }

    protected String getIndent(int indent) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < indent; i++)
            sb.append('\t');

        return sb.toString();
    }

    public String generate(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));

        for (int i = 0; i < mArgs.size(); i++) {
            sb.append(mArgs.get(i));

            if (i != mArgs.size() - 1)
                sb.append(" ");
        }
        sb.append('\n');

        return sb.toString();
    }
}
