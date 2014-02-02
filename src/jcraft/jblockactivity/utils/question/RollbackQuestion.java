package jcraft.jblockactivity.utils.question;

import jcraft.jblockactivity.editor.BlockEditor;

public class RollbackQuestion implements QuestionData {

    private BlockEditor editor = null;

    public BlockEditor getEditor() {
        return editor;
    }

    public void setEditor(BlockEditor editor) {
        this.editor = editor;
    }

}
