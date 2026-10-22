package com.bumbumapps.mynotes.Methods;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Scoped-storage helpers: picks images with the system photo picker (no storage permission),
 * keeps a private copy for the note, and exports files to Downloads via MediaStore.
 */
public final class NoteFiles {
    private NoteFiles() {
    }

    /** Photo picker intent (falls back to ACTION_GET_CONTENT on devices without it). */
    public static Intent pickImageIntent(@NonNull Context context) {
        return new ActivityResultContracts.PickVisualMedia().createIntent(context,
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
    }

    /** Copies the picked image into app storage and returns its absolute path, or null. */
    @Nullable
    public static String copyToAppStorage(@NonNull Context context, @NonNull Uri uri) {
        File dir = new File(context.getFilesDir(), "note_images");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return null;
        }
        File out = new File(dir, imageFileName(System.currentTimeMillis()));
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            return out.getAbsolutePath();
        } catch (IOException | SecurityException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            return null;
        }
    }

    static String imageFileName(long timeMillis) {
        return "note_" + timeMillis + ".jpg";
    }

    /** Folder (relative to the Downloads collection) used for exports. */
    public static String exportFolder(@NonNull String appName) {
        String clean = appName.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        return clean.isEmpty() ? "Notes" : clean;
    }

    /** Builds the plain-text export of a note. */
    public static String exportText(@Nullable String title, @Nullable String subtitle, @Nullable String body) {
        return nonEmpty(title) + "\n\n" + nonEmpty(subtitle) + "\n\n" + nonEmpty(body);
    }

    private static String nonEmpty(@Nullable String s) {
        return s == null || s.isEmpty() ? " " : s;
    }

    /**
     * Opens an output stream for a new file in Downloads/&lt;folder&gt;. API 29+ uses MediaStore
     * (no permission); older versions write the file directly (WRITE_EXTERNAL_STORAGE, maxSdk 28).
     * Returns the content/file Uri or null; the caller writes through {@link Writer}.
     */
    @Nullable
    public static Uri saveToDownloads(@NonNull Context context, @NonNull String folder,
                                      @NonNull String fileName, @NonNull String mimeType,
                                      @NonNull Writer writer) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + folder);
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    return null;
                }
                try (OutputStream os = resolver.openOutputStream(uri)) {
                    if (os == null) {
                        throw new IOException("No stream for " + uri);
                    }
                    writer.write(os);
                } catch (IOException e) {
                    resolver.delete(uri, null, null);
                    throw e;
                }
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
                return uri;
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder);
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    return null;
                }
                File file = new File(dir, fileName);
                try (OutputStream os = new FileOutputStream(file)) {
                    writer.write(os);
                }
                return Uri.fromFile(file);
            }
        } catch (IOException | SecurityException | IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    public interface Writer {
        void write(OutputStream os) throws IOException;
    }

    /** True if exporting needs the legacy storage permission (API 28 and lower). */
    public static boolean needsLegacyWritePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

}
