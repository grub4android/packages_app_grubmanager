package org.grub4android.grubmanager.models.grubcfg;

import java.util.ArrayList;
import java.util.List;

public class GRUBGroup extends GRUBItem {
    public final List<GRUBItem> mItems = new ArrayList<>();
    boolean isRootMenu = false;

    public GRUBGroup(List<String> args) {
        super(args);
    }

    public GRUBGroup() {
        super();
    }

    @Override
    public boolean isSubMenu() {
        return true;
    }

    @Override
    public String generate(int indent) {
        StringBuilder sb = new StringBuilder();

        if (!isRootMenu) {
            sb.append(super.generate(indent));
        }

        for (GRUBItem i : mItems) {
            sb.append(i.generate(isRootMenu ? indent : indent + 1));
        }

        if (!isRootMenu) {
            sb.append(getIndent(indent));
            sb.append("}\n");
        }

        return sb.toString();
    }
}
