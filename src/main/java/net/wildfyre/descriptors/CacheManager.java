/*
 * Copyright 2018 Wildfyre.net
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

/**
 * Specifies for how long an object is kept in the local cache.
 */
public class CacheManager {

    private long expiresAfter;

    public CacheManager(){
        expiresAfter = 1000 * 60 * 30; // 30 min
    }

    /**
     * Sets for how long objects are kept in the cache, before they are updated or deleted.
     * @param millis how long they are kept in the cache, in milliseconds.
     * @return This object, to allow method-chaining.
     */
    public CacheManager setExpirationTime(long millis){
        if(millis < 0)
            throw new IllegalArgumentException("The time 'millis' should not be negative: " + millis);
        else if(millis == 0)
            System.err.println("WARN:CacheManager.setExpirationTime: millis=0");

        expiresAfter = millis;
        return this;
    }

    /**
     * How long objects are kept in the cache.
     * @return How long objects are kept in the cache, in milliseconds.
     */
    public long objectsExpireAfter(){
        return expiresAfter;
    }

}
