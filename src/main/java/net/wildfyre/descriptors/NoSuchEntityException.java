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

import net.wildfyre.areas.Area;
import net.wildfyre.users.User;

import java.util.Optional;

/**
 * Exception thrown when the client queries an entity that does not exist server-side.
 *
 * <p>This exception stores internally a reference to the entity that did not exist, as a Descriptor. You can get it
 * using the methods {@link #get() get()}, or downcast it using methods such has asUser(), etc. Downcasting is generally
 * considered a bad practice since the introduction of Generic types in Java, but we kept it here because it is not
 * allowed to use Generics for classes that inherit from {@link java.lang.Throwable}.</p>
 */
public class NoSuchEntityException extends Exception {

    private Descriptor descriptor;

    /**
     * Creates a NoSuchEntityException.
     * @param message the message.
     */
    public NoSuchEntityException(String message){
        super(message);
    }

    /**
     * Creates a NoSuchEntityException.
     * @param message the message.
     * @param descriptor the entity that failed.
     */
    public NoSuchEntityException(String message, Descriptor descriptor){
       this(message + "\nMissing entity: " + descriptor.toString());
       this.descriptor = descriptor;
    }

    /**
     * The entity that does not exist server-side, or an empty optional if none was specified when creating this
     * exception.
     * @return The entity that does not exist server-side.
     */
    public Optional<Descriptor> get(){
       return Optional.ofNullable(descriptor);
    }

    /**
     * Was an entity specified when creating this exception?
     * @return {@code true} if an entity was given to the constructor of this exception.
     */
    public boolean isPresent(){
       return descriptor != null;
    }

    /**
     * Downcasts the descriptor stored in this exception to a User; fails if there is no descriptor stored or if the
     * descriptor is of the wrong type.
     * @return The Descriptor casted as a User, or an empty optional if no descriptor is stored or if the descriptor
     *      stored is not a subclass of User.
     */
    public Optional<User> asUser(){
       return descriptor instanceof User ?
           Optional.of((User) descriptor) :
           Optional.empty();
    }

    /**
     * Downcasts the descriptor stored in this exception to an Area; fails if there is no descriptor stored or if the
     * descriptor is of the wrong type.
     * @return The Descriptor casted as an Area, or an empty optional if no descriptor is stored or if the descriptor
     *      stored is not a subclass of Area.
     */
    public Optional<Area> asArea(){
        return descriptor instanceof Area ?
            Optional.of((Area) descriptor) :
            Optional.empty();
    }

}
