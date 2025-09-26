package com.AS.Yuppi.Raspi.DataWorkers;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FilePickerHelper {

    public interface FilePickerCallback {
        void onFilesPicked(List<Uri> uris);
        void onPickError(String error);
    }

    private static final int REQUEST_CODE_PICK_FILES = 1001;

    private Activity activity;
    private FilePickerCallback callback;
    private boolean allowMultiple;

    public FilePickerHelper(Activity activity) {
        this.activity = activity;
    }

    /*
     * Запускает выбор файлов.
     * @param mimeType MIME тип для фильтрации файлов например, * / * или image/ *
     * @param allowMultiple разрешить множественный выбор
     * @param callback коллбэк с выбранными Uri
     */
    public void pickFiles(String mimeType, boolean allowMultiple, FilePickerCallback callback) {
        this.callback = callback;
        this.allowMultiple = allowMultiple;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

        try {
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_FILES);
        } catch (Exception e) {
            if (callback != null) callback.onPickError("Не удалось запустить выбор файла: " + e.getMessage());
        }
    }

    /**
     * Вызывается из onActivityResult() вашей Activity:
     *
     * @param requestCode код запроса из onActivityResult
     * @param resultCode код результата из onActivityResult
     * @param data Intent с результатом из onActivityResult
     * @return true, если обработано событие выбора файла
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICK_FILES) {
            return false; // не наш запрос — не обрабатываем
        }

        if (resultCode != Activity.RESULT_OK) {
            if (callback != null) callback.onPickError("Выбор файла отменён");
            return true;
        }

        if (data == null) {
            if (callback != null) callback.onPickError("Данные выбора файла отсутствуют");
            return true;
        }

        List<Uri> uris = new ArrayList<>();

        // Обрабатываем множественный выбор
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                if (uri != null) uris.add(uri);
            }
        } else {
            // Один файл выбран
            Uri uri = data.getData();
            if (uri != null) uris.add(uri);
        }

        if (callback != null) {
            callback.onFilesPicked(uris);
        }

        return true;
    }
}