package jp.co.misyobun.basyou.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * 日本語では銀板写真とも呼ばれる。
 * Created by takahashinaoto
 */
public class Daguerrotype {

    public static final String TAG = Daguerrotype.class.getName();

    final public static String TMP_DIR = ".tmp";
    final public static String THUMBNAIL_DIR = ".thumb";
    final public static String MEDIA_DIR = "YOU_SET_THIS_FIELD";


    /**
     * 一時ファイルを全て削除する
     */
    public static void cleanTempFiles(final Context context) {
        final File tempfile = getTempFile(context);
        if (tempfile == null) return;
        final File[] tempfiles = tempfile.getParentFile().listFiles();
        for (final File file: tempfiles) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    /**
     * 画像ファイルからオリエンテーション情報を取得して返す
     * @param filename
     * @return
     */
    public static int getExifOrientation(final String filename) {
        try {
            final ExifInterface exif = new ExifInterface(filename);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (final IOException e) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }

    /**
     * 画像ファイルから日付情報を取得する
     * @param filename
     * @return
     */
    public static Date getExifDate(final String filename) {
        try {
            final ExifInterface exif = new ExifInterface(filename);
            final String exifDate = exif.getAttribute(ExifInterface.TAG_DATETIME);

            if (exifDate == null) {
                return null;
            }

            // The format of DateTime is "YYYY:MM:DD HH:MM:SS"
            Pattern pattern = Pattern.compile("\\d{4}:\\d{2}:\\d{2}\\s\\d{2}:\\d{2}:\\d{2}");
            Matcher matcher = pattern.matcher(exifDate);
            final boolean blnMatch= matcher.matches();

            if (blnMatch) {

                Date date;

                try {

                    pattern = Pattern.compile("\\d{4}:\\d{2}:\\d{2}");
                    matcher = pattern.matcher(exifDate);
                    if (!matcher.find()) {
                        return null;
                    }
                    final String[] format = matcher.group(0).split(":");
                    date = new Date(
                            Integer.parseInt(format[0]) + "/" + Integer.parseInt(format[1]) + "/" + Integer.parseInt(format[2]));
                } catch (final Throwable e) {
                   
                    return null;
                }

                return date;
            }

            return null;

            // 年/月/日を返却する

        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * オリエンテーションに応じた変換行列を返すナッツ！
     * @param orientation
     * @return 変換行列
     */
    public static Matrix getOriengationMatrix(final int orientation) {
        final Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_UNDEFINED:
                // 特定できない場合は単位行列を返す
                return matrix;
            case ExifInterface.ORIENTATION_NORMAL:
                return matrix;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1.0f, 1.0f);
                return matrix;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1.0f, -1.0f);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90.0f);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180.0f);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270.0f);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                // top-left軸をbottom-right軸へ変換
                matrix.postRotate(90.0f);
                matrix.postScale(-1.0f, 1.0f);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                // top-right軸をbottom-left軸へ変換
                matrix.postRotate(90.0f);
                matrix.postScale(1.0f, -1.0f);
                return matrix;
            default:
                return matrix;
        }
    }

    /**
     * オリエンテーションに応じた角度を返す（メディアストア登録用）
     * @param orientation 向き
     * @return 角度
     */
    public static int getOriengationDegree(final int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_UNDEFINED:
                return 0;
            case ExifInterface.ORIENTATION_NORMAL:
                return 0;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                // 対応不可
                return 0;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                // 対応不可
                return 0;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                // 対応不可
                return 0;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                // 対応不可
                return 0;
            default:
                return 0;
        }
    }

    /**
     * ファイルを指定してビットマップのサイズのみを返すナッツ！
     * @param filename ファイル名
     * @return サイズ
     */
    public static Rect getBitmapBounds(final String filename) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Config.ARGB_4444;

        BitmapFactory.decodeFile(filename, options);

        if ((options.outWidth < 0) || (options.outHeight < 0)) {
            return null;
        }

        return new Rect(0, 0, options.outWidth, options.outHeight);
    }

    /**
     * ファイルを指定してオリエンテーションを考慮したビットマップのサイズのみを返す
     * @param filename    ファイル名
     * @param orientation 向き
     * @return サイズ
     */
    public static Rect getBitmapBounds(final String filename, final int orientation) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Config.ARGB_4444;

        BitmapFactory.decodeFile(filename, options);

        if ((options.outWidth < 0) || (options.outHeight < 0)) {
            return null;
        }

        switch (orientation) {
            case ExifInterface.ORIENTATION_UNDEFINED:
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            case ExifInterface.ORIENTATION_ROTATE_180:
                return new Rect(0, 0, options.outWidth, options.outHeight);
            // 縦横反転系
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return new Rect(0, 0, options.outHeight, options.outWidth);
            default:
                return new Rect(0, 0, options.outWidth, options.outHeight);
        }
    }

    /**
     * 行列を取得
     * @param context
     * @param size
     * @return
     */
    public static Matrix getForDisplayMatrix(final Context context, final Rect size) {
        final Matrix matrix = new Matrix();
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();

        // 縦長の場合のサイズを求める
        int width = size.width();
        int height = size.height();
        if (width > height) {
            final int swap = height;
            height = width;
            width = swap;
        }
        int displayWidth = display.getWidth();
        int displayHeight = display.getHeight();
        if (displayWidth > displayHeight) {
            final int swap = displayHeight;
            displayHeight = displayWidth;
            displayWidth = swap;
        }
        Log.i("getForDisplayMatrix", "Bitmap size: " + width + "," + height);
        Log.i("getForDisplayMatrix", "Display size: " + displayWidth + "," + displayHeight);

        // ディスプレイサイズより小さい場合は単位行列を返す

        if ((width <= displayWidth) && (height <= displayHeight)) return matrix;

        final float scaleX = (float)displayWidth / (float)width;
        final float scaleY = (float)displayHeight / (float)height;
        final float scale = Math.min(scaleX, scaleY);
        Log.i("getForDisplayMatrix", "Scale: " + scale);
        matrix.postScale(scale, scale);
        return matrix;
    }

    /**
     * 行列により変換したビットマップを読み込む
     * @param context
     * @param uri
     * @return
     */
    public static Bitmap decodeForDisplayBitmap(
            final Context context,
            final Uri uri) {

        Bitmap original = null;
        Bitmap scaled = null;
        try {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            int enoughLength = 0;
            enoughLength = Math.max(enoughLength, display.getWidth());
            enoughLength = Math.max(enoughLength, display.getHeight());
            original = loadEnoughBitmap(context, uri, enoughLength, enoughLength);

            if (original == null) return null;
            final Rect rect = new Rect(0, 0, original.getWidth(), original.getHeight());
            final Matrix matrix = getForDisplayMatrix(context, rect);
            scaled = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            return scaled;
        } finally {
            if ((original != null) && (original != scaled)) original.recycle();
        }
    }
    /**
     * 画面サイズに応じてビットマップを縮小する。
     * 縮小画像はinmutedな画像として外部領域に保存される。
     * @param context
     * @param uri
     * @return bitmap
     * @throws FileNotFoundException
     */
    public static Bitmap makeForDisplayBitmap(final Context context, Uri uri) throws Exception {
        Log.i("makeForDisplayBitmap", "ContentUri: " + uri.toString());
        uri = contentUriToFileUri(context, uri);
        Log.i("makeForDisplayBitmap", "FileUri: " + uri.toString());

        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();

        int enoughLength = 0;

        enoughLength = Math.max(enoughLength, display.getWidth());
        enoughLength = Math.max(enoughLength, display.getHeight());

        Bitmap original = loadEnoughBitmap(context, uri, enoughLength, enoughLength);

        if (original == null) {
            return null;
        }

        final int orientation = Daguerrotype.getExifOrientation(uri.getPath());
        final Matrix matrix = Daguerrotype.getOriengationMatrix(orientation);

        final Matrix scaleMatrix = Daguerrotype.getForDisplayMatrix(
                context,
                new Rect(0, 0, original.getWidth(), original.getHeight()));

        matrix.postConcat(scaleMatrix);

        Bitmap scaled = null;

        try {

            scaled = Bitmap.createBitmap(
                    original,
                    0,
                    0,
                    original.getWidth(),
                    original.getHeight(),
                    matrix,
                    true);

            if (original != scaled) {
                original.recycle();
                original = null;
            }

        } catch (final Throwable t) {
            t.printStackTrace();
            scaled = null;
        }


        return scaled;
    }

    /**
     * 画面サイズに応じてビットマップを縮小する。
     * 縮小画像はinmutedな画像として外部領域に保存される。
     * @param context
     * @param uri
     * @return
     * @throws FileNotFoundException
     */
    public static Uri copyForDisplayBitmap(final Context context, final Uri uri) throws Exception {
        final File outputFile = getTempFile(context);

        Bitmap scaled = makeForDisplayBitmap(context, uri);
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(outputFile);
            scaled.compress(CompressFormat.JPEG, 100, out);
        } catch (final IOException e) {
            throw new Exception("ファイルが見つかりませんでした");
        } finally {
            try {
                if (out != null) out.close();
            } catch (final IOException e) {
            }
        }

        // EXIF情報の修正
        copyExif(context, uri, Uri.fromFile(outputFile), scaled);

        Log.i("copyForDisplayBitmap", "Bitmap scaled to: " + scaled.getWidth() + "," + scaled.getHeight());
        scaled.recycle();
        scaled = null;
        return Uri.fromFile(outputFile);
    }

    /**
     * ファイルパスのURIをコンテンツパスのURIに変換する
     * @param context
     * @param uri
     * @return
     * @throws Exception
     */
    public static Uri fileUriToContentUri(final Context context, final Uri uri) throws Exception {
        if (!uri.toString().startsWith("file:")) return uri;

        final String[] projection = new String[]{MediaStore.Images.ImageColumns._ID};
        final String selection = MediaStore.Images.ImageColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{uri.getPath()};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
            if (cursor.getCount() == 0) {
                throw new Exception("ファイルが見つかりませんでした");
            }
            cursor.moveToFirst();
            final String id = cursor.getString(0);
            if (id == null) {
                throw new Exception("ファイルが見つかりませんでした");
            }
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
        } catch (final NullPointerException e) {
            throw new Exception("ファイルが見つかりませんでした");
        } finally {
            try{if (cursor != null) cursor.close();}catch(final Exception ex){};
        }
    }

    /**
     * コンテンツURIをファイルパスのURIに変換する
     * @param context
     * @param uri
     * @return
     * @throws Exception
     */
    public static Uri contentUriToFileUri(final Context context, final Uri uri) throws Exception {
        if (!uri.toString().startsWith("content:")) return uri;

        final String[] projection = new String[]{MediaStore.Images.ImageColumns.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor.getCount() == 0) {
                throw new Exception("ファイルが見つかりませんでした");
            }
            cursor.moveToFirst();
            final String path = cursor.getString(0);
            if (path == null) {
                throw new Exception("ファイルが見つかりませんでした");
            }
            return Uri.fromFile(new File(path));
        } catch (final NullPointerException e) {
            throw new Exception("ファイルが見つかりませんでした");
        } finally {
            try{if (cursor != null) cursor.close();}catch(final Exception ex){};
        }
    }

    /**
     * EXIF情報をコピーする。
     * このメソッドはORIENTATIONをノーマル固定で書き込みます。
     * @param fromUri
     * @param toUri
     * @param toBitmap
     */
    public static void copyExif(final Context context, Uri fromUri, Uri toUri, final Bitmap toBitmap) {
        if ((fromUri == null) || (toUri == null) || fromUri.getPath().equals(toUri.getPath())) return;

        Log.i(Daguerrotype.class.getName(), "copyExif.fromUri: " + fromUri);
        Log.i(Daguerrotype.class.getName(), "copyExif.toUri: " + toUri);
        try {
            fromUri = contentUriToFileUri(context, fromUri);
            toUri = contentUriToFileUri(context, toUri);
        } catch (final Exception e) {
            // EXIFのコピー失敗
            return;
        }

        ExifInterface originalExif = null;
        ExifInterface scaledExif = null;
        try {
            originalExif = new ExifInterface(fromUri.getPath());
            scaledExif = new ExifInterface(toUri.getPath());
            Daguerrotype.copyExif(originalExif, scaledExif);
            scaledExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, String.valueOf(toBitmap.getWidth()));
            scaledExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, String.valueOf(toBitmap.getHeight()));
            scaledExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            scaledExif.saveAttributes();
        } catch (final IOException e) {
            // EXIF情報を取得できなかった場合はスルー
            Log.i(Daguerrotype.class.getName(), "copyExif: through");
        }
    }

    /**
     * EXIF情報をコピーする。
     * コピー元の画像サイズやオリエンテーションまでコピーされてしまうため、
     * {@link ExifInterface#saveAttributes()}を呼び出す前に
     * 適切な画像サイズ、オリエンテーションを設定すること。
     * @param from
     * @param to
     */
    private static void copyExif(final ExifInterface from, final ExifInterface to) {
        final String[] tags = new String[]{
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_WHITE_BALANCE
        };
        for (final String tag: tags) {
            final String value = from.getAttribute(tag);
            Log.i("EXIF COPY", "" + tag + ": " + value);
            if (value != null) to.setAttribute(tag, value);
        }
    }

    /**
     * exif情報の座標文字列を浮動小数点小数に変換する
     * @param coordinate
     * @return
     */
    public static double parseGeoCoordinate(final String ref, final String coordinate) {
        try {
            if ((coordinate == null) || (ref == null)) return 0.0d;
            final String[] parts = coordinate.split(",");
            if (parts.length != 3) return 0.0d;
            final String[] degree = parts[0].split("/");
            final String[] minutes = parts[1].split("/");
            final String[] seconds = parts[2].split("/");
            if ((degree.length != 2) || (minutes.length != 2) || (seconds.length != 2)) return 0.0d;
            double result = ((double)Integer.parseInt(degree[0]) / (double)Integer.parseInt(degree[1]));
            result += (Integer.parseInt(minutes[0]) / 60.0d / Integer.parseInt(minutes[1]));
            result += (Integer.parseInt(seconds[0]) / 3600.0d / Integer.parseInt(seconds[1]));
            if ("S".equals(ref) || "W".equals(ref)) {
                result *= -1.0d;
            }
            Log.i("parseGeoCoordinate", "Original: " + ref + " " + coordinate);
            Log.i("parseGeoCoordinate", "Parsed: " + result);
            return result;
        } catch (final NumberFormatException e) {
            return 0.0d;
        }
    }

    /**
     * 渡されたURIが処理できる画像であるかどうかを返す
     * @param context
     * @param uri
     * @return　
     * @throws Exception
     */
    public static boolean isValidImage(final Context context, final Uri uri) throws Exception {
        // 縮小画像を作成できたらValidImage
        final Bitmap bmp = loadEnoughBitmap(context, uri, 2, 2);

        final boolean result = bmp != null;

        if  ((bmp != null) && !bmp.isRecycled()) {
            bmp.recycle();
        }

        return result;
    }


    /**
     * 指定したファイルをコンテンツプロバイダに保存する
     *
     * @param contentResolver コンテンツプロバイダにアクセスするためリゾルバー
     * @param file コンテンツプロバイダに保存するファイル
     * @return 保存したURI
     */
    public static Uri addImageStorage(final ContentResolver contentResolver, final File file) {

        // JPEGで保存する
        final String mimeType = "image/jpeg";

        final Uri uri = Uri.fromFile(file);
        final ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, uri.getLastPathSegment());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, uri.getLastPathSegment());
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.DATA, uri.getPath());
        final long nowDate = System.currentTimeMillis() / 1000;
        values.put(MediaStore.Images.Media.DATE_ADDED, nowDate);
        values.put(MediaStore.Images.Media.DATE_MODIFIED, nowDate);
        final int degree = getOriengationDegree(getExifOrientation(file.getPath()));
        values.put(MediaStore.Images.Media.ORIENTATION, String.valueOf(degree));

        final Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        return imageUri;
    }

    /**
     * 必要十分のサイズのビットマップを読み込む
     * @param uri
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    public static Bitmap loadEnoughBitmap(
            final Context context,
            final Uri uri,
            final int targetWidth,
            final int targetHeight) {

        final BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
        boundsOpts.inJustDecodeBounds = true;

        // ARGBでそれぞれ0～127段階の色を使用（メモリ対策）
        boundsOpts.inPreferredConfig = Config.ARGB_4444;

        BitmapFactory.decodeFile(uri.getPath(), boundsOpts);

        // 丈、幅が取れない画像は壊れている
        if ((boundsOpts.outWidth < 0) || (boundsOpts.outHeight < 0)) {
            return null;
        }

        Log.e(Daguerrotype.class.getName(), "boundsOpts.outWidth->" + boundsOpts.outWidth);
        Log.e(Daguerrotype.class.getName(), "boundsOpts.outHeigh->" + boundsOpts.outHeight);


        // 画面サイズに併せる
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        boundsOpts.inDensity = metrics.densityDpi;


        int width = boundsOpts.outWidth / 2;
        int height = boundsOpts.outHeight / 2;
        int scale = 1;

        while ((width > targetWidth) && (height > targetHeight)) {
            width /= 2;
            height /= 2;
            scale *= 2;
        }

//        final Config config = Config.ARGB_4444;

//        if (boundsOpts.outWidth * boundsOpts.outHeight * 2.0d > 10000000.0d) {
//            config = Config.ARGB_4444;
//        }

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = scale;

        opts.inDensity = metrics.densityDpi;
        opts.inPreferredConfig = Config.ARGB_8888;

        Bitmap  bitmap = null;


        Log.e(Daguerrotype.class.getName(), "scale->" + scale);
        Log.e(Daguerrotype.class.getName(), "metrics.densityDpi->" + metrics.densityDpi);

        try {
            bitmap = BitmapFactory.decodeFile(uri.getPath(), opts);

            Log.e(Daguerrotype.class.getName(), "bitmap.getWidth()->" + bitmap.getWidth());
            Log.e(Daguerrotype.class.getName(), "bitmap.getHeight()->" + bitmap.getHeight());
            Log.e(Daguerrotype.class.getName(), "opts->" + opts.outWidth);
            Log.e(Daguerrotype.class.getName(), "opts->" + opts.outHeight);


        } catch (final Throwable t) {

            try {
                opts.inSampleSize = scale * 2;
                bitmap = BitmapFactory.decodeFile(uri.getPath(), opts);

            } catch (final Throwable t2) {

            }
        }

        return bitmap;
    }

    /**
     * パスからファイル名を取得して返す
     * @param filename
     * @return
     */
    public static String getFilename(final String filename) {
        int start = 0;
        while (true) {
            final int found = filename.indexOf(File.separator, start);
            if (found == -1) break;
            start = found + 1;
            if (start >= filename.length()) return "";
        }
        return filename.substring(start);
    }

    /**
     * ファイルをコピーする
     * @param fromUri contentスキームもしくはfileスキーム
     * @param toUri fileスキームのみ
     * @throws Exception
     */
    public static void copy(final Context context, final Uri fromUri, final Uri toUri) throws Exception {
        final Uri fromUriFile = Daguerrotype.contentUriToFileUri(context, fromUri);

        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(new File(fromUriFile.getPath()));
            out = new FileOutputStream(new File(toUri.getPath()));
            int length = 0;
            final byte[] buffer = new byte[1024];
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (final IOException e) {
            throw new Exception(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (final IOException e) {
            }
            try {
                if (out != null) out.close();
            } catch (final IOException e) {
            }
        }
    }

    /**
     * 一時保存先ファイルを返す。
     * 一時保存ファイルはアプリケーション起動時などに一括削除を行う。
     * @param context
     * @return 一時保存先ファイル
     */
    public static File getTempFile(final Context context) {
        while (true) {
            final Date now = Calendar.getInstance().getTime();
            final SimpleDateFormat format = new SimpleDateFormat("yMdHmsS");
            final File file = new File(context.getExternalFilesDir(TMP_DIR), format.format(now));
            file.getParentFile().mkdirs();

            // ファイルが衝突していなければ返す
            if (!file.exists()) return file;

            // フリーズさせないためにスリープを入れる
            try {
                Thread.sleep(10, 0);
            } catch (final InterruptedException e) {
            }
        }
    }

    /**
     * サムネイル保存先ファイルを返す
     * @param  context
     * @return 保存先ファイル
     */
    public static File getThumbnailDir(final Context context) {
        final File file = new File(context.getExternalFilesDir(THUMBNAIL_DIR), "");
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * 画像保存先ファイルを返す。
     * @return 保存先ファイル
     */
    public static File getMediaFile() {
        while (true) {
            final Date now = Calendar.getInstance().getTime();
            final SimpleDateFormat format = new SimpleDateFormat("yMdHmsS");
            final File file =  new File(Environment.getExternalStorageDirectory() + MEDIA_DIR, format.format(now) + ".jpg");
            file.getParentFile().mkdirs();

            // ファイルが衝突していなければ返す
            if (!file.exists()) return file;

            // フリーズさせないためにスリープを入れる
            try {
                Thread.sleep(10, 0);
            } catch (final InterruptedException e) {
            }
        }
    }

}
