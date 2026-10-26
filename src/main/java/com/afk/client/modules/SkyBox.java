package com.afk.client.modules;

import com.afk.client.module.Category;
import com.afk.client.module.Module;
import com.afk.client.util.PackToggle;

/**
 * Sky Box: a galaxy sky. Turns on a built-in texture pack that replaces the sun with a glowing star
 * and gives each night a different planet instead of the moon.
 */
public class SkyBox extends Module {
    private static final String SKY_PACK_FILE = "AFK-Galaxy-Sky.zip";
    private static final String SKY_PACK_RESOURCE = "/afk-packs/galaxy-sky.zip";

    private boolean on;

    public SkyBox() {
        super(Category.VISUALS, "sky-box", "A galaxy sky: a glowing star for the sun and a different planet every night.");
    }

    @Override
    public void onActivate() {
        info("Loading the galaxy sky... the game freezes for a few seconds.");
        PackToggle.enableLater(SKY_PACK_FILE, SKY_PACK_RESOURCE, ok -> on = ok);
    }

    @Override
    public void onDeactivate() {
        PackToggle.disableLater(SKY_PACK_FILE); // harmless if it never got turned on
        on = false;
    }
}
