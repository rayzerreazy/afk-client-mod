package com.afk.client.util;

import org.lwjgl.glfw.GLFW;

/** Readable names for GLFW key codes. */
public final class Keys {
    private Keys() { }

    public static String name(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            default: break;
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) return "Numpad " + (key - GLFW.GLFW_KEY_KP_0);
        String printable = null;
        try {
            printable = GLFW.glfwGetKeyName(key, 0);
        } catch (Throwable ignored) { }
        if (printable != null && !printable.isEmpty()) return printable.toUpperCase();
        return "Key " + key;
    }
}
