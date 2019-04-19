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
import net.wildfyre.descriptors.CacheManager;
import net.wildfyre.descriptors.NoSuchEntityException;
import net.wildfyre.http.IssueInTransferException;
import net.wildfyre.http.Request;
import net.wildfyre.utils.InvalidJsonException;

import static net.wildfyre.http.Method.GET;

/**
 * Posts are the main objects in WildFyre.
 *
 * <p>You cannot create a Post directly, as they cannot be modified. To post something, first create a {@link Draft}
 * then use its {@link Draft#publish()} method to convert it to a Post.</p>
 */
public class Post extends PostData {

    //region Constructors

    /**
     * Creates an empty Post object pointing to a Post on the server-side.
     *
     * <p>Note that this method is provided for advanced users. If you want to create a new Post, you should first
     * create a Draft, edit it, then convert it to a Post. More information can be found {@link Draft here}.</p>
     *
     * @param ID the ID of the post
     * @param areaID the ID of the area the post is in
     */
    public Post(long ID, String areaID){ // public because required by Area#post
        super();
        if(ID < 0)
            throw new IllegalArgumentException("The ID of a post cannot be negative: " + ID);
        if(areaID == null || areaID.isEmpty())
            throw new IllegalArgumentException("The ID of the area cannot be 'null' or empty: '" + areaID + "'");

        super.postID = ID;
        super.areaID = areaID;
    }

    Post(PostData p){
        super(p);
    }

    //endregion
    //region Cache manager

    private static CacheManager cacheManager = new CacheManager().setExpirationTime(1000*60*10); // 10 minutes

    @Override
    public CacheManager cacheManager() {
        return cacheManager;
    }

    /**
     * The cache manager used by Posts.
     *
     * @return The cache manager used by Posts.
     */
    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    //endregion
    //region Update

    @Override
    public void update() throws NoSuchEntityException, Request.CantConnectException {
        try {
            JsonObject json = new Request(GET, "/areas/" + areaID + "/" + postID + "/")
                .addToken(Internal.token())
                .getJson()
                .asObject();

            super.update(json);
        } catch (IssueInTransferException e) {
            e.ifDetailsAre("Not found.",  () -> {
                throw new NoSuchEntityException("This object was deleted.", this);
            });

        } catch (InvalidJsonException e) {
            throw new RuntimeException(e); //TODO: Proper exception handling will be done in T262
        }
    }

    //endregion

}
