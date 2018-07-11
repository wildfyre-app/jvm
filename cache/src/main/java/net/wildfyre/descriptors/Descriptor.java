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
 * Descriptors represent the raw data from the server, and are used by the internal cache.
 *
 * This class is NOT part of the public API.
 */
public abstract class Descriptor {

    //region Data validation

    /** The time during which the cache is kept, in milliseconds. Default value: 5 min. */
    private static long timeBeforeRemoval = 1000*60*5;

    private long lastUsage;

    /**
     * Set the time of validation of the cache.
     * @param timeBeforeRemoval the time of validation, in milliseconds.
     */
    public static void setTimeBeforeRemoval(long timeBeforeRemoval){
        if(timeBeforeRemoval <= 0)
            throw new IllegalArgumentException("The parameter timeBeforeRemoval cannot be less than 0: "
                + timeBeforeRemoval);

        Descriptor.timeBeforeRemoval = timeBeforeRemoval;
    }

    /**
     * Checks if this descriptor is still valid.
     * @param currentTime the current time in milliseconds, as provided by {@link System#currentTimeMillis()}.
     * @return {@code true} if this descriptor is valid.
     * @see #isValid() Shortcut without the currentTime parameter
     */
    public boolean isValid(long currentTime){
        return currentTime - lastUsage < timeBeforeRemoval;
    }

    /**
     * Checks if this descriptor is still valid.
     * @return {@code true} if this descriptor is valid.
     * @see #isValid(long) For large collections
     */
    public final boolean isValid(){
        return isValid(System.currentTimeMillis());
    }

    //endregion
    //region Updating

    /**
     * Updates this Descriptor.
     */
    public abstract void update();

    //endregion
    //region References

    /**
     * This class represents a reference to a Descriptor in the cache.
     *
     * <p>This class is important because of how the cache is stored. This class stores the ID of the Descriptor it
     * references, and not an actual pointer. This class then simulates being a pointer by searching for the ID in the
     * cache. The reason this is important is for the case where the object was deleted. In pure Java, the garbage
     * collector will not remove objects as long as any references to them exists. Since this class does not count as a
     * reference (it holds the ID, not the object), the Descriptor is only referenced by the cache -- which means that
     * when the cache decides to remove it, it is actually deleted. In that case, this class will throw an exception
     * when the user tries to recover the object.</p>
     *
     * @param <T> The type of the Descriptor referenced by this object.
     */
    public abstract static class Reference<T extends Descriptor> {

        /**
         * Gets the Descriptor referenced by this object.
         * @return The Descriptor referenced by this object.
         * @throws NoSuchFieldException if the object does not exist.
         */
        public abstract T get() throws NoSuchFieldException;

        /**
         * Updates the Descriptor referenced by this object.
         * @throws NoSuchFieldException if the object does not exist.
         */
        public final void update() throws NoSuchFieldException {
            get().update();
        }

    }

    //endregion

}
