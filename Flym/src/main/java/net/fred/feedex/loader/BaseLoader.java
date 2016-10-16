package net.fred.feedex.loader;

/*
 * Copyright (C) 2011 Alexander Blom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.AsyncTaskLoader;
import android.content.Context;

/**
 * Loader which extends AsyncTaskLoaders and handles caveats as pointed out in http://code.google.com/p/android/issues/detail?id=14944.
 * <p/>
 * Based on CursorLoader.java in the Fragment compatibility package
 *
 * @param <D> data type
 * @author Alexander Blom (me@alexanderblom.se)
 */
public abstract class BaseLoader<D> extends AsyncTaskLoader<D> {
    private D mData;

    protected BaseLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(D data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            return;
        }

        mData = data;

        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        mData = null;
    }
}