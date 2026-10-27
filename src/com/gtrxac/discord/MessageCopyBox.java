package com.gtrxac.discord;

import javax.microedition.lcdui.*;

public class MessageCopyBox extends TextBox implements CommandListener, Strings {
    private Command backCommand;
    private Command saveNoteCommand;
    public Object lastScreen;

    public MessageCopyBox(String content) {
        this(Locale.get(MESSAGE_COPY_BOX_TITLE), content);
    }

    public MessageCopyBox(String title, String content) {
        super(title, content, content.length(), 0);
        setCommandListener(this);
        lastScreen = App.disp.getCurrent();

        backCommand = Locale.createCommand(BACK, Command.BACK, 0);
        addCommand(backCommand);

        // Only embedded builds have a host notepad to save into.
        if (App.hostBridge != null) {
            saveNoteCommand = Locale.createCommand(SAVE_TO_NOTE, Command.SCREEN, 1);
            addCommand(saveNoteCommand);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            App.disp.setCurrent(lastScreen);
        }
        else if (c == saveNoteCommand) {
            boolean saved = App.hostBridge.saveNote(getString());
            if (saved) {
                App.disp.setCurrent(new Dialog(
                    Locale.get(SAVE_TO_NOTE_L),
                    Locale.get(SAVE_TO_NOTE_DONE),
                    this
                ));
            } else {
                App.disp.setCurrent(new Dialog(
                    Locale.get(ERROR_TITLE),
                    Locale.get(SAVE_TO_NOTE_ERROR),
                    this
                ));
            }
        }
    }
}
