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

package net.wildfyre.posts;

import net.wildfyre.areas.Area;
import net.wildfyre.areas.Areas;
import net.wildfyre.http.Request;
import net.wildfyre.http.RequestTest;
import net.wildfyre.users.Users;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class PostTest {

    @Before
    public void before() throws Request.CantConnectException {
        RequestTest.Companion.connectToTestDB();
    }

    @Test
    public void getOtherPost() {
        Optional<Post> op = Areas.get("fun").orElseThrow(RuntimeException::new)
            .post(378627832);

        assertTrue(op.isPresent());

        Post p = op.get();
        assertFalse(p.isAnonymous());
        assertTrue(p.hasSubscribed());
        assertTrue(p.isActive());

        Comment c1 = p.commentsList().get(0);
        assertEquals(1, c1.author().getID());

        Comment c2 = p.commentsList().get(1);
        assertEquals(Users.me(), c2.author());
    }

    @Test
    public void myPosts(){
        List<Post> posts = Users.me()
            .asLogged()
            .postsList();

        assertEquals(2, posts.size());

        Post p1 = posts.get(0);
        assertTrue(p1.isAnonymous());
        assertTrue(p1.hasSubscribed());
        assertTrue(p1.isActive());
        assertFalse(p1.author().isPresent());

        Post p2 = posts.get(1);
        assertFalse(p2.isAnonymous());
        assertTrue(p2.hasSubscribed());
        assertTrue(p2.isActive());
        assertTrue(p2.author().isPresent());
        assertEquals(Users.me(), p2.author().get());
    }

    @Test
    public void createDraft(){
        Area a = Areas.get("fun").orElseThrow(RuntimeException::new);

        Draft d = a.draft()
            .setText("This is a test")
            .setIsAnonymous(false)
            .subscribe()
            .save();

        assertNotEquals(-1, d.postID);

        d.delete();
    }

}
