package org.grub4android.grubmanager.models.grubcfg;

import java.util.List;

public class GRUBMenuEntry extends GRUBGroup {
    public String mTitle;

    public GRUBMenuEntry(String title) {
        super();

        mTitle = title;
    }

    public GRUBMenuEntry(List<String> args) {
        super(args);
        mTitle = args.get(1);
    }
}
