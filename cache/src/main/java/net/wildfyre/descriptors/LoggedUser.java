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

import com.eclipsesource.json.JsonObject;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;

import static net.wildfyre.http.Method.PATCH;

/**
 * Represents the user you are logging-in with.
 */
public class LoggedUser extends User {

    LoggedUser(int id) {
        super(id);
    }

    @Override
    public boolean canEdit(){
        return true;
    }

    //region UniqueSetters

    /**
     * Sets this user's bio.
     *
     * <p>This method is a wrapper around {@link #set(String, String, String)}, see its documentation for more
     * information.</p>
     *
     * @param bio the new bio
     */
    @SuppressWarnings("WeakerAccess")
    public void setBio(String bio){
        set(null, bio, null);
    }

    /**
     * Sets this user's name.
     *
     * <p>This method is a wrapper around {@link #set(String, String, String)}, see its documentation for more
     * information.</p>
     *
     * @param username the new user's name
     */
    @SuppressWarnings("WeakerAccess")
    public void setUsername(String username){
        set(username, null, null);
    }

    /**
     * Sets this user's avatar.
     *
     * <p>This method is a wrapper around {@link #set(String, String, String)}, see its documentation for more
     * information.</p>
     *
     * @param avatar the new user's avatar
     */
    @SuppressWarnings("WeakerAccess")
    public void setAvatar(String avatar){
        set(null, null, avatar);
    }

    //endregion
    //region Setter

    /**
     * Changes the username, bio or avatar of the user.
     *
     * <p>This method uses client-side prediction, which means that the changes will be reflected to the current
     * object immediately, and the query will be sent to the server.</p>
     *
     * <p>You can select which fields to update and which fields to keep by giving {@code null} to any unchanged field.
     * However, you cannot specify ONLY {@code null} values (the request to the server would be empty!)</p>
     *
     * @param username the user's name
     * @param bio the user's bio
     * @param avatar the user's avatar (URL path)
     * @see #setAvatar(String) Set only the avatar
     * @see #setBio(String) Set only the bio
     * @see #setUsername(String) Set only the username
     */
    @SuppressWarnings("WeakerAccess")
    public void set(String username, String bio, String avatar){
        JsonObject json = new JsonObject();

        if(username != null){
            super.name = username;
            json.add("name", username);
        }

        if(bio != null){
            super.bio = bio;
            json.add("bio", bio);
        }

        if(avatar != null){
            super.avatar = avatar;
            json.add("avatar", avatar);
        }

        if(json.isEmpty())
            throw new NullPointerException("Every provided parameter was null, at least one should not be!");

        Internal.submit(() -> { // Send query to server
            try {
                new Request(PATCH, "/users/")
                    .addToken(Internal.getToken())
                    .addJson(json)
                    .get();

            } catch (IssueInTransferException | Request.CantConnectException e) {
                //TODO: Better exception-in-cache handling
                throw new RuntimeException();
            }

            this.update();
        });
    }

    //endregion

}
