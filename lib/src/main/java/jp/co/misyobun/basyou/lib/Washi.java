package jp.co.misyobun.basyou.lib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * これは和紙です。
 * Created by takahashinaoto
 */
public class Washi {

    /**
     * デバッグ用
     */
    final public static String TAG = Washi.class.getName();

    /** プリファレンス　**/
    final public static String SHARED_PREFERENCE_CONFIG = "YOU_SET_THIS_FIELD";

    /**
     * 共有リファレンスから読み込み
     * @param context      Context
     * @param key          キー
     * @return キー値
     */
    public static String getStringValue(final Context context,
                                        final String key) {

        if (context.getApplicationContext() == null || key == null) {
            throw new IllegalArgumentException("");
        }

        return context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).getString(key, "");
    }

    /**
     * 共有リファレンスに書き込み
     * @param context  Context
     * @param key      キー
     * @param value    書き込み値
     */
    public static void putStringValue(final Context context,
                                      final String key,
                                      final String value) {

        if (context.getApplicationContext() == null || key == null) {
            throw new IllegalArgumentException("");
        }

        context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    /**
     * 共有リファレンスから読み込み
     * @param context      Context
     * @param key          キー
     * @return キー値
     */
    public static boolean getBooleanValue(final Context context,
                                          final String key) {

        if (context.getApplicationContext() == null || key == null) {
            throw new IllegalArgumentException("");
        }

        return context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).getBoolean(key, false);
    }

    /**
     * 共有リファレンスに書き込み
     * @param context  Context
     * @param key      キー
     * @param flag     書き込み値
     */
    public static void putBooleanValue(final Context context,
                                       final String key,
                                       final boolean flag) {

        if (context.getApplicationContext() == null || key == null) {
            throw new IllegalArgumentException("");
        }

        context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).edit().putBoolean(key, flag).commit();
    }

    /**
     * 共有リファレンスから読み込み
     * @param context      Context
     * @param key          キー
     * @return キー値(初期値:0)
     */
    public static int getIntValue(final Context context,
                                  final String key) {

        if (context.getApplicationContext() == null || key == null) {
            return 0;
        }

        return context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).getInt(key, 0);
    }

    /**
     * 共有プリファレンスに書き込み
     * @param context  Context
     * @param key      キー
     * @param value    書き込む値
     */
    public static void putIntValue(final Context context,
                                   final String key,
                                   final int value) throws NullPointerException{

        if (context.getApplicationContext() == null || key == null) {
            throw new IllegalArgumentException("");
        }

        context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).edit().putInt(key, value).commit();
    }

    /**
     * アプリケーション用の共有リファレンスを返す
     * @param context
     * @return 共有リファレンス
     */
    public static SharedPreferences getSharedPreference(final Context context) {

        if (context.getApplicationContext() == null) {
            throw new IllegalArgumentException("");
        }

        return context.getSharedPreferences(Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE);
    }

    /**
     * アプリケーション用の共有リファレンを期化する
     * @param context コンテキスト
     */
    public static void clear(final Context context){

        if (context.getApplicationContext() == null) {
            throw new IllegalArgumentException("");
        }

        context.getApplicationContext().getSharedPreferences(
                Washi.SHARED_PREFERENCE_CONFIG, Context.MODE_PRIVATE).edit().clear().commit();

    }
}
