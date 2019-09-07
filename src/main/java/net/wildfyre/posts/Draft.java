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

package net.wildfyre.posts;

import com.eclipsesource.json.JsonObject;
import net.wildfyre.api.Internal;
import net.wildfyre.areas.Area;
import net.wildfyre.areas.Areas;
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;
import net.wildfyre.users.Users;
import net.wildfyre.utils.InvalidJsonException;

import java.util.Optional;

import static net.wildfyre.http.Method.*;

/**
 * This class represents a Draft, which is essentially a builder for a Post. Drafts can be saved server-side before they
 * are converted to a Post, using {@link #publish()}. They can be saved server-side using {@link #save()}. Drafts will
 * not attempt to synchronize themselves, it is the responsibility of the user to either save or publish them, each time
 * they are modified.
 */
public class Draft extends PostData implements PostData.Setters<Draft> {

    /** Is this draft only local, or has it already been saved server-side? */
    private boolean isOnlyLocal;

    //region Constructors

    /**
     * Creates a new local Draft.
     *
     * <p>The method {@link Area#draft()} is essentially the same as this constructor.</p>
     *
     * @param areaID the area it will be in.
     */
    public Draft(String areaID){
        super();

        if(areaID == null || areaID.equals(""))
            throw new IllegalArgumentException("The parameter areaID should not be null nor empty: '" + areaID + "'");

        super.areaID = areaID;
        super.authorID = Users.myID().orElseThrow(RuntimeException::new);
        isOnlyLocal = true;
    }

    /**
     * Creates a new Draft that points to a server-side Draft.
     *
     * <p>This method is for advanced-users only. If you want to create a Draft, use {@link #Draft(String)} or
     * {@link Area#draft()}.</p>
     *
     * @param draftID the ID of the draft
     * @param areaID the ID of the area the draft is in
     */
    public Draft(long draftID, String areaID){
        super();

        if(areaID == null || areaID.equals(""))
            throw new IllegalArgumentException("The parameter areaID should not be null nor empty: '" + areaID + "'");

        if(draftID <= 0)
            throw new IllegalArgumentException("The parameter draftID should be a positive integer: " + draftID);

        super.areaID = areaID;
        super.postID = draftID;
        isOnlyLocal = false;
    }

    //endregion
    //region Server modifications
    //region Publishing

    /**
     * Converts this Draft to a Post and publishes it. This Draft does not need to have been previously saved using
     * {@link #save()}.
     *
     * <p>This method will perform the API calls in the current thread.</p>
     *
     * @return The Post resulting from the publishing of this Draft.
     */
    public Post publish(){
        JsonObject json;
        if(isOnlyLocal) json = createAndPublish();
        else            json = publishDraft();

        try {
            this.update(json);

        } catch (InvalidJsonException e) {
            throw new RuntimeException(); //TODO: See T262
        }
        return new Post(this);
    }

    JsonObject createAndPublish(){
        try {
            return new Request(POST, "/areas/" + areaID + "/")
                .addToken(Internal.token())
                .addJson(toJsonSimple())
                .getJson()
                .asObject();

        } catch (IssueInTransferException e) {
            throw new RuntimeException(); //TODO: See T262

        } catch (Request.CantConnectException e) {
            throw new RuntimeException(); //TODO: See T262
        }

    }

    JsonObject publishDraft(){
        try {
            return new Request(POST, "/areas/" + areaID + "/drafts/" + postID + "/publish/")
                .addToken(Internal.token())
                .getJson().asObject();

        } catch (IssueInTransferException e) {
            throw new RuntimeException(); //TODO: See T262

        } catch (Request.CantConnectException e) {
            throw new RuntimeException(); //TODO: See T262
        }
    }

    //endregion
    //region Saving to server

    /**
     * Saves this Draft server-side.
     *
     * <p>This method will perform the API calls in the current thread.</p>
     *
     * @return This object itself, to allow method-chaining.
     */
    public Draft save(){
        if (isOnlyLocal) saveFirstTime();
        else             saveAsEdit();

        return this;
    }

    void saveFirstTime(){
        try {
            JsonObject json = new Request(POST, "/areas/" + areaID + "/drafts/")
                .addToken(Internal.token())
                .addJson(toJsonSimple())
                .getJson()
                .asObject();

            this.update(json);
            area().cachedDraft(this);
            isOnlyLocal = false;
        } catch (IssueInTransferException e) {
            e.printStackTrace(); //TODO

        } catch (Request.CantConnectException e) {
            e.printStackTrace(); //TODO

        } catch (InvalidJsonException e) {
            e.printStackTrace(); //TODO
        }
    }

    void saveAsEdit(){
        try {
            new Request(PATCH, "/areas/" + areaID + "/drafts/" + postID + "/")
                .addToken(Internal.token())
                .addJson(toJsonSimple())
                .getJson();
            //Nothing to do

        } catch (IssueInTransferException e) {
            e.printStackTrace(); //TODO

        } catch (Request.CantConnectException e) {
            e.printStackTrace(); //TODO
        }
    }

    //endregion
    //region Deleting

    @Override
    public void delete() {
        if(isOnlyLocal) deleteLocal();
        else            deleteServerSide();
    }

    void deleteLocal() {
        Optional<Area> area = Areas.INSTANCE.get(areaID);

        if(area.isPresent())
            area.get().removeCached(this);
        else throw new RuntimeException("It looks like this Draft doesn't belong to any area... " +
                "this should not happen. The area ID used is '" + areaID + "'");
    }

    void deleteServerSide() {
        deleteLocal();

        try {
            new Request(DELETE, "/areas/" + areaID + "/drafts/" + postID)
                .addToken(Internal.token())
                .getRaw();
            //Nothing to do

        } catch (IssueInTransferException e) {
            e.printStackTrace(); //TODO: see T262

        } catch (Request.CantConnectException e) {
            e.printStackTrace(); //TODO: see T262
        }
    }

    //endregion
    //endregion
    //region Updates

    private final static CacheManager manager = new CacheManager().setExpirationTime(1000 * 60 * 60); // 1 hour

    @Override
    public CacheManager cacheManager() {
        return manager;
    }

    /**
     * The cache manager that handles the Drafts.
     *
     * @return The Draft Cache Manager.
     */
    public static CacheManager getCacheManager() {
        return manager;
    }

    @Override
    public void update() {
        //TODO: See T256 about nesting
    }

    //endregion
    //region Setters

    @Override
    public Draft setIsAnonymous(boolean isAnonymous) {
        super.isAnonymous = isAnonymous;

        return this;
    }

    @Override
    public Draft subscribe() {
        super.hasSubscribed = true;

        return this;
    }

    @Override
    public Draft unsubscribe() {
        super.hasSubscribed = false;

        return this;
    }

    @Override
    public Draft setText(String newText) {
        super.text = newText;

        return this;
    }

    //endregion
}
