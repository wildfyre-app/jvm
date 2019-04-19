/*
 * Copyright 2019 Wildfyre.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wildfyre.descriptors;

import net.wildfyre.http.Request;

/**
 * Descriptors represent the raw data from the server, and are used by the internal cache.
 *
 * This class is NOT part of the public API.
 */
public abstract class Descriptor {

    //region Data validation

    private long lastUsage;
    private boolean isNew;

    {   // Initializer that calls the method just before any new object gets created.
        use();
        isNew = true;
    }

    /**
     * Checks if this descriptor is still valid.
     * @param currentTime the current time in milliseconds, as provided by {@link System#currentTimeMillis()}.
     * @return {@code true} if this descriptor is valid.
     * @see #isValid() Shortcut without the currentTime parameter
     */
    public boolean isValid(long currentTime){
        return currentTime - lastUsage < cacheManager().objectsExpireAfter();
    }

    /**
     * Checks if this descriptor is still valid.
     * @return {@code true} if this descriptor is valid.
     * @see #isValid(long) For large collections
     */
    public final boolean isValid(){
        return isValid(System.currentTimeMillis());
    }

    /**
     * Permits to know if this object was ever used before.
     * <p>This method tracks whether {@link #use()} was ever called on this object (except by the initializer).</p>
     * @return {@code true} if this object has already been used at least once.
     */
    public final boolean isNew(){
        return isNew;
    }

    /**
     * Marks this descriptors' last usage as now.
     */
    public final void use() {
        lastUsage = System.currentTimeMillis(); isNew = false;
    }

    /**
     * The Cache Manager that handles this object.
     * @return This object.
     */
    public abstract CacheManager cacheManager();

    //endregion
    //region Updating

    /**
     * Updates this Descriptor.
     * <p>The update is always executed in the current thread.</p>
     */
    public abstract void update() throws NoSuchEntityException, Request.CantConnectException;

    //endregion

}
