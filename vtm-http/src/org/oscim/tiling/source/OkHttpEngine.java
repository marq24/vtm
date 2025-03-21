/*
 * Copyright 2014 Charles Greb
 * Copyright 2014 Hannes Janetzek
 * Copyright 2017-2021 devemux86
 * Copyright 2017 Mathieu De Brito
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.tiling.source;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.oscim.core.Tile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class OkHttpEngine implements HttpEngine {

    private static final Logger log = Logger.getLogger(OkHttpEngine.class.getName());

    private final OkHttpClient mClient;
    private final UrlTileSource mTileSource;

    private InputStream mInputStream;
    private byte[] mCachedData;

    public static class OkHttpFactory implements HttpEngine.Factory {
        private final OkHttpClient.Builder mClientBuilder;

        public OkHttpFactory() {
            mClientBuilder = new OkHttpClient.Builder();
        }

        public OkHttpFactory(OkHttpClient.Builder clientBuilder) {
            mClientBuilder = clientBuilder;
        }

        @Override
        public HttpEngine create(UrlTileSource tileSource) {
            return new OkHttpEngine(mClientBuilder.build(), tileSource);
        }
    }

    public OkHttpEngine(OkHttpClient client, UrlTileSource tileSource) {
        mClient = client;
        mTileSource = tileSource;
    }

    @Override
    public InputStream read() throws IOException {
        return mInputStream;
    }

    @Override
    public void sendRequest(Tile tile) throws IOException {
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null.");
        }
        try {
            URL url = new URL(mTileSource.getTileUrl(tile));
            Request.Builder builder = new Request.Builder()
                    .url(url);
            for (Entry<String, String> opt : mTileSource.getRequestHeader().entrySet())
                builder.addHeader(opt.getKey(), opt.getValue());
            Request request = builder.build();
            Response response = mClient.newCall(request).execute();
            if (mTileSource.tileCache != null) {
                mCachedData = response.body().bytes();
                mInputStream = new ByteArrayInputStream(mCachedData);
            } else
                mInputStream = response.body().byteStream();
        } catch (Exception e) {
            log.severe(e.toString());
        }
    }

    @Override
    public void close() {
        if (mInputStream == null)
            return;

        try {
            mInputStream.close();
        } catch (Exception e) {
            log.severe(e.toString());
        }
        mInputStream = null;
    }

    @Override
    public void setCache(OutputStream os) {
        if (mTileSource.tileCache != null) {
            try {
                os.write(mCachedData);
            } catch (IOException e) {
                log.severe(e.toString());
            }
        }
    }

    @Override
    public boolean requestCompleted(boolean success) {
        close();
        return success;
    }
}
