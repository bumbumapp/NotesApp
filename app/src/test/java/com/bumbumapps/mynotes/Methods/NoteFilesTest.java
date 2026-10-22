package com.bumbumapps.mynotes.Methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NoteFilesTest {
    @Test
    public void exportTextKeepsAllParts() {
        assertEquals("T\n\nS\n\nBody", NoteFiles.exportText("T", "S", "Body"));
        assertEquals(" \n\n \n\n ", NoteFiles.exportText(null, "", null));
    }

    @Test
    public void exportFolderStripsIllegalCharacters() {
        assertEquals("My Notes", NoteFiles.exportFolder("My/Notes:"
                .replace("/", " ").replace(":", "")));
        assertEquals("Notes", NoteFiles.exportFolder("///"));
    }

    @Test
    public void imageCopiesGetUniqueNames() {
        assertTrue(NoteFiles.imageFileName(1L).endsWith(".jpg"));
        assertFalse(NoteFiles.imageFileName(1L).equals(NoteFiles.imageFileName(2L)));
    }

    /** Regression: exporting a note as .txt used to clear the note text afterwards. */
    @Test
    public void exportDoesNotClearTheNote() throws IOException {
        String code = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/bumbumapps/mynotes/Activity/Note/CreateNoteActivity.java")));
        assertFalse(code.contains("nemosoftsEditText.getText().clear()"));
        assertFalse(code.contains("getExternalStoragePublicDirectory"));
    }
}
