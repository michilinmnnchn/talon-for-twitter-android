package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Adapter;

import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ImageUtils;

import org.lucasr.smoothie.SimpleItemLoader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class CursorListLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    final BitmapLruCache mCache;
    private Context context;

    public CursorListLoader(BitmapLruCache cache, Context context) {
        mCache = cache;
        this.context = context;
    }

    public CursorListLoader(BitmapLruCache cache, Context context, boolean circle) {
        mCache = cache;
        this.context = context;
    }

    @Override
    public CacheableBitmapDrawable loadItemFromMemory(String url) {
        return mCache.getFromMemoryCache(url);
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        try {
            Cursor cursor = (Cursor) adapter.getItem(0);
            cursor.moveToPosition(cursor.getCount() - position - 1);
            String url = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            HomeDataSource.getInstance(context).close();
            ((Activity) context).recreate();
            return "";
        }

    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {

        CacheableBitmapDrawable wrapper = mCache.get(url);
        if (wrapper == null) {

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());

                Bitmap image = decodeSampledBitmapFromResourceMemOpt(is, 1000, 1000);

                try {
                    is.close();
                } catch (Exception e) {

                }
                try {
                    conn.disconnect();
                } catch (Exception e) {

                }

                wrapper = mCache.put(url, image);
            } catch (Exception e) {

            }
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        final TimeLineCursorAdapter.ViewHolder holder = (TimeLineCursorAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        holder.profilePic.setImageDrawable(result);
    }

    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}